package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.data.db.WorkoutDatabase
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton


@Serializable
data class PreferencesBackup(
    val stringPrefs: Map<String, String> = emptyMap(),
    val intPrefs: Map<String, Int> = emptyMap(),
    val booleanPrefs: Map<String, Boolean> = emptyMap(),
    val floatPrefs: Map<String, Float> = emptyMap(),
    val longPrefs: Map<String, Long> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

// This handles the actual backup/restore operations
class DatabaseBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val database: WorkoutDatabase
) {
    private val MAX_DATASTORE_BACKUP_BYTES = 5L * 1024 * 1024 // 5 MB

    /**
     * Creates a consistent backup *without* closing the Room database.
     */
    suspend fun backupDatabase(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("backup", ".sqlite", context.cacheDir)
        try {
            val db = database.openHelper.writableDatabase // SupportSQLiteDatabase

            // Try VACUUM INTO (best: atomic, consistent snapshot)
            val vacuumOk = try {
                db.execSQL("VACUUM INTO ?", arrayOf(temp.absolutePath))
                true
            } catch (e: Exception) {
                false
            }

            if (!vacuumOk) {
                // Fallback: take a brief write lock, checkpoint WAL, then copy the main file.
                // This minimizes the window and yields a consistent copy of the main DB file.
                db.execSQL("BEGIN IMMEDIATE")
                try {
                    db.query("PRAGMA wal_checkpoint(FULL)").close()
                    // Optional: compact the db file a bit (older devices), not strictly needed:
                    // db.execSQL("VACUUM")
                    val liveFile = context.getDatabasePath(database.openHelper.databaseName)
                    liveFile.inputStream().use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                } finally {
                    db.execSQL("COMMIT")
                }
            }

            // Stream the temp backup file to the chosen Uri
            context.contentResolver.openOutputStream(uri)?.use { out ->
                temp.inputStream().use { it.copyTo(out) }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            temp.delete()
        }
    }

    // --- Helpers ---
    fun File.sizeOrZero(): Long = runCatching { length() }.getOrElse { 0L }

    fun isLikelySqlite(file: File): Boolean {
        if (!file.exists() || file.sizeOrZero() < 100) return false // SQLite header + minimum size
        return runCatching {
            file.inputStream().use { ins ->
                val header = ByteArray(16)
                if (ins.read(header) == 16) {
                    val text = String(header, Charsets.US_ASCII)
                    text.startsWith("SQLite format 3\u0000")
                } else false
            }
        }.getOrDefault(false)
    }

    fun copyUriToFile(uri: Uri, out: File): Result<Unit> = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Failed to open input stream")
    }

    fun openReadonlySqlite(path: String): android.database.sqlite.SQLiteDatabase =
        android.database.sqlite.SQLiteDatabase.openDatabase(
            path,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        )

    fun runIntegrityCheck(db: android.database.sqlite.SQLiteDatabase): Boolean = try {
        db.rawQuery("PRAGMA integrity_check;", null).use { c ->
            if (c.moveToFirst()) c.getString(0).equals("ok", ignoreCase = true) else false
        }
    } catch (_: Exception) { false }

    fun hasRequiredTables(
        db: android.database.sqlite.SQLiteDatabase,
        requiredTables: List<String>
    ): Boolean = try {
        requiredTables.all { table ->
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(table)
            ).use { it.moveToFirst() }
        }
    } catch (_: Exception) { false }

    fun readRoomIdentityHash(db: android.database.sqlite.SQLiteDatabase): String? = try {
        db.rawQuery(
            "SELECT identity_hash FROM room_master_table WHERE id=42 LIMIT 1",
            null
        ).use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) { null }

    fun readCurrentIdentityHashOrNull(): String? = try {
        // Ensure the DB is open before reading.
        val wdb = database.openHelper.writableDatabase
        wdb.query("SELECT identity_hash FROM room_master_table WHERE id=42 LIMIT 1").use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: Exception) { null }

    fun deleteSidecars(dbFile: File) {
        File("${dbFile.absolutePath}-wal").takeIf { it.exists() }?.delete()
        File("${dbFile.absolutePath}-shm").takeIf { it.exists() }?.delete()
    }

    suspend fun restoreDatabase(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val dbName = database.openHelper.databaseName
        val dbFile = context.getDatabasePath(dbName)
        val tempFile = File.createTempFile("restore-", ".sqlite", context.cacheDir)
        val backupOfCurrent = File(dbFile.parentFile, "${dbFile.name}.pre-restore.bak")

        val requiredTables = listOf(
            "room_master_table",
        )

        // Capture identity hash of the *current* DB (if any), before we close it.
        val currentIdentityHash = readCurrentIdentityHashOrNull()

        try {
            // 1) Copy to TEMP and do quick checks
            copyUriToFile(uri, tempFile).getOrThrow()

            if (!isLikelySqlite(tempFile)) {
                return@withContext Result.failure(IllegalArgumentException("Selected file is not a valid SQLite database"))
            }

            // 2) Open TEMP read-only and validate
            val roDb = runCatching { openReadonlySqlite(tempFile.absolutePath) }.getOrElse { e ->
                return@withContext Result.failure(IllegalArgumentException("Cannot open selected file as a database", e))
            }

            roDb.use { candidate ->
                // a) integrity check
                if (!runIntegrityCheck(candidate)) {
                    return@withContext Result.failure(IllegalStateException("Database integrity check failed"))
                }
                // b) schema presence
                if (requiredTables.isNotEmpty() && !hasRequiredTables(candidate, requiredTables)) {
                    return@withContext Result.failure(IllegalStateException("Database does not contain required tables"))
                }
                // c) Room identity hash check (optional but recommended)
                val restoredIdentity = readRoomIdentityHash(candidate)
                if (restoredIdentity == null) {
                    return@withContext Result.failure(IllegalStateException("Not a Room database (missing identity hash)"))
                }
                // Do not restore if hashes don't match  // TODO: allow and add migration
                if (currentIdentityHash != null && currentIdentityHash != restoredIdentity) {
                    return@withContext Result.failure(IllegalStateException("Incompatible database version (identity hash mismatch)"))
                }
            }

            // 3) Close current Room/SQLite and prepare replacement
            database.close()

            // Remove WAL/SHM of the target before replacing
            deleteSidecars(dbFile)

            // 4) Create a backup of the current DB (for rollback)
            if (dbFile.exists()) {
                // Best effort backup (overwrite old .bak)
                runCatching {
                    dbFile.copyTo(backupOfCurrent, overwrite = true)
                }
            }

            // 5) Replace DB atomically (best effort)
            runCatching {
                // Replace file contents
                tempFile.inputStream().use { ins ->
                    dbFile.outputStream().use { outs ->
                        ins.copyTo(outs)
                    }
                }
            }.onFailure { e ->
                // Rollback on replace failure
                if (backupOfCurrent.exists()) {
                    runCatching { backupOfCurrent.copyTo(dbFile, overwrite = true) }
                }
                return@withContext Result.failure(IOException("Failed to replace database", e))
            }

            // 6) Clean sidecars again (in case source had WAL mode)
            deleteSidecars(dbFile)

            // 7) Try opening the restored DB to ensure it’s usable
            val sanityOpen = runCatching {
                database.openHelper.writableDatabase // Reopens via Room
            }
            if (sanityOpen.isFailure) {
                // Rollback to previous DB
                if (backupOfCurrent.exists()) {
                    runCatching {
                        database.close()
                        deleteSidecars(dbFile)
                        backupOfCurrent.copyTo(dbFile, overwrite = true)
                        deleteSidecars(dbFile)
                        database.openHelper.writableDatabase
                    }
                }
                return@withContext Result.failure(IllegalStateException("Restored database could not be opened; rolled back", sanityOpen.exceptionOrNull()))
            }

            // 8) Clear prefs referencing stale IDs
            runCatching { dataStore.edit { it.remove(PrefKeys.currentPlan) } }
            runCatching { dataStore.edit { it.remove(PrefKeys.currentWorkout) } }

            // 9) Clean up temp
            runCatching { tempFile.delete() }

            // 10) Restart to reopen and use the db
             restartApp()

            Result.success(Unit)
        } catch (e: Exception) {
            // Cleanup temp and return failure
            runCatching { tempFile.delete() }
            Result.failure(e)
        }
    }

    // restart app when replacing db // TODO: delay app restart to notify user of successful restore
    private fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(it)
            Runtime.getRuntime().exit(0)
        }
    }

    suspend fun backupDataStore(backupPath: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val currentPrefs = dataStore.data.first()

                val stringPrefs = mutableMapOf<String, String>()
                val intPrefs = mutableMapOf<String, Int>()
                val booleanPrefs = mutableMapOf<String, Boolean>()
                val floatPrefs = mutableMapOf<String, Float>()
                val longPrefs = mutableMapOf<String, Long>()

                currentPrefs.asMap().forEach { (key, value) ->
                    when (value) {
                        is String -> stringPrefs[key.name] = value
                        is Int -> intPrefs[key.name] = value
                        is Boolean -> booleanPrefs[key.name] = value
                        is Float -> floatPrefs[key.name] = value
                        is Long -> longPrefs[key.name] = value
                    }
                }

                val backup = PreferencesBackup(
                    stringPrefs = stringPrefs,
                    intPrefs = intPrefs,
                    booleanPrefs = booleanPrefs,
                    floatPrefs = floatPrefs,
                    longPrefs = longPrefs
                )
                val json = Json.encodeToString(backup)
                // Write to URI
                context.contentResolver.openOutputStream(backupPath)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun restoreByReplacing(uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // 1) Quick sanity check on file size (if available)
                val size = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx != -1 && cursor.moveToFirst()) cursor.getLong(idx) else null
                }

                if (size != null && size > MAX_DATASTORE_BACKUP_BYTES) {
                    return@withContext Result.failure(IllegalArgumentException("Backup file is too large"))
                }

                // 2) Open stream and decode safely (streaming + tolerant JSON)
                val backup: PreferencesBackup = context.contentResolver.openInputStream(uri)?.use { raw ->
                    val input = BufferedInputStream(raw)
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }.decodeFromStream(input)
                } ?: return@withContext Result.failure(IllegalArgumentException("Unable to read the selected file"))

                // 3) Minimal validation to avoid random JSON being applied
                val hasAnyData =
                    backup.stringPrefs.isNotEmpty() ||
                            backup.intPrefs.isNotEmpty() ||
                            backup.booleanPrefs.isNotEmpty() ||
                            backup.floatPrefs.isNotEmpty() ||
                            backup.longPrefs.isNotEmpty()

                if (!hasAnyData) {
                    return@withContext Result.failure(IllegalArgumentException("Selected file doesn't look like a preferences backup"))
                }

                // (Optional but helpful) ensure keys aren't blank – avoids weird keys
                fun Map<String, *>.hasBlankKeys() = keys.any { it.isBlank() }
                if (backup.stringPrefs.hasBlankKeys() ||
                    backup.intPrefs.hasBlankKeys() ||
                    backup.booleanPrefs.hasBlankKeys() ||
                    backup.floatPrefs.hasBlankKeys() ||
                    backup.longPrefs.hasBlankKeys()
                ) {
                    return@withContext Result.failure(IllegalArgumentException("Backup contains invalid keys"))
                }

                // 4) Apply atomically – if anything throws inside, nothing is committed
                dataStore.edit { preferences ->
                    preferences.clear()

                    backup.stringPrefs.forEach { (keyName, value) ->
                        preferences[stringPreferencesKey(keyName)] = value
                    }
                    backup.intPrefs.forEach { (keyName, value) ->
                        preferences[intPreferencesKey(keyName)] = value
                    }
                    backup.booleanPrefs.forEach { (keyName, value) ->
                        preferences[booleanPreferencesKey(keyName)] = value
                    }
                    backup.floatPrefs.forEach { (keyName, value) ->
                        preferences[floatPreferencesKey(keyName)] = value
                    }
                    backup.longPrefs.forEach { (keyName, value) ->
                        preferences[longPreferencesKey(keyName)] = value
                    }
                }

                Result.success(Unit)
            } catch (e: SecurityException) {
                Result.failure(IllegalStateException("No permission to read the selected file", e))
            } catch (e: SerializationException) {
                Result.failure(IllegalArgumentException("Selected file isn't a valid backup", e))
            } catch (e: IOException) {
                Result.failure(IOException("Failed to read backup file", e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }


    suspend fun validateBackupFile(uri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                    val fileName = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                    val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L

                    val backupInfo = BackupInfo(
                        fileName = fileName,
                        filePath = uri.toString(),
                        size = size,
                        createdDate = ZonedDateTime.now()
                    )

                    Result.success(backupInfo)
                } else {
                    Result.failure(Exception("Could not read file information"))
                }
            } ?: Result.failure(Exception("Could not access file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class BackupInfo(
    val fileName: String,
    val filePath: String,
    val size: Long,
    val createdDate: ZonedDateTime
)

@Singleton
class BackupRepository @Inject constructor(
    private val backupManager: DatabaseBackupManager
) {
    suspend fun backupDb(path: Uri): Result<Unit> {
        return backupManager.backupDatabase(path)
    }

    suspend fun restoreDb(backupPath: Uri): Result<Unit> {
        return backupManager.restoreDatabase(backupPath)
    }

    suspend fun backupPreferences(backupPath: Uri): Result<Unit> {
        return backupManager.backupDataStore(backupPath)
    }

    suspend fun restorePreferences(backupPath: Uri): Result<Unit> {
        return backupManager.restoreByReplacing(backupPath)
    }

    suspend fun validateBackup(uri: Uri): Result<BackupInfo> {
        return backupManager.validateBackupFile(uri)
    }
}
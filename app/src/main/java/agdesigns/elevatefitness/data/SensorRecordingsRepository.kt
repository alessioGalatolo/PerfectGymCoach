package agdesigns.elevatefitness.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Raw watch sensor recordings (developer option), one per completed set, received from the watch
 * by SensorRecordingListenerService. See the wear SensorRecorder for the format.
 */
@Singleton
class SensorRecordingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dir = recordingsDir(context)

    private val _count = MutableStateFlow(countRecordings())
    val count: StateFlow<Int> = _count.asStateFlow()

    fun refreshCount() {
        _count.value = countRecordings()
    }

    suspend fun exportZip(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val output = context.contentResolver.openOutputStream(uri)
                ?: error("Cannot open $uri")
            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("README.txt"))
                zip.write(README.toByteArray())
                zip.closeEntry()
                completeFiles().forEach { file ->
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dir.deleteRecursively()
        refreshCount()
    }

    // a recording is complete once its metadata is written, which happens last
    private fun completeFiles(): List<File> {
        val ids = dir.listFiles { file -> file.name.endsWith(METADATA_SUFFIX) }
            ?.map { it.name.removeSuffix(METADATA_SUFFIX) }
            ?: return emptyList()
        return ids.flatMap { listOf(File(dir, "$it$SAMPLES_SUFFIX"), File(dir, "$it$METADATA_SUFFIX")) }
            .filter { it.exists() }
    }

    private fun countRecordings(): Int =
        dir.listFiles { file -> file.name.endsWith(METADATA_SUFFIX) }?.size ?: 0

    companion object {
        private const val METADATA_SUFFIX = ".json"
        private const val SAMPLES_SUFFIX = ".csv.gz"

        private fun recordingsDir(context: Context) = File(context.filesDir, "sensor_recordings")

        /** Called from the data layer listener, possibly while the app is not running. */
        fun save(context: Context, id: String, metadata: String, samples: InputStream) {
            val dir = recordingsDir(context).also { it.mkdirs() }
            File(dir, "$id$SAMPLES_SUFFIX").outputStream().use { samples.copyTo(it) }
            File(dir, "$id$METADATA_SUFFIX").writeText(metadata)
        }

        private val README = """
            Raw watch sensor recordings, one per completed set.

            Each set is a pair of files sharing the same id:
            - <id>.csv.gz: gzipped CSV with header sensor,timestamp_ns,accuracy,v0,...,v5
              One row per Android SensorEvent, unprocessed. timestamp_ns is SensorEvent.timestamp
              (same clock as SystemClock.elapsedRealtimeNanos on the watch). Each sensor reports
              as many values as Android defines for it (e.g. 3 for accelerometer, 5 for
              rotation vectors, 1 for pressure); missing trailing columns are omitted.
            - <id>.json: metadata
              - set: ground truth entered by the user (reps, weight, tare...), exercise info and
                the output of the current on-watch detector (tracking_result, if it was enabled)
              - start/end_elapsed_ns and start/end_wall_ms: segment bounds on both clocks, use
                them to convert sensor timestamps to wall clock time
              - markers: timestamped UI events and other annotations (elapsed_ns and wall_ms)
              - sensors: hardware details of every recorded sensor
              - sample_counts, sampling_period_us, device, format_version

            Recording is continuous during a workout: a segment starts when the previous set is
            completed (or when recording starts) and ends when its set is completed, so it
            includes the rest before the set.
        """.trimIndent()
    }
}

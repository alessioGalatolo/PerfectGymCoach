package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Sex
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.utils.getLocalizedString
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    /*
     * DATA STORE (SETTINGS)
     */
    fun getCurrentPlan(): Flow<Long?> = dataStore.data.map{ it[PrefKeys.currentPlan] }

    suspend fun setCurrentPlan(planId: Long, overrideValue: Boolean){
        dataStore.edit {
            if (it[PrefKeys.currentPlan] == null || overrideValue){
                it[PrefKeys.currentPlan] = planId
            }
        }

    }


    fun getUserWeight(): Flow<Float> = dataStore.data.map{
        it[PrefKeys.userWeight] ?: 60f
    }

    suspend fun setUserWeight(newWeight: Float) = dataStore.edit {
        it[PrefKeys.userWeight] = newWeight
    }


    fun getUserHeight(): Flow<Float> = dataStore.data.map{
        it[PrefKeys.userHeight] ?: 170f
    }

    suspend fun setUserHeight(newHeight: Float) = dataStore.edit {
        it[PrefKeys.userHeight] = newHeight
    }


    fun getUserYear(): Flow<Int> = dataStore.data.map{
        it[PrefKeys.userAgeYear] ?: 2000
    }

    suspend fun setUserYear(newYear: Int) = dataStore.edit {
        it[PrefKeys.userAgeYear] = newYear
    }


    fun getUserSex(): Flow<Sex> = dataStore.data.map{
        Sex.fromResKey(it[PrefKeys.userSex])
    }

    suspend fun setUserSex(newSex: Sex) = dataStore.edit {
        it[PrefKeys.userSex] = newSex.nameResKey
    }

    fun getTheme(): Flow<Theme> = dataStore.data.map{
        Theme.fromResKey(it[PrefKeys.theme])
    }

    suspend fun setTheme(newTheme: Theme) = dataStore.edit {
        it[PrefKeys.theme] = newTheme.nameResKey
    }


    fun getUserName(): Flow<String> = dataStore.data.map{
        it[PrefKeys.userName] ?: ""
    }

    suspend fun setUserName(newName: String) = dataStore.edit {
        it[PrefKeys.userName] = newName
    }


    fun getDontWantNotificationAccess(): Flow<Boolean> = dataStore.data.map{
        it[PrefKeys.dontWantNotificationAccess] ?: false
    }

    suspend fun setDontWantNotificationAccess(newValue: Boolean) = dataStore.edit {
        it[PrefKeys.dontWantNotificationAccess] = newValue
    }



    fun getDontWantOngoingWorkoutNotification(): Flow<Boolean> = dataStore.data.map{
        it[PrefKeys.dontWantOngoingWorkoutNotification] ?: false
    }

    suspend fun setDontWantOngoingWorkoutNotification(newValue: Boolean) = dataStore.edit {
        it[PrefKeys.dontWantOngoingWorkoutNotification] = newValue
    }


    fun getImperialSystem(): Flow<Boolean> = dataStore.data.map{
        it[PrefKeys.imperialSystem] ?: false
    }

    suspend fun setImperialSystem(newValue: Boolean) = dataStore.edit {
        it[PrefKeys.imperialSystem] = newValue
    }


    fun getBodyweightIncrement(): Flow<Float> = dataStore.data.map{
        it[PrefKeys.incrementBodyweight] ?: 2.5f
    }

    suspend fun setBodyweightIncrement(newValue: Float) = dataStore.edit {
        it[PrefKeys.incrementBodyweight] = newValue
    }


    fun getBarbellIncrement(): Flow<Float> = dataStore.data.map{
        it[PrefKeys.incrementBarbell] ?: 2.5f
    }

    suspend fun setBarbellIncrement(newValue: Float) = dataStore.edit {
        it[PrefKeys.incrementBarbell] = newValue
    }



    fun getDumbbellIncrement(): Flow<Float> = dataStore.data.map{ it[PrefKeys.incrementDumbbell] ?: 2f }

    suspend fun setDumbbellIncrement(newValue: Float) = dataStore.edit { it[PrefKeys.incrementDumbbell] = newValue }

    fun getMachineIncrement(): Flow<Float> = dataStore.data.map{ it[PrefKeys.incrementMachine] ?: 5f }

    suspend fun setMachineIncrement(newValue: Float) = dataStore.edit { it[PrefKeys.incrementMachine] = newValue }

    fun getCableIncrement(): Flow<Float> = dataStore.data.map{ it[PrefKeys.incrementCable] ?: 2.5f }

    suspend fun setCableIncrement(newValue: Float) = dataStore.edit { it[PrefKeys.incrementCable] = newValue }


    fun getCurrentWorkout(): Flow<Long?> = dataStore.data.map{ it[PrefKeys.currentWorkout] }

    suspend fun setCurrentWorkout(newValue: Long?) = dataStore.edit {
        if (newValue == null)
            it.remove(PrefKeys.currentWorkout)
        else
            it[PrefKeys.currentWorkout] = newValue
    }



    fun getLanguage(): Flow<String?> = dataStore.data.map{ it[PrefKeys.language] }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun setLanguage(newValue: String) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager.applicationLocales = LocaleList.forLanguageTags(newValue)
        dataStore.edit { it[PrefKeys.language] = newValue }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun resetLanguage() {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
        dataStore.edit { it.remove(PrefKeys.language) }
    }

    fun getLockHorizontalScroll(): Flow<Boolean> = dataStore.data.map{
        it[PrefKeys.lockHorizontalScroll] ?: false
    }

    suspend fun setLockHorizontalScroll(newValue: Boolean) = dataStore.edit {
        it[PrefKeys.lockHorizontalScroll] = newValue
    }

    fun getAutoOpenWear(): Flow<Boolean> = dataStore.data.map{
        it[PrefKeys.autoOpenWear] ?: true
    }

    suspend fun setAutoOpenWear(newValue: Boolean) = dataStore.edit {
        it[PrefKeys.autoOpenWear] = newValue
    }

    fun isDismissedPlanChangeReminder(planId: Long): Flow<Boolean> = dataStore.data.map{
        val dismissedPlans = it[PrefKeys.dismissedPlanChangeReminders] ?: ""
        dismissedPlans.split(",").contains(planId.toString())
    }

    suspend fun dismissPlanChangeReminder(planId: Long) = dataStore.edit {
        val current = it[PrefKeys.dismissedPlanChangeReminders] ?: ""
        val dismissedPlans = current.split(",").filter { it.isNotBlank() }.toMutableSet()
        dismissedPlans.add(planId.toString())
        it[PrefKeys.dismissedPlanChangeReminders] = dismissedPlans.joinToString(",")
    }
}

internal object PrefKeys {
    val dataStoreVersionKey = intPreferencesKey("Data store version")
    val currentPlan = longPreferencesKey("Current plan")
    val currentWorkout = longPreferencesKey("Current workout")
    val userWeight = floatPreferencesKey("User weight")
    val userHeight = floatPreferencesKey("User height")
    val userSex = stringPreferencesKey("User sex")
    val theme = stringPreferencesKey("Theme")
    val userName = stringPreferencesKey("User name")
    val userAgeYear = intPreferencesKey("User age year")
    val imperialSystem = booleanPreferencesKey("Imperial system user")
    val dontWantNotificationAccess = booleanPreferencesKey("Don't want notification access")
    val dontWantOngoingWorkoutNotification = booleanPreferencesKey("Don't want ongoing workout notification")
    val incrementBodyweight = floatPreferencesKey("Increment body weight")
    val incrementBarbell = floatPreferencesKey("Increment barbell")
    val incrementDumbbell = floatPreferencesKey("Increment dumbbell")
    val incrementMachine = floatPreferencesKey("Increment machine")
    val incrementCable = floatPreferencesKey("Increment cable")
    val language = stringPreferencesKey("Language")
    val lockHorizontalScroll = booleanPreferencesKey("Lock horizontal scroll")
    val autoOpenWear = booleanPreferencesKey("Auto open wear")
    val dismissedPlanChangeReminders = stringPreferencesKey("Dismissed plan change reminders")
}


// v0 -> v1 datastore migration
class V1PrefsMigration(
    private val context: Context
) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[PrefKeys.dataStoreVersionKey] ?: 0
        return version < 1
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val out = currentData.toMutablePreferences()

        Log.d("V1PrefsMigration", "Migrating")
        // Map legacy userSex strings (from resources) to canonical enum names
        val currentUserSex = currentData[PrefKeys.userSex].orEmpty()
        Log.d("V1PrefsMigration", "currentUserSex=$currentUserSex")
        val sexEnumName = when (currentUserSex) {
            context.getLocalizedString(R.string.sexes_other, Locale.ENGLISH) -> Sex.OTHER.nameResKey
            context.getLocalizedString(R.string.sexes_male, Locale.ENGLISH) -> Sex.MALE.nameResKey
            context.getLocalizedString(R.string.sexes_female, Locale.ENGLISH) -> Sex.FEMALE.nameResKey
            else -> Sex.OTHER.nameResKey
        }
        Log.d("V1PrefsMigration", "sexEnumName=$sexEnumName")
        out[PrefKeys.userSex] = sexEnumName

        // Map legacy theme strings (from resources) to canonical enum names
        val currentTheme = currentData[PrefKeys.theme].orEmpty()
        Log.d("V1PrefsMigration", "currentTheme=$currentTheme")
        val themeEnumName = when (currentTheme) {
            context.getLocalizedString(R.string.themes_system, Locale.ENGLISH) -> Theme.SYSTEM.nameResKey
            context.getLocalizedString(R.string.themes_dark, Locale.ENGLISH) -> Theme.DARK.nameResKey
            context.getLocalizedString(R.string.themes_light, Locale.ENGLISH) -> Theme.LIGHT.nameResKey
            else -> Theme.SYSTEM.nameResKey
        }
        Log.d("V1PrefsMigration", "themeEnumName=$themeEnumName")
        out[PrefKeys.theme] = themeEnumName

        out[PrefKeys.dataStoreVersionKey] = 1
        return out
    }

    override suspend fun cleanUp() { /* no-op */ }
}

// move from context.dataStore to PreferenceDataStoreFactory
class V2PrefsMigration(
    private val context: Context
) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[PrefKeys.dataStoreVersionKey] ?: 0
        return version < 2
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val out = currentData.toMutablePreferences()

        val old = context.dataStore.data.first()
        if (old[PrefKeys.currentPlan] != null) {
            out[PrefKeys.currentPlan] = old[PrefKeys.currentPlan]!!
        }
        if (old[PrefKeys.currentWorkout] != null) {
            out[PrefKeys.currentWorkout] = old[PrefKeys.currentWorkout]!!
        }
        out[PrefKeys.userWeight] = old[PrefKeys.userWeight] ?: 60f
        out[PrefKeys.userHeight] = old[PrefKeys.userHeight] ?: 170f
        out[PrefKeys.userSex] = old[PrefKeys.userSex] ?: Sex.OTHER.nameResKey
        out[PrefKeys.theme] = old[PrefKeys.theme] ?: Theme.SYSTEM.nameResKey
        out[PrefKeys.userName] = old[PrefKeys.userName] ?: ""
        out[PrefKeys.userAgeYear] = old[PrefKeys.userAgeYear] ?: 2000
        out[PrefKeys.imperialSystem] = old[PrefKeys.imperialSystem] ?: false
        out[PrefKeys.dontWantNotificationAccess] = old[PrefKeys.dontWantNotificationAccess] ?: false
        out[PrefKeys.incrementBodyweight] = old[PrefKeys.incrementBodyweight] ?: 2.5f
        out[PrefKeys.incrementBarbell] = old[PrefKeys.incrementBarbell] ?: 2.5f
        out[PrefKeys.incrementDumbbell] = old[PrefKeys.incrementDumbbell] ?: 2f
        out[PrefKeys.incrementMachine] = old[PrefKeys.incrementMachine] ?: 5f
        out[PrefKeys.incrementCable] = old[PrefKeys.incrementCable] ?: 2.5f

        out[PrefKeys.dataStoreVersionKey] = 2
        return out
    }

    override suspend fun cleanUp() { /* no-op */ }
}
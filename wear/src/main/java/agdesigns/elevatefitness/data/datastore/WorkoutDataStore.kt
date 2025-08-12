package agdesigns.elevatefitness.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WorkoutDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = WALKING_WORKOUTS_DATASTORE_NAME)

    val activeWorkoutFlow: Flow<Boolean> = context.dataStore.data.map {
        it[ACTIVE_WORKOUT_KEY] == true
    }

    suspend fun setActiveWorkout(activeWalkingWorkout: Boolean) {
        context.dataStore.edit {
            it[ACTIVE_WORKOUT_KEY] = activeWalkingWorkout
        }
    }

    companion object {
        private const val WALKING_WORKOUTS_DATASTORE_NAME = "workouts_datastore"

        private val ACTIVE_WORKOUT_KEY =
            booleanPreferencesKey("active_workout")
    }
}
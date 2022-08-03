package com.anexus.perfectgymcoach.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.res.TypedArrayUtils.getString
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class Repository @Inject constructor(
    private val db: WorkoutDatabase,
    private val context: Context
) {
    private val CURRENT_PLAN = longPreferencesKey("Current plan")

    fun getPlans() = db.workoutPlanDao.getPlans()

    fun getPrograms(planId: Long): Flow<Map<WorkoutProgram, List<WorkoutExercise>>> = db.workoutProgramDao.getPrograms(planId)

    fun getWorkoutExercises(programId: Long) = db.workoutExerciseDao.getExercises(programId)

    fun getExercises(muscle: Exercise.Muscle): Flow<List<Exercise>> {
        return if (muscle == Exercise.Muscle.EVERYTHING) {
            db.exerciseDao.getAllExercises()
        } else {
            db.exerciseDao.getExercises(muscle)
        }
    }

    fun getCurrentPlan(): Flow<Long?> = context.dataStore.data.map{ it[CURRENT_PLAN] }

    suspend fun setCurrentPlan(planId: Long, overrideValue: Boolean){
        context.dataStore.edit {
            if (it[CURRENT_PLAN] == null || overrideValue){
                it[CURRENT_PLAN] = planId
            }
        }
    }

    suspend fun addPlan(plan: WorkoutPlan) = db.workoutPlanDao.insert(plan)

    suspend fun addProgram(program: WorkoutProgram) = db.workoutProgramDao.insert(program)

    suspend fun addWorkoutExercise(exercise: WorkoutExercise) = db.workoutExerciseDao.insert(exercise)

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: Repository? = null

        fun getInstance(workoutDatabase: WorkoutDatabase, context: Context) =
            instance ?: synchronized(this) {
                instance ?: Repository(workoutDatabase, context).also { instance = it }
            }
    }
}
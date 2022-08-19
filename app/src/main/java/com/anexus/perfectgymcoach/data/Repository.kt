package com.anexus.perfectgymcoach.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecordFinish
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
    private val currentPlan = longPreferencesKey("Current plan")
    private val currentProgram = longPreferencesKey("Current program")
    private val currentWorkout = longPreferencesKey("Current workout") // TODO

    fun getPlans() = db.workoutPlanDao.getPlans()

    fun getPrograms(planId: Long): Flow<Map<WorkoutProgram, List<WorkoutExercise>>> = db.workoutProgramDao.getPrograms(planId)

    fun getWorkoutExercises(programId: Long) = db.workoutExerciseDao.getExercises(programId)

    fun getExerciseRecords(exerciseId: Long) = db.exerciseRecordDao.getRecords(exerciseId)

    fun getWorkoutHistory() = db.workoutRecordDao.getRecords()

    fun getExercises(muscle: Exercise.Muscle): Flow<List<Exercise>> {
        return if (muscle == Exercise.Muscle.EVERYTHING) {
            db.exerciseDao.getAllExercises()
        } else {
            db.exerciseDao.getExercises(muscle)
        }
    }

    fun getCurrentPlan(): Flow<Long?> = context.dataStore.data.map{ it[currentPlan] }

    suspend fun setCurrentPlan(planId: Long, overrideValue: Boolean){
        context.dataStore.edit {
            if (it[currentPlan] == null || overrideValue){
                it[currentPlan] = planId
            }
        }
    }

    fun getCurrentProgram(): Flow<Long?> = context.dataStore.data.map{ it[currentProgram] }

    suspend fun setCurrentProgram(programId: Long, overrideValue: Boolean){
        context.dataStore.edit {
            if (it[currentProgram] == null || overrideValue){
                it[currentProgram] = programId
            }
        }
    }

    suspend fun addPlan(plan: WorkoutPlan) = db.workoutPlanDao.insert(plan)

    suspend fun addProgram(program: WorkoutProgram) = db.workoutProgramDao.insert(program)

    suspend fun addWorkoutExercise(exercise: WorkoutExercise) = db.workoutExerciseDao.insert(exercise)

    suspend fun addWorkoutRecord(workoutRecord: WorkoutRecord) = db.workoutRecordDao.insert(workoutRecord)

    suspend fun completeWorkoutRecord(workoutRecordFinish: WorkoutRecordFinish) = db.workoutRecordDao.updateFinish(workoutRecordFinish)

    suspend fun addExerciseRecord(exerciseRecord: ExerciseRecord) = db.exerciseRecordDao.insert(exerciseRecord)

    fun renameProgram(program: WorkoutProgram) = db.workoutProgramDao.update(program)

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: Repository? = null

        fun getInstance(workoutDatabase: WorkoutDatabase, context: Context) =
            instance ?: synchronized(this) {
                instance ?: Repository(workoutDatabase, context).also { instance = it }
            }
    }
}
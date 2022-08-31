package com.anexus.perfectgymcoach.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.ExerciseRecord
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanUpdateProgram
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramRename
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramReorder
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
    private val currentWorkout = longPreferencesKey("Current workout") // TODO ?
    private val userWeight = floatPreferencesKey("User weight")


    fun getPlans() = db.workoutPlanDao.getPlans()

    fun getPlan(planId: Long) = db.workoutPlanDao.getPlan(planId)

    fun getPlanMapPrograms(): Flow<Map<WorkoutPlan, List<WorkoutProgram>>> =
        db.workoutPlanDao.getPlanMapPrograms()

    suspend fun addPlan(plan: WorkoutPlan) = db.workoutPlanDao.insert(plan)

    suspend fun updateCurrentPlan(workoutPlanUpdateProgram: WorkoutPlanUpdateProgram) =
        db.workoutPlanDao.updateCurrentProgram(workoutPlanUpdateProgram)


    fun getProgramMapExercises(planId: Long): Flow<Map<WorkoutProgram, List<WorkoutExercise>>> =
        db.workoutProgramDao.getProgramMapExercises(planId)

    fun getPrograms(planId: Long) = db.workoutProgramDao.getPrograms(planId)

    suspend fun addProgram(program: WorkoutProgram) = db.workoutProgramDao.insert(program)

    suspend fun renameProgram(workoutProgramRename: WorkoutProgramRename) = db.workoutProgramDao.updateName(workoutProgramRename)

    suspend fun reorderPrograms(workoutProgramReorder: List<WorkoutProgramReorder>) = db.workoutProgramDao.updateOrder(workoutProgramReorder)


    fun getWorkoutExercisesAndInfo(programId: Long): Flow<List<WorkoutExerciseAndInfo>> = db.workoutExerciseDao.getExercisesAndInfo(programId)

    fun getWorkoutExercisesAndInfo(programIds: List<Long>): Flow<List<WorkoutExerciseAndInfo>> = db.workoutExerciseDao.getExercisesAndInfo(programIds)

    fun getWorkoutExercises(programId: Long) = db.workoutExerciseDao.getExercises(programId)

    suspend fun addWorkoutExercise(exercise: WorkoutExercise) = db.workoutExerciseDao.insert(exercise)


    fun getExerciseRecords(exerciseId: Long) = db.exerciseRecordDao.getRecords(exerciseId)

    fun getWorkoutExerciseRecords(workoutId: Long) = db.exerciseRecordDao.getRecordsByWorkout(workoutId)

    fun getWorkoutExerciseRecordsAndInfo(workoutId: Long) = db.exerciseRecordDao.getRecordsAndInfoByWorkout(workoutId)

    fun getExerciseRecords(exerciseIds: List<Long>) = db.exerciseRecordDao.getRecords(exerciseIds)

    suspend fun addExerciseRecord(exerciseRecord: ExerciseRecord) = db.exerciseRecordDao.insert(exerciseRecord)


    fun getWorkoutRecord(workoutId: Long) = db.workoutRecordDao.getRecord(workoutId)

    fun getWorkoutHistory() = db.workoutRecordDao.getRecords()

    fun getWorkoutHistoryAndName() = db.workoutRecordDao.getRecordsAndName()

    suspend fun addWorkoutRecord(workoutRecord: WorkoutRecord) = db.workoutRecordDao.insert(workoutRecord)

    suspend fun completeWorkoutRecord(workoutRecordFinish: WorkoutRecordFinish) = db.workoutRecordDao.updateFinish(workoutRecordFinish)


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


    // TODO: move default value outside (60 kg)
    fun getUserWeight(): Flow<Float> = context.dataStore.data.map{ it[userWeight] ?: 60f }

    suspend fun setUserWeight(newWeight: Float) = context.dataStore.edit { it[userWeight] = newWeight }


    companion object {

        // For Singleton instantiation
        @Volatile private var instance: Repository? = null

        fun getInstance(workoutDatabase: WorkoutDatabase, context: Context) =
            instance ?: synchronized(this) {
                instance ?: Repository(workoutDatabase, context).also { instance = it }
            }
    }
}
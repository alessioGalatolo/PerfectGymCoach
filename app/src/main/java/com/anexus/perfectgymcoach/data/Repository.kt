package com.anexus.perfectgymcoach.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.anexus.perfectgymcoach.data.exercise.*
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
    private val currentWorkout = longPreferencesKey("Current workout")
    private val userWeight = floatPreferencesKey("User weight")
    private val userHeight = floatPreferencesKey("User height")
    private val userSex = stringPreferencesKey("User sex")
    private val userName = stringPreferencesKey("User name")
    private val userAgeYear = intPreferencesKey("User age year")


    fun getPlans() = db.workoutPlanDao.getPlans()

    fun getPlan(planId: Long) = db.workoutPlanDao.getPlan(planId)

    fun getPlanMapPrograms(): Flow<Map<WorkoutPlan, List<WorkoutProgram>>> =
        db.workoutPlanDao.getPlanMapPrograms()

    suspend fun addPlan(plan: WorkoutPlan) = db.workoutPlanDao.insert(plan)

    suspend fun updateCurrentPlan(workoutPlanUpdateProgram: WorkoutPlanUpdateProgram) =
        db.workoutPlanDao.updateCurrentProgram(workoutPlanUpdateProgram)


    fun getProgramsMapExercises(planId: Long): Flow<Map<WorkoutProgram, List<WorkoutExercise>>> =
        db.workoutProgramDao.getProgramsMapExercises(planId)

    fun getProgramMapExercises(programId: Long): Flow<Map<WorkoutProgram, List<WorkoutExercise>>> =
        db.workoutProgramDao.getProgramMapExercises(programId)

    fun getPrograms(planId: Long) = db.workoutProgramDao.getPrograms(planId)

    suspend fun addProgram(program: WorkoutProgram) = db.workoutProgramDao.insert(program)

    suspend fun renameProgram(workoutProgramRename: WorkoutProgramRename) =
        db.workoutProgramDao.updateName(workoutProgramRename)

    suspend fun reorderPrograms(workoutProgramReorder: List<WorkoutProgramReorder>) =
        db.workoutProgramDao.updateOrder(workoutProgramReorder)

    suspend fun deleteProgram(programId: Long) = db.workoutProgramDao.delete(programId)


    fun getWorkoutExercisesAndInfo(programId: Long): Flow<List<WorkoutExerciseAndInfo>> = db.workoutExerciseDao.getExercisesAndInfo(programId)

    fun getWorkoutExercisesAndInfo(programIds: List<Long>): Flow<List<WorkoutExerciseAndInfo>> = db.workoutExerciseDao.getExercisesAndInfo(programIds)

    fun getWorkoutExercises(programId: Long) = db.workoutExerciseDao.getExercises(programId)

    fun getWorkoutExercise(workoutExerciseId: Long) = db.workoutExerciseDao.getWorkoutExercise(workoutExerciseId)

    suspend fun addWorkoutExercise(exercise: WorkoutExercise) = db.workoutExerciseDao.insert(exercise)

    suspend fun reorderWorkoutExercises(workoutExerciseReorders: List<WorkoutExerciseReorder>) =
        db.workoutExerciseDao.updateOrder(workoutExerciseReorders)

    suspend fun deleteWorkoutExercise(workoutExerciseId: Long) = db.workoutExerciseDao.delete(workoutExerciseId)

    suspend fun updateExerciseSuperset(updateExerciseSupersets: List<UpdateExerciseSuperset>) =
        db.workoutExerciseDao.updateSuperset(updateExerciseSupersets)


    fun getExerciseRecords(exerciseId: Long) = db.exerciseRecordDao.getRecords(exerciseId)

    fun getWorkoutExerciseRecords(workoutId: Long) = db.exerciseRecordDao.getByWorkout(workoutId)

    suspend fun deleteWorkoutExerciseRecords(workoutId: Long) = db.exerciseRecordDao.deleteByWorkout(workoutId)

    fun getWorkoutExerciseRecordsAndInfo(workoutId: Long) = db.exerciseRecordDao.getByWorkoutWithInfo(workoutId)

    fun getExerciseRecords(exerciseIds: List<Long>) = db.exerciseRecordDao.getRecords(exerciseIds)

    suspend fun addExerciseRecord(exerciseRecord: ExerciseRecord) = db.exerciseRecordDao.insert(exerciseRecord)


    fun getWorkoutRecord(workoutId: Long) = db.workoutRecordDao.getRecord(workoutId)

    fun getWorkoutRecordsByProgram(programId: Long) = db.workoutRecordDao.getRecordsByProgram(programId)

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

    fun getExercise(exerciseId: Long) = db.exerciseDao.getExercise(exerciseId)

    suspend fun addExercise(exercise: Exercise) = db.exerciseDao.insert(exercise)


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


    // TODO: move default value outside
    fun getUserHeight(): Flow<Float> = context.dataStore.data.map{ it[userHeight] ?: 170f }

    suspend fun setUserHeight(newHeight: Float) = context.dataStore.edit { it[userHeight] = newHeight }


    // TODO: move default value outside
    fun getUserYear(): Flow<Int> = context.dataStore.data.map{ it[userAgeYear] ?: 2000 }

    suspend fun setUserYear(newYear: Int) = context.dataStore.edit { it[userAgeYear] = newYear }


    // TODO: move default value outside
    fun getUserSex(): Flow<String> = context.dataStore.data.map{ it[userSex] ?: "Male" }

    suspend fun setUserSex(newSex: String) = context.dataStore.edit { it[userSex] = newSex }


    // TODO: move default value outside
    fun getUserName(): Flow<String> = context.dataStore.data.map{ it[userName] ?: "what's your name?" }

    suspend fun setUserName(newName: String) = context.dataStore.edit { it[userName] = newName }


    fun getCurrentWorkout(): Flow<Long?> = context.dataStore.data.map{ it[currentWorkout] }

    suspend fun setCurrentWorkout(newValue: Long?) = context.dataStore.edit {
        if (newValue == null)
            it.remove(currentWorkout)
        else
            it[currentWorkout] = newValue
    }


    companion object {

        // For Singleton instantiation
        @Volatile private var instance: Repository? = null

        fun getInstance(workoutDatabase: WorkoutDatabase, context: Context) =
            instance ?: synchronized(this) {
                instance ?: Repository(workoutDatabase, context).also { instance = it }
            }
    }
}
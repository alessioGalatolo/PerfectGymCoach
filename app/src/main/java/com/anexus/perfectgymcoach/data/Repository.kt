package com.anexus.perfectgymcoach.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.anexus.perfectgymcoach.data.exercise.*
import com.anexus.perfectgymcoach.data.workout_exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_exercise.WorkoutExerciseReorder
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanUpdateProgram
import com.anexus.perfectgymcoach.data.workout_program.RemovePlan
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramRename
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgramReorder
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecord
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecordFinish
import com.anexus.perfectgymcoach.data.workout_record.WorkoutRecordStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class Repository @Inject constructor(
    private val db: WorkoutDatabase,
    context: Context
) {
    private val dataStore = context.dataStore
    private val currentPlan = longPreferencesKey("Current plan")
    private val currentWorkout = longPreferencesKey("Current workout")
    private val userWeight = floatPreferencesKey("User weight")
    private val userHeight = floatPreferencesKey("User height")
    private val userSex = stringPreferencesKey("User sex")
    private val theme = stringPreferencesKey("Theme")
    private val userName = stringPreferencesKey("User name")
    private val userAgeYear = intPreferencesKey("User age year")
    private val imperialSystem = booleanPreferencesKey("Imperial system user")


    fun getPlans() = db.workoutPlanDao.getPlans()

    fun getPlan(planId: Long) = db.workoutPlanDao.getPlan(planId)

    fun getPlanMapPrograms(): Flow<Map<WorkoutPlan, List<WorkoutProgram>>> =
        db.workoutPlanDao.getPlanMapPrograms()

    suspend fun addPlan(plan: WorkoutPlan) = db.workoutPlanDao.insert(plan)

    suspend fun updateCurrentPlan(workoutPlanUpdateProgram: WorkoutPlanUpdateProgram) =
        db.workoutPlanDao.updateCurrentProgram(workoutPlanUpdateProgram)


    fun getProgramsMapExercises(planId: Long): Flow<Map<WorkoutProgram, List<ProgramExercise>>> =
        db.workoutProgramDao.getProgramsMapExercises(planId)

    fun getProgramMapExercises(programId: Long): Flow<Map<WorkoutProgram, List<ProgramExercise>>> =
        db.workoutProgramDao.getProgramMapExercises(programId)

    fun getPrograms(planId: Long) = db.workoutProgramDao.getPrograms(planId)

    suspend fun addProgram(program: WorkoutProgram) = db.workoutProgramDao.insert(program)

    suspend fun renameProgram(workoutProgramRename: WorkoutProgramRename) =
        db.workoutProgramDao.updateName(workoutProgramRename)

    suspend fun reorderPrograms(workoutProgramReorder: List<WorkoutProgramReorder>) =
        db.workoutProgramDao.updateOrder(workoutProgramReorder)

    // FIXME: should check that the 'plan.currentProgram' is updated as well
    suspend fun removeProgramFromPlan(programId: Long) = db.workoutProgramDao.removeFromPlan(
        RemovePlan(programId = programId)
    )

    fun getProgramExercisesAndInfo(programId: Long): Flow<List<ProgramExerciseAndInfo>> =
        db.programExerciseDao.getExercisesAndInfo(programId)

    fun getProgramExercisesAndInfo(programIds: List<Long>): Flow<List<ProgramExerciseAndInfo>> =
        db.programExerciseDao.getExercisesAndInfo(programIds)

    fun getProgramExercises(programId: Long) = db.programExerciseDao.getExercises(programId)

    fun getProgramExercise(programExerciseId: Long) =
        db.programExerciseDao.getProgramExercise(programExerciseId)

    suspend fun addProgramExercise(exercise: ProgramExercise) =
        db.programExerciseDao.insert(exercise)

    suspend fun reorderProgramExercises(programExerciseReorders: List<ProgramExerciseReorder>) =
        db.programExerciseDao.updateOrder(programExerciseReorders)

    suspend fun deleteProgramExercise(programExerciseId: Long) =
        db.programExerciseDao.delete(programExerciseId)

    suspend fun updateExerciseSuperset(updateExerciseSupersets: List<UpdateExerciseSuperset>) =
        db.programExerciseDao.updateSuperset(updateExerciseSupersets)


    suspend fun addWorkoutExercise(workoutExercise: WorkoutExercise) =
        db.workoutExerciseDao.insert(workoutExercise)

    suspend fun addWorkoutExercises(workoutExercises: List<WorkoutExercise>) =
        db.workoutExerciseDao.insert(workoutExercises)

    suspend fun getWorkoutExercises(workoutId: Long) =
        db.workoutExerciseDao.getWorkoutExercises(workoutId)

    suspend fun deleteWorkoutExercise(workoutExerciseId: Long) =
        db.workoutExerciseDao.delete(workoutExerciseId)

    suspend fun updateWorkoutExerciseNumber(workoutExerciseReorder: WorkoutExerciseReorder) =
        db.workoutExerciseDao.updateOrder(workoutExerciseReorder)


    fun getExerciseRecords(exerciseId: Long) = db.exerciseRecordDao.getRecords(exerciseId)

    fun getExerciseRecords(exerciseIds: List<Long>) = db.exerciseRecordDao.getRecords(exerciseIds)

    fun getExerciseRecordsAndEquipment(exerciseIds: List<Long>) = db.exerciseRecordDao.getRecordsWithEquipment(exerciseIds)

    fun getWorkoutExerciseRecords(workoutId: Long) = db.exerciseRecordDao.getByWorkout(workoutId)

    suspend fun deleteWorkoutExerciseRecords(workoutId: Long) = db.exerciseRecordDao.deleteByWorkout(workoutId)

    // FIXME: bad name
    fun getWorkoutExerciseRecordsAndInfo(workoutId: Long) = db.exerciseRecordDao.getByWorkoutWithInfo(workoutId)

    suspend fun addExerciseRecord(exerciseRecord: ExerciseRecord) = db.exerciseRecordDao.insert(exerciseRecord)


    fun getWorkoutRecord(workoutId: Long) = db.workoutRecordDao.getRecord(workoutId)

    fun getWorkoutRecordsByProgram(programId: Long) = db.workoutRecordDao.getRecordsByProgram(programId)

    fun getWorkoutHistory() = db.workoutRecordDao.getRecords()

    fun getWorkoutHistoryAndName() = db.workoutRecordDao.getRecordsAndName()

    suspend fun addWorkoutRecord(workoutRecord: WorkoutRecord) = db.workoutRecordDao.insert(workoutRecord)

    suspend fun startWorkout(workoutRecordStart: WorkoutRecordStart) =
        db.workoutRecordDao.updateStart(workoutRecordStart)

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


    fun getCurrentPlan(): Flow<Long?> = dataStore.data.map{ it[currentPlan] }

    suspend fun setCurrentPlan(planId: Long, overrideValue: Boolean){
        dataStore.edit {
            if (it[currentPlan] == null || overrideValue){
                it[currentPlan] = planId
            }
        }

    }


    // TODO: move default value outside (60 kg)
    fun getUserWeight(): Flow<Float> = dataStore.data.map{ it[userWeight] ?: 60f }

    suspend fun setUserWeight(newWeight: Float) = dataStore.edit { it[userWeight] = newWeight }


    // TODO: move default value outside
    fun getUserHeight(): Flow<Float> = dataStore.data.map{ it[userHeight] ?: 170f }

    suspend fun setUserHeight(newHeight: Float) = dataStore.edit { it[userHeight] = newHeight }


    // TODO: move default value outside
    fun getUserYear(): Flow<Int> = dataStore.data.map{ it[userAgeYear] ?: 2000 }

    suspend fun setUserYear(newYear: Int) = dataStore.edit { it[userAgeYear] = newYear }


    // TODO: move default value outside
    fun getUserSex(): Flow<Sex> = dataStore.data.map{ Sex.fromName(it[userSex]) }

    suspend fun setUserSex(newSex: Sex) = dataStore.edit { it[userSex] = newSex.sexName }

    // TODO: move default value outside
    fun getTheme(): Flow<Theme> = dataStore.data.map{ Theme.fromName(it[theme]) }

    suspend fun setTheme(newTheme: Theme) = dataStore.edit { it[theme] = newTheme.themeName }


    // TODO: move default value outside
    fun getUserName(): Flow<String> = dataStore.data.map{ it[userName] ?: "what's your name?" }

    suspend fun setUserName(newName: String) = dataStore.edit { it[userName] = newName }


    // TODO: move default value outside
    fun getImperialSystem(): Flow<Boolean> = dataStore.data.map{ it[imperialSystem] ?: false }

    suspend fun setImperialSystem(newValue: Boolean) = dataStore.edit { it[imperialSystem] = newValue }


    fun getCurrentWorkout(): Flow<Long?> = dataStore.data.map{ it[currentWorkout] }

    suspend fun setCurrentWorkout(newValue: Long?) = dataStore.edit {
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
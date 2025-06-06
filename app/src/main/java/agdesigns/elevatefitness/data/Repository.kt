package agdesigns.elevatefitness.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import agdesigns.elevatefitness.data.exercise.*
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExercise
import agdesigns.elevatefitness.data.workout_exercise.WorkoutExerciseReorder
import agdesigns.elevatefitness.data.workout_plan.ArchiveWorkoutPlan
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlan
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanUpdateProgram
import agdesigns.elevatefitness.data.workout_program.RemovePlan
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import agdesigns.elevatefitness.data.workout_program.WorkoutProgramRename
import agdesigns.elevatefitness.data.workout_program.WorkoutProgramReorder
import agdesigns.elevatefitness.data.workout_record.WorkoutRecord
import agdesigns.elevatefitness.data.workout_record.WorkoutRecordFinish
import agdesigns.elevatefitness.data.workout_record.WorkoutRecordStart
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.DrawableRes
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class Repository @Inject constructor(
    private val db: WorkoutDatabase,
    private val watchMessageReceiver: WatchMessageReceiver,
    @ApplicationContext  private val context: Context
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
    private val dontWantNotificationAccess = booleanPreferencesKey("Don't want notification access")
    private val incrementBodyweight = floatPreferencesKey("Increment body weight")
    private val incrementBarbell = floatPreferencesKey("Increment barbell")
    private val incrementDumbbell = floatPreferencesKey("Increment dumbbell")
    private val incrementMachine = floatPreferencesKey("Increment machine")
    private val incrementCable = floatPreferencesKey("Increment cable")

    /*
     * Wear connection
     */
    private val _watchIsAlive = MutableStateFlow(true)
    private var lastHeartbeat = System.currentTimeMillis()
    private var listenHeartbeatJob: Job? = null
    private var checkWatchHealthJob: Job? = null

    fun startListeningForWatch() {
        if (checkWatchHealthJob == null) {
            lastHeartbeat = System.currentTimeMillis()
            checkWatchHealthJob = CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    // send phone heartbeat, then check watch's
                    val nodes = Wearable.getNodeClient(context).connectedNodes
                    nodes.addOnSuccessListener {
                        for (node in it) {
                            Wearable.getMessageClient(context)
                                .sendMessage(node.id, "/heartbeat", "ping".toByteArray())
                        }
                    }
                    val alive = System.currentTimeMillis() - lastHeartbeat < 2000
                    _watchIsAlive.tryEmit(alive)
                    delay(1000)
                }
            }
        }
        if (listenHeartbeatJob == null) {
            listenHeartbeatJob = CoroutineScope(Dispatchers.Default).launch {
                watchMessageReceiver.watchHeartbeat.collect {
                    lastHeartbeat = System.currentTimeMillis()
                }
            }
        }
    }

    fun stopWearWorkout() {
        val message = PutDataMapRequest.create("/stop_workout")
        message.dataMap.putLong("message_timestamp", System.currentTimeMillis())
        val putReq = message.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context)
            .putDataItem(putReq)
            .addOnSuccessListener { dataItem ->
                Log.d("WearSender", "Stop workout sent: $dataItem")
            }
        checkWatchHealthJob?.cancel()
        listenHeartbeatJob?.cancel()
        checkWatchHealthJob = null
        listenHeartbeatJob = null
    }

    fun sendWorkout2Wear(message: PutDataMapRequest, overrideDeadWatch: Boolean = false) {
        startListeningForWatch()  // This should actually be called first by the view model
        if (!_watchIsAlive.value && !overrideDeadWatch) {
            Log.d("Repository", "Skipping sending workout to wear as it is dead")
            return
        }
        // FIXME: it should work even without a timestamp but it doesn't ><
        message.dataMap.putLong("message_timestamp", System.currentTimeMillis())
        val putReq = message.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context)
            .putDataItem(putReq)
            .addOnSuccessListener { dataItem ->
                Log.d("WearSender", "DataItem sent: $dataItem")
            }
            .addOnFailureListener { e ->
                Log.e("WearSender", "Failed sending DataItem", e)
            }
    }

    fun getWatchSetCompletion(): Flow<JSONObject> = watchMessageReceiver.setCompletionInfo

    fun getSyncRequest(): Flow<Boolean> = watchMessageReceiver.syncRequest

    /*
     * WORKOUT PLAN
     */
    fun getPlans() = db.workoutPlanDao.getPlans()

    fun getPlan(planId: Long) = db.workoutPlanDao.getPlan(planId)

    fun getPlanMapPrograms(): Flow<Map<WorkoutPlan, List<WorkoutProgram>>> =
        db.workoutPlanDao.getPlanMapPrograms()

    suspend fun addPlan(plan: WorkoutPlan) = db.workoutPlanDao.insert(plan)

    suspend fun updateCurrentPlan(workoutPlanUpdateProgram: WorkoutPlanUpdateProgram) =
        db.workoutPlanDao.updateCurrentProgram(workoutPlanUpdateProgram)

    suspend fun archivePlan(planId: Long) = db.workoutPlanDao.archivePlan(ArchiveWorkoutPlan(planId))

    suspend fun unarchivePlan(planId: Long) = db.workoutPlanDao.archivePlan(ArchiveWorkoutPlan(planId, false))


    /*
     * WORKOUT PROGRAM
     */
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

    suspend fun addProgramExercise(exercise: ProgramExercise): Long =
        db.programExerciseDao.insert(exercise)

    suspend fun reorderProgramExercises(programExerciseReorders: List<ProgramExerciseReorder>) =
        db.programExerciseDao.updateOrder(programExerciseReorders)

    suspend fun deleteProgramExercise(programExerciseId: Long) =
        db.programExerciseDao.delete(programExerciseId)

    suspend fun updateExerciseSuperset(updateExerciseSupersets: List<UpdateExerciseSuperset>) =
        db.programExerciseDao.updateSuperset(updateExerciseSupersets)


    /*
     * WORKOUT EXERCISE
     */
    suspend fun addWorkoutExercise(workoutExercise: WorkoutExercise) =
        db.workoutExerciseDao.insert(workoutExercise)

    suspend fun addWorkoutExercises(workoutExercises: List<WorkoutExercise>) =
        db.workoutExerciseDao.insert(workoutExercises)

    fun getWorkoutExercises(workoutId: Long) =
        db.workoutExerciseDao.getWorkoutExercises(workoutId)

    suspend fun deleteWorkoutExercise(workoutExerciseId: Long) =
        db.workoutExerciseDao.delete(workoutExerciseId)

    suspend fun updateWorkoutExerciseNumber(workoutExerciseReorder: WorkoutExerciseReorder) =
        db.workoutExerciseDao.updateOrder(workoutExerciseReorder)


    /*
     * EXERCISE RECORD
     */
    fun getExerciseRecords(exerciseId: Long) = db.exerciseRecordDao.getRecords(exerciseId)

    fun getExerciseRecords(exerciseIds: List<Long>) = db.exerciseRecordDao.getRecords(exerciseIds)

    fun getExerciseRecordsAndEquipment(exerciseIds: List<Long>) = db.exerciseRecordDao.getRecordsWithEquipment(exerciseIds)

    fun getWorkoutExerciseRecords(workoutId: Long) = db.exerciseRecordDao.getByWorkout(workoutId)

    suspend fun deleteWorkoutExerciseRecords(workoutId: Long) = db.exerciseRecordDao.deleteByWorkout(workoutId)

    // FIXME: bad name
    fun getWorkoutExerciseRecordsAndInfo(workoutId: Long) = db.exerciseRecordDao.getByWorkoutWithInfo(workoutId)

    suspend fun addExerciseRecord(exerciseRecord: ExerciseRecord) = db.exerciseRecordDao.insert(exerciseRecord)


    /*
     * WORKOUT RECORD
     */
    fun getWorkoutRecord(workoutId: Long) = db.workoutRecordDao.getRecord(workoutId)

    fun getWorkoutRecordsByProgram(programId: Long) = db.workoutRecordDao.getRecordsByProgram(programId)

    fun getWorkoutHistory() = db.workoutRecordDao.getRecords()

    fun getWorkoutHistoryAndName() = db.workoutRecordDao.getRecordsAndName()

    suspend fun addWorkoutRecord(workoutRecord: WorkoutRecord) = db.workoutRecordDao.insert(workoutRecord)

    suspend fun startWorkout(workoutRecordStart: WorkoutRecordStart) =
        db.workoutRecordDao.updateStart(workoutRecordStart)

    suspend fun completeWorkoutRecord(workoutRecordFinish: WorkoutRecordFinish) = db.workoutRecordDao.updateFinish(workoutRecordFinish)


    /*
     * EXERCISE
     */
    fun getExercises(muscle: Exercise.Muscle): Flow<List<Exercise>> {
        return if (muscle == Exercise.Muscle.EVERYTHING) {
            db.exerciseDao.getAllExercises()
        } else {
            db.exerciseDao.getExercises(muscle)
        }
    }

    fun getExercise(exerciseId: Long) = db.exerciseDao.getExercise(exerciseId)

    suspend fun addExercise(exercise: Exercise) = db.exerciseDao.insert(exercise)

    suspend fun updateExerciseProbability(exerciseId: Long, newProbability: Double = 1.0) = db.exerciseDao.resetProbability(exerciseId, newProbability)

    suspend fun resetAllExerciseProbability() = db.exerciseDao.resetAllProbabilities()

    /*
     * DATA STORE (SETTINGS)
     */
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
    fun getDontWantNotificationAccess(): Flow<Boolean> = dataStore.data.map{ it[dontWantNotificationAccess] ?: false }

    suspend fun setDontWantNotificationAccess(newValue: Boolean) = dataStore.edit { it[dontWantNotificationAccess] = newValue }


    // TODO: move default value outside
    fun getImperialSystem(): Flow<Boolean> = dataStore.data.map{ it[imperialSystem] ?: false }

    suspend fun setImperialSystem(newValue: Boolean) = dataStore.edit { it[imperialSystem] = newValue }


    // TODO: move default value outside
    fun getBodyweightIncrement(): Flow<Float> = dataStore.data.map{ it[incrementBodyweight] ?: 2.5f }

    suspend fun setBodyweightIncrement(newValue: Float) = dataStore.edit { it[incrementBodyweight] = newValue }


    // TODO: move default value outside
    fun getBarbellIncrement(): Flow<Float> = dataStore.data.map{ it[incrementBarbell] ?: 2.5f }

    suspend fun setBarbellIncrement(newValue: Float) = dataStore.edit { it[incrementBarbell] = newValue }



    // TODO: move default value outside
    fun getDumbbellIncrement(): Flow<Float> = dataStore.data.map{ it[incrementDumbbell] ?: 2f }

    suspend fun setDumbbellIncrement(newValue: Float) = dataStore.edit { it[incrementDumbbell] = newValue }


    // TODO: move default value outside
    fun getMachineIncrement(): Flow<Float> = dataStore.data.map{ it[incrementMachine] ?: 5f }

    suspend fun setMachineIncrement(newValue: Float) = dataStore.edit { it[incrementMachine] = newValue }


    // TODO: move default value outside
    fun getCableIncrement(): Flow<Float> = dataStore.data.map{ it[incrementCable] ?: 2.5f }

    suspend fun setCableIncrement(newValue: Float) = dataStore.edit { it[incrementCable] = newValue }


    fun getCurrentWorkout(): Flow<Long?> = dataStore.data.map{ it[currentWorkout] }

    suspend fun setCurrentWorkout(newValue: Long?) = dataStore.edit {
        if (newValue == null)
            it.remove(currentWorkout)
        else
            it[currentWorkout] = newValue
    }

    /*
     * Utils
     */
    fun getBitmapFromResId(@DrawableRes resId: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, resId, options)

        // FIXME: is this enough res?
        options.inSampleSize = calculateInSampleSize(options, reqWidth = 200, reqHeight = 200)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeResource(context.resources, resId, options)
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    fun getAssetFromResId(@DrawableRes imageId: Int): Asset {
        val bitmap = getBitmapFromResId(imageId)
        return ByteArrayOutputStream().let { byteStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteStream)
            Asset.createFromBytes(byteStream.toByteArray())
        }
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: Repository? = null

        fun getInstance(workoutDatabase: WorkoutDatabase, watchMessageReceiver: WatchMessageReceiver, context: Context) =
            instance ?: synchronized(this) {
                instance ?: Repository(workoutDatabase, watchMessageReceiver, context).also { instance = it }
            }
    }
}
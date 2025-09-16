package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.WorkoutDatabase
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.ExerciseRecord
import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseReorder
import agdesigns.elevatefitness.data.db.entity.Sex
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.data.db.entity.UpdateExerciseSuperset
import agdesigns.elevatefitness.data.wearos.WatchMessageReceiver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import agdesigns.elevatefitness.data.db.entity.WorkoutExercise
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseReorder
import agdesigns.elevatefitness.data.db.entity.ArchiveWorkoutPlan
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutPlan
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanUpdateProgram
import agdesigns.elevatefitness.data.db.entity.RemovePlan
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramReorder
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordFinish
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordStart
import agdesigns.elevatefitness.utils.getLocalizedString
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.AnyRes
import androidx.annotation.DrawableRes
import androidx.datastore.core.DataMigration
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okio.SYSTEM
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.ZonedDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { ctx ->
        listOf(V1PrefsMigration(ctx))
    }
)

@Singleton
class Repository @Inject constructor(
    private val db: WorkoutDatabase,
    private val watchMessageReceiver: WatchMessageReceiver,
    @ApplicationContext  private val context: Context
) {
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

    suspend fun renamePlan(workoutPlanRename: WorkoutPlanRename) =
        db.workoutPlanDao.updateName(workoutPlanRename)

    /*
     * WORKOUT PROGRAM
     */
    fun getProgramsMapExercises(planId: Long): Flow<Map<WorkoutProgram, List<ProgramExercise>>> =
        db.workoutProgramDao.getProgramsMapExercises(planId).map {
            it.mapValues {
                it.value.map {
                    resolveResources(it)
                }
            }
        }

    fun getProgramMapExercises(programId: Long): Flow<Map<WorkoutProgram, List<ProgramExercise>>> =
        db.workoutProgramDao.getProgramMapExercises(programId).map {
            it.mapValues {
                it.value.map {
                    resolveResources(it)
                }
            }
        }

    fun getPrograms(planId: Long) = db.workoutProgramDao.getPrograms(planId)

    suspend fun addProgram(program: WorkoutProgram) = db.workoutProgramDao.insert(program)

    suspend fun renameProgram(workoutProgramRename: WorkoutProgramRename) =
        db.workoutProgramDao.updateName(workoutProgramRename)

    suspend fun reorderPrograms(workoutProgramReorder: List<WorkoutProgramReorder>) =
        db.workoutProgramDao.updateOrder(workoutProgramReorder)

    suspend fun removeProgramFromPlan(programId: Long) = db.workoutProgramDao.removeFromPlan(
        RemovePlan(programId = programId)
    )

    fun getProgramExercisesAndInfo(programId: Long): Flow<List<ProgramExerciseAndInfo>> =
        db.programExerciseDao.getExercisesAndInfo(programId).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    fun getProgramExercisesAndInfo(programIds: List<Long>): Flow<List<ProgramExerciseAndInfo>> =
        db.programExerciseDao.getExercisesAndInfo(programIds).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    fun getProgramExercises(programId: Long) = db.programExerciseDao.getExercises(programId).map {
        it.map { exercise -> resolveResources(exercise) }
    }

    fun getProgramExercise(programExerciseId: Long) =
        db.programExerciseDao.getProgramExercise(programExerciseId).map {
            resolveResources(it)
        }

    suspend fun addProgramExercise(exercise: ProgramExercise): Long =
        db.programExerciseDao.insert(exercise)

    suspend fun reorderProgramExercises(programExerciseReorders: List<ProgramExerciseReorder>) =
        db.programExerciseDao.updateOrder(programExerciseReorders)

    suspend fun deleteProgramExercise(programExerciseId: Long) =
        db.programExerciseDao.delete(programExerciseId)

    suspend fun updateExerciseSuperset(updateExerciseSupersets: List<UpdateExerciseSuperset>) =
        db.programExerciseDao.updateSuperset(updateExerciseSupersets)

    fun resolveResources(exercise: ProgramExercise): ProgramExercise {
        if (exercise.variationResKey.isBlank()) {
            // can either be no variation or user-defined variation
            // while not currently implemented, user-defined variation will be stored in 'variation'
            return exercise
        }
        return exercise.copy(
            variation = context.getString(exercise.variationResource)
        )
    }

    fun resolveResources(exerciseAndInfo: ProgramExerciseAndInfo): ProgramExerciseAndInfo {
        val name = if (exerciseAndInfo.userDefined)
            exerciseAndInfo.name
        else
            context.getString(exerciseAndInfo.nameResource)
        val description = if (exerciseAndInfo.userDefined)
            exerciseAndInfo.description
        else
            context.getString(exerciseAndInfo.descriptionResource)
        val image = if (exerciseAndInfo.userDefined)
            R.drawable.finish_workout
        else
            exerciseAndInfo.imageResource
        val variation = if (exerciseAndInfo.variationResKey.isBlank())
            exerciseAndInfo.variation  // could be user defined
        else
            context.getString(exerciseAndInfo.variationResource)
        return exerciseAndInfo.copy(
            name = name,
            description = description,
            image = image,
            variation = variation
        )
    }

    /*
     * WORKOUT EXERCISE
     */
    suspend fun addWorkoutExercise(workoutExercise: WorkoutExercise) =
        db.workoutExerciseDao.insert(workoutExercise)

    suspend fun addWorkoutExercises(workoutExercises: List<WorkoutExercise>) =
        db.workoutExerciseDao.insert(workoutExercises)

    fun getWorkoutExercises(workoutId: Long) =
        db.workoutExerciseDao.getWorkoutExercises(workoutId).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    suspend fun deleteWorkoutExercise(workoutExerciseId: Long) =
        db.workoutExerciseDao.delete(workoutExerciseId)

    suspend fun updateWorkoutExerciseNumber(workoutExerciseReorder: WorkoutExerciseReorder) =
        db.workoutExerciseDao.updateOrder(workoutExerciseReorder)

    fun resolveResources(workoutExercise: WorkoutExercise): WorkoutExercise {
        val name = if (workoutExercise.userDefined)
            workoutExercise.name
        else
            context.getString(workoutExercise.nameResource)
        val image = if (workoutExercise.userDefined)
            R.drawable.finish_workout
        else
            workoutExercise.imageResource
        val variation = if (workoutExercise.variationResKey.isBlank())
            workoutExercise.variation  // could be user defined
        else
            context.getString(workoutExercise.variationResource)
        val description = if (workoutExercise.userDefined)
            workoutExercise.description
        else
            context.getString(workoutExercise.descriptionResource)
        return workoutExercise.copy(
            name = name,
            image = image,
            variation = variation,
            description = description
        )
    }

    /*
     * EXERCISE RECORD
     */
    fun getExerciseRecords(exerciseId: Long) = db.exerciseRecordDao.getRecords(exerciseId).map {
        it.map { exercise -> resolveResources(exercise) }
    }

    fun getExerciseRecords(exerciseIds: List<Long>) = db.exerciseRecordDao.getRecords(exerciseIds).map {
        it.map { exercise -> resolveResources(exercise) }
    }

    fun getExerciseRecordsInRange(startDate: ZonedDateTime, endDate: ZonedDateTime) =
        db.exerciseRecordDao.getRecordsInRange(startDate, endDate).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    fun getExerciseRecordsAndEquipment(exerciseIds: List<Long>) =
        db.exerciseRecordDao.getRecordsWithEquipment(exerciseIds).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    fun getExerciseRecordsAndEquipment(exerciseId: Long) =
        db.exerciseRecordDao.getRecordsWithEquipment(exerciseId).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    fun getAllExerciseRecordsAndEquipment() =
        db.exerciseRecordDao.getAllRecordsWithEquipment().map {
            it.map { exercise -> resolveResources(exercise) }
        }

    fun getWorkoutExerciseRecords(workoutId: Long) =
        db.exerciseRecordDao.getByWorkout(workoutId).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    suspend fun deleteWorkoutExerciseRecords(workoutId: Long) = db.exerciseRecordDao.deleteByWorkout(workoutId)

    // FIXME: bad name
    fun getWorkoutExerciseRecordsAndInfo(workoutId: Long) =
        db.exerciseRecordDao.getByWorkoutWithInfo(workoutId).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    suspend fun addExerciseRecord(exerciseRecord: ExerciseRecord) = db.exerciseRecordDao.insert(exerciseRecord)

    fun resolveResources(exerciseRecord: ExerciseRecord): ExerciseRecord {
        if (exerciseRecord.variationResKey.isBlank()) {
            return exerciseRecord
        }
        return exerciseRecord.copy(
            variation = context.getString(exerciseRecord.variationResource)
        )
    }

    fun resolveResources(exerciseRecord: ExerciseRecordAndEquipment): ExerciseRecordAndEquipment {
        if (exerciseRecord.variationResKey.isBlank()) {
            return exerciseRecord
        }
        return exerciseRecord.copy(
            variation = context.getString(exerciseRecord.variationResource)
        )
    }

    fun resolveResources(exerciseRecord: ExerciseRecordAndInfo): ExerciseRecordAndInfo {
        val name = if (exerciseRecord.userDefined)
            exerciseRecord.name
        else
            context.getString(exerciseRecord.nameResource)
        val image = if (exerciseRecord.userDefined)
            R.drawable.finish_workout
        else
            exerciseRecord.imageResource
        val variation = if (exerciseRecord.variationResKey.isBlank())
            exerciseRecord.variation  // could be user defined
        else
            context.getString(exerciseRecord.variationResource)
        return exerciseRecord.copy(
            name = name,
            image = image,
            variation = variation
        )

    }


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

    suspend fun getWorkoutsInRange(startDate: ZonedDateTime, endDate: ZonedDateTime) = db.workoutRecordDao.getWorkoutsBetween(startDate, endDate)

    /*
     * EXERCISE
     */
    fun getExercises(muscle: Exercise.Muscle): Flow<List<Exercise>> {
        return (if (muscle == Exercise.Muscle.EVERYTHING) {
            db.exerciseDao.getAllExercises()
        } else {
            db.exerciseDao.getExercises(muscle)
        }).map {
            it.map { exercise -> resolveResources(exercise) }
        }
    }

    fun resolveResources(exercise: Exercise): Exercise {
        val name = if (exercise.userDefined)
            exercise.name
        else
            context.getString(exercise.nameResource)
        val description = if (exercise.userDefined)
            // currently user cannot specify description but if ever allowed, it will be stored here
            exercise.description
        else
            context.getString(exercise.descriptionResource)
        val image = if (exercise.userDefined)
            R.drawable.finish_workout
        else
            exercise.imageResource
        val variations = if (exercise.userDefined)
            exercise.variations
        else
            exercise.variationsResource.map {
                context.getString(it)
            }
        return exercise.copy(
            name = name,
            description = description,
            image = image,
            variations = variations
        )
    }

    fun getExercise(exerciseId: Long) =
        db.exerciseDao.getExercise(exerciseId).map { resolveResources(it) }

    suspend fun addExercise(exercise: Exercise) = db.exerciseDao.insert(exercise)

    suspend fun updateExerciseProbability(exerciseId: Long, newProbability: Double = 1.0) = db.exerciseDao.resetProbability(exerciseId, newProbability)

    suspend fun resetAllExerciseProbability() = db.exerciseDao.resetAllProbabilities()

    /*
     * Utils
     */
    fun getBitmapFromResId(@DrawableRes resId: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        fun Resources.debugName(@AnyRes id: Int) = runCatching { getResourceName(id) }.getOrNull()
        Log.d("ResCheck", "resId=$resId name=${context.resources.debugName(resId)}")
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
package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.WorkoutDatabase
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.ExerciseRecord
import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseReorder
import agdesigns.elevatefitness.data.db.entity.UpdateExerciseSuperset
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
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseUpdateSets
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramReorder
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordFinish
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordStart
import agdesigns.elevatefitness.data.db.entity.getDuplicatePlanName
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.AnyRes
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper
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
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.ZonedDateTime
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
    @param:ApplicationContext  private val context: Context
) {
    fun openWearWorkout() {
        // maybe open wear os app
        val openWearIntent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            data = "elevatefitnesswear://startworkout".toUri()
        }
        val remoteActivityHelper = RemoteActivityHelper(context)
        remoteActivityHelper.startRemoteActivity(openWearIntent)
    }

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

    suspend fun duplicatePlan(planId: Long) {
        // this is a deep copy: copy plan, programs in plan and program exercise in program
        val plan = getPlan(planId).first() ?: return
        val newPlanId = addPlan(
            plan.copy(
                planId = 0L,
                creationDate = ZonedDateTime.now(),
                name = getDuplicatePlanName(plan.name)
            )
        )
        val programs = getPrograms(planId).first()
        for (program in programs) {
            val newProgramId = addProgram(
                program.copy(
                    programId = 0L,
                    extPlanId = newPlanId
                )
            )
            val programExercises = getProgramExercises(program.programId).first()
            for (programExercise in programExercises) {
                addProgramExercise(
                    programExercise.copy(
                        programExerciseId = 0L,
                        extProgramId = newProgramId
                    )
                )
            }
        }
    }

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

    fun getProgramExercisesWithExercise(programId: Long): Flow<List<Pair<ProgramExercise, Exercise?>>> =
        db.programExerciseDao.getProgramExercisesWithExercise(programId).map {
            it.map{
                resolveResources(it.programExercise) to
                        it.exercise?.let { resolveResources(it) }
            }
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

    fun getWorkoutExercise(workoutExerciseId: Long) =
        db.workoutExerciseDao.getWorkoutExercise(workoutExerciseId).map {
            resolveResources(it)
        }

    fun getWorkoutExercises(workoutId: Long) =
        db.workoutExerciseDao.getWorkoutExercises(workoutId).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    suspend fun deleteWorkoutExercise(workoutExerciseId: Long) =
        db.workoutExerciseDao.delete(workoutExerciseId)

    suspend fun updateWorkoutExerciseNumber(workoutExerciseReorder: WorkoutExerciseReorder) =
        db.workoutExerciseDao.updateOrder(workoutExerciseReorder)

    suspend fun updateWorkoutExerciseSets(workoutExerciseUpdateSets: WorkoutExerciseUpdateSets) =
        db.workoutExerciseDao.updateSets(workoutExerciseUpdateSets)

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

    suspend fun shiftWorkoutExercisesToRight(workoutId: Long, fromPosition: Int) {
        return shiftWorkoutExercises(workoutId, fromPosition, 1)
    }

    suspend fun shiftWorkoutExercisesToLeft(workoutId: Long, fromPosition: Int) {
        return shiftWorkoutExercises(workoutId, fromPosition, -1)
    }

    private suspend fun shiftWorkoutExercises(workoutId: Long, fromPosition: Int, offset: Int) {

        // we need to shift all the exercises after the insert position
        val exs = getWorkoutExercises(workoutId).first()
        val exsToShift = buildList {
            // the "orderInProgram" of the last ex added to the list
            var lastInsertedIndex = fromPosition
            // we should assume that some orderInProgram may be missing
            for (ex in exs.sortedBy { it.orderInProgram }) {
                if (ex.orderInProgram > lastInsertedIndex + 1)
                    break
                if (ex.orderInProgram >= lastInsertedIndex) {
                    add(ex)
                    lastInsertedIndex = ex.orderInProgram
                }
            }
        }
        for (ex in exsToShift.reversed())
            updateWorkoutExerciseNumber(
                WorkoutExerciseReorder(
                    ex.workoutExerciseId,
                    ex.orderInProgram + offset
                )
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

    suspend fun deleteExerciseRecord(recordId: Long) = db.exerciseRecordDao.delete(recordId)

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

    /**
     * Calculate how many times a plan has been cycled through.
     * A cycle is counted when any programs in the plan have been completed at least once.
     * Returns the maximum completion count across all programs.
     * Note: we should take the minimum instead but what if user skips leg day?
     */
    suspend fun getPlanCycleCount(planId: Long): Int {
        val programs = getPrograms(planId).first()
        if (programs.isEmpty()) return 0

        val completionCounts = programs.map { program ->
            getWorkoutRecordsByProgram(program.programId).first()
                .count { it.startDate != null && it.durationSeconds > 60L * 5L }
        }

        return completionCounts.maxOrNull() ?: 0
    }

    /**
     * Check if the plan is showing diminishing returns.
     * Returns true if average volume has decreased or plateaued over recent workouts.
     */
    suspend fun isPlanShowingDiminishingReturns(planId: Long): Boolean {
        val programs = getPrograms(planId).first()
        if (programs.isEmpty()) return false

        // Get all workout records for this plan's programs
        val allRecords = programs.flatMap { program ->
            getWorkoutRecordsByProgram(program.programId).first()
        }.filter { it.startDate != null && it.durationSeconds > 60L * 5L }
            .sortedByDescending { it.startDate }

        // Need at least 12 workouts to detect a trend
        if (allRecords.size < 12) return false

        // Compare average volume of last 6 workouts vs previous 6 workouts
        val recentVolume = allRecords.take(6).map { it.volume }.average()
        val previousVolume = allRecords.drop(6).take(6).map { it.volume }.average()

        // If recent volume is less than 95% of previous volume, consider it diminishing returns
        return recentVolume < previousVolume * 0.95
    }

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

        fun getInstance(workoutDatabase: WorkoutDatabase, /*watchMessageReceiver: WatchMessageReceiver,*/ context: Context) =
            instance ?: synchronized(this) {
                instance ?: Repository(workoutDatabase, /*watchMessageReceiver,*/ context).also { instance = it }
            }
    }
}
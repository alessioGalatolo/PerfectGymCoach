package agdesigns.elevatefitness.data

import kotlin.math.abs
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.WorkoutDatabase
import agdesigns.elevatefitness.data.db.computeWorkoutAchievements
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
import agdesigns.elevatefitness.shared.SetType
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseUpdateSetTypes
import agdesigns.elevatefitness.data.db.entity.UpdateExerciseRecordSetTypes
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseUpdateSets
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramRename
import agdesigns.elevatefitness.data.db.entity.WorkoutProgramReorder
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordFinish
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordHealthId
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordStart
import agdesigns.elevatefitness.data.db.entity.WorkoutAchievementAndInfo
import agdesigns.elevatefitness.data.db.entity.getDuplicatePlanName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import com.google.android.gms.wearable.Asset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
            copyProgramExercises(program.programId, newProgramId)
        }
    }

    /**
     * Copies all exercises of a program into another one. Supersets are remapped to the copies,
     * otherwise they would still point to the exercises of the original program.
     */
    private suspend fun copyProgramExercises(fromProgramId: Long, toProgramId: Long) {
        val programExercises = getProgramExercises(fromProgramId).first()
        val oldToNewId = programExercises.associate { programExercise ->
            programExercise.programExerciseId to addProgramExercise(
                programExercise.copy(
                    programExerciseId = 0L,
                    extProgramId = toProgramId,
                    supersetExercise = null
                )
            )
        }
        val supersets = programExercises.mapNotNull { programExercise ->
            val newSuperset = programExercise.supersetExercise?.let { oldToNewId[it] }
                ?: return@mapNotNull null
            UpdateExerciseSuperset(oldToNewId.getValue(programExercise.programExerciseId), newSuperset)
        }
        if (supersets.isNotEmpty())
            updateExerciseSuperset(supersets)
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

    fun getProgram(programId: Long) = db.workoutProgramDao.getProgram(programId)

    suspend fun addProgram(program: WorkoutProgram) = db.workoutProgramDao.insert(program)

    suspend fun renameProgram(workoutProgramRename: WorkoutProgramRename) =
        db.workoutProgramDao.updateName(workoutProgramRename)

    suspend fun duplicateProgram(programId: Long) {
        val program = getProgram(programId).first()
        if (program.extPlanId != null) {
            val programsToShift = getPrograms(program.extPlanId).first().filter {
                it.orderInWorkoutPlan > program.orderInWorkoutPlan
            }
            val reorders = programsToShift.map {
                WorkoutProgramReorder(
                    it.programId, it.orderInWorkoutPlan + 1
                )
            }
            reorderPrograms(reorders)
        }
        // place the copy right after the original, in the slot freed by the shift above
        val newProgramId = addProgram(
            program.copy(
                programId = 0L,
                orderInWorkoutPlan = program.orderInWorkoutPlan + 1
            )
        )
        copyProgramExercises(program.programId, newProgramId)
    }

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
        db.programExerciseDao.upsert(exercise)

    suspend fun reorderProgramExercises(programExerciseReorders: List<ProgramExerciseReorder>) =
        db.programExerciseDao.updateOrder(programExerciseReorders)

    suspend fun shiftProgramExercisesToRight(programId: Long, fromPosition: Int) {
        val reorders = getProgramExercises(programId).first()
            .filter { it.orderInProgram >= fromPosition }
            .map { ProgramExerciseReorder(it.programExerciseId, it.orderInProgram + 1) }
        reorderProgramExercises(reorders)
    }

    suspend fun duplicateProgramExercise(programExerciseId: Long) {
        val exercise = getProgramExercise(programExerciseId).first()
        val exercisesToShift = getProgramExercises(exercise.extProgramId)
            .first().filter {
                it.orderInProgram > exercise.orderInProgram
            }
        val reorders = exercisesToShift.map {
            ProgramExerciseReorder(
                it.programExerciseId, it.orderInProgram + 1
            )
        }
        reorderProgramExercises(reorders)
        // place the copy right after the original, in the slot freed by the shift above.
        // Do not copy the superset, the partner would not point back to the copy
        addProgramExercise(
            exercise.copy(
                programExerciseId = 0L,
                orderInProgram = exercise.orderInProgram + 1,
                supersetExercise = null
            )
        )
    }
    suspend fun deleteProgramExercise(programExerciseId: Long) =
        db.programExerciseDao.delete(programExerciseId)

    suspend fun updateProgramExercise(exercise: ProgramExercise) =
        db.programExerciseDao.update(exercise)

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
            it?.let { workoutExercise -> resolveResources(workoutExercise) }
        }

    fun getWorkoutExercises(workoutId: Long) =
        db.workoutExerciseDao.getWorkoutExercises(workoutId).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    // resources not resolved, only meant to inspect what was planned (e.g., reps)
    suspend fun getWorkoutExercisesByIds(workoutExerciseIds: List<Long>) =
        workoutExerciseIds.chunked(500).flatMap {
            db.workoutExerciseDao.getWorkoutExercisesByIds(it)
        }

    suspend fun deleteWorkoutExercise(workoutExerciseId: Long) =
        db.workoutExerciseDao.delete(workoutExerciseId)

    suspend fun updateWorkoutExerciseNumber(workoutExerciseReorder: WorkoutExerciseReorder) =
        db.workoutExerciseDao.updateOrder(workoutExerciseReorder)

    suspend fun updateWorkoutExerciseSets(workoutExerciseUpdateSets: WorkoutExerciseUpdateSets) =
        db.workoutExerciseDao.updateSets(workoutExerciseUpdateSets)

    suspend fun updateWorkoutExerciseSetTypes(
        workoutExerciseId: Long,
        setTypes: List<SetType>
    ) = db.workoutExerciseDao.updateSetTypes(
        WorkoutExerciseUpdateSetTypes(
            workoutExerciseId,
            setTypes
        )
    )

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

    fun getWorkoutExerciseRecordsAndInfo(workoutId: Long) =
        db.exerciseRecordDao.getByWorkoutWithInfo(workoutId).map {
            it.map { exercise -> resolveResources(exercise) }
        }

    suspend fun addExerciseRecord(exerciseRecord: ExerciseRecord) = db.exerciseRecordDao.insert(exerciseRecord)

    suspend fun updateExerciseRecordSetTypes(
        recordId: Long,
        setTypes: List<SetType>
    ) = db.exerciseRecordDao.updateSetTypes(
        UpdateExerciseRecordSetTypes(
            recordId,
            setTypes
        )
    )

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
     * WORKOUT ACHIEVEMENT
     */
    fun getWorkoutAchievements(workoutId: Long) =
        db.workoutAchievementDao.getByWorkoutWithInfo(workoutId).map {
            it.map { achievement -> resolveResources(achievement) }
        }

    fun getAllWorkoutAchievements() =
        db.workoutAchievementDao.getAllWithInfo().map {
            it.map { achievement -> resolveResources(achievement) }
        }

    fun resolveResources(achievement: WorkoutAchievementAndInfo): WorkoutAchievementAndInfo {
        if (achievement.name == null || achievement.userDefined != false)
            return achievement
        return achievement.copy(name = context.getString(achievement.nameResource))
    }

    /**
     * Computes and stores the achievements of a finished workout. Does nothing if the workout
     * already has achievements stored, they are never recomputed.
     */
    suspend fun computeWorkoutAchievements(workoutId: Long, currentPlanId: Long?) =
        db.computeWorkoutAchievements(workoutId, currentPlanId)

    suspend fun updateWorkoutRecordHealthId(workoutId: Long, healthRecordId: String) =
        db.workoutRecordDao.updateHealthId(
            WorkoutRecordHealthId(
                workoutId,
                healthRecordId
            )
        )

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
        BitmapFactory.decodeResource(context.resources, resId, options)

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

    /*
     * Plan sharing / importing
     */
    suspend fun getFullPlanData(planId: Long): SharedWorkoutPlanModel {
        val plan = getPlan(planId).first() ?: throw IllegalArgumentException("Plan not found")
        val programs = getPrograms(planId).first()
        val missingExercises = mutableListOf<Exercise>()
        val sharedPrograms = programs.map { program ->
            val exercises = getProgramExercisesAndInfo(program.programId).first()
            val sharedExercises = exercises.map { exercise ->
                if (exercise.userDefined && missingExercises.none { it.exerciseId == exercise.extExerciseId }) {
                    missingExercises.add(getExercise(exercise.extExerciseId).first())
                }
                SharedExerciseModel(
                    nameResKey = exercise.nameResKey,
                    localizedName = exercise.name,
                    reps = exercise.reps,
                    rest = exercise.rest,
                    note = exercise.note,
                    variationResKey = exercise.variationResKey,
                    orderInProgram = exercise.orderInProgram,
                    overriddenDurationBased = exercise.overriddenDurationBased,
                    setTypes = exercise.setTypes,
                    supersetExerciseNameResKey = exercise.supersetExercise?.let {
                        exercises.find { ex -> ex.programExerciseId == it }?.nameResKey
                    },
                    supersetExerciseName = exercise.supersetExercise?.let {
                        exercises.find { ex -> ex.programExerciseId == it }?.name
                    }
                )
            }
            SharedProgramModel(
                name = program.name,
                orderInWorkoutPlan = program.orderInWorkoutPlan,
                exercises = sharedExercises
            )
        }
        return SharedWorkoutPlanModel(
            planName = plan.name,
            programs = sharedPrograms,
            missingExercises = missingExercises.map {
                it.toSharedBaseExercise()
            }
        )
    }

    suspend fun importSharedPlan(sharedPlan: SharedWorkoutPlanModel): Long {
        val nameToMissingExercise = sharedPlan.missingExercises.associateBy { it.name }
        // user-defined exercises resolved so far, so that each is created only once
        val resolvedUserExercises = mutableMapOf<String, Long>()
        val newPlanId = addPlan(WorkoutPlan(name = sharedPlan.planName, creationDate = ZonedDateTime.now()))
        for (program in sharedPlan.programs.sortedBy { it.orderInWorkoutPlan }) {
            val newProgramId = addProgram(
                WorkoutProgram(extPlanId = newPlanId, orderInWorkoutPlan = program.orderInWorkoutPlan, name = program.name)
            )
            // shared exercises that were imported, with the id of their new program exercise
            val created = mutableListOf<Pair<SharedExerciseModel, Long>>()
            for (exercise in program.exercises.sortedBy { it.orderInProgram }) {
                val exerciseId = resolveOrCreateExercise(
                    exercise,
                    nameToMissingExercise,
                    resolvedUserExercises
                ) ?: continue
                val programExercise = ProgramExercise(
                    extProgramId = newProgramId,
                    extExerciseId = exerciseId,
                    // position among the imported ones, exercises that could not be resolved leave no gap
                    orderInProgram = created.size,
                    reps = exercise.reps,
                    rest = exercise.rest,
                    note = exercise.note,
                    variationResKey = exercise.variationResKey,
                    overriddenDurationBased = exercise.overriddenDurationBased,
                    setTypes = exercise.setTypes,
                )
                created.add(exercise to addProgramExercise(programExercise))
            }
            // after creating all the program exercises, we check for supersets
            val supersets = created.mapIndexedNotNull { index, (exercise, programExerciseId) ->
                if (exercise.supersetExerciseNameResKey == null && exercise.supersetExerciseName == null)
                    return@mapIndexedNotNull null
                // user-defined exercises have a blank nameResKey, match them by name instead.
                // If the same exercise appears more than once, the closest one is the partner
                val partner = created.withIndex()
                    .filter { (otherIndex, other) ->
                        otherIndex != index && if (!exercise.supersetExerciseNameResKey.isNullOrBlank())
                            other.first.nameResKey == exercise.supersetExerciseNameResKey
                        else
                            other.first.localizedName == exercise.supersetExerciseName
                    }
                    .minByOrNull { (otherIndex, _) -> abs(otherIndex - index) }
                    ?: return@mapIndexedNotNull null
                UpdateExerciseSuperset(programExerciseId, partner.value.second)
            }
            if (supersets.isNotEmpty())
                updateExerciseSuperset(supersets)
        }
        return newPlanId
    }

    private suspend fun resolveOrCreateExercise(
        shared: SharedExerciseModel,
        nameToMissingExercise: Map<String, SharedBaseExercise>,
        resolvedUserExercises: MutableMap<String, Long>
    ): Long? {
        // user-defined exercises have a blank nameResKey, which would match any user-defined one
        if (shared.nameResKey.isNotBlank()) {
            val existing = db.exerciseDao.getExerciseByNameResKey(shared.nameResKey)
            if (existing != null) return existing.exerciseId
        }

        resolvedUserExercises[shared.localizedName]?.let { return it }
        val missingExercise = nameToMissingExercise[shared.localizedName] ?: return null
        // reuse a user-defined exercise with the same name, if any
        val exerciseId = db.exerciseDao.getExerciseByName(missingExercise.name)
            ?.takeIf { it.userDefined }
            ?.exerciseId
            ?: addExercise(missingExercise.toExercise())
        resolvedUserExercises[shared.localizedName] = exerciseId
        return exerciseId
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
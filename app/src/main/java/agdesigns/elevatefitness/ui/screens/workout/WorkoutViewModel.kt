package agdesigns.elevatefitness.ui.screens.workout

import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.PhoneWorkoutRepository
import agdesigns.elevatefitness.data.PreferenceRepository
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.ExerciseRecord
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.shared.SetType
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.data.db.entity.WorkoutExercise
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseReorder
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseUpdateSets
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanUpdateProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordFinish
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordStart
import agdesigns.elevatefitness.service.NotificationService
import agdesigns.elevatefitness.service.WorkoutNotificationState
import agdesigns.elevatefitness.utils.computeVolume
import agdesigns.elevatefitness.utils.getMetFromIntensity
import android.os.Build
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.WORKOUT_IMAGES_PATH
import agdesigns.elevatefitness.shared.bitmapArrayStore
import agdesigns.elevatefitness.shared.grpc.WorkoutWearServiceGrpcKt
import agdesigns.elevatefitness.shared.maybeKgToLb
import agdesigns.elevatefitness.shared.maybeLbToKg
import agdesigns.elevatefitness.shared.toProtoTimestamp
import agdesigns.elevatefitness.shared.toZonedDateTime
import agdesigns.elevatefitness.shared.urgentProtoDataStore
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.protobuf.Empty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.collections.toList
import kotlin.math.max
import kotlin.math.min


// elements that change frequently e.g., current exercise, reps, timer, etc.
data class CurrentExerciseState(
    val isLoading: Boolean = true,
    val exerciseTitle: String? = null,  // if null, means we are on last recap page
    val repsBottomBar: String = "0", // reps to be displayed in bottom bar
    val repsIsValid: Boolean = true,
    val weightBottomBar: String = "0.0", // weight to be displayed in bottom bar
    val weightIsValid: Boolean = true,
    val workoutTimeFormatted: String = "",
    val restTimeSecs: Long? = null,
    val restProgress: Float = 0f,
    val restTimestamp: ZonedDateTime? = null, // workout time of end of rest // FIXME: sometimes timer shows negative e.g., resume workout
    val currentExerciseRest: Long? = null, // useful to compute progress of rest
    val currentTime: ZonedDateTime = ZonedDateTime.now(),
    val currentExerciseOngoingRecord: ExerciseRecordAndEquipment? = null,
    val currentExercise: WorkoutExercise? = null,
    val setsDone: Int = 0 // sets done in current exercise
)

@Target(
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn("This property is not necessarily updated, use WorkoutState instead.")
annotation class OutOfSyncProperty

@Target(
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn("This property is internal and should not be used.")
annotation class InternalProperty

data class SetDisplayRow(
    val reps: String,
    val weight: String,
    val toBeDone: Boolean,
    val setType: SetType = SetType.NORMAL,
    // expectations based on current session
    val projectedReps: String? = null,
    val projectedWeight: String? = null
)

data class ModificationSuggestion(
    val type: WorkoutRecord.ModificationType,
    // keep this reference so that we can remove it from the possible suggestions on acceptance
    val originalModification: WorkoutRecord.WorkoutModification,
    val newWorkoutExercise: WorkoutExercise? = null,
) {
    fun toProto(): Workout.ProtoSuggestedModification = Workout.ProtoSuggestedModification.newBuilder()
        .setType(type.toProto())
        .setHasSuggestion(true)
        .setTargetExerciseName(newWorkoutExercise?.name ?: "")
        .setSourceId(originalModification.sourceWorkoutExerciseId
            ?: originalModification.sourceProgramExerciseId
            ?: originalModification.sourceExerciseId
            ?: 0L
        )
        .setTargetId(
            originalModification.targetWorkoutExerciseId ?:
            originalModification.targetExerciseId ?: 0L
        )
        .build()
}

data class WorkoutPagesContent(
    val exercises: List<WorkoutExercise> = emptyList(),
    val exerciseRecords: List<List<ExerciseRecordAndEquipment>> = emptyList(),
    val ongoingRecords: List<ExerciseRecordAndEquipment?> = emptyList(),
    val modificationsSuggestions: List<ModificationSuggestion?> = emptyList(),
    // values to display in each exercise set card e.g., completed sets will show input from user,
    // incomplete sets will show **historic data** and have projected reps/weight
    val exerciseRepsWeightRows: List<List<SetDisplayRow>> = emptyList(),
    val exerciseSetsDone: List<Int> = emptyList(),
    @OutOfSyncProperty
    @InternalProperty
    val suggestedTares: List<Float> = emptyList(),
    @InternalProperty
    val workoutId: Long = 0L,
    @InternalProperty
    val imperialSystem: Boolean = false,
)

data class WorkoutState(
    val workoutStarted: Boolean = false,
    val otherEquipmentDialogOpen: Boolean = false,
    val enterIntensityDialogOpen: Boolean = false,
    val cantRequestOngoingWorkoutNotification: Boolean = true,
    val requestNotificationAccessDialogOpen: Boolean = false,
    val cantRequestNotificationAccess: Boolean = true,
    val programId: Long = 0L,
    val startDate: ZonedDateTime? = null,
    val userTheme: Theme = Theme.SYSTEM,
    val incrementBodyweight: Float = 0f,
    val incrementBarbell: Float = 0f,
    val incrementDumbbell: Float = 0f,
    val incrementMachine: Float = 0f,
    val incrementCable: Float = 0f,
    val hasRecordedExercise: Boolean = false, // used to add a flag in cancel workout
    val lastWorkoutIntensity: Float? = null,
    val workoutId: Long = 0L,
    val imperialSystem: Boolean = false,
    val lockHorizontalScroll: Boolean = false,
    val autoOpenWear: Boolean? = null, // set as null as we want to wait for the actual first value
    // TODO: really not happy about this. Belongs to WorkoutPagesContent but it was not to be
    //  updated directly by the user by the tares are
    val tares: List<Float> = emptyList(),
    val canPostPromotedNotifications: Boolean = false,
    val lastWorkoutModifications: List<WorkoutRecord.WorkoutModification> = emptyList()
)

sealed class WorkoutEffect {
    data object NavigateBack: WorkoutEffect()
    data class ShowMessage(val message: Int): WorkoutEffect()
    data class ShowErrorAndBack(val message: Int): WorkoutEffect()
    data class AdvancePage(val page: Int): WorkoutEffect()
    data object ShutDown: WorkoutEffect()
}

sealed class WorkoutEvent{
    data class InitWorkout(
        val programId: Long,
        val resumeWorkout: Boolean,
        val quickStart: Boolean
    ): WorkoutEvent()

    data object StartWorkout: WorkoutEvent()

    data class FinishWorkout(val workoutIntensity: Float): WorkoutEvent()

    data object CancelWorkout: WorkoutEvent()

    data object DeleteCurrentRecords: WorkoutEvent()

    data object ToggleOtherEquipmentDialog: WorkoutEvent()

    data object CompleteSet: WorkoutEvent()

    data object ToggleRequestNotificationAccessDialog : WorkoutEvent()

    data object ToggleEnterIntensityDialog : WorkoutEvent()

    data object DontRequestNotificationAgain : WorkoutEvent()

    data object DontRequestOngoingWorkoutNotification: WorkoutEvent()

    data class AcceptSuggestedModification(val exerciseInWorkout: Int): WorkoutEvent()
    data object ResetWaitingForExerciseAdd: WorkoutEvent()
    data class AddExercise(val exerciseInWorkout: Int, val originalSize: Int): WorkoutEvent()

    data class ReplaceExercise(val exerciseInWorkout: Int, val originalSize: Int): WorkoutEvent()

    data class RemoveExercise(val exerciseInWorkout: Int): WorkoutEvent()

    data object AddSetToCurrentExercise: WorkoutEvent()

    data class UpdateReps(val newValue: String): WorkoutEvent()

    data class UpdateWeight(val newValue: String): WorkoutEvent()

    data class UpdateSetType(val page: Int, val set: Int, val type: SetType): WorkoutEvent()

    // same as above but updates the weight based on the equipment's default de/increment value
    data class AutoStepWeight(
        val newValue: String,
        val equipment: Equipment,
        val subtract: Boolean
    ): WorkoutEvent()

    data class UpdateTare(val newValue: Float): WorkoutEvent()

    data class EditSetRecord(
        val reps: Int,
        val weight: Float,
        val exerciseInWorkout: Int,
        val set: Int
    ): WorkoutEvent()

    data class DeleteSetRecord(
        val exerciseInWorkout: Int,
        val set: Int
    ): WorkoutEvent()

    data class UpdateCurrentPage(val currentPage: Int) : WorkoutEvent()

    data object RefreshHasPromptedNotificationsAccess: WorkoutEvent()
}

@OptIn(InternalProperty::class, OutOfSyncProperty::class, ExperimentalHorologistApi::class)
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository,
    private val notificationService: NotificationService,
    private val registry: WearDataLayerRegistry,
    private val phoneWorkoutRepository: PhoneWorkoutRepository,
    private val phoneToWatchService: WorkoutWearServiceGrpcKt.WorkoutWearServiceCoroutineStub
): ViewModel() {
    // when resuming workout, we want to scroll to latest exercise completed.
    // set this flag to do it
    private var resumeAndScrollToLatest = false
    private val workoutModifications: MutableList<WorkoutRecord.WorkoutModification> = mutableListOf()
    private var observeAddingExerciseJob: Job? = null

    // split static data (e.g., exercises) with data that is frequently changing to avoid too many messages
    private val wearWorkoutStatic = registry.urgentProtoDataStore<Workout.WorkoutStaticData>(viewModelScope)
    private val wearWorkoutDynamic = registry.urgentProtoDataStore<Workout.WorkoutDynamicData>(viewModelScope)
    private val wearWorkoutImages = registry.bitmapArrayStore(viewModelScope, WORKOUT_IMAGES_PATH)

    // effects to happen in the UI
    private val _effects = Channel<WorkoutEffect>(capacity = Channel.BUFFERED)

    val effects: Flow<WorkoutEffect> = _effects.receiveAsFlow()

    private val _workoutState = MutableStateFlow(WorkoutState())
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    private val _currentExerciseState = MutableStateFlow(CurrentExerciseState())
    val currentExerciseState: StateFlow<CurrentExerciseState> = _currentExerciseState.asStateFlow()

    // Core state flows
    private val _currentPage = MutableStateFlow(0)
    private val _workoutExercises = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    private val _allRecords =
        MutableStateFlow<Map<Long, List<ExerciseRecordAndEquipment>>>(emptyMap())

    // Static content to be in the horizontalPager
    val pagesContent: StateFlow<WorkoutPagesContent> = combine(
        _workoutExercises,
        _allRecords,
        workoutState.map{ it.imperialSystem }.distinctUntilChanged(),
        workoutState.map { it.workoutId }.distinctUntilChanged(),
        workoutState.map { it.tares }.distinctUntilChanged(),
        workoutState.map { it.lastWorkoutModifications }.distinctUntilChanged(),
    ) { values ->
        /**
         * Glocal stuff for pages, precompute all so that pager can cache stuff
         * Should be recomputed if exercises, records, imperialSystem, or workoutId changes
         * Should NOT be recomputed if the only change is current page
         */
        var exercises = values[0] as List<WorkoutExercise>
        val records = values[1] as Map<Long, List<ExerciseRecordAndEquipment>>
        val imperialSystem = values[2] as Boolean
        val workoutId = values[3] as Long
        val tares = values[4] as List<Float>
        val modifications = values[5] as List<WorkoutRecord.WorkoutModification>
        val recordsAndOngoingForAllExercises = exercises.mapIndexed { index, exercise ->
            getUpdatedRecords(exercise, records, index, workoutId)
        }
        val recordsForAllExercises = recordsAndOngoingForAllExercises.map { it.first }
            // filter out empty records
            .map {
                it.filter { it.reps.isNotEmpty() }
            }
        if (resumeAndScrollToLatest) {
            val index = recordsAndOngoingForAllExercises.map { it.second }.indexOfLast {
                it != null && it.reps.isNotEmpty()
            }

            if (index != -1) {
                _effects.trySend(
                    WorkoutEffect.AdvancePage(index)
                )
                resumeAndScrollToLatest = false
            }
        }
        val exerciseSetsDone = recordsAndOngoingForAllExercises.map {
            it.second?.reps?.size ?: 0
        }
        // check if we can "auto-add" sets to exercises (when user added one last time, re-propose)
        exercises = exercises.zip(recordsForAllExercises).map { (ex, records) ->
            val lastRecord = records.firstOrNull()
            if (lastRecord?.reps?.size?.let { it > ex.reps.size } == true) {
                val additionalReps = lastRecord.reps.slice(ex.reps.size until lastRecord.reps.size)
                val additionalRest = lastRecord.rest.slice(ex.rest.size until lastRecord.rest.size)
                val additionalSetTypes = List(additionalReps.size) { SetType.AWESOME }
                ex.copy(
                    reps = ex.reps.plus(additionalReps),
                    rest = ex.rest.plus(additionalRest),
                    setTypes = (ex.setTypes ?: List(ex.reps.size) { SetType.NORMAL })
                        .plus(additionalSetTypes)
                )

            } else {
                ex
            }
        }
        val exerciseRepsWeights =
            exercises.zip(recordsAndOngoingForAllExercises).mapIndexed { index, (exercise, records) ->
                val ongoingRecord = records.second
                val allRecords = records.first
                var lastOne: SetDisplayRow? = null
                val tare = tares.getOrNull(index) ?: 0f
                val isPyramid = exercise.reps
                    .zip(exercise.setTypes ?: List(exercise.reps.size) { SetType.NORMAL})
                    .filter {
                        it.second == SetType.NORMAL
                    }.distinctBy { it.first }.size != 1
                val exerciseIncrement = when (exercise.equipment) {
                    Equipment.BARBELL -> _workoutState.value.incrementBarbell
                    Equipment.DUMBBELL -> _workoutState.value.incrementDumbbell
                    Equipment.MACHINE -> _workoutState.value.incrementMachine
                    Equipment.CABLES -> _workoutState.value.incrementCable
                    else -> _workoutState.value.incrementBodyweight
                }
                exercise.reps.mapIndexed { setCount, reps ->
                    val toBeDone =
                        (ongoingRecord?.reps?.size ?: 0) <= setCount  // setsDone <= setCount
                    val isUpcomingSet = ongoingRecord?.reps?.size == setCount
                    var projectedWeight: String? = null
                    var projectedReps: String? = null
                    val repsInRow: String
                    val weightInRow: String
                    var barbellDiff = 0f
                    if (toBeDone) {
                        // user has not done this set
                        repsInRow = reps.toString()
                        val historicRecord = allRecords.firstOrNull()
                        barbellDiff = if (historicRecord != null && exercise.equipment == Equipment.BARBELL)
                            tare - historicRecord.tare
                        else
                            0f
                        weightInRow =
                            if (historicRecord != null && historicRecord.weights.isNotEmpty()) {
                                // user has done this exercise in the past, place historic values
                                maybeKgToLb(
                                    historicRecord.weights.getOrNull(setCount) ?: historicRecord.weights.last(),
                                    imperialSystem
                                ).toString()
                            } else {
                                "..."
                            }
                        projectedReps = computeNextRep(
                            exercise,
                            allRecords,
                            (lastOne?.projectedReps ?: lastOne?.reps)?.toIntOrNull(),
                            setCount
                        )?.toString()
                        projectedWeight = computeNextWeight(
                            recordsToDisplay = allRecords,
                            ongoingLastWeight = (lastOne?.projectedWeight ?: lastOne?.weight)?.toFloatOrNull(),
                            setsDone = setCount,
                            barbellDiff = barbellDiff,
                            imperialSystem = imperialSystem,
                            setTypes = exercise.setTypes,
                            isUpcomingSet = isUpcomingSet,
                            isPyramid = isPyramid,
                            increment = exerciseIncrement
                        )?.toString()
                    } else {
                        // if ongoingRecord is null, it should go in the other branch anyway
                        repsInRow =
                            ongoingRecord?.reps?.getOrNull(setCount)
                                ?.toString() ?: "0"
                        weightInRow = maybeKgToLb(
                            ongoingRecord?.weights?.getOrNull(setCount)
                                ?: 0f, imperialSystem
                        ).toString()
                    }
                    if (projectedWeight == weightInRow)
                        projectedWeight = null
                    if (projectedReps == repsInRow)
                        projectedReps = null
                    lastOne = SetDisplayRow(
                        reps = repsInRow,
                        weight = weightInRow,
                        toBeDone = toBeDone,
                        projectedReps = projectedReps,
                        projectedWeight = projectedWeight,
                        setType = exercise.setTypes?.getOrNull(setCount) ?: SetType.NORMAL
                    )
                    // result of mapping
                    if (barbellDiff == 0f) {
                        lastOne // no changes needed
                    } else {
                        lastOne.copy(
                            projectedWeight = (lastOne
                                .projectedWeight
                                ?.toFloatOrNull() ?: lastOne.weight.toFloatOrNull())
                                // split by two as it is barbell
                                ?.minus(barbellDiff / 2)?.toString()
                        )
                    }
                }
            }
        val exercisesTares = recordsAndOngoingForAllExercises.map {
            computeExerciseTare(
                it.first,
                it.second,
            ) ?: 0f
        }
        // compute modifications, this is a bit slow
        val newSuggestions = List<ModificationSuggestion?>(exercises.size) { null }.toMutableList()
        val modificationsByProgramExercise = modifications.groupBy { it.sourceProgramExerciseId }
        val modificationsByExercise = modifications.groupBy { it.sourceExerciseId }
        for (i in exercises.indices) {
            val exercise = exercises.getOrNull(i)
            if (exercise == null) {
                newSuggestions[i] = null
                continue
            }
            if (exercise.extProgramExerciseId != null) {
                modificationsByProgramExercise[exercise.extProgramExerciseId]
                    ?.firstOrNull()
                    ?.let {
                        val newWorkoutExercise = it.targetWorkoutExerciseId?.let {
                            repository.getWorkoutExercise(it).first()
                        }
                        newSuggestions[i] = ModificationSuggestion(
                            type = it.modificationType,
                            originalModification = it,
                            newWorkoutExercise = newWorkoutExercise
                        )
                        continue
                    }
            }
            if (exercise.extProgramExerciseId == null) {
                modificationsByExercise[exercise.extExerciseId]
                    ?.firstOrNull()
                    ?.let {
                        newSuggestions[i] = ModificationSuggestion(
                            type = it.modificationType,
                            originalModification = it,
                            newWorkoutExercise = it.targetWorkoutExerciseId?.let {
                                repository.getWorkoutExercise(it).first()
                            }
                        )
                    }
            }
        }
        _currentExerciseState.update {
            it.copy(
                // very bad proxy to make sure we have just processed the real values
                isLoading = exercises.isEmpty()
            )
        }
        WorkoutPagesContent(
            exercises = exercises,
            exerciseRecords = recordsForAllExercises,
            exerciseRepsWeightRows = exerciseRepsWeights,
            exerciseSetsDone = exerciseSetsDone,
            suggestedTares = exercisesTares,
            imperialSystem = imperialSystem,
            workoutId = workoutId,
            ongoingRecords = recordsAndOngoingForAllExercises.map { it.second },
            modificationsSuggestions = newSuggestions
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WorkoutPagesContent()
    )

    private var retrieveExercisesRecords: Job? = null
    private var timerJob: Job? = null
    private var startWorkoutJob: Job? = null
    private var pageChangeJob: Job? = null
    private var retrieveExercisesJob: Job? = null
    private var completeSetJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        phoneWorkoutRepository.stopOngoingWorkout()
        timerJob?.cancel()
        notificationService.stop()
    }

    init {
        phoneWorkoutRepository.startOngoingWorkout()
        viewModelScope.launch {
            combine(
                preferences.getTheme(),
                preferences.getImperialSystem(),
                preferences.getBodyweightIncrement(),
                preferences.getBarbellIncrement(),
                preferences.getDumbbellIncrement(),
                preferences.getMachineIncrement(),
                preferences.getCableIncrement(),
                preferences.getDontWantNotificationAccess(),
                preferences.getLockHorizontalScroll(),
                preferences.getAutoOpenWear(),
                preferences.getDontWantOngoingWorkoutNotification()
            ) { values: Array<Any?> ->
                _workoutState.update {
                    it.copy(
                        userTheme = values[0] as Theme,
                        imperialSystem = values[1] as Boolean,
                        incrementBodyweight = values[2] as Float,
                        incrementBarbell = values[3] as Float,
                        incrementDumbbell = values[4] as Float,
                        incrementMachine = values[5] as Float,
                        incrementCable = values[6] as Float,
                        cantRequestNotificationAccess = values[7] as Boolean,
                        lockHorizontalScroll = values[8] as Boolean,
                        autoOpenWear = values[9] as Boolean,
                        cantRequestOngoingWorkoutNotification = values[10] as Boolean
                    )
                }
            }.collect()
        }
        // listens to relevant changes and sends them to wear
        checkWorkoutDataChangesForWear()
        observeSetCompletionsFromWear()
        observeWorkoutCompletionsFromWear()
        observeAcceptedModificationsFromWear()
        /*
          Compute stuff specific to current exercise (should be recomputed if any value changes)
         */
        viewModelScope.launch {
            combine(
                _currentPage,
                pagesContent,
            ) { page, pagesContent ->
                val currentExercise = pagesContent.exercises.getOrNull(page)
                if (currentExercise == null) {
                    // last page, reset values
                    _currentExerciseState.update {
                        it.copy(
                            exerciseTitle = null,
                            currentExercise = null,
                            currentExerciseOngoingRecord = null,
                        )
                    }
                } else {
                    val variation = if ((currentExercise.variation).isNotBlank())
                        " (${currentExercise.variation})"
                    else ""
                    val title = currentExercise.name.plus(variation)
                    val recordsToDisplay =
                        pagesContent.exerciseRecords.getOrNull(page) ?: emptyList()
                    val currentExerciseOngoingRecord = pagesContent.ongoingRecords.getOrNull(page)
                    val setsDone = currentExerciseOngoingRecord?.reps?.size ?: 0
                    val repsWeight = pagesContent.exerciseRepsWeightRows
                        .getOrNull(page)
                        ?.getOrNull(setsDone)
                    val repsToShow = repsWeight?.let{ it.projectedReps ?: it.reps }
                    var weightToShow = repsWeight?.let { it.projectedWeight ?: it.weight }
                    if (weightToShow == "...") {
                        // TODO: it would be cool to infer this value from other exercises
                        weightToShow = null
                    }
                    _currentExerciseState.update {
                        it.copy(
                            exerciseTitle = title,
                            repsBottomBar = repsToShow ?: it.repsBottomBar,
                            repsIsValid = true,
                            weightBottomBar = weightToShow ?: "0.0",
                            weightIsValid = true,
                            currentExerciseOngoingRecord = currentExerciseOngoingRecord,
                            currentExercise = currentExercise,
                            setsDone = setsDone
                        )
                    }
                }
            }.collect()
        }
        viewModelScope.launch {
            pagesContent.map { it.suggestedTares }.distinctUntilChanged().collect {
                // We only want to use suggestedTare as init
                _workoutState.update { state ->
                    state.copy(
                        tares = it
                    )
                }
            }
        }
        // gather stuff for notification
        viewModelScope.launch {
            combine(
                pagesContent.map { it.exercises }.distinctUntilChanged(),
                pagesContent.map { it.exerciseSetsDone }.distinctUntilChanged(),
                _currentPage,
                currentExerciseState.map { it.restTimeSecs }.distinctUntilChanged(),
                currentExerciseState.map { it.restTimestamp }.distinctUntilChanged(),
                currentExerciseState.map { it.currentExerciseRest }.distinctUntilChanged(),
                workoutState.map { it.workoutStarted }.distinctUntilChanged(),
            ) { values ->
                val exercises = values[0] as List<WorkoutExercise>
                val exerciseSetsDone = values[1] as List<Int>
                val currentPage = values[2] as Int
                val restTime = values[3] as Long?
                val restTimestamp = values[4] as ZonedDateTime?
                val totalRest = values[5] as Long?
                val workoutStarted = values[6] as Boolean

                WorkoutNotificationState(
                    setsPerExercise = exercises.map { it.reps.size },
                    setsDonePerExercise = exerciseSetsDone,
                    currentExercise = currentPage,
                    restTimeSecs = restTime,
                    restTimestamp = restTimestamp?.toInstant()?.toEpochMilli() ?:
                        (Date().time + (restTime ?: 0L) * 1000L),
                    totalRest = totalRest,
                    workoutStarted = workoutStarted
                )
            }.collect {
                if (it.workoutStarted) {
                    notificationService.updateNotification(it)
                }
            }
        }
        // if android 16+ get promoted notification state once (and then manually refresh)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            _workoutState.update {
                it.copy(
                    canPostPromotedNotifications = notificationService.canPostPromotedNotifications()
                )
            }
        }
        startTimer()
    }

    fun onEvent(event: WorkoutEvent) {
        when (event) {
            is WorkoutEvent.InitWorkout -> {
                viewModelScope.launch {
                    if (workoutState.value.workoutId != 0L) {
                        Log.d("WorkoutViewModel", "Trying to re-init workout!")
                        return@launch
                    }
                    if (event.resumeWorkout) {
                        resumeWorkout()
                    } else {
                        // 0L is no program, try to infer next program from db data
                        val programId = if (event.programId == 0L)
                            inferProgramId()
                        else
                            event.programId
                        if (programId == null) {
                            _effects.trySend(
                                WorkoutEffect.ShowErrorAndBack(R.string.autostart_workout_failed)
                            )
                            return@launch
                        }

                        _workoutState.update { it.copy(programId = programId) }
                        // get workout id
                        var workoutId = 0L
                        _workoutState.update {
                            workoutId = repository.addWorkoutRecord(
                                WorkoutRecord(extProgramId = programId)
                            )
                            it.copy(workoutId = workoutId)
                        }
                        // once we have workout id, retrieve program exercises
                        val exercises = repository.getProgramExercisesAndInfo(programId)
                            .first().sortedBy { it.orderInProgram }
                        // and create the relative workout exercises
                        val workoutExercises = exercises.map {
                            WorkoutExercise(
                                extExerciseId = it.extExerciseId,
                                extWorkoutId = workoutId,
                                extProgramExerciseId = it.programExerciseId,
                                orderInProgram = it.orderInProgram,
                                variation = it.variation,
                                variationResKey = it.variationResKey,
                                name = it.name,
                                nameResKey = it.nameResKey,
                                image = it.image,
                                imageResKey = it.imageResKey,
                                description = it.description,
                                descriptionResKey = it.descriptionResKey,
                                equipment = it.equipment,
                                note = it.note,
                                reps = it.reps.toList(),
                                rest = it.rest,
                                supersetExercise = it.supersetExercise,
                                userDefined = it.userDefined,
                                overriddenDurationBased = it.overriddenDurationBased,
                                setTypes = it.setTypes
                            )
                        }
                        // add workout exercises to db
                        repository.addWorkoutExercises(workoutExercises)
                    }
                    if (event.quickStart) {
                        startWorkout()
                    }
                    // get records to gather last workout's intensity
                    val records =
                        repository.getWorkoutRecordsByProgram(workoutState.value.programId).first()
                    if (records.isNotEmpty()) {
                        _workoutState.update { state ->
                            val lastRecord = records
                                // only consider workouts that were completed
                                .filter{ it.durationSeconds > 0L }
                                .sortedBy { it.startDate }
                                .lastOrNull()
                            if (lastRecord == null)
                                return@update state
                            state.copy(
                                lastWorkoutIntensity = lastRecord.intensityPercent,
                                lastWorkoutModifications = lastRecord.workoutModifications
                            )
                        }
                    }
                    startRetrievingExercises()
                }
            }
            is WorkoutEvent.StartWorkout -> {
                startWorkout()
            }
            is WorkoutEvent.ToggleRequestNotificationAccessDialog -> {
                _workoutState.update {
                    it.copy(
                        requestNotificationAccessDialogOpen = !workoutState.value.requestNotificationAccessDialogOpen
                    )
                }
            }
            is WorkoutEvent.ToggleEnterIntensityDialog -> {
                _workoutState.update {
                    it.copy(
                        enterIntensityDialogOpen = !workoutState.value.enterIntensityDialogOpen
                    )
                }
            }
            is WorkoutEvent.DontRequestNotificationAgain -> {
                viewModelScope.launch {
                    preferences.setDontWantNotificationAccess(true)
                }
            }
            is WorkoutEvent.DontRequestOngoingWorkoutNotification -> {
                viewModelScope.launch {
                    preferences.setDontWantOngoingWorkoutNotification(true)
                }
            }
            is WorkoutEvent.CompleteSet -> {
                viewModelScope.launch {
                    startWorkoutJob?.join()
                    if (currentExerciseState.value.repsBottomBar.toUIntOrNull() == null ||
                        currentExerciseState.value.weightBottomBar.toFloatOrNull() == null) {
                        // should not happen
                        Log.d("WorkoutViewModel", "Tried to complete a set with invalid weight or reps: $currentExerciseState")
                        _effects.trySend(WorkoutEffect.ShowMessage(R.string.complete_set_fail))
                        return@launch
                    }

                    val exercise = currentExerciseState.value.currentExercise
                    var exerciseRest = exercise
                        ?.rest
                        ?.getOrNull(currentExerciseState.value.setsDone)
                        ?.toLong() ?: 0L
                    val nextExercise = pagesContent.value.exercises.getOrNull(
                        _currentPage.value + 1
                    )
                    val prevExercise = pagesContent.value.exercises.getOrNull(
                        _currentPage.value - 1
                    )
                    // if superset with next exercise, skip rest
                    if (exercise?.supersetExercise != null && exercise.supersetExercise == nextExercise?.extProgramExerciseId) {
                        exerciseRest = 0L
                    }
                    val restTimestamp = ZonedDateTime.now().plusSeconds(exerciseRest)
                    // if sets done == total sets, scroll to next if any
                    val setsDone = currentExerciseState.value.setsDone
                    val totalSets = currentExerciseState.value.currentExercise?.reps?.size ?: 0
                    val shouldAdvanceEx = setsDone == totalSets - 1 && (_currentPage.value+1 < pagesContent.value.exercises.size)
                    // check if superset
                    val exerciseToScrollTo = if (exercise?.supersetExercise != null && exercise.supersetExercise == prevExercise?.extProgramExerciseId) {
                        if (shouldAdvanceEx)
                            _currentPage.value + 1
                        else
                            _currentPage.value - 1
                    } else if (exercise?.supersetExercise != null && exercise.supersetExercise == nextExercise?.extProgramExerciseId) {
                        _currentPage.value + 1
                    } else if (shouldAdvanceEx) {
                        _currentPage.value + 1
                    } else
                        _currentPage.value
                    // complete first, then send to watch (will take time)
                    completeSet(
                        exercise = exercise,
                        exerciseRest = exerciseRest,
                        reps = currentExerciseState.value.repsBottomBar.toInt(),
                        weight = currentExerciseState.value.weightBottomBar.toFloat(),
                        restTimestamp = restTimestamp
                    )
                    _effects.trySend(
                        WorkoutEffect.AdvancePage(exerciseToScrollTo)
                    )
                    try {
                        phoneToWatchService.scrollToExercise(
                            Workout.ExerciseToScrollTo.newBuilder()
                                .setExerciseIndex(exerciseToScrollTo)
                                .build()
                        )
                    } catch (e: Exception) {
                        Log.e("WorkoutViewModel", "Failed to scroll to exercise on watch", e)
                    }
                    try {
                        phoneToWatchService.setRest(
                            Workout.RestPhone2Watch.newBuilder()
                                .setRest(exerciseRest)
                                .setRestTimestamp(restTimestamp.toProtoTimestamp())
                                .build()
                        )
                    } catch (e: Exception) {
                        Log.e("WorkoutViewModel", "Failed to set rest on watch", e)
                    }
                }
            }
            is WorkoutEvent.FinishWorkout -> {
                viewModelScope.launch {
                    val healthData = try {
                        phoneToWatchService.getHealthData(Empty.newBuilder().build())
                    } catch (e: Exception) {
                        Log.e("WorkoutViewModel", "Failed to get health data from watch", e)
                        Workout.CompleteWorkout.newBuilder()
                            .setCalories(0f)
                            .setAvgHeartRate(0)
                            .setMaxHeartRate(0)
                            .setMinHeartRate(0)
                            .build()
                    }
                    finishWorkout(
                        intensity = event.workoutIntensity,
                        calories = if (healthData.calories != 0f) healthData.calories else null,
                        maxHeartRate = if (healthData.maxHeartRate != 0) healthData.maxHeartRate else null,
                        minHeartRate = if (healthData.minHeartRate != 0) healthData.minHeartRate else null,
                        avgHeartRate = if (healthData.avgHeartRate != 0) healthData.avgHeartRate else null,
                        heartRates = if (healthData.heartRatesCount > 0) healthData.heartRatesList.toList() else null
                    )
                }
            }
            is WorkoutEvent.CancelWorkout -> {
                viewModelScope.launch {
                    preferences.setCurrentWorkout(null)
                }
            }
            is WorkoutEvent.DeleteCurrentRecords -> {
                viewModelScope.launch {
                    repository.deleteWorkoutExerciseRecords(workoutState.value.workoutId)
                }
            }
            is WorkoutEvent.AddSetToCurrentExercise -> {
                // FIXME: check that other internals are not indirectly updated
                val exercise = currentExerciseState.value.currentExercise
                if (exercise == null) {
                    Log.e("WorkoutViewModel", "Tried to add a set to a null current exercise")
                    _effects.trySend(WorkoutEffect.ShowMessage(R.string.add_set_fail))
                    return
                }
                viewModelScope.launch {
                    addSetToExercise(exercise)
                }
            }
            is WorkoutEvent.UpdateReps -> {
                _currentExerciseState.update {
                    it.copy(
                        repsBottomBar = event.newValue,
                        repsIsValid = event.newValue.toUIntOrNull()?.let { it > 0U } == true
                    )
                }
            }
            is WorkoutEvent.UpdateWeight -> {
                _currentExerciseState.update {
                    it.copy(
                        weightBottomBar = event.newValue,
                        weightIsValid = event.newValue.toFloatOrNull() != null
                    )
                }
            }
            is WorkoutEvent.AutoStepWeight -> {
                var increment = when (event.equipment) {
                    Equipment.EVERYTHING -> throw Exception("Was asked about the increment of 'everything' equipment. This should not happen.")  // should never happen
                    Equipment.BARBELL -> workoutState.value.incrementBarbell
                    Equipment.BODY_WEIGHT -> workoutState.value.incrementBodyweight
                    Equipment.CABLES -> workoutState.value.incrementCable
                    Equipment.DUMBBELL -> workoutState.value.incrementDumbbell
                    Equipment.MACHINE -> workoutState.value.incrementMachine
                }
                if (event.subtract)
                    increment *= -1f
                val newValue = (event.newValue.toFloatOrNull() ?: 0f) + increment
                _currentExerciseState.update { it.copy(weightBottomBar = newValue.toString()) }
            }
            is WorkoutEvent.UpdateTare -> {
                val tares = workoutState.value.tares.toMutableList()
                tares[_currentPage.value] = event.newValue
                _workoutState.update { it.copy(tares = tares) }
            }
            is WorkoutEvent.UpdateSetType -> {
                val exercise = pagesContent.value.exercises
                    .getOrNull(event.page)
                val exerciseSetTypes = exercise?.setTypes
                val newTypes = exerciseSetTypes?.toMutableList() ?: mutableListOf()
                val currentType = newTypes.getOrElse(event.set) { SetType.NORMAL }
                var setToChange = event.set
                if (currentType == SetType.WARMUP && event.type != SetType.WARMUP) {
                    // if trying to remove a warmup set that is followed by warmup sets, change the last one instead
                    val lastIndex = newTypes.lastIndexOf(SetType.WARMUP)
                    if (lastIndex != -1) {
                        setToChange = lastIndex
                    }
                }
                if (newTypes.size <= setToChange) {
                    newTypes.add(event.type)
                } else {
                    newTypes[setToChange] = event.type
                }
                if (exercise != null) {
                    viewModelScope.launch {
                        repository.updateWorkoutExerciseSetTypes(
                            workoutExerciseId = exercise.workoutExerciseId,
                            setTypes = newTypes
                        )
                    }
                }
            }
            is WorkoutEvent.EditSetRecord -> {
                viewModelScope.launch {
                    val record = currentExerciseState.value.currentExerciseOngoingRecord

                    if (record == null) {
                        // There is a problem
                        Log.e("WorkoutViewModel", "Tried to edit a record that does not exist")
                        return@launch
                    }
                    val reps = record.reps.toMutableList()
                    val weights = record.weights.toMutableList()
                    reps[event.set] = event.reps
                    weights[event.set] = event.weight
                    repository.addExerciseRecord(
                        ExerciseRecord(
                            recordId = record.recordId,
                            extExerciseId = record.extExerciseId,
                            extWorkoutId = record.extWorkoutId,
                            extWorkoutExerciseId = record.extWorkoutExerciseId,
                            exerciseInWorkout = record.exerciseInWorkout,
                            date = record.date,
                            reps = reps,
                            weights = weights,
                            variation = record.variation,
                            variationResKey = record.variationResKey,
                            rest = record.rest,
                            tare = record.tare,
                            overriddenDurationBased = record.overriddenDurationBased,
                            setTypes = record.setTypes
                        )
                    )
                }
            }
            is WorkoutEvent.DeleteSetRecord -> {
                viewModelScope.launch {
                    val record = currentExerciseState.value.currentExerciseOngoingRecord

                    if (record == null) {
                        // There is a problem
                        Log.e("WorkoutViewModel", "Tried to edit a record that does not exist")
                        return@launch
                    }
                    val reps = record.reps.toMutableList()
                    val weights = record.weights.toMutableList()
                    if (event.set >= reps.size || event.set >= weights.size) {
                        Log.e("WorkoutViewModel", "Tried to delete a set that does not exist")
                        return@launch
                    }
                    reps.removeAt(event.set)
                    weights.removeAt(event.set)
                    if (reps.isEmpty()) {
                        // delete record instead
                        repository.deleteExerciseRecord(record.recordId)
                    } else {
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                recordId = record.recordId,
                                extExerciseId = record.extExerciseId,
                                extWorkoutId = record.extWorkoutId,
                                extWorkoutExerciseId = record.extWorkoutExerciseId,
                                exerciseInWorkout = record.exerciseInWorkout,
                                date = record.date,
                                reps = reps,
                                weights = weights,
                                variation = record.variation,
                                variationResKey = record.variationResKey,
                                rest = record.rest,
                                tare = record.tare,
                                overriddenDurationBased = record.overriddenDurationBased,
                                setTypes = record.setTypes
                            )
                        )
                    }
                }
            }
            is WorkoutEvent.AcceptSuggestedModification -> {
                acceptModification(event.exerciseInWorkout)
            }
            is WorkoutEvent.ResetWaitingForExerciseAdd -> {
                /*
                This is used to check if user added an exercise to the workout; flow is:
                - user taps add and navigates to adding exercise
                - observeAddingExerciseJob checks if db gets updated
                - once screen is resumed, ResetWaitingForExerciseAdd gets called
                - wait for db to write and possibly observeAddingExerciseJob to finish
                    - if doesn't finish in 2000ms we assume the user did not add an exercise
                      (and just went back), thus terminate job
                 */
                viewModelScope.launch {
                    // FIXME: currently waiting for db to update but can be variable
                    //  should change overall logic to navigating back with result
                    delay(2000)
                    observeAddingExerciseJob?.cancel()
                    observeAddingExerciseJob = null
                }
            }
            is WorkoutEvent.AddExercise -> {
                observeAddingExerciseJob?.cancel()
                val ex = pagesContent.value.exercises.getOrNull(event.exerciseInWorkout)
                observeAddingExerciseJob = viewModelScope.launch {
                    val pendingModification = ex?.let {
                        WorkoutRecord.WorkoutModification(
                            sourceProgramExerciseId = it.extProgramExerciseId,
                            sourceExerciseId = it.extExerciseId,
                            sourceWorkoutExerciseId = it.workoutExerciseId,
                            targetWorkoutExerciseId = null,
                            targetExerciseId = null,
                            modificationType = WorkoutRecord.ModificationType.EXERCISE_ADDED
                        )
                    }
                    // wait for exercise to be added to possible scroll position
                    pagesContent.mapNotNull { stateFlow ->
                        if (stateFlow.exercises.size > event.originalSize) {
                            stateFlow.exercises.getOrNull(event.exerciseInWorkout+1)
                        } else {
                            null
                        }
                    }
                    .first()
                    .let { exercise ->
                        _effects.trySend(
                            WorkoutEffect.AdvancePage(
                                event.exerciseInWorkout+1
                            )
                        )
                        pendingModification?.let {
                            workoutModifications.add(
                                it.copy(
                                    targetWorkoutExerciseId = exercise.workoutExerciseId,
                                    targetExerciseId = exercise.extExerciseId
                                )
                            )
                        }

                    }

                }
            }
            is WorkoutEvent.ReplaceExercise -> {
                observeAddingExerciseJob?.cancel()
                val ex = pagesContent.value.exercises.getOrNull(event.exerciseInWorkout)
                // wait for exercise to be added, then remove old one
                observeAddingExerciseJob = viewModelScope.launch {
                    val pendingModification = ex?.let {
                        WorkoutRecord.WorkoutModification(
                            sourceProgramExerciseId = it.extProgramExerciseId,
                            sourceExerciseId = it.extExerciseId,
                            sourceWorkoutExerciseId = it.workoutExerciseId,
                            targetWorkoutExerciseId = null,
                            targetExerciseId = null,
                            modificationType = WorkoutRecord.ModificationType.EXERCISE_REPLACED
                        )
                    }
                    pagesContent.mapNotNull { stateFlow ->
                            if (stateFlow.exercises.size > event.originalSize) {
                                stateFlow.exercises.last()
                            } else {
                                null
                            }
                        }
                        .first() // Get the first non-null emission (meaning the condition is met)
                        .let { newEx ->
                            // Now that the condition is met, perform your repository operations
                            repository.deleteWorkoutExercise(
                                pagesContent.value.exercises[event.exerciseInWorkout].workoutExerciseId
                            )
                            repository.updateWorkoutExerciseNumber(
                                WorkoutExerciseReorder(
                                    newEx.workoutExerciseId,
                                    event.exerciseInWorkout
                                )
                            )
                            pendingModification?.let {
                                workoutModifications.add(
                                    it.copy(
                                        targetWorkoutExerciseId = newEx.workoutExerciseId,
                                        targetExerciseId = newEx.extExerciseId
                                    )
                                )
                            }
                        }
                }
            }
            is WorkoutEvent.RemoveExercise -> {
                val ex = pagesContent.value.exercises.getOrNull(event.exerciseInWorkout)
                viewModelScope.launch {
                    if (ex == null) {
                        Log.w("WorkoutViewModel", "Tried to remove an exercise that does not exist")
                        return@launch
                    }
                    workoutModifications.add(
                        WorkoutRecord.WorkoutModification(
                            sourceProgramExerciseId = ex.extProgramExerciseId,
                            sourceExerciseId = ex.extExerciseId,
                            sourceWorkoutExerciseId = ex.workoutExerciseId,
                            targetWorkoutExerciseId = null,
                            targetExerciseId = null,
                            modificationType = WorkoutRecord.ModificationType.EXERCISE_SKIPPED
                        )
                    )
                    repository.deleteWorkoutExercise(
                        ex.workoutExerciseId
                    )
                    repository.shiftWorkoutExercisesToLeft(
                        workoutState.value.workoutId,
                        event.exerciseInWorkout
                    )
                }
            }
            is WorkoutEvent.ToggleOtherEquipmentDialog -> {
                _workoutState.update { it.copy(
                    otherEquipmentDialogOpen = !workoutState.value.otherEquipmentDialogOpen
                ) }
            }

            is WorkoutEvent.UpdateCurrentPage -> {
                pageChangeJob?.cancel()

                // Debounce rapid page changes (e.g., during fast swiping)
                pageChangeJob = viewModelScope.launch {
                    delay(50) // 50ms debounce - adjust as needed
                    if (event.currentPage != _currentPage.value) {
                        _currentPage.update { event.currentPage }
                    }
                }
            }
            is WorkoutEvent.RefreshHasPromptedNotificationsAccess -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    _workoutState.update {
                        it.copy(
                            canPostPromotedNotifications = notificationService.canPostPromotedNotifications()
                        )
                    }
                }
            }
        }
    }

    private fun acceptModification(index: Int) {
        val modification = pagesContent.value.modificationsSuggestions.getOrNull(
            index
        )
        if (modification == null) {
            Log.e("WorkoutViewModel", "Tried to accept a modification that does not exist with index: $index")
            return
        }
        if (
            modification.type != WorkoutRecord.ModificationType.EXERCISE_SKIPPED &&
            modification.newWorkoutExercise == null
        ) {
            Log.e("WorkoutViewModel", "Tried to accept a modification ${modification.type} but newWorkoutExercise is null")
            return
        }

        val sourceEx = pagesContent.value.exercises.getOrNull(
            index
        )
        if (sourceEx == null) {
            Log.e("WorkoutViewModel", "Tried to modify from a workout exercise that does not exist")
            return
        }
        // when adding/replacing
        var targetWorkoutExerciseId: Long? = null
        viewModelScope.launch {
            when (modification.type) {
                WorkoutRecord.ModificationType.EXERCISE_ADDED -> {
                    repository.shiftWorkoutExercisesToRight(
                        workoutState.value.workoutId,
                        index+1
                    )
                    targetWorkoutExerciseId = repository.addWorkoutExercise(
                        modification.newWorkoutExercise!!.copy(
                            workoutExerciseId = 0L,
                            extWorkoutId = workoutState.value.workoutId,
                            orderInProgram = index+1
                        )
                    )
                    _effects.trySend(
                        WorkoutEffect.AdvancePage(
                            index+1
                        )
                    )
                }

                WorkoutRecord.ModificationType.EXERCISE_REPLACED -> {
                    repository.deleteWorkoutExercise(sourceEx.workoutExerciseId)
                    targetWorkoutExerciseId = repository.addWorkoutExercise(
                        modification.newWorkoutExercise!!.copy(
                            workoutExerciseId = 0L,
                            extWorkoutId = workoutState.value.workoutId,
                            orderInProgram = index
                        )
                    )
                }
                WorkoutRecord.ModificationType.EXERCISE_SKIPPED -> {
                    repository.deleteWorkoutExercise(sourceEx.workoutExerciseId)
                    repository.shiftWorkoutExercisesToLeft(
                        workoutState.value.workoutId,
                        index
                    )
                }
            }
            _workoutState.update {
                it.copy(
                    lastWorkoutModifications = it.lastWorkoutModifications.minus(
                        modification.originalModification
                    )
                )
            }
            workoutModifications.add(
                modification.originalModification.copy(
                    sourceWorkoutExerciseId = sourceEx.workoutExerciseId,
                    targetWorkoutExerciseId = targetWorkoutExerciseId,
                )
            )
        }
    }

    private fun getUpdatedRecords(
        currentExercise: WorkoutExercise?,
        records: Map<Long, List<ExerciseRecordAndEquipment>>,
        currentPage: Int,
        workoutId: Long
    ): Pair<List<ExerciseRecordAndEquipment>, ExerciseRecordAndEquipment?> {
        if (currentExercise == null) {
            return emptyList<ExerciseRecordAndEquipment>() to null
        }

        val currentExerciseRecords = records[currentExercise.extExerciseId] ?: emptyList()

        // record being set right now for current exercise
        val ongoingRecord = currentExerciseRecords.find {
            it.extWorkoutId == workoutId && it.extWorkoutExerciseId == currentExercise.workoutExerciseId
        }

        // records for current exercise minus ongoingRecord
        val recordsToDisplay = if (ongoingRecord != null)
            currentExerciseRecords.minus(ongoingRecord).sortedByDescending { it.date }
        else
            currentExerciseRecords.sortedByDescending { it.date }

        return recordsToDisplay to ongoingRecord
    }

    private fun computeNextWeight(
        recordsToDisplay: List<ExerciseRecordAndEquipment>,
        ongoingLastWeight: Float?,
        setsDone: Int,
        barbellDiff: Float,
        imperialSystem: Boolean,
        setTypes: List<SetType>?,
        isUpcomingSet: Boolean, // true if is the very next set user will do
        isPyramid: Boolean, // true if pyramid exercise e.g., reps = 2, 4, 6 (or reverse)
        increment: Float,
    ): Float? {
        /*
         weight is taken in this order:
         0. If first set (or first set of a new set type), take from last record
         0a. If not pyramid and normal set, will average over the weight of old records;
             and take ceil following default increment
         1. If not first set, check last set weight:
         1a. If equals to the same set from last record, take from last record
         2. Otherwise, check whether the weight also changed between sets in last record (e.g., pyramid)
         2a. If not, keep weight from previous set
         3. Otherwise, take last record increased/decreased by same amount as previous set
         */

        // do not propagate ongoing weight across set type boundaries (e.g., warmup -> normal)
        val previousSetType = setTypes?.getOrNull(setsDone - 1) ?: SetType.NORMAL
        val currentSetType = setTypes?.getOrNull(setsDone) ?: SetType.NORMAL
        val setTypeBoundary = setsDone > 0 && previousSetType != currentSetType

        // this is the record of the last record before current workout
        val lastOldRecord = recordsToDisplay.firstOrNull()

        var weightCandidate: Float?
        var oldRecordWeightCurrentSet: Float? = null
        var oldRecordWeightPreviousSet: Float? = null
        var ongoingRecordWeightPreviousSet: Float? = null
        var avgOldRecordWeight: Float? = null
        // for the weight, try to copy from last old record
        if (lastOldRecord != null) {
            oldRecordWeightCurrentSet = lastOldRecord.weights.getOrNull(setsDone)?.let {
                maybeKgToLb(
                    it,
                    imperialSystem
                )
            }
            oldRecordWeightPreviousSet = lastOldRecord.weights.getOrNull(setsDone-1)?.let {
                maybeKgToLb(
                    it,
                    imperialSystem
                )
            }
            if (currentSetType == SetType.NORMAL && !isPyramid) {
                lastOldRecord.weights.zip(
                    lastOldRecord.setTypes ?: List(lastOldRecord.weights.size) { SetType.NORMAL }
                ).filter { it.second == SetType.NORMAL }.map { it.first }.average().let {
                    avgOldRecordWeight = it.toFloat()
                    // now round it to the nearest increment
                    if (avgOldRecordWeight % increment != 0f) {
                        avgOldRecordWeight += increment - (avgOldRecordWeight % increment)
                    }
                }
            }
        }
        if (ongoingLastWeight != null) {
            ongoingRecordWeightPreviousSet = ongoingLastWeight
        }
        if (setsDone == 0 || setTypeBoundary) {
            weightCandidate = avgOldRecordWeight ?: oldRecordWeightCurrentSet
        } else if (ongoingRecordWeightPreviousSet != null) {
            weightCandidate = ongoingRecordWeightPreviousSet
        } else {
            weightCandidate = avgOldRecordWeight ?: oldRecordWeightCurrentSet
        }

        // I am not particularly convinced by this 'delta' logic. For now, let's comment it out
//        } else if (oldRecordWeightCurrentSet != null && oldRecordWeightPreviousSet != oldRecordWeightCurrentSet) {
//            var delta = oldRecordWeightPreviousSet?.let { ongoingRecordWeightPreviousSet?.minus(it) }
//            weightCandidate = oldRecordWeightCurrentSet.plus(delta ?: 0f)

        if (isUpcomingSet) {
            weightCandidate = weightCandidate?.plus(barbellDiff / 2)
        }
        return weightCandidate
    }

    fun computeNextRep(
        currentExercise: WorkoutExercise,
        recordsToDisplay: List<ExerciseRecordAndEquipment>,
        ongoingLastRep: Int?,
        setsDone: Int
    ): Int? {
        val setTypes = currentExercise.setTypes

        // do not propagate ongoing reps across set type boundaries (e.g., warmup -> normal)
        val previousSetType = setTypes?.getOrNull(setsDone - 1) ?: SetType.NORMAL
        val currentSetType = setTypes?.getOrNull(setsDone) ?: SetType.NORMAL
        val setTypeBoundary = setsDone > 0 && previousSetType != currentSetType

        val lastOldRecord = recordsToDisplay.firstOrNull()
        val oldRecordRepCurrentSet = lastOldRecord?.reps?.getOrNull(setsDone)
        val oldRecordRepPreviousSet = lastOldRecord?.reps?.getOrNull(setsDone - 1)

        // reps user should do per program
        val upcomingReps = currentExercise.reps.getOrNull(setsDone)

        if (setsDone == 0 || setTypeBoundary) {
            // first set (or first set of a new type): prefer what user actually did last time
            return oldRecordRepCurrentSet ?: upcomingReps
        }

        // reps done last set
        val lastRepsDone = ongoingLastRep
        val lastRepsThatShouldHaveBeenDone = currentExercise.reps.getOrNull(setsDone - 1)
        if (oldRecordRepPreviousSet == lastRepsDone) {
            // user is on track with history: follow historical reps for current set
            return oldRecordRepCurrentSet ?: upcomingReps
        }
        if (lastRepsThatShouldHaveBeenDone == lastRepsDone) {
            // user is following program
            return upcomingReps
        }
        if (lastRepsThatShouldHaveBeenDone == upcomingReps) {
            // user is not following the program and is in a situation like this:
            // (reps to be done -> reps done)
            // set 1: 10 -> 11
            // set 2: 10 -> ??
            // here, we suggest 11
            return lastRepsDone
        }
        // here, additional heuristics can be added. For now, we fall back to the program
        return upcomingReps
    }

    fun computeExerciseTare(
        recordsToDisplay: List<ExerciseRecordAndEquipment>,
        ongoingRecord: ExerciseRecordAndEquipment?
    ): Float? {
        // heuristic: tare is taken from previous set if available, otherwise from previous record


        // this is the record of the last record before current workout
        val lastOldRecord = recordsToDisplay.firstOrNull()

        var tareCandidate: Float? = null
        if (lastOldRecord != null) {
            tareCandidate = lastOldRecord.tare
        }
        if (ongoingRecord != null) {
            tareCandidate = ongoingRecord.tare
        }
        return tareCandidate
    }

    private fun startRetrievingExercises() {
        // should not called more than once
        if (retrieveExercisesJob != null) {
            Log.w("WorkoutViewModel", "Tried to start retrieving exercises more than once")
            return
        }
        retrieveExercisesJob = viewModelScope.launch {
            repository.getWorkoutExercises(workoutState.value.workoutId).collect{ exs ->
                val sortedExs = exs.sortedBy { it.orderInProgram }
                _workoutExercises.update { sortedExs }
                retrieveExercisesRecords?.cancel()
                retrieveExercisesRecords = this.launch {
                    repository.getExerciseRecordsAndEquipment(
                        sortedExs.map { it.extExerciseId }
                    ).collect { records ->
                        val allRecords = records.groupBy { it.extExerciseId }
                        // TODO: sort by date before putting in
                        _allRecords.update { allRecords }
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.getWorkoutExerciseRecordsAndInfo(workoutState.value.workoutId).collect { exs ->
                _workoutState.update { it.copy(hasRecordedExercise = exs.isNotEmpty()) }
            }
        }
    }

    private fun startWorkout() {
        if (workoutState.value.startDate == null) {
            startWorkoutJob = viewModelScope.launch {
                val currentDateTime = ZonedDateTime.now()
                // possibly blocking
                val workoutId = workoutState.mapNotNull {
                    if (it.workoutId != 0L) it.workoutId else null
                }.first()
                repository.startWorkout(
                    WorkoutRecordStart(
                        workoutId = workoutId,
                        startDate = currentDateTime
                    )
                )
                preferences.setCurrentWorkout(workoutId)

                val autoOpenWear = workoutState.mapNotNull { it.autoOpenWear }.first()
                if (autoOpenWear) {
                    repository.openWearWorkout()
                }
                _workoutState.update {
                    it.copy(
                        startDate = currentDateTime,
                        workoutStarted = true
                    )
                }
            }
        }
    }

    private suspend fun resumeWorkout() {
        val workoutId = preferences.getCurrentWorkout().first()
        if (workoutId != null) {
            _workoutState.update {
                it.copy(
                    workoutId = workoutId
                )
            }
            val workout = repository.getWorkoutRecord(
                workoutId
            ).first()
            _workoutState.update {
                it.copy(
                    startDate = workout.startDate,
                    programId = workout.extProgramId,
                    workoutStarted = true,
                    workoutId = workoutId
                )
            }
            val autoOpenWear = workoutState.mapNotNull { it.autoOpenWear }.first()
            if (autoOpenWear) {
                repository.openWearWorkout()
            }
            // set flag to scroll to last exercise completed when records are available
            resumeAndScrollToLatest = true
        } else {
            Log.e(
                "WorkoutViewModel",
                "Tried to resume workout but current workout id is null."
            )
            _effects.trySend(
                WorkoutEffect.ShowErrorAndBack(
                    R.string.resume_workout_fail
                )
            )
        }
    }

    private suspend fun inferProgramId(): Long? {
        Log.d("WorkoutViewModel", "Inferring program id")
        val currentPlanId = preferences.getCurrentPlan().first()
        if (currentPlanId == null) {
            Log.e("WorkoutViewModel", "Tried to auto start workout but current plan is null.")
            _effects.trySend(WorkoutEffect.ShowErrorAndBack(R.string.autostart_workout_failed))
            return null
        }
        val currentPlan = repository.getPlan(currentPlanId).first()
        val programs = repository.getPrograms(currentPlanId).first()
        if (programs.isEmpty()) {
            Log.e("WorkoutViewModel", "Tried to auto start workout but current plan has no programs.")
            _effects.trySend(WorkoutEffect.ShowErrorAndBack(R.string.autostart_workout_failed))
            return null
        }
        val upcomingProgram = programs[min(
            currentPlan?.currentProgram ?: (programs.size - 1), programs.size-1)]
        Log.d("WorkoutViewModel", "Inferred program id: ${upcomingProgram.programId}")
        return upcomingProgram.programId
    }

    private fun completeSet(
        exercise: WorkoutExercise?,
        exerciseRest: Long,
        restTimestamp: ZonedDateTime?,
        reps: Int,
        weight: Float,
        tare: Float? = null
    ) {
        if (completeSetJob?.isActive == true)
            return
        completeSetJob = viewModelScope.launch {
            _currentExerciseState.update {
                it.copy(
                    restTimestamp = restTimestamp ?: ZonedDateTime.now().plusSeconds(exerciseRest),
                    currentExerciseRest = exerciseRest
                )
            }
            if (exercise == null) {
                Log.e("WorkoutViewModel", "Tried to complete a set but something went wrong")
                _effects.trySend(WorkoutEffect.ShowMessage(R.string.complete_set_generic_error))
                return@launch
            }
            val exerciseIndex = pagesContent.value.exercises.indexOf(exercise)
            var oldTare = tare ?: workoutState.value.tares.getOrNull(exerciseIndex) ?: 0f
            val record = pagesContent.value.ongoingRecords.getOrNull(exerciseIndex)
            // when first set completed, we need to create the record
            if (record == null) {
                if (exercise.equipment == Equipment.BODY_WEIGHT)
                    oldTare = preferences.getUserWeight().first()
                repository.addExerciseRecord(
                    ExerciseRecord(
                        extWorkoutId = workoutState.value.workoutId,
                        extExerciseId = exercise.extExerciseId,
                        extWorkoutExerciseId = exercise.workoutExerciseId,
                        exerciseInWorkout = exerciseIndex,
                        date = ZonedDateTime.now(),  // FIXME? if completed from watch, this may be a few secs off
                        reps = listOf(reps),
                        weights = listOf(
                            maybeLbToKg(weight, pagesContent.value.imperialSystem)
                        ),
                        variation = exercise.variation,
                        variationResKey = exercise.variationResKey,
                        rest = listOf(exerciseRest.toInt()),
                        tare = oldTare,
                        overriddenDurationBased = exercise.overriddenDurationBased,
                        setTypes = exercise.setTypes
                    )
                )
            } else {
                // update exercise record with new set
                repository.addExerciseRecord(
                    ExerciseRecord(
                        recordId = record.recordId,
                        extExerciseId = record.extExerciseId,
                        extWorkoutId = record.extWorkoutId,
                        extWorkoutExerciseId = record.extWorkoutExerciseId,
                        exerciseInWorkout = record.exerciseInWorkout,
                        date = record.date,
                        reps = record.reps.plus(reps),
                        weights = record.weights.plus(
                            maybeLbToKg(weight, pagesContent.value.imperialSystem)
                        ),
                        variation = record.variation,
                        variationResKey = record.variationResKey,
                        record.rest.plus(exerciseRest.toInt()),
                        tare = oldTare,  // allow user to change the initial tare, in case they selected wrong one
                        overriddenDurationBased = record.overriddenDurationBased,
                        setTypes = record.setTypes
                    )
                )
            }
        }
    }

    private fun checkWorkoutDataChangesForWear() {
        viewModelScope.launch {
            combine(
                workoutState.map { it.workoutId }.distinctUntilChanged(),
                workoutState.map { it.startDate }.distinctUntilChanged(),
                pagesContent.map { it.exercises }.distinctUntilChanged(),
                pagesContent.map { it.suggestedTares }.distinctUntilChanged(),
                workoutState.map { it.incrementBarbell }.distinctUntilChanged(),
                workoutState.map { it.incrementBodyweight }.distinctUntilChanged(),
                workoutState.map { it.incrementCable }.distinctUntilChanged(),
                workoutState.map { it.incrementDumbbell }.distinctUntilChanged(),
                workoutState.map { it.incrementMachine }.distinctUntilChanged(),
                workoutState.map { it.imperialSystem }.distinctUntilChanged(),
                workoutState.map { it.lastWorkoutIntensity }.distinctUntilChanged(),
                pagesContent.map { it.modificationsSuggestions }.distinctUntilChanged()
            ) { values: Array<Any?> ->
                val workoutId = values[0] as Long
                val startDate = values[1] as ZonedDateTime?
                val exercises = values[2] as List<WorkoutExercise>
                val suggestedTares = values[3] as List<Float>
                val incrementBarbell = values[4] as Float
                val incrementBodyweight = values[5] as Float
                val incrementCable = values[6] as Float
                val incrementDumbbell = values[7] as Float
                val incrementMachine = values[8] as Float
                val imperialSystem = values[9] as Boolean
                val lastWorkoutIntensity = values[10] as Float?
                val suggestedModifications = values[11] as List<ModificationSuggestion?>

                val startDateTimestamp = startDate.toProtoTimestamp()
                wearWorkoutStatic.urgentUpdateData { _ ->
                    Workout.WorkoutStaticData.newBuilder()
                        .setWorkoutId(workoutId)
                        .setStartDate(startDateTimestamp)
                        .addAllExercises(exercises.map { it.toProto() })
                        .addAllSuggestedTares(suggestedTares)
                        .setDefaultIncrements(
                            Workout.DefaultIncrements.newBuilder()
                                .setBarbell(incrementBarbell)
                                .setBodyweight(incrementBodyweight)
                                .setCable(incrementCable)
                                .setDumbbell(incrementDumbbell)
                                .setMachine(incrementMachine)
                                .build()
                        )
                        .setImperialSystem(imperialSystem)
                        .setActiveWorkout(true)
                        .setPreviousIntensity(lastWorkoutIntensity ?: -1f)
                        .addAllSuggestedModifications(
                            suggestedModifications.map {
                                it?.toProto() ?: Workout.ProtoSuggestedModification.newBuilder()
                                    .setHasSuggestion(false)
                                    .build()
                            }
                        )
                        .build()
                }
            }.collect()
        }
        viewModelScope.launch {
            combine(
                pagesContent.map { it.exerciseRepsWeightRows }.distinctUntilChanged(),
                pagesContent.map { it.exerciseSetsDone }.distinctUntilChanged()
            ) { repsWeightRows, setsDone ->
                wearWorkoutDynamic.urgentUpdateData { _ ->
                    Workout.WorkoutDynamicData.newBuilder()
                        .addAllSuggestedRepsWeight(
                            repsWeightRows.map { repsWeights ->
                                Workout.SuggestedRepsWeight.newBuilder()
                                    .addAllReps(repsWeights.map {
                                        it.projectedReps ?: it.reps
                                    })
                                    .addAllWeight(repsWeights.map {
                                        it.projectedWeight ?: it.weight
                                    })
                                    .build()
                            }
                        )
                        .addAllSetsDone(setsDone)
                        .build()
                }
            }.collect()
        }
        viewModelScope.launch {
            pagesContent.map { it.exercises }.map { it.map{ it.image } }.distinctUntilChanged().collect {
                images ->
                wearWorkoutImages.updateData {
                    images.map { repository.getBitmapFromResId(it) }
                }
            }
        }
    }

    private suspend fun addSetToExercise(exercise: WorkoutExercise) {
        repository.updateWorkoutExerciseSets(
            WorkoutExerciseUpdateSets(
                exercise.workoutExerciseId,
                exercise.reps.plus(exercise.reps.last()),
                exercise.rest.plus(exercise.rest.last())
            )
        )
    }

    private fun observeSetCompletionsFromWear() {
        viewModelScope.launch {
            for (setCompletion in phoneWorkoutRepository.setCompletions) {
                if (setCompletion.workoutId != workoutState.value.workoutId) {
                    Log.e("WorkoutViewModel", "Received set completion from wear for wrong workout")
                    continue
                }
                if (workoutState.value.startDate == null) {
                    // user completed set from watch before starting workout
                    startWorkout()
                    // StartWorkout is async, need to wait for it to finish
                    startWorkoutJob?.join()
                }
                if (setCompletion.reps > 0) {
                    val exerciseIndex =
                        pagesContent.value.exercises.indexOfFirst { it.workoutExerciseId == setCompletion.exerciseId }
                    if (exerciseIndex == -1) {
                        Log.e(
                            "WorkoutViewModel",
                            "Tried to complete a set from watch with no current exercise"
                        )
                        _effects.trySend(
                            WorkoutEffect.ShowMessage(R.string.complete_set_from_watch_fail)
                        )
                        continue
                    }
                    val exercise = pagesContent.value.exercises.getOrNull(exerciseIndex)
                    if (exercise == null) {
                        // Should never happen, but just in case...
                        Log.e(
                            "WorkoutViewModel",
                            "Tried to complete a set from watch with no current exercise set"
                        )
                        _effects.trySend(
                            WorkoutEffect.ShowMessage(R.string.complete_set_from_watch_fail)
                        )
                        continue
                    }

                    val setsDone = pagesContent.value.exerciseSetsDone.getOrNull(exerciseIndex) ?: 0
                    if (setsDone >= exercise.rest.size) {
                        // user has done all sets and is adding another one from watch
                        addSetToExercise(exercise)
                    }


                    val nextExercise = pagesContent.value.exercises.getOrNull(
                        exerciseIndex + 1
                    )
                    val prevExercise = pagesContent.value.exercises.getOrNull(
                        exerciseIndex - 1
                    )
                    // if sets done == total sets, scroll to next if any
                    val totalSets = exercise.reps.size
                    val shouldAdvanceEx =
                        setsDone == totalSets - 1 && (exerciseIndex + 1 < pagesContent.value.exercises.size)
                    // check if superset
                    val exerciseToScrollTo =
                        if (prevExercise != null && exercise.supersetExercise == prevExercise.extProgramExerciseId) {
                            if (shouldAdvanceEx)
                                exerciseIndex + 1
                            else
                                exerciseIndex - 1
                        } else if (nextExercise != null && exercise.supersetExercise == nextExercise.extProgramExerciseId) {
                            exerciseIndex + 1
                        } else if (shouldAdvanceEx) {
                            exerciseIndex + 1
                        } else
                            exerciseIndex
                    if (exerciseToScrollTo != _currentPage.value) {
                        // index of exercise completed from watch is different than exercise currently
                        // being shown to user, scroll to that index
                        _effects.trySend(
                            WorkoutEffect.AdvancePage(exerciseToScrollTo)
                        )
                    }
                    completeSet(
                        exercise = exercise,
                        exerciseRest = setCompletion.rest,
                        restTimestamp = setCompletion.restTimestamp.toZonedDateTime(),
                        reps = setCompletion.reps,
                        // weight from watch is in lb
                        weight = setCompletion.weight,
                        tare = if (setCompletion.tare != 0f)
                        /*maybeLbToKg(*/ setCompletion.tare/*, pagesContent.value.imperialSystem)*/
                        else null
                    )
                } else {
                    Log.d(
                        "WorkoutViewModel",
                        "Tried to complete a set from watch with no or negative reps"
                    )
                }
            }
        }
    }

    private fun observeWorkoutCompletionsFromWear() {
        viewModelScope.launch {
            for (workout in phoneWorkoutRepository.workoutCompletions) {
                if (workoutState.value.startDate == null) {
                    // user completed workout from watch before starting workout, weird but anyway
                    startWorkout()
                    // StartWorkout is async, need to wait for it to finish
                    startWorkoutJob?.join()
                }
                finishWorkout(
                    intensity = workout.intensity,
                    calories = if (workout.calories != 0f) workout.calories else null,
                    maxHeartRate = if (workout.maxHeartRate != 0) workout.maxHeartRate else null,
                    minHeartRate = if (workout.minHeartRate != 0) workout.minHeartRate else null,
                    avgHeartRate = if (workout.avgHeartRate != 0) workout.avgHeartRate else null,
                    heartRates = if (workout.heartRatesCount > 0) workout.heartRatesList.toList() else null
                )
            }
        }
    }


    private fun observeAcceptedModificationsFromWear() {
        viewModelScope.launch {
            for (modification in phoneWorkoutRepository.acceptedModifications) {
                acceptModification(modification)
            }
        }
    }

    private suspend fun finishWorkout(
        intensity: Float,
        calories: Float? = null,
        maxHeartRate: Int? = null,
        minHeartRate: Int? = null,
        avgHeartRate: Int? = null,
        heartRates: List<Int>? = emptyList()
    ) {
        val exercises = repository.getWorkoutExerciseRecordsAndInfo(workoutState.value.workoutId).first().distinct()
        // add all exercises with no records to modifications as skipped
        val skippedExs = pagesContent.value.exercises.filter { original ->
            exercises.find { it.extWorkoutExerciseId == original.workoutExerciseId } == null
        }
        workoutModifications.addAll(
            skippedExs.map {
                WorkoutRecord.WorkoutModification(
                    sourceProgramExerciseId = it.extProgramExerciseId,
                    sourceExerciseId = it.extExerciseId,
                    sourceWorkoutExerciseId = it.workoutExerciseId,
                    targetWorkoutExerciseId = null,
                    targetExerciseId = null,
                    modificationType = WorkoutRecord.ModificationType.EXERCISE_SKIPPED
                )
            }
        )
        val workoutTimeMillis = currentExerciseState.value.currentTime.toInstant().toEpochMilli() - workoutState.value.startDate!!.toInstant().toEpochMilli()
        val workoutTimeSeconds = workoutTimeMillis / 1000
        // intensity is 0-100, need to convert within reasonable met values
        val intensityMet = getMetFromIntensity(intensity)
        repository.completeWorkoutRecord(
            WorkoutRecordFinish(
                workoutId = workoutState.value.workoutId,
                intensity = WorkoutRecord.WorkoutIntensity.NORMAL_INTENSITY, // deprecated
                intensityPercent = intensity,
                durationSeconds = workoutTimeSeconds,
                volume = exercises.sumOf {
                    computeVolume(
                        it.weights,
                        it.reps,
                        it.tare,
                        it.equipment
                    ).toDouble() },
                activeTimeSeconds = max(0L, workoutTimeSeconds -
                        exercises.sumOf { it.rest.sum() }),
                calories = calories ?: (intensityMet *
                        preferences.getUserWeight().first() *
                        workoutTimeSeconds / 3600),
                workoutModifications = workoutModifications,
                heartRates = heartRates,
                maxHeartRate = maxHeartRate,
                minHeartRate = minHeartRate,
                avgHeartRate = avgHeartRate
            )
        )
        val planPrograms = repository.getPlanMapPrograms().first().entries.find {
            it.value.find { it1 -> it1.programId == workoutState.value.programId } != null
        }!!
        val currentProgram = planPrograms.value.find {
            it.programId == workoutState.value.programId
        }!!
        /*
        scenario: user does not do the upcoming workout, does another one instead
            Now, after he finishes, should the next workout be the old upcoming one
            or the one following the workout the user actually do?

            Currently the latter
         */
        repository.updateCurrentPlan(WorkoutPlanUpdateProgram(
            planId = planPrograms.key.planId,
            currentProgram = (currentProgram.orderInWorkoutPlan+1) % planPrograms.value.size
        ))
        preferences.setCurrentWorkout(null)
        _effects.trySend(WorkoutEffect.ShutDown)
    }

    private fun startTimer(){
        timerJob?.cancel(CancellationException("Duplicate call"))
        timerJob = flow {
            var counter = 0
            while (true) {
                emit(counter++)
                delay(500)
            }
        }.onEach {
            _currentExerciseState.update { it.copy(currentTime = ZonedDateTime.now()) }
            if (workoutState.value.startDate != null) {
                val workoutTimeMillis = currentExerciseState.value.currentTime.toInstant().toEpochMilli() -
                        workoutState.value.startDate!!.toInstant().toEpochMilli()
                _currentExerciseState.update {
                    it.copy(
                        workoutTimeFormatted = DateUtils.formatElapsedTime(workoutTimeMillis / 1000)
                    )
                }
                if (
                    currentExerciseState.value.currentExercise != null
                    && currentExerciseState.value.restTimestamp != null
                ) {
                    val restTimeMillis = max(
                        0L,
                        currentExerciseState.value.restTimestamp?.toInstant()?.toEpochMilli()
                            ?.minus(
                                currentExerciseState.value.currentTime.toInstant()
                                    .toEpochMilli()
                            ) ?: 0L
                    )
                    val restTimeSecs = restTimeMillis / 1000  // convert from millis to secs
                    _currentExerciseState.update {
                        var rest = it.currentExerciseRest?.times(1000L)?.toFloat() ?: 1f
                        if (rest == 0f)
                            rest = 1f
                        it.copy(
                            restTimeSecs = restTimeSecs,
                            restProgress = restTimeMillis.toFloat().div(rest)
                        )
                    }
                } else {
                    _currentExerciseState.update {
                        it.copy(
                            restTimeSecs = null,
                            restProgress = 0f
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }
}

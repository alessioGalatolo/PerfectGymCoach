package agdesigns.elevatefitness.ui.screens.workout

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.NotificationService
import agdesigns.elevatefitness.data.WorkoutNotificationState
import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.data.db.entity.ExerciseRecord
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.db.entity.WorkoutExercise
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseReorder
import agdesigns.elevatefitness.data.db.entity.WorkoutExerciseUpdateSets
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanUpdateProgram
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordFinish
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordStart
import agdesigns.elevatefitness.utils.computeVolume
import agdesigns.elevatefitness.utils.getMetFromIntensity
import android.os.Build
import android.text.format.DateUtils
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.math.max
import com.agdesignes.shared.Equipment
import com.agdesignes.shared.maybeKgToLb
import com.agdesignes.shared.maybeLbToKg
import com.google.android.gms.wearable.PutDataMapRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import java.time.ZonedDateTime
import kotlin.Boolean
import kotlin.collections.firstOrNull
import kotlin.collections.last
import kotlin.math.min
import kotlin.text.toUIntOrNull


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


data class WorkoutPagesContent(
    val exercises: List<WorkoutExercise> = emptyList(),
    val exerciseRecords: List<List<ExerciseRecordAndEquipment>> = emptyList(),
    val ongoingRecords: List<ExerciseRecordAndEquipment?> = emptyList(),
    val exerciseRepsWeightRows: List<List<Pair<String, String>>> = emptyList(),  // values to display in each exercise set card
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
    val shutDown: Boolean = false,  // used when finishing workout, waits to save then exit
    val userTheme: Theme = Theme.SYSTEM,
    val incrementBodyweight: Float = 0f,
    val incrementBarbell: Float = 0f,
    val incrementDumbbell: Float = 0f,
    val incrementMachine: Float = 0f,
    val incrementCable: Float = 0f,
    val hasRecordedExercise: Boolean = false, // used to add a flag in cancel workout
    val autoStartFailed: Boolean = false,  // autoStart was not able to find a workout program, navigate up
    val lastWorkoutIntensity: Float? = null,
    val workoutId: Long = 0L,
    val imperialSystem: Boolean = false,
    val lockHorizontalScroll: Boolean = false,
    val autoOpenWear: Boolean? = null, // set as null as we want to wait for the actual first value
    // TODO: really not happy about this. Belongs to WorkoutPagesContent but it was not to be
    //  updated directly by the user by the tares are
    val tares: List<Float> = emptyList(),
    val canPostPromotedNotifications: Boolean = false
)

sealed class WorkoutEffect {
    data object NavigateBack: WorkoutEffect()
    data class ShowMessage(val message: Int): WorkoutEffect()
    data class ShowErrorAndBack(val message: Int): WorkoutEffect()
    data class AdvancePage(val page: Int): WorkoutEffect()
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

    data class ReplaceExercise(val exerciseInWorkout: Int, val originalSize: Int): WorkoutEvent()

    data class RemoveExercise(val exerciseInWorkout: Int): WorkoutEvent()

    data object AddSetToCurrentExercise: WorkoutEvent()

    data class UpdateExerciseProbability(val exerciseInWorkout: Int, val probability: Int): WorkoutEvent()

    data class UpdateReps(val newValue: String): WorkoutEvent()

    data class UpdateWeight(val newValue: String): WorkoutEvent()

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

    data class UpdateCurrentPage(val currentPage: Int) : WorkoutEvent()

    data object RefreshHasPromptedNotificationsAccess: WorkoutEvent()
}

@OptIn(InternalProperty::class, OutOfSyncProperty::class)
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository,
    private val notificationService: NotificationService
): ViewModel() {
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
    ) { exercises, records, imperialSystem, workoutId ->
        /**
         * Glocal stuff for pages, precompute all so that pager can cache stuff
         * Should be recomputed if exercises, records, imperialSystem, or workoutId changes
         * Should NOT be recomputed if the only change is current page
         */
        val recordsAndOngoingForAllExercises = exercises.mapIndexed { index, exercise ->
            getUpdatedRecords(exercise, records, index, workoutId)
        }
        val recordsForAllExercises = recordsAndOngoingForAllExercises.map { it.first }
        val exerciseSetsDone = recordsAndOngoingForAllExercises.map {
            it.second?.reps?.size ?: 0
        }
        val exerciseRepsWeights =
            exercises.zip(recordsAndOngoingForAllExercises).map { (exercise, records) ->
                val ongoingRecord = records.second
                val allRecords = records.first
                exercise.reps.mapIndexed { setCount, reps ->
                    val toBeDone =
                        (ongoingRecord?.reps?.size ?: 0) <= setCount  // setsDone <= setCount
                    val repsInRow: String
                    val weightInRow: String
                    if (toBeDone) {
                        repsInRow = reps.toString()
                        val currentRecord = allRecords.firstOrNull()
                        weightInRow =
                            if (currentRecord != null && setCount < currentRecord.weights.size) {
                                maybeKgToLb(
                                    currentRecord.weights[setCount],
                                    imperialSystem
                                ).toString()
                            } else if (currentRecord != null && ongoingRecord != null) {
                                maybeKgToLb(
                                    ongoingRecord.weights.last(),
                                    imperialSystem
                                ).toString()
                            } else {
                                "..."
                            }
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
                    repsInRow to weightInRow
                }
            }
        val exercisesTares = recordsAndOngoingForAllExercises.map {
            computeExerciseTare(
                it.first,
                it.second,
            ) ?: 0f
        }
        WorkoutPagesContent(
            exercises = exercises,
            exerciseRecords = recordsForAllExercises,
            exerciseRepsWeightRows = exerciseRepsWeights,
            exerciseSetsDone = exerciseSetsDone,
            suggestedTares = exercisesTares,
            imperialSystem = imperialSystem,
            workoutId = workoutId,
            ongoingRecords = recordsAndOngoingForAllExercises.map { it.second }
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

    override fun onCleared() {
        super.onCleared()
        repository.stopWearWorkout()
        timerJob?.cancel()
        notificationService.stop()
    }

    init {
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

        viewModelScope.launch {
            // check if user complete set from watch
            repository.getWatchSetCompletion().collect {
                val exerciseName = it.getString("exerciseName")
                val weight = it.getDouble("weight").toFloat()
                val reps = it.getInt("reps")
                val tare = it.getDouble("tare").toFloat()
                var exercise = currentExerciseState.value.currentExercise
                // FIXME: should find another way of checking this, strings may be slightly different
                if (exercise == null || !exerciseName.trim()
                        .startsWith(exercise.name.trim(), ignoreCase = true)
                ) {
                    Log.e(
                        "WorkoutViewModel",
                        "Exercise name does not match, $exerciseName != ${exercise?.name}"
                    )
                    _effects.trySend(
                        WorkoutEffect.ShowMessage(
                            R.string.complete_set_from_watch_fail
                        )
                    )
                    return@collect
                }
                if (workoutState.value.startDate == null) {
                    // user completed set from watch before starting workout
                    onEvent(WorkoutEvent.StartWorkout)
                    // StartWorkout is async, need to wait for it to finish
                    startWorkoutJob?.join()
                }
                // tare on watch can be lb/kg
                val tareKg = maybeLbToKg(tare, pagesContent.value.imperialSystem)
                // Need to store these in state otherwise TryCompleteSet may fail
                _currentExerciseState.update { state ->
                    state.copy(
                        repsBottomBar = reps.toString(),
                        weightBottomBar = weight.toString(),
                    )
                }
                _workoutState.update { state ->
                    val tares = state.tares.toMutableList()
                    tares[_currentPage.value] = tareKg
                    state.copy(
                        tares = tares
                    )
                }
                if (currentExerciseState.value.setsDone >= exercise.rest.size) {
                    // user has done all sets and is adding another one from watch
                    onEvent(WorkoutEvent.AddSetToCurrentExercise)
                    exercise = currentExerciseState.value.currentExercise
                }
                if (pagesContent.value.exerciseSetsDone[_currentPage.value] == exercise?.rest?.size?.minus(
                        1
                    )
                ) {
                    _effects.trySend(
                        WorkoutEffect.AdvancePage(_currentPage.value + 1)
                    )
                }
                onEvent(WorkoutEvent.CompleteSet)
            }
        }
        viewModelScope.launch {
            // check for sync requests
            repository.getSyncRequest().collect {
                Log.d("WorkoutViewModel", "Sync request received")
                // watch sent a sync request, can't be dead
                sendWorkout2Wear(sendImage = true, overrideDeadWatch = true)
            }
        }
        /*
          Compute stuff specific to current exercise (should be recomputed if any value changes)
         */
        viewModelScope.launch {
            combine(
                _currentPage,
                pagesContent
            ) { page, pagesContent ->
                val currentExercise = pagesContent.exercises.getOrNull(page)
                if (currentExercise == null) {
                    // last page, reset values
                    _currentExerciseState.update {
                        it.copy(
                            exerciseTitle = null,
                            currentExercise = null,  // TODO: does this create problems?
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
                    val repsToShow = currentExercise.reps.getOrNull(setsDone)?.toString()
                    val weightToShow = computeNextWeight(
                        recordsToDisplay,
                        currentExerciseOngoingRecord,
                        setsDone,
                        pagesContent.imperialSystem
                    )?.toString() ?: "0.0"
                    _currentExerciseState.update {
                        it.copy(
                            exerciseTitle = title,
                            repsBottomBar = repsToShow ?: it.repsBottomBar,
                            repsIsValid = true,
                            weightBottomBar = weightToShow,
                            weightIsValid = true,
                            currentExerciseOngoingRecord = currentExerciseOngoingRecord,
                            currentExercise = currentExercise,
                            setsDone = setsDone
                        )
                    }
                }
                sendWorkout2Wear(sendImage = true)
            }.collect()
        }
        viewModelScope.launch {
            pagesContent.map { it.suggestedTares }.distinctUntilChanged().collect {
                // We only want to use suggestedTare as init
                if (workoutState.value.tares.size != it.size) {
                    _workoutState.update { state ->
                        state.copy(
                            tares = it
                        )
                    }
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
                currentExerciseState.map { it.currentExerciseRest }.distinctUntilChanged(),
                workoutState.map { it.workoutStarted }.distinctUntilChanged(),
            ) { values ->
                val exercises = values[0] as List<WorkoutExercise>
                val exerciseSetsDone = values[1] as List<Int>
                val currentPage = values[2] as Int
                val restTime = values[3] as Long?
                val totalRest = values[4] as Long?
                val workoutStarted = values[5] as Boolean

                WorkoutNotificationState(
                    setsPerExercise = exercises.map { it.reps.size },
                    setsDonePerExercise = exerciseSetsDone,
                    currentExercise = currentPage,
                    restTimeSecs = restTime,
                    restTimestamp = Date().time + (restTime ?: 0L) * 1000L,
                    totalRest = totalRest,
                    workoutStarted = workoutStarted
                )
            }.collect {
                notificationService.updateNotification(it)
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
                        // get records to gather last workout's intensity
                        val records =
                            repository.getWorkoutRecordsByProgram(programId).first()
                        if (records.isNotEmpty()) {
                            _workoutState.update { state ->
                                state.copy(
                                    lastWorkoutIntensity = records.sortedBy { it.startDate }
                                        .last().intensityPercent
                                )
                            }
                        }
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
                                userDefined = it.userDefined
                            )
                        }
                        // add workout exercises to db
                        repository.addWorkoutExercises(workoutExercises)
                    }
                    if (event.quickStart) {
                        startWorkout()
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
                // TODO: check if superset and if
                if (currentExerciseState.value.repsBottomBar.toUIntOrNull() == null ||
                    currentExerciseState.value.weightBottomBar.toFloatOrNull() == null) {
                    // should not happen
                    Log.d("WorkoutViewModel", "Tried to complete a set with invalid weight or reps: $currentExerciseState")
                    _effects.trySend(WorkoutEffect.ShowMessage(R.string.complete_set_fail))
                    return
                }
                viewModelScope.launch {
                    val record = currentExerciseState.value.currentExerciseOngoingRecord

                    val exerciseRest = currentExerciseState.value.currentExercise?.rest[currentExerciseState.value.setsDone]?.toLong() ?: 0L
                    _currentExerciseState.update { it.copy(
                        restTimestamp = ZonedDateTime.now().plusSeconds(exerciseRest),
                        currentExerciseRest = exerciseRest
                    ) }
                    sendWorkout2Wear()
                    // when first set completed, we need to create the record
                    val exercise = currentExerciseState.value.currentExercise
                    if (exercise == null) {
                        Log.e("WorkoutViewModel", "Tried to complete a set from watch with no current exercise set")
                        _effects.trySend(WorkoutEffect.ShowMessage(R.string.complete_set_from_watch_fail))
                        return@launch
                    }
                    val exerciseIndex = pagesContent.value.exercises.indexOf(exercise)
                    var oldTare = workoutState.value.tares.getOrNull(exerciseIndex) ?: 0f
                    if (record == null) {
                        if (exercise.equipment == Equipment.BODY_WEIGHT)
                            oldTare = preferences.getUserWeight().first()
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                extWorkoutId = workoutState.value.workoutId,
                                extExerciseId = exercise.extExerciseId,
                                exerciseInWorkout = exerciseIndex,
                                date = ZonedDateTime.now(),
                                reps = listOf(currentExerciseState.value.repsBottomBar.toInt()),
                                weights = listOf(
                                    maybeLbToKg(currentExerciseState.value.weightBottomBar.toFloat(), pagesContent.value.imperialSystem)
                                ),
                                variation = exercise.variation,
                                variationResKey = exercise.variationResKey,
                                rest = listOf(exerciseRest.toInt()),
                                tare = oldTare
                            )
                        )
                    } else {
                        // update exercise record with new set
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                recordId = record.recordId,
                                extExerciseId = record.extExerciseId,
                                extWorkoutId = record.extWorkoutId,
                                exerciseInWorkout = record.exerciseInWorkout,
                                date = record.date,
                                reps = record.reps.plus(currentExerciseState.value.repsBottomBar.toInt()),
                                weights = record.weights.plus(
                                    maybeLbToKg(currentExerciseState.value.weightBottomBar.toFloat(), pagesContent.value.imperialSystem)
                                ),
                                variation = record.variation,
                                variationResKey = record.variationResKey,
                                    record.rest.plus(exerciseRest.toInt()),
                                tare = oldTare  // allow user to change the initial tare, in case they selected wrong one
                            )
                        )
                    }
                }
            }
            is WorkoutEvent.FinishWorkout -> {
                viewModelScope.launch {
                    val exercises = repository.getWorkoutExerciseRecordsAndInfo(workoutState.value.workoutId).first().distinct()
                    val workoutTimeMillis = currentExerciseState.value.currentTime.toInstant().toEpochMilli() - workoutState.value.startDate!!.toInstant().toEpochMilli()
                    val workoutTimeSeconds = workoutTimeMillis / 1000
                    // event.workoutIntensity is 0-100, need to convert within reasonable met values
                    val intensityMet = getMetFromIntensity(event.workoutIntensity)
                    repository.completeWorkoutRecord(
                        WorkoutRecordFinish(
                            workoutId = workoutState.value.workoutId,
                            intensity = WorkoutRecord.WorkoutIntensity.NORMAL_INTENSITY, // deprecated
                            intensityPercent = event.workoutIntensity,
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
                            calories = intensityMet *
                                    preferences.getUserWeight().first() *
                                    workoutTimeSeconds / 3600
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
                    _workoutState.update { it.copy(shutDown = true) }
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
                    repository.updateWorkoutExerciseSets(
                        WorkoutExerciseUpdateSets(
                            exercise.workoutExerciseId,
                            exercise.reps.plus(exercise.reps.last()),
                            exercise.rest.plus(exercise.rest.last())
                        )
                    )
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
                sendWorkout2Wear()
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
                sendWorkout2Wear()
            }
            is WorkoutEvent.UpdateTare -> {
                val tares = workoutState.value.tares.toMutableList()
                tares[_currentPage.value] = event.newValue
                _workoutState.update { it.copy(tares = tares) }
                sendWorkout2Wear()
            }
            is WorkoutEvent.EditSetRecord -> {
                viewModelScope.launch {
                    val record = currentExerciseState.value.currentExerciseOngoingRecord

                    if (record == null) {
                        // There is a problem
                        Log.d("WorkoutViewModel", "Tried to edit a record that does not exist")
                    } else {
                        val reps = record.reps.toMutableList()
                        val weights = record.weights.toMutableList()
                        reps[event.set] = event.reps
                        weights[event.set] = event.weight
                        repository.addExerciseRecord(
                            ExerciseRecord(
                                recordId = record.recordId,
                                extExerciseId = record.extExerciseId,
                                extWorkoutId = record.extWorkoutId,
                                exerciseInWorkout = record.exerciseInWorkout,
                                date = record.date,
                                reps = reps,
                                weights = weights,
                                variation = record.variation,
                                variationResKey = record.variationResKey,
                                rest = record.rest,
                                tare = record.tare
                            )
                        )
                    }
                }
            }
            is WorkoutEvent.ReplaceExercise -> {
                viewModelScope.launch {
                    pagesContent.mapNotNull { stateFlow ->
                            if (stateFlow.exercises.size > event.originalSize) {
                                stateFlow.exercises.last().workoutExerciseId
                            } else {
                                null
                            }
                        }
                        .first() // Get the first non-null emission (meaning the condition is met)
                        .let { lastWorkoutExerciseId ->
                            // Now that the condition is met, perform your repository operations
                            repository.deleteWorkoutExercise(
                                pagesContent.value.exercises[event.exerciseInWorkout].workoutExerciseId
                            )
                            repository.updateWorkoutExerciseNumber(
                                WorkoutExerciseReorder(
                                    lastWorkoutExerciseId,
                                    event.exerciseInWorkout
                                )
                            )
                        }
                }
            }
            is WorkoutEvent.RemoveExercise -> {
                viewModelScope.launch {
                    repository.deleteWorkoutExercise(
                        pagesContent.value.exercises[event.exerciseInWorkout].workoutExerciseId
                    )
                }
            }
            is WorkoutEvent.ToggleOtherEquipmentDialog -> {
                _workoutState.update { it.copy(
                    otherEquipmentDialogOpen = !workoutState.value.otherEquipmentDialogOpen
                ) }
            }

            is WorkoutEvent.UpdateExerciseProbability -> {
                val exerciseId = pagesContent.value.exercises[event.exerciseInWorkout].extExerciseId
                viewModelScope.launch {
                    var probability = repository.getExercise(exerciseId).first().probability
                    when (event.probability) {
                        1 -> probability *= 1.1
                        2 -> probability = (probability / 0.9) * 1.1
                        -1 -> probability *= 0.9
                        -2 -> probability = (probability / 1.1) * 0.9
                    }
                    if (probability <= 0.0)
                        probability = 0.01
                    else if (probability > 2.0)
                        probability = 2.0
                    repository.updateExerciseProbability(
                        exerciseId,
                        probability
                    )
                }
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
            it.extWorkoutId == workoutId && it.exerciseInWorkout == currentPage
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
        currentExerciseOngoingRecord: ExerciseRecordAndEquipment?,
        setsDone: Int,
        imperialSystem: Boolean
    ): Float? {
        /*
         weight is taken in this order:
         0. If first set, take from last record
         1. If not first set, check last set weight:
         1a. If == to the same set from last record, take from last record
         2. Otherwise, check whether the weight also changed between sets in last record (e.g., pyramid)
         2a. If not, keep weight from previous set
         3. Otherwise, take last record increased/decreased by same amount as previous set
         */
        // FIXME: this heuristic is not transparent to the user that might question what these
        //  "random" changes in weight are. Perhaps it is better to always have ongoingRecord and
        //  have the rest as a suggestion

        val recordsToDisplay = recordsToDisplay
        val setsDone = setsDone
        val ongoingRecord = currentExerciseOngoingRecord

        // this is the record of the last record before current workout
        val lastOldRecord = recordsToDisplay.firstOrNull()

        var weightCandidate: Float?
        var oldRecordWeightCurrentSet: Float? = null
        var oldRecordWeightPreviousSet: Float? = null
        var ongoingRecordWeightPreviousSet: Float? = null
        // for the weight, try to copy from last old record
        if (lastOldRecord != null) {
            oldRecordWeightCurrentSet = lastOldRecord.weights.getOrNull(setsDone)
            oldRecordWeightPreviousSet = lastOldRecord.weights.getOrNull(setsDone-1)
        }
        if (ongoingRecord != null) {
            ongoingRecordWeightPreviousSet = ongoingRecord.weights.getOrNull(setsDone-1)
        }
        if (setsDone == 0) {
            weightCandidate = oldRecordWeightCurrentSet
        } else if (oldRecordWeightCurrentSet != null && oldRecordWeightPreviousSet == ongoingRecordWeightPreviousSet) {
            weightCandidate = oldRecordWeightCurrentSet
        } else if (oldRecordWeightCurrentSet != null && oldRecordWeightPreviousSet != oldRecordWeightCurrentSet) {
            val delta = oldRecordWeightPreviousSet?.let { ongoingRecordWeightPreviousSet?.minus(it) }
            weightCandidate = oldRecordWeightCurrentSet.plus(delta ?: 0f)
        } else {
            weightCandidate = ongoingRecordWeightPreviousSet
        }
        weightCandidate = weightCandidate?.let { maybeKgToLb(it, imperialSystem) }

        return weightCandidate
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
                _currentExerciseState.update {
                    it.copy(
                        isLoading = false
                    )
                }
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
                _workoutState.update {
                    it.copy(
                        startDate = currentDateTime,
                        workoutStarted = true
                    ) }
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
            _workoutState.update { it.copy(autoStartFailed = true) }
            return null
        }
        val currentPlan = repository.getPlan(currentPlanId).first()
        val programs = repository.getPrograms(currentPlanId).first()
        if (programs.isEmpty()) {
            Log.e("WorkoutViewModel", "Tried to auto start workout but current plan has no programs.")
            _workoutState.update { it.copy(autoStartFailed = true) }
            return null
        }
        val upcomingProgram = programs[min(
            currentPlan?.currentProgram ?: (programs.size - 1), programs.size-1)]
        Log.d("WorkoutViewModel", "Inferred program id: ${upcomingProgram.programId}")
        return upcomingProgram.programId
    }

    private fun sendWorkout2Wear(
        sendImage: Boolean = false,
        overrideDeadWatch: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val exercise = currentExerciseState.value.currentExercise
            if (exercise != null) {
                // get current page from exercise position in workoutExercise (TODO: improve)
                val nextExerciseIndex = pagesContent.value.exercises.indexOf(exercise) + 1
                val nextExercise = pagesContent.value.exercises.getOrNull(nextExerciseIndex)
                val exerciseIncrement = when (exercise.equipment) {
                    Equipment.EVERYTHING -> throw Exception("Was asked about the increment of 'everything' equipment. This should not happen.")  // should never happen
                    Equipment.BARBELL -> workoutState.value.incrementBarbell
                    Equipment.BODY_WEIGHT -> workoutState.value.incrementBodyweight
                    Equipment.CABLES -> workoutState.value.incrementCable
                    Equipment.DUMBBELL -> workoutState.value.incrementDumbbell
                    Equipment.MACHINE -> workoutState.value.incrementMachine
                }
                val dataMapReq = PutDataMapRequest.create("/phone2watch")
                val exerciseName = currentExerciseState.value.exerciseTitle ?: (exercise.name + " ${exercise.variation}".trim())
                dataMapReq.dataMap.putString("exerciseName", exerciseName)
                if (nextExercise != null) {
                    var nextExerciseName = nextExercise.name
                    if (nextExercise.variation.isNotEmpty())
                        nextExerciseName += " " + nextExercise.variation
                    dataMapReq.dataMap.putString("nextExerciseName", nextExerciseName)
                }
                dataMapReq.dataMap.putFloat("exerciseIncrement", exerciseIncrement)
                dataMapReq.dataMap.putInt("setsDone", currentExerciseState.value.setsDone)
                dataMapReq.dataMap.putIntegerArrayList("rest", exercise.rest as ArrayList<Int>)
                dataMapReq.dataMap.putIntegerArrayList("reps", exercise.reps as ArrayList<Int>)
                dataMapReq.dataMap.putString("note", exercise.note)
                dataMapReq.dataMap.putFloat("weight", currentExerciseState.value.weightBottomBar.toFloatOrNull() ?: 0f)
                dataMapReq.dataMap.putFloat("tareBarbell", workoutState.value.tares[_currentPage.value])
                dataMapReq.dataMap.putString("equipmentResKey", exercise.equipment.equipmentResKey)
                // not necessary but can help verify exercise needs barbell choice
                dataMapReq.dataMap.putBoolean("imperialSystem", pagesContent.value.imperialSystem)
                dataMapReq.dataMap.putBoolean("isOnFinishPage", false)
                if (currentExerciseState.value.restTimestamp != null)
                    dataMapReq.dataMap.putLong("restTimestamp", currentExerciseState.value.restTimestamp?.toInstant()?.toEpochMilli() ?: 0L)
                if (currentExerciseState.value.currentExerciseRest != null)
                    dataMapReq.dataMap.putLong("currentRestSeconds", currentExerciseState.value.currentExerciseRest!!)
                repository.sendWorkout2Wear(
                    dataMapReq,
                    overrideDeadWatch
                )
                if (sendImage) {
                    val imageAsset = repository.getAssetFromResId(exercise.image)
                    val imageReq = PutDataMapRequest.create("/image2watch")
                    imageReq.dataMap.putAsset("image", imageAsset)
                    repository.sendWorkout2Wear(
                        imageReq,
                        overrideDeadWatch
                    )
                }

            } else {
                if (pagesContent.value.exercises.isNotEmpty()) {
                    // likely on finish page
                    val dataMapReq = PutDataMapRequest.create("/phone2watch")
                    dataMapReq.dataMap.putBoolean("isOnFinishPage", true)

                } else {
                    // if it's empty it's either uninitialised or empty workout
                    // TODO: handle latter case
                }
            }
        }
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
                val restTimeSecs = if (
                    currentExerciseState.value.currentExercise != null
                    && currentExerciseState.value.restTimestamp != null
                ) {
                    max(
                        0L,
                        currentExerciseState.value.restTimestamp?.toInstant()?.toEpochMilli()
                            ?.minus(
                                currentExerciseState.value.currentTime.toInstant()
                                    .toEpochMilli()
                            ) ?: 0L
                    ) / 1000  // convert from millis to secs
                } else null
                _currentExerciseState.update {
                    it.copy(
                        restTimeSecs = restTimeSecs
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}

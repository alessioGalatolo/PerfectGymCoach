package agdesigns.elevatefitness.presentation.screens.workout

import agdesigns.elevatefitness.shared.R as sharedR
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.WearRepository
import agdesigns.elevatefitness.presentation.screens.common.MediaPlayingState
import agdesigns.elevatefitness.service.WorkoutService
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.shared.BarbellType
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.MEDIA_IMAGES_PATH
import agdesigns.elevatefitness.shared.WORKOUT_IMAGES_PATH
import agdesigns.elevatefitness.shared.barbellIndexFromWeight
import agdesigns.elevatefitness.shared.bitmapFlow
import agdesigns.elevatefitness.shared.getPlates
import agdesigns.elevatefitness.shared.grpc.Media
import agdesigns.elevatefitness.shared.grpc.MediaServiceGrpcKt
import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.shared.grpc.WorkoutServiceGrpcKt
import agdesigns.elevatefitness.shared.toProtoTimestamp
import agdesigns.elevatefitness.shared.toZonedDateTime
import agdesigns.elevatefitness.utils.RepAndTempoCounter
import android.os.Build
import androidx.annotation.StringRes
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoFlow
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.protobuf.Empty
import dagger.hilt.android.lifecycle.HiltViewModel
import io.grpc.StatusException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.math.max

data class ExercisesState(
    val workoutId: Long = 0L,
    val startDate: ZonedDateTime? = null,
    val exercises: List<Workout.Exercise> = emptyList(),
    val images: List<Bitmap> = emptyList(),
    val defaultIncrements: Workout.DefaultIncrements = Workout.DefaultIncrements.getDefaultInstance(),
    val exercisesSetsDone: List<Int> = emptyList(),
    val suggestedRepsWeight: List<Workout.SuggestedRepsWeight> = emptyList(),
    val suggestedTare: List<Float> = emptyList(),
    val imperialSystem: Boolean = false,
    val activeWorkout: Boolean = true,
    val lastIntensity: Float? = null,
    val suggestedModifications: List<Workout.ProtoSuggestedModification?> = emptyList(),
    val inRestHintsEnabled: Boolean = true,
    val tempoRomTrackingEnabled: Boolean = false,
)

data class InRestHint(
    @param:StringRes val titleResId: Int,
    @param:StringRes val descResId: Int,
    val descVarArgs: List<Any>
)


data class WorkoutState(
    val currentExerciseIndex: Int = 0,
    val currentWeight: Float = 0f,
    val currentExercise: Workout.Exercise? = null,
    val restTimestamp: ZonedDateTime? = null,
    val currentExerciseRest: Long? = null,
    val currentTime: ZonedDateTime = ZonedDateTime.now(),
    val ongoingRestSecs: Long? = null,
    val ongoingRestProgression: Float? = null,
    val currentReps: Int = 0,
    val tareBarbell: Float = 0f,
    val tareIndex: Int = 0,
    // true when user completed a set from watch but has not yet set reps and weight values
    val settingSetValues: Boolean = false,
    // true when user navigated to SelectValuesScreen and completed (instead of going back)
    val successfullySetValues: Boolean = false,
    // hints to show to the user when they are in rest e.g., move to X exercise, change weight, etc.
    val inRestHints: List<InRestHint> = emptyList(),
    val showHintDialog: Boolean = false,
    val nextSetExerciseName: String = "",
    val isLastSet: Boolean = false,
    val totalCalories: Double? = null,
    val currentHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val averageHeartRate: Int? = null,
    val minHeartRate: Int? = null,
    val heartRateBySecond: Map<Long, Int> = emptyMap(),
    val workoutTime: String? = null,
    val modificationIsDismissed: Map<Workout.ProtoSuggestedModification, Boolean> = emptyMap(),
    val showOtherAppExerciseDialog: Boolean = false,
    val autoRepsCount: Int? = null, // null if not available
    val needsCalibration: Boolean = false,
    val calibrationInProgress: Boolean = false,
    val calibrationProgress: Float = 0f,
    val calibrationComplete: Boolean = false,
    val calibrationStarted: ZonedDateTime? = null,
    val lastSetResult: RepAndTempoCounter.SetResult? = null,
)

sealed class WorkoutEvent {
    data object ResetRest: WorkoutEvent()
    data class ChangeReps(val change: Int): WorkoutEvent()
    data class ChangeWeight(val change: Int): WorkoutEvent()
    data class FineGrainedChangeWeight(val change: Int): WorkoutEvent()
    data object CompleteSet: WorkoutEvent()
    data object StopActivity: WorkoutEvent()
    data class EndWorkout(val workoutIntensity: Float): WorkoutEvent()
    data class ChangeTare(val change: Int): WorkoutEvent()
    data object StartRest: WorkoutEvent()
    data object NextExercise: WorkoutEvent()
    data object PreviousExercise: WorkoutEvent()
    data object RetrySendSetCompleted: WorkoutEvent()

    data object PlayPauseMedia: WorkoutEvent()
    data object NextMedia: WorkoutEvent()
    data object PreviousMedia: WorkoutEvent()
    data object RaiseVolume: WorkoutEvent()
    data object LowerVolume: WorkoutEvent()

    data object DismissHint: WorkoutEvent()
    data object CancelSetValues: WorkoutEvent()
    data object ConfirmKillOtherApp: WorkoutEvent()
    data object DismissOtherAppDialog: WorkoutEvent()

    data class AcceptModification(val index: Int): WorkoutEvent()
    data class DismissModification(val index: Int): WorkoutEvent()

    data object AddSetToCurrentExercise: WorkoutEvent()
    data class ExtendRest(val additionalSeconds: Long = 5L): WorkoutEvent()
    data object StartCalibration: WorkoutEvent()
    data object DismissCalibration: WorkoutEvent()
}

// effects that should be propagated to the UI
sealed class WorkoutEffect {
    data object RetriableError: WorkoutEffect()
    data object NonRetriableError: WorkoutEffect()
    data object NavigateToSelectValues: WorkoutEffect()
}

@OptIn(ExperimentalHorologistApi::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutViewModel
@Inject constructor(
    private val repository: WearRepository,
    private val registry: WearDataLayerRegistry,
    private val workoutService: WorkoutServiceGrpcKt.WorkoutServiceCoroutineStub,
    private val mediaService: MediaServiceGrpcKt.MediaServiceCoroutineStub
): ViewModel() {
    private var retryJob: Job? = null
    private var calibrationDismissedThisSession = false
    val exercisesState = combine(
        registry.protoFlow<Workout.WorkoutStaticData>(TargetNodeId.PairedPhone).distinctUntilChanged(),
        registry.protoFlow<Workout.WorkoutDynamicData>(TargetNodeId.PairedPhone).distinctUntilChanged(),
        registry.bitmapFlow(TargetNodeId.PairedPhone, WORKOUT_IMAGES_PATH).distinctUntilChanged()
    ) { staticData, dynamicData, images ->
        val startDate = staticData.startDate.toZonedDateTime()
        ExercisesState(
            workoutId = staticData.workoutId,
            startDate = startDate,
            exercises = staticData.exercisesList,
            images = images,
            defaultIncrements = staticData.defaultIncrements,
            exercisesSetsDone = dynamicData.setsDoneList,
            suggestedRepsWeight = dynamicData.suggestedRepsWeightList,
            imperialSystem = staticData.imperialSystem,
            activeWorkout = staticData.activeWorkout,
            suggestedTare = staticData.suggestedTaresList,
            lastIntensity = if (staticData.previousIntensity == -1f) null else staticData.previousIntensity / 100f,
            suggestedModifications = staticData.suggestedModificationsList.map { if (!it.hasSuggestion) null else it },
            inRestHintsEnabled = staticData.smartFeaturesInRestHints,
            tempoRomTrackingEnabled = staticData.smartFeaturesTempoRomTracking,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
    ExercisesState()
    )

    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    // This should always be empty unless there is an error communicating with the phone
    private val setCompletedQueue = mutableListOf<Workout.SetCompleted>()

    // effects to happen in the UI
    private val _effects = Channel<WorkoutEffect>(capacity = Channel.BUFFERED)

    val effects: Flow<WorkoutEffect> = _effects.receiveAsFlow()

    val mediaState: StateFlow<MediaPlayingState> = combine(
        registry.protoFlow<Media.MediaPlaying>(TargetNodeId.PairedPhone).distinctUntilChanged(),
        registry.bitmapFlow(TargetNodeId.PairedPhone, MEDIA_IMAGES_PATH).distinctUntilChanged()
    ) { media, image ->
        MediaPlayingState(
            isPlaying = media.isPlaying,
            title = media.title.ifEmpty { null },
            artist = media.artist.ifEmpty { null },
            artwork = image.getOrNull(0)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
    MediaPlayingState()
    )
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.repCountFlow.collect { count ->
                _state.update { it.copy(autoRepsCount = count) }
            }
        }
        viewModelScope.launch {
            // ensure binding happens
            // wait until the service is available once, then start
            repository.bindForegroundOnlyService()
            repository.startWorkout()
        }
        viewModelScope.launch {
            observeUpdateRepsWeightExercises()
        }
        viewModelScope.launch {
            // listen for scroll to exercise requests from phone
            for (event in repository.scrollToExerciseChannel) {
                _state.update {
                    it.copy(currentExerciseIndex = event)
                }
            }
        }
        viewModelScope.launch {
            // listen for set rest requests from phone
            for (event in repository.setRestChannel) {
                // TODO: superset case
                repository.stopSetTracking()
                _state.update {
                    it.copy(
                        currentExerciseRest = event.rest,
                        restTimestamp = event.restTimestamp.toZonedDateTime(),
                        successfullySetValues = true
                    )
                }
                val restFromNow = event.restTimestamp.toZonedDateTime()?.let {
                    it.toInstant().toEpochMilli() - ZonedDateTime.now().toInstant().toEpochMilli()
                }?.div(1000L)
                val restForVibration = (restFromNow ?: event.rest) - 2
                repository.scheduleRestAlarm(restForVibration * 1000L)
            }
        }
        viewModelScope.launch {
            observeChangesForInRestHints()
        }
        viewModelScope.launch {
            repository.hintAlarmFiredFlow.collect {
                if (
                    _state.value.inRestHints.isNotEmpty() &&
                    _state.value.ongoingRestSecs?.let { it > 0L } == true
                ) {
                    _state.update {
                        it.copy(showHintDialog = true)
                    }
                }
            }
        }
        viewModelScope.launch {
            // FIXME: do not user service directly
            repository.service
                .flatMapLatest { it?.exerciseMetricsFlow() ?: flowOf(WorkoutService.ExerciseMetrics()) }
                .collect {
                    _state.update { state ->
                        val secondsToHR = state.heartRateBySecond.toMutableMap()
                        it.heartRates.forEach {
                            val timeInSeconds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                it.first.toSeconds()
                            } else {
                                it.first.toMillis() / 1000L
                            }
                            secondsToHR[timeInSeconds] = it.second.toInt()
                        }
                        state.copy(
                            totalCalories = it.totalCalories ?: state.totalCalories,
                            currentHeartRate = it.heartRates
                                .getOrNull(it.heartRates.size-1)?.second?.toInt()
                                ?: state.currentHeartRate,
                            maxHeartRate = it.maxHeartRate?.toInt() ?: state.maxHeartRate,
                            averageHeartRate = it.averageHeartRate?.toInt() ?: state.averageHeartRate,
                            minHeartRate = it.minHeartRate?.toInt() ?: state.minHeartRate,
                            heartRateBySecond = secondsToHR.toMap()
                        )
                    }
                }
        }
        viewModelScope.launch {
            // FIXME: do not use service directly
            repository.service
                .flatMapLatest { it?.otherAppInProgress ?: flowOf(false) }
                .collect { inProgress ->
                    _state.update { it.copy(showOtherAppExerciseDialog = inProgress) }
                }
        }
        viewModelScope.launch {
            exercisesState.map { it.tempoRomTrackingEnabled }.distinctUntilChanged().collect {
                repository.tempoRomTrackingEnabled = it
                if (it) {
                    repository.startSetTracking()
                }
            }
        }
        viewModelScope.launch {
            combine(
                exercisesState.map { it.exercises }.distinctUntilChanged(),
                state.map { it.currentExerciseIndex }.distinctUntilChanged(),
                exercisesState.map { it.tempoRomTrackingEnabled }.distinctUntilChanged(),
                repository.calibrationDataStore.isCalibrated.distinctUntilChanged()
            ) { exercises, index, tempoEnabled, calibrated ->
                Pair(exercises.getOrNull(index), tempoEnabled && calibrated)
            }
            .distinctUntilChanged()
            .collect { (exercise, canTrack) ->
                if (exercise != null && canTrack) {
                    repository.initSetTracking(
                        exercise.exerciseId,
                        exercise.wearRepTrackable,
                        exercise.firstPhase
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(
                exercisesState.map { it.tempoRomTrackingEnabled }.distinctUntilChanged(),
                repository.calibrationDataStore.isCalibrated.distinctUntilChanged(),
            ) { enabled, calibrated -> enabled && !calibrated }
            .collect { needed ->
                if (needed && !calibrationDismissedThisSession) {
                    _state.update { it.copy(needsCalibration = true) }
                }
            }
        }
        repository.getHealthData = {
            Workout.CompleteWorkout.newBuilder()
                .setIntensity(0f)
                .setAvgHeartRate(state.value.averageHeartRate ?: 0)
                .setMaxHeartRate(state.value.maxHeartRate ?: 0)
                .setMinHeartRate(state.value.minHeartRate ?: 0)
                .setCalories(state.value.totalCalories?.toFloat() ?: 0f)
                .addAllHeartRates(
                    state.value.heartRateBySecond.toList().sortedBy {
                        it.first
                    }.interpolateMissingValues().map { it.second }
                )
                .build()
        }
        startTimer()
    }

    override fun onCleared() {
        Log.d("WorkoutViewModel", "onCleared")
        repository.cancelRestAlarm()
        repository.cancelHintAlarm()
        repository.stopWorkout()
        super.onCleared()
    }

    fun onEvent(event: WorkoutEvent){
        when (event) {
            is WorkoutEvent.ResetRest -> {
                repository.cancelRestAlarm()
                repository.cancelHintAlarm()
                if (exercisesState.value.tempoRomTrackingEnabled) {
                    repository.startSetTracking()
                }

                viewModelScope.launch {
                    _state.update { it.copy(restTimestamp = ZonedDateTime.now()) }
                }
            }
            is WorkoutEvent.ChangeReps -> {
                _state.update { it.copy(currentReps = state.value.currentReps + event.change) }
            }
            is WorkoutEvent.CancelSetValues -> {
                _state.update {
                    it.copy(
                        settingSetValues = false,
                        successfullySetValues = false,
                        restTimestamp = null,
                        currentExerciseRest = null,
                    )
                }

                if (exercisesState.value.tempoRomTrackingEnabled) {
                    repository.startSetTracking()
                }
                repository.cancelRestAlarm()
                repository.cancelHintAlarm()
            }
            is WorkoutEvent.ChangeWeight -> {
                val currentExercise = exercisesState.value.exercises.getOrNull(
                    state.value.currentExerciseIndex
                )
                val equipment = Equipment.fromResKey(currentExercise?.equipment)
                val increment = when (equipment) {
                    Equipment.BARBELL -> exercisesState.value.defaultIncrements.barbell
                    Equipment.BODY_WEIGHT -> exercisesState.value.defaultIncrements.bodyweight
                    Equipment.CABLES -> exercisesState.value.defaultIncrements.cable
                    Equipment.DUMBBELL -> exercisesState.value.defaultIncrements.dumbbell
                    Equipment.MACHINE -> exercisesState.value.defaultIncrements.machine
                    else -> 1f
                }
                val deincrement = increment * event.change.toFloat()
                _state.update { it.copy(currentWeight = state.value.currentWeight + deincrement) }
            }
            is WorkoutEvent.FineGrainedChangeWeight -> {
                val deincrement = event.change.toFloat() * 0.5f
                _state.update { it.copy(currentWeight = state.value.currentWeight + deincrement) }
            }
            is WorkoutEvent.CompleteSet -> {
                val currentExercise = exercisesState.value.exercises.getOrNull(
                    state.value.currentExerciseIndex
                ) ?: return
                val setTrackingResults = repository.setTrackingTruthAndGetResults(
                    exerciseId = currentExercise.exerciseId,
                    groundTruthReps = state.value.currentReps
                )
                _state.update { it.copy(lastSetResult = setTrackingResults) }
                val protoTrackingResults = setTrackingResults?.toProto() ?: RepAndTempoCounter.SetResult.protoNoResult

                val shouldAdvancePage =
                    (exercisesState.value.exercisesSetsDone.getOrNull(
                        state.value.currentExerciseIndex
                    )?.plus(1) ?: 0) == currentExercise.restCount
                // if part of superset with exercise before, go back
                // if part of superset with exercise after, go forward
                val prevExercise = exercisesState.value.exercises.getOrNull(
                    state.value.currentExerciseIndex - 1
                )
                val nextExercise = exercisesState.value.exercises.getOrNull(
                    state.value.currentExerciseIndex + 1
                )
                val nextSupersetIndex = if (currentExercise.supersetExercise != 0L && currentExercise.supersetExercise == nextExercise?.programExerciseId)
                    state.value.currentExerciseIndex + 1
                else if (currentExercise.supersetExercise != 0L && currentExercise.supersetExercise == prevExercise?.programExerciseId) {
                    if (shouldAdvancePage)
                        state.value.currentExerciseIndex + 1
                    else
                        state.value.currentExerciseIndex - 1
                } else null
                if (nextSupersetIndex == null) {
                    if (shouldAdvancePage && exercisesState.value.exercises.size > state.value.currentExerciseIndex + 1) {
                        _state.update {
                            it.copy(
                                currentExerciseIndex = it.currentExerciseIndex + 1
                            )
                        }
                    }
                } else {
                    _state.update {
                        it.copy(
                            currentExerciseIndex = nextSupersetIndex
                        )
                    }
                }
                val equipment = Equipment.fromResKey(currentExercise.equipment)
                val tare = if (equipment == Equipment.BARBELL)
                    BarbellType.entries[state.value.tareIndex].weight[exercisesState.value.imperialSystem] ?: 0f
                else 0f
                val protoTimestamp = state.value.restTimestamp.toProtoTimestamp()
                val queuedSetCompleted = Workout.SetCompleted.newBuilder()
                    .setWorkoutId(exercisesState.value.workoutId)
                    .setExerciseId(currentExercise.workoutExerciseId)
                    .setReps(state.value.currentReps)
                    .setWeight(state.value.currentWeight)
                    .setTare(tare)
                    .setRest(state.value.currentExerciseRest ?: 0L)
                    .setRestTimestamp(protoTimestamp)
                    .setSetTracking(protoTrackingResults)
                    .build()
                setCompletedQueue.add(queuedSetCompleted)
                _state.update {
                    it.copy(
                        settingSetValues = false,
                        successfullySetValues = true
                    )
                }
                viewModelScope.launch {
                    try {
                        val result = workoutService.setCompleted(
                            queuedSetCompleted
                        )
                        if (!result.success) {
                            // Note, this should never happen because
                            Log.e(
                                "WorkoutViewModel",
                                "Error completing set with message: ${result.message}"
                            )
                            _effects.trySend(WorkoutEffect.RetriableError)
                            return@launch
                        }
                        setCompletedQueue.remove(queuedSetCompleted)
                    } catch (e: StatusException) {
                        Log.e(
                            "WorkoutViewModel",
                            "Error completing set with error: ${e.message}"
                        )
                        _effects.trySend(WorkoutEffect.RetriableError)
                    }
                }
            }
            is WorkoutEvent.ChangeTare -> {
                val totalItems = BarbellType.entries.size
                _state.update {
                    it.copy(
                        tareIndex = (it.tareIndex + event.change + totalItems) % totalItems
                    )
                }
            }
            is WorkoutEvent.StopActivity -> {
                viewModelScope.launch {
                    repository.stopWorkout()
                }
            }
            is WorkoutEvent.EndWorkout -> {
                viewModelScope.launch {
                    try {
                        workoutService.completeWorkout(
                            Workout.CompleteWorkout.newBuilder()
                                .setIntensity(event.workoutIntensity * 100f) // phone expects values 0-100
                                .setAvgHeartRate(state.value.averageHeartRate ?: 0)
                                .setMaxHeartRate(state.value.maxHeartRate ?: 0)
                                .setMinHeartRate(state.value.minHeartRate ?: 0)
                                .setCalories(state.value.totalCalories?.toFloat() ?: 0f)
                                .addAllHeartRates(
                                    state.value.heartRateBySecond.toList().sortedBy {
                                        it.first
                                    }.interpolateMissingValues().map { it.second }
                                )
                                .build()
                        )
                    } catch (e: StatusException) {
                        Log.e(
                            "WorkoutViewModel",
                            "Error completing workout with error: ${e.message}"
                        )
                    }
                    repository.stopWorkout()
                    repository.cancelRestAlarm()
                    repository.cancelHintAlarm()
                }
            }
            is WorkoutEvent.StartRest -> {
                repository.stopSetTracking()
                _state.update { state ->
                    val currentExercise = exercisesState.value.exercises.getOrNull(
                        state.currentExerciseIndex
                    )
                    if (currentExercise == null) {
                        _effects.trySend(WorkoutEffect.NonRetriableError)
                        return@update state
                    }

                    val setsDone = exercisesState.value.exercisesSetsDone.getOrNull(
                        state.currentExerciseIndex
                    ) ?: 0
                    val rest = if (setsDone < currentExercise.restCount)
                        currentExercise.getRest(setsDone)
                    else if (currentExercise.restCount > 0)
                        currentExercise.restList.last()
                    else 0

                    // check if superset
                    val nextExercise = exercisesState.value.exercises.getOrNull(
                        state.currentExerciseIndex + 1
                    )
                    if (nextExercise != null &&
                        currentExercise.supersetExercise == nextExercise.programExerciseId &&
                        currentExercise.supersetExercise != 0L
                    ) {
                        // if superset with exercise after, we should move to next page and not start rest
                        // vibrate now to hint user to start next exercise
                        repository.scheduleRestAlarm(0L)
                        state.copy(
                            restTimestamp = null,
                            settingSetValues = true,
                            currentExerciseRest = null,
                            successfullySetValues = false,
                            showHintDialog = false,
                        )
                    } else {
                        repository.scheduleRestAlarm((rest.toLong() - 2L) * 1000L)
                        state.copy(
                            restTimestamp = ZonedDateTime
                                .now()
                                .plusSeconds(
                                    rest.toLong()
                                ),
                            settingSetValues = true,
                            successfullySetValues = false,
                            currentExerciseRest = rest.toLong(),
                            showHintDialog = false,
                        )
                    }
                }
                _effects.trySend(WorkoutEffect.NavigateToSelectValues)
            }
            is WorkoutEvent.NextExercise -> {
                if (state.value.currentExerciseIndex == exercisesState.value.exercises.size - 1)
                    return
                _state.update {
                    it.copy(
                        currentExerciseIndex = it.currentExerciseIndex + 1
                    )
                }
            }
            is WorkoutEvent.PreviousExercise -> {
                if (state.value.currentExerciseIndex == 0) return
                _state.update {
                    it.copy(
                        currentExerciseIndex = it.currentExerciseIndex - 1
                    )
                }
            }
            is WorkoutEvent.PlayPauseMedia -> {
                viewModelScope.launch {
                    try {
                        mediaService.playPause(Empty.getDefaultInstance())
                    } catch (e: StatusException) {
                        _effects.trySend(WorkoutEffect.NonRetriableError)
                        Log.e("WorkoutViewModel", "Error playing/pausing media with error: ${e.message}")
                    }
                }
            }
            is WorkoutEvent.NextMedia -> {
                viewModelScope.launch {
                    try {
                        mediaService.next(Empty.getDefaultInstance())
                    } catch (e: StatusException) {
                        _effects.trySend(WorkoutEffect.NonRetriableError)
                        Log.e("WorkoutViewModel", "Error skipping media with error: ${e.message}")
                    }
                }
            }
            is WorkoutEvent.PreviousMedia -> {
                viewModelScope.launch {
                    try {
                        mediaService.previous(Empty.getDefaultInstance())
                    } catch (e: StatusException) {
                        _effects.trySend(WorkoutEffect.NonRetriableError)
                        Log.e("WorkoutViewModel", "Error skipping media with error: ${e.message}")
                    }
                }
            }
            is WorkoutEvent.RaiseVolume -> {
                viewModelScope.launch {
                    try {
                        mediaService.raiseVolume(Empty.getDefaultInstance())
                    } catch (e: StatusException) {
                        _effects.trySend(WorkoutEffect.NonRetriableError)
                        Log.e("WorkoutViewModel", "Error raising volume with error: ${e.message}")
                    }
                }
            }
            is WorkoutEvent.LowerVolume -> {
                viewModelScope.launch {
                    try {
                        mediaService.lowerVolume(Empty.getDefaultInstance())
                    } catch (e: StatusException) {
                        _effects.trySend(WorkoutEffect.NonRetriableError)
                        Log.e("WorkoutViewModel", "Error lowering volume with error: ${e.message}")
                    }
                }
            }
            is WorkoutEvent.RetrySendSetCompleted -> {
                if (retryJob?.isActive == true)
                    return
                retryJob = viewModelScope.launch {
                    val failedSets = mutableListOf<Workout.SetCompleted>()
                    for (set in setCompletedQueue) {
                        try {
                            val result = workoutService.setCompleted(set)
                            if (!result.success) {
                                Log.e(
                                    "WorkoutViewModel",
                                    "Error completing set with message: ${result.message}"
                                )
                                failedSets.add(set)
                            }
                        } catch (e: StatusException) {
                            Log.e(
                                "WorkoutViewModel",
                                "Error completing set with error: ${e.message}"
                            )
                            failedSets.add(set)
                        }
                        setCompletedQueue.remove(set)
                    }
                    if (failedSets.isNotEmpty()) {
                        setCompletedQueue.addAll(failedSets)
                        _effects.trySend(WorkoutEffect.RetriableError)
                    }
                    _state.update {
                        it.copy(
                            settingSetValues = false
                        )
                    }
                }
            }
            is WorkoutEvent.DismissHint -> {
                // Immediately hide the dialog, then pop the hint after dismiss animation
                _state.update { it.copy(showHintDialog = false) }
                viewModelScope.launch {
                    delay(1000)
                    _state.update {
                        val newHints = it.inRestHints.toMutableList()
                        newHints.removeFirstOrNull()
                        it.copy(inRestHints = newHints)
                    }
                    val remainingRest = state.value.ongoingRestSecs ?: 0L
                    if (_state.value.inRestHints.isNotEmpty() && remainingRest > 10L) {
                        repository.scheduleHintAlarm()
                    }
                }
            }
            is WorkoutEvent.AcceptModification -> {
                viewModelScope.launch {
                    try {
                        workoutService.acceptModification(
                            Workout.AcceptedModification.newBuilder()
                                .setExerciseIndex(event.index)
                                .build()
                        )
                    } catch (e: StatusException) {
                        _effects.trySend(WorkoutEffect.NonRetriableError)
                        Log.e(
                            "WorkoutViewModel",
                            "Error accepting modification with error: ${e.message}"
                        )
                    }
                }
            }
            is WorkoutEvent.DismissModification -> {
                viewModelScope.launch {
                    val modification =
                        exercisesState.value.suggestedModifications.getOrNull(event.index)
                            ?: return@launch
                    _state.update {
                        it.copy(
                            modificationIsDismissed = it.modificationIsDismissed.toMutableMap()
                                .apply {
                                    this[modification] = true
                                }
                        )
                    }
                }
            }
            is WorkoutEvent.ConfirmKillOtherApp -> {
                repository.foregroundOnlyWorkoutService?.confirmKillOtherApp()
            }
            is WorkoutEvent.DismissOtherAppDialog -> {
                _state.update { it.copy(showOtherAppExerciseDialog = false) }
            }
            is WorkoutEvent.AddSetToCurrentExercise -> {
                val exerciseId = state.value.currentExercise?.workoutExerciseId ?: return
                viewModelScope.launch {
                    try {
                        workoutService.addSet(
                            Workout.AddSetRequest.newBuilder()
                                .setWorkoutId(exercisesState.value.workoutId)
                                .setExerciseId(exerciseId)
                                .build()
                        )
                    } catch (e: StatusException) {
                        Log.e("WorkoutViewModel", "Error adding set from watch: ${e.message}")
                        _effects.trySend(WorkoutEffect.NonRetriableError)
                    }
                }
            }
            is WorkoutEvent.ExtendRest -> {
                val currentRestTimestamp = state.value.restTimestamp ?: return
                val newTimestamp = currentRestTimestamp.plusSeconds(event.additionalSeconds)
                val newRestTotal = (state.value.currentExerciseRest ?: 0L) + event.additionalSeconds
                _state.update {
                    it.copy(
                        restTimestamp = newTimestamp,
                        currentExerciseRest = newRestTotal
                    )
                }
                val restForVibration = (newTimestamp.toInstant().toEpochMilli() -
                    ZonedDateTime.now().toInstant().toEpochMilli()) / 1000L - 2L
                repository.scheduleRestAlarm(restForVibration * 1000L)
                viewModelScope.launch {
                    try {
                        workoutService.extendRest(
                            Workout.ExtendRestRequest.newBuilder()
                                .setAdditionalSeconds(event.additionalSeconds)
                                .build()
                        )
                    } catch (e: StatusException) {
                        Log.e("WorkoutViewModel", "Error extending rest from watch: ${e.message}")
                    }
                }
            }
            is WorkoutEvent.StartCalibration -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            calibrationInProgress = true,
                            calibrationProgress = 0f,
                            calibrationStarted = ZonedDateTime.now()
                        )
                    }
                    repository.runCalibration(5000L)
                    _state.update {
                        it.copy(
                            calibrationInProgress = false,
                            calibrationProgress = 1f,
                            calibrationComplete = true,
                            needsCalibration = false,
                        )
                    }
                }
            }
            is WorkoutEvent.DismissCalibration -> {
                calibrationDismissedThisSession = true
                _state.update {
                    it.copy(
                        needsCalibration = false,
                        calibrationInProgress = false,
                        calibrationComplete = false,
                    )
                }
            }
        }
    }

    private fun List<Pair<Long, Int>>.interpolateMissingValues(): List<Pair<Long, Int>> {
        // Here, Long is a timestamp, Int is a heart rate value
        // We want to interpolate for missing values
        return buildList {
            if (this@interpolateMissingValues.isEmpty()) return@buildList
            var lastValue: Int = this@interpolateMissingValues.first().second
            var lastTime: Long = this@interpolateMissingValues.first().first
            for ((time, value) in this@interpolateMissingValues) {
                if (time > lastTime+1L) {
                    val timeDiff = time - lastTime
                    val valueDiff = value - lastValue
                    val increment = valueDiff.toFloat() / timeDiff.toFloat()
                    for (i in 1 until timeDiff) {
                        add(
                            lastTime + i to (lastValue + (increment * i)).toInt()
                        )
                    }
                }
                lastTime = time
                lastValue = value
                add(time to value)
            }
        }
    }

    // current time
    private fun startTimer(){
        timerJob?.cancel(CancellationException("Duplicate call"))
        timerJob = flow {
            var counter = 0
            while (true) {
                emit(counter++)
                delay(TIME_REFRESH_DELAY_MILLIS)
            }
        }.onEach {
            _state.update {
                val currentTime = ZonedDateTime.now()
                val ongoingRestMillis = if (it.restTimestamp != null) {
                    max(
                        0L,
                        it.restTimestamp.toInstant()?.toEpochMilli()?.minus(
                        currentTime.toInstant().toEpochMilli()
                        ) ?: 0L
                    )
                } else null
                val ongoingRestSecs = ongoingRestMillis?.div(1000L)
                val ongoingRestProgression = if (
                    ongoingRestMillis != null &&
                    (it.currentExerciseRest ?: 0L) > 0L
                ) {
                    ongoingRestMillis.toFloat() / it.currentExerciseRest!!.times(1000L).toFloat()
                } else null
                val workoutTime = exercisesState.value.startDate?.let {
                    val seconds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Duration.between(it, currentTime).toSeconds()
                    } else {
                        Duration.between(it, currentTime).toMillis() / 1000L
                    }
                    "%02d:%02d".format(seconds / 60, seconds % 60)
                }
                // if calibration
                val calibrationProgress = if (it.calibrationInProgress) {
                    Duration.between(it.calibrationStarted, currentTime).toMillis().toFloat() / 5000f
                } else it.calibrationProgress
                it.copy(
                    currentTime = currentTime,
                    ongoingRestSecs = ongoingRestSecs,
                    ongoingRestProgression = ongoingRestProgression,
                    workoutTime = workoutTime,
                    calibrationProgress = calibrationProgress
                )
            }
        }.launchIn(viewModelScope)
    }
    private suspend fun observeUpdateRepsWeightExercises() {
        combine(
            state.map { it.currentExerciseIndex }.distinctUntilChanged(),
            exercisesState.map { it.suggestedRepsWeight }.distinctUntilChanged(),
            exercisesState.map { it.exercisesSetsDone }.distinctUntilChanged(),
            exercisesState.map { it.suggestedTare }.distinctUntilChanged(),
            exercisesState.map { it.exercises }.distinctUntilChanged(),
            exercisesState.map { it.imperialSystem }.distinctUntilChanged(),
            state.map { it.settingSetValues }.distinctUntilChanged()
        ) { values ->
            val index = values[0] as Int
            val suggestedRepsWeight = values[1] as List<Workout.SuggestedRepsWeight>
            val allSetsDone = values[2] as List<Int>
            val tares = values[3] as List<Float>
            val exercises = values[4] as List<Workout.Exercise>
            val imperialSystem = values[5] as Boolean
            val settingSetValues = values[6] as Boolean
            if (settingSetValues) return@combine
            _state.update {
                val currentExercise = exercises.getOrNull(index) ?: exercises.lastOrNull()
                val repsWeight = suggestedRepsWeight.getOrNull(index)
                val setsDone = allSetsDone.getOrNull(index) ?: 0
                val suggestedTare = tares.getOrNull(index)
                val suggestedTareIndex = suggestedTare?.let {
                    barbellIndexFromWeight(it)
                }
                var suggestedWeight: String? = null
                var suggestedReps: String? = null
                if (repsWeight != null) {
                    suggestedWeight = if (repsWeight.weightCount > setsDone)
                        repsWeight.getWeight(setsDone)
                    else if (repsWeight.weightCount > 0)
                        repsWeight.getWeight(repsWeight.weightCount - 1)
                    else
                        null
                    suggestedReps = if (repsWeight.repsCount > setsDone)
                        repsWeight.getReps(setsDone)
                    else if (repsWeight.repsCount > 0)
                        repsWeight.getReps(repsWeight.repsCount - 1)
                    else
                        null
                }
                val nextSetExerciseName = if (currentExercise?.let { setsDone < it.restCount } ?: false) {
                    (currentExercise.name ?: "") + if (suggestedReps != null && suggestedWeight != null)
                        " (${suggestedReps}x${suggestedWeight}${
                        if (imperialSystem)
                            repository.stringResToString(sharedR.string.lb)
                        else
                            repository.stringResToString(sharedR.string.kg)
                    })"
                    else ""
                } else {
                    exercises.getOrNull(index + 1)?.name
                        ?: ""
                }
                it.copy(
                    currentWeight = suggestedWeight?.toFloatOrNull() ?: 0f,
                    currentReps = suggestedReps?.toIntOrNull() ?: it.currentReps,
                    tareBarbell = suggestedTare ?: it.tareBarbell,
                    tareIndex = suggestedTareIndex ?: it.tareIndex,
                    nextSetExerciseName = nextSetExerciseName,
                    isLastSet = currentExercise?.let { setsDone >= it.restCount } ?: false,
                    currentExercise = currentExercise
                )
            }
        }.collect()
    }

    private suspend fun observeChangesForInRestHints() {
        combine(
            state.map { it.currentExerciseIndex }.distinctUntilChanged(),
            exercisesState.map { it.exercises }.distinctUntilChanged(),
            exercisesState.map { it.exercisesSetsDone },
            exercisesState.map { it.suggestedRepsWeight },
            exercisesState.map { it.imperialSystem }.distinctUntilChanged(),
            exercisesState.map { it.suggestedModifications }.distinctUntilChanged(),
            exercisesState.map { it.inRestHintsEnabled }.distinctUntilChanged(),
        ) { values ->
            val index = values[0] as Int
            val exercises = values[1] as List<Workout.Exercise>
            val exercisesSetsDone = values[2] as List<Int>
            val suggestedRepsWeight = values[3] as List<Workout.SuggestedRepsWeight>
            val imperialSystem = values[4] as Boolean
            val modifications = values[5] as List<Workout.ProtoSuggestedModification>
            val inRestHintsEnabled = values[6] as Boolean
            if (!inRestHintsEnabled) {
                _state.update { it.copy(inRestHints = emptyList(), showHintDialog = false) }
                repository.cancelHintAlarm()
                return@combine
            }

            // This try catch should be unnecessary but I keep getting Index error for Workout (grpc)
            try {
                val currentExercise = exercises.getOrNull(index) ?: return@combine

                val setsDone = exercisesSetsDone.getOrNull(index) ?: 0
                // Determine if we're continuing the same exercise or moving to a new one
                // index is not reliable because gets +1ed as soon as we start rest
                val isNextSetSameExercise = setsDone != 0  // this must be a new exercise
                val repsWeight = if (isNextSetSameExercise)
                    suggestedRepsWeight.getOrNull(index)
                else
                    suggestedRepsWeight.getOrNull(index - 1)
                if (repsWeight == null) {
                    return@combine
                }

                var nextExercise = if (!isNextSetSameExercise) {
                    exercises.getOrNull(index)
                } else null

                // new: we do not hint next exercise if a suggestion for it says either replace or skip
                // or suggestion for current says add
                if (!isNextSetSameExercise) {
                    if (modifications.getOrNull(index-1)?.type == Workout.ProtoModificationType.EXERCISE_ADDED) {
                        nextExercise = null
                    }
                    val nextModification = modifications.getOrNull(index)
                    if (nextModification?.type == Workout.ProtoModificationType.EXERCISE_REPLACED ||
                        nextModification?.type == Workout.ProtoModificationType.EXERCISE_SKIPPED
                    ) {
                        nextExercise = null
                    }
                }

                val nextExerciseRepsWeight = if (!isNextSetSameExercise) {
                    suggestedRepsWeight.getOrNull(index)
                } else null

                // Get next set weight if continuing same exercise
                val nextWeight = if (isNextSetSameExercise &&
                    repsWeight.weightCount > setsDone
                ) {
                    repsWeight.getWeight(setsDone).toFloatOrNull()
                } else if (!isNextSetSameExercise &&
                    nextExerciseRepsWeight != null &&
                    nextExerciseRepsWeight.weightCount > 0
                ) {
                    nextExerciseRepsWeight.getWeight(0).toFloatOrNull()
                } else null

                // Get next set reps if continuing same exercise
                val nextReps = if (
                    isNextSetSameExercise
                    && repsWeight.repsCount > setsDone
                ) {
                    repsWeight.getReps(setsDone).toIntOrNull()
                } else if (
                    !isNextSetSameExercise &&
                    nextExerciseRepsWeight != null &&
                    nextExerciseRepsWeight.repsCount > 0
                )
                    nextExerciseRepsWeight.getReps(0).toIntOrNull()
                else null

                val equipment = Equipment.fromResKey(currentExercise.equipment)
                val unitString =
                    repository.stringResToString(if (imperialSystem) sharedR.string.lb else sharedR.string.kg)
                val hintList: List<InRestHint> = buildList {
                    if (nextExercise != null) {
                        // Moving to a different exercise
                        add(
                            InRestHint(
                                titleResId = R.string.rest_hint_change_exercise_title,
                                descResId = R.string.rest_hint_change_exercise,
                                descVarArgs = listOf(nextExercise.name)
                            )
                        )
                        if (nextWeight != null) {
                            when (equipment) {
                                Equipment.DUMBBELL -> {
                                    add(
                                        InRestHint(
                                            titleResId = R.string.rest_hint_start_weight_title,
                                            descResId = R.string.rest_hint_start_weight_dumbbell,
                                            descVarArgs = listOf(nextWeight, unitString)
                                        )
                                    )
                                }

                                Equipment.BARBELL -> {
                                    val plates = getPlates(nextWeight)
                                    val platesFormatted = plates.map { (plate, count) ->
                                        "${count}x${plate}$unitString"
                                    }.joinToString(", ")
                                    add(
                                        InRestHint(
                                            titleResId = R.string.rest_hint_start_weight_title,
                                            descResId = R.string.rest_hint_start_weight_barbell,
                                            descVarArgs = listOf(platesFormatted)
                                        )
                                    )
                                }

                                else -> {
                                    add(
                                        InRestHint(
                                            titleResId = R.string.rest_hint_start_weight_title,
                                            descResId = R.string.rest_hint_start_weight_generic,
                                            descVarArgs = listOf(nextWeight, unitString)
                                        )
                                    )
                                }
                            }
                        }
                        if (nextReps != null) {
                            add(
                                InRestHint(
                                    titleResId = R.string.rest_hint_start_reps_title,
                                    descResId = R.string.rest_hint_start_reps,
                                    descVarArgs = listOf(nextReps)
                                )
                            )
                        }
                    } else if (isNextSetSameExercise) {
                        // Same exercise
                        val currentWeight = if (setsDone > 0 && repsWeight.weightCount > setsDone) {
                            repsWeight.getWeight(setsDone - 1).toFloatOrNull()
                        } else null
                        val currentReps = if (setsDone > 0 && repsWeight.repsCount > setsDone) {
                            repsWeight.getReps(setsDone - 1).toIntOrNull()
                        } else null
                        if (currentWeight == null || currentReps == null) return@combine
                        if (nextWeight != null /*&& currentWeight > 0.0*/) {
                            // weight is changing
                            val weightDiff = nextWeight - currentWeight
                            when {
                                weightDiff > 0.0 -> {
                                    when (equipment) {
                                        Equipment.DUMBBELL -> {
                                            add(
                                                InRestHint(
                                                    titleResId = R.string.rest_hint_increase_weight_title,
                                                    descResId = R.string.rest_hint_increase_weight_dumbbell,
                                                    descVarArgs = listOf(nextWeight, unitString)
                                                )
                                            )
                                        }

                                        Equipment.BARBELL -> {
                                            val plates = getPlates(weightDiff)
                                            val formattedPlates = plates.map { (plate, count) ->
                                                "${count}x${plate}$unitString"
                                            }.joinToString(", ")

                                            add(
                                                InRestHint(
                                                    titleResId = R.string.rest_hint_increase_weight_title,
                                                    descResId = R.string.rest_hint_increase_weight_barbell,
                                                    descVarArgs = listOf(formattedPlates)
                                                )
                                            )
                                        }

                                        else -> {
                                            add(
                                                InRestHint(
                                                    titleResId = R.string.rest_hint_increase_weight_title,
                                                    descResId = R.string.rest_hint_increase_weight_generic,
                                                    descVarArgs = listOf(nextWeight, unitString)
                                                )
                                            )
                                        }
                                    }
                                }

                                weightDiff < 0.0 -> {
                                    when (equipment) {
                                        Equipment.DUMBBELL -> {
                                            add(
                                                InRestHint(
                                                    titleResId = R.string.rest_hint_decrease_weight_title,
                                                    descResId = R.string.rest_hint_decrease_weight_dumbbell,
                                                    descVarArgs = listOf(nextWeight, unitString)
                                                )
                                            )
                                        }

                                        Equipment.BARBELL -> {
                                            add(
                                                InRestHint(
                                                    titleResId = R.string.rest_hint_decrease_weight_title,
                                                    descResId = R.string.rest_hint_decrease_weight_barbell,
                                                    descVarArgs = listOf(nextWeight, unitString)
                                                )
                                            )
                                        }

                                        else -> {
                                            add(
                                                InRestHint(
                                                    titleResId = R.string.rest_hint_decrease_weight_title,
                                                    descResId = R.string.rest_hint_decrease_weight_generic,
                                                    descVarArgs = listOf(nextWeight, unitString)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (nextReps != null && currentReps != nextReps) {
                            // same exercise but increase / decrease reps
                            if (currentReps < nextReps) {
                                add(
                                    InRestHint(
                                        titleResId = R.string.rest_hint_increase_reps_title,
                                        descResId = R.string.rest_hint_increase_reps,
                                        descVarArgs = listOf(nextReps)
                                    )
                                )
                            } else {
                                add(
                                    InRestHint(
                                        titleResId = R.string.rest_hint_decrease_reps_title,
                                        descResId = R.string.rest_hint_decrease_reps,
                                        descVarArgs = listOf(nextReps)
                                    )
                                )
                            }
                        }
                    }
                }
                _state.update {
                    it.copy(
                        inRestHints = hintList,
                        showHintDialog = false
                    )
                }
                val remainingRest = state.value.ongoingRestSecs ?: 0L
                if (hintList.isNotEmpty() && remainingRest > 10L) {
                    repository.scheduleHintAlarm()
                } else {
                    repository.cancelHintAlarm()
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("WorkoutViewModel", "Index out of bounds when generating in rest hints", e)
            }
        }.collect()

    }


    companion object {
        const val TIME_REFRESH_DELAY_MILLIS = 500L  // how much delay before currentTime is updated
    }
}
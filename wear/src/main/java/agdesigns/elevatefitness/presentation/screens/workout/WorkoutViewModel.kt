package agdesigns.elevatefitness.presentation.screens.workout

import agdesigns.elevatefitness.shared.R as sharedR
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.WearRepository
import agdesigns.elevatefitness.presentation.screens.common.MediaPlayingState
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val lastIntensity: Float? = null
)

data class InRestHint(
    @param:StringRes val titleResId: Int,
    @param:StringRes val descResId: Int,
    val descVarArgs: List<Any>
)


data class WorkoutState(
    val currentExerciseIndex: Int = 0,
    val currentWeight: Float = 0f,
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
    // hints to show to the user when they are in rest e.g., move to X exercise, change weight, etc.
    val inRestHints: List<InRestHint> = emptyList()
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
}

// effects that should be propagated to the UI
sealed class WorkoutEffect {
    data object RetriableError: WorkoutEffect()
    data object NonRetriableError: WorkoutEffect()
}

@OptIn(ExperimentalHorologistApi::class)
@HiltViewModel
class WorkoutViewModel
@Inject constructor(
    private val repository: WearRepository,
    private val registry: WearDataLayerRegistry,
    private val workoutService: WorkoutServiceGrpcKt.WorkoutServiceCoroutineStub,
    private val mediaService: MediaServiceGrpcKt.MediaServiceCoroutineStub
): ViewModel() {
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
            lastIntensity = if (staticData.previousIntensity == -1f) null else staticData.previousIntensity / 100f
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
            // ensure binding happens
            // wait until the service is available once, then start
            repository.bindForegroundOnlyService()
            repository.startWorkout()
        }
        viewModelScope.launch {
            observeUpdateRepsWeight()
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
                _state.update {
                    it.copy(
                        currentExerciseRest = event.rest,
                        restTimestamp = event.restTimestamp.toZonedDateTime()
                    )
                }
                val restFromNow = event.restTimestamp.toZonedDateTime()?.let {
                    it.toInstant().toEpochMilli() - ZonedDateTime.now().toInstant().toEpochMilli()
                }?.div(1000L)
                val restForVibration = (restFromNow ?: event.rest) - 2
                repository.scheduleVibrationAlarm(restForVibration * 1000L)
            }
        }
        viewModelScope.launch {
            observeChangesForInRestHints()
        }
        startTimer()
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopWorkout()
    }

    fun onEvent(event: WorkoutEvent){
        when (event) {
            is WorkoutEvent.ResetRest -> {
                repository.cancelAlarm()
                viewModelScope.launch {
                    _state.update { it.copy(restTimestamp = ZonedDateTime.now()) }
                }
            }
            is WorkoutEvent.ChangeReps -> {
                _state.update { it.copy(currentReps = state.value.currentReps + event.change) }
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
                viewModelScope.launch {
                    val currentExercise = exercisesState.value.exercises.getOrNull(
                        state.value.currentExerciseIndex
                    )
                    if (currentExercise != null) {
                        val shouldAdvancePage = (exercisesState.value.exercisesSetsDone.getOrNull(
                            state.value.currentExerciseIndex
                        )?.plus(1) ?: 0) == currentExercise.restCount
                        if (shouldAdvancePage && exercisesState.value.exercises.size > state.value.currentExerciseIndex + 1) {
                            _state.update {
                                it.copy(
                                    currentExerciseIndex = it.currentExerciseIndex + 1
                                )
                            }
                        }
                        val equipment = Equipment.fromResKey(currentExercise.equipment)
                        val tare = if (equipment == Equipment.BARBELL)
                            BarbellType.entries[state.value.tareIndex].weight[exercisesState.value.imperialSystem] ?: 0f
                        else 0f
                        val protoTimestamp = state.value.restTimestamp.toProtoTimestamp()
                        try {
                            val queuedSetCompleted = Workout.SetCompleted.newBuilder()
                                .setWorkoutId(exercisesState.value.workoutId)
                                .setExerciseId(currentExercise.exerciseId)
                                .setReps(state.value.currentReps)
                                .setWeight(state.value.currentWeight)
                                .setTare(tare)
                                .setRest(state.value.currentExerciseRest ?: 0L)
                                .setRestTimestamp(protoTimestamp)
                                .build()
                            setCompletedQueue.add(queuedSetCompleted)
                            val result = workoutService.setCompleted(
                                queuedSetCompleted
                            )
                            if (!result.success) {
                                // Note, this should never happen because
                                Log.e("WorkoutViewModel", "Error completing set with message: ${result.message}")
                                _effects.trySend(WorkoutEffect.RetriableError)
                                return@launch
                            }
                            setCompletedQueue.remove(queuedSetCompleted)
                        } catch (e: StatusException) {
                            Log.e("WorkoutViewModel", "Error completing set with error: ${e.message}")
                            _effects.trySend(WorkoutEffect.RetriableError)
                        }
                    }
                    _state.update {
                        it.copy(
                            settingSetValues = false
                        )
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
                    repository.service.firstOrNull()?.stopWorkout()
                }
            }
            is WorkoutEvent.EndWorkout -> {
                viewModelScope.launch {
                    try {
                        workoutService.completeWorkout(
                            Workout.CompleteWorkout.newBuilder()
                                .setIntensity(event.workoutIntensity * 100f) // phone expects values 0-100
                                .build()
                        )
                    } catch (e: StatusException) {
                        Log.e(
                            "WorkoutViewModel",
                            "Error completing workout with error: ${e.message}"
                        )
                    }
                }
            }
            is WorkoutEvent.StartRest -> {
                _state.update { state ->
                    val currentExercise = exercisesState.value.exercises.getOrNull(
                        state.currentExerciseIndex
                    )
                    val setsDone = exercisesState.value.exercisesSetsDone.getOrNull(
                        state.currentExerciseIndex
                    ) ?: 0
                    val rest = if (currentExercise?.let { setsDone < it.restCount } ?: false)
                        currentExercise.getRest(setsDone)
                    else if (currentExercise?.let { it.restCount > 0 } ?: false)
                        currentExercise.restList.last()
                    else 0
                    repository.scheduleVibrationAlarm((rest.toLong() - 2L) * 1000L)
                    state.copy(
                        restTimestamp = ZonedDateTime
                            .now()
                            .plusSeconds(
                                rest.toLong()
                            ),
                        settingSetValues = true,
                        currentExerciseRest = rest.toLong(),
                    )
                }
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
                viewModelScope.launch {
                    for (set in setCompletedQueue) {
                        try {
                            val result = workoutService.setCompleted(set)
                            if (!result.success) {
                                Log.e(
                                    "WorkoutViewModel",
                                    "Error completing set with message: ${result.message}"
                                )
                            }
                        } catch (e: StatusException) {
                            Log.e(
                                "WorkoutViewModel",
                                "Error completing set with error: ${e.message}"
                            )
                        }
                        setCompletedQueue.remove(set)
                    }
                }
            }
            is WorkoutEvent.DismissHint -> {
                _state.update {
                    // pop first element
                    val newHints = it.inRestHints.toMutableList()
                    newHints.removeFirstOrNull()
                    it.copy(
                        inRestHints = newHints
                    )
                }
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
                it.copy(
                    currentTime = currentTime,
                    ongoingRestSecs = ongoingRestSecs,
                    ongoingRestProgression = ongoingRestProgression
                )
            }
        }.launchIn(viewModelScope)
    }
    private suspend fun observeUpdateRepsWeight() {
        combine(
            state.map { it.currentExerciseIndex }.distinctUntilChanged(),
            exercisesState.map { it.suggestedRepsWeight }.distinctUntilChanged(),
            exercisesState.map { it.exercisesSetsDone }.distinctUntilChanged(),
            exercisesState.map { it.suggestedTare }.distinctUntilChanged()
        ) { index, suggestedRepsWeight, allSetsDone, tares ->
            // NOTE: if user is selecting values and this gets called, it will override user's values
            // TODO: fix
            _state.update {
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
                it.copy(
                    currentWeight = suggestedWeight?.toFloatOrNull() ?: 0f,
                    currentReps = suggestedReps?.toIntOrNull() ?: it.currentReps,
                    tareBarbell = suggestedTare ?: it.tareBarbell,
                    tareIndex = suggestedTareIndex ?: it.tareIndex
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
        ) { index, exercises, exercisesSetsDone, suggestedRepsWeight, imperialSystem ->
            val currentExercise = exercises.getOrNull(index) ?: return@combine

            val setsDone = exercisesSetsDone.getOrNull(index) ?: 0
            // Determine if we're continuing the same exercise or moving to a new one
            // index is not reliable because gets +1ed as soon as we start rest
            val isNextSetSameExercise = setsDone != 0  // this must be a new exercise
            val repsWeight = if (isNextSetSameExercise)
                suggestedRepsWeight.getOrNull(index)
            else
                suggestedRepsWeight.getOrNull(index-1)
            if (repsWeight == null) {
                return@combine
            }

            val nextExercise = if (!isNextSetSameExercise) {
                exercises.getOrNull(index)
            } else null

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
                currentExercise.getReps(setsDone)
            } else if (
                !isNextSetSameExercise &&
                nextExerciseRepsWeight != null &&
                nextExerciseRepsWeight.repsCount > 0
            )
                nextExerciseRepsWeight.getReps(0).toIntOrNull()
            else null

            val equipment = Equipment.fromResKey(currentExercise.equipment)
            val unitString = repository.stringResToString(if (imperialSystem) sharedR.string.lb else sharedR.string.kg)
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
                } else {
                    // Same exercise
                    val currentWeight = if (setsDone > 0) {
                        repsWeight.getWeight(setsDone-1).toFloatOrNull()
                    } else null
                    val currentReps = if (setsDone > 0) {
                        repsWeight.getReps(setsDone-1).toIntOrNull()
                    } else null
                    if (currentWeight == null || currentReps == null) return@combine
                    if (nextWeight != null && currentWeight > 0) {
                        // weight is changing
                        val weightDiff = nextWeight - currentWeight
                        when {
                            weightDiff > 0 -> {
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
                                        InRestHint(
                                            titleResId = R.string.rest_hint_increase_weight_title,
                                            descResId = R.string.rest_hint_increase_weight_generic,
                                            descVarArgs = listOf(nextWeight, unitString)
                                        )
                                    }
                                }
                            }

                            weightDiff < 0 -> {
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
                    inRestHints = hintList
                )
            }
        }.collect()

    }


    companion object {
        const val TIME_REFRESH_DELAY_MILLIS = 500L  // how much delay before currentTime is updated
    }
}
package agdesigns.elevatefitness.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class HomeState(
    val exerciseName: String = "",
    val setsDone: Int = 0,
    val reps: List<Int> = emptyList(),
    val weight: Float = 0f,
    val rest: List<Int> = emptyList(),
    val restTimestamp: ZonedDateTime? = null,
    val note: String = "",
    val currentTime: ZonedDateTime = ZonedDateTime.now(),
    val currentReps: Int = 0,
    val exerciseIncrement: Float = 0.5f,
    val nextExerciseName: String = "",
    val imageBitmap: Bitmap? = null,
    val equipment: String = "",
    val barbellNames: List<String> = emptyList(),
    val barbellSizes: List<Float> = emptyList(),
    val tareIndex: Int = 0,
    val imperialSystem: Boolean = false
)

sealed class HomeEvent {
    data object ResetRest: HomeEvent()
    data class ChangeReps(val change: Int): HomeEvent()
    data class ChangeWeight(val change: Int): HomeEvent()
    data object CompleteSet: HomeEvent()
    data object ForceSync: HomeEvent()
    data class ChangeTare(val newIndex: Int): HomeEvent()
}


@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: WearRepository): ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeWearWorkout().collect { workout ->
                val setsDone = (workout.setsDone ?: state.value.setsDone)
                val reps = workout.reps ?: state.value.reps
                val currentReps = reps.getOrNull(setsDone) ?: state.value.currentReps
                var exerciseIncrement = workout.exerciseIncrement ?: state.value.exerciseIncrement
                if (exerciseIncrement == 0f) {
                    exerciseIncrement = state.value.exerciseIncrement  // FIXME: sometimes arrives 0, why?
                }
                var tareIndex: Int? = null
                var barbellSizes: List<Float>? = workout.barbellSizes
                if (workout.tareBarbellName != null && workout.barbellNames != null) {
                    tareIndex = workout.barbellNames.indexOf(workout.tareBarbellName)
                    if (tareIndex == -1) {
                        // is other, format is "Other (number kg/lb)" extract number
                        val regex = Regex("""Other \((\d+(\.\d+)?)""")
                        val match = regex.find(workout.tareBarbellName)
                        val number = match?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                        barbellSizes = barbellSizes!!.dropLast(1).plus(number)
                        tareIndex = barbellSizes!!.indexOf(number)
                    }
                }
                Log.d("HomeViewModel", "got wear workout: $workout")
                _state.update {
                    it.copy(
                        exerciseName = workout.exerciseName ?: state.value.exerciseName,
                        setsDone = workout.setsDone ?: state.value.setsDone,
                        reps = reps,
                        weight = workout.weight ?: state.value.weight,
                        rest = workout.rest ?: state.value.rest,
                        note = workout.note ?: state.value.note,
                        restTimestamp = workout.restTimestamp?.let {
                            ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(it),
                                ZoneId.systemDefault()
                            )
                        } ?: state.value.restTimestamp,
                        currentReps = currentReps,
                        exerciseIncrement = exerciseIncrement,
                        nextExerciseName = workout.nextExerciseName ?: state.value.nextExerciseName,
                        equipment = workout.equipment ?: state.value.equipment,
                        barbellNames = workout.barbellNames ?: state.value.barbellNames,
                        barbellSizes = barbellSizes ?: state.value.barbellSizes,
                        tareIndex = tareIndex ?: state.value.tareIndex,
                        imperialSystem = workout.imperialSystem ?: state.value.imperialSystem
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeWearImage().collect { image ->
                _state.update { it.copy(imageBitmap = image) }
            }
        }
        viewModelScope.launch {
            repository.isPhoneAlive().collect {
                // reset state
                if (!it) {
                    _state.value = HomeState()
                }
            }
        }
        viewModelScope.launch {
            repository.observeWorkoutInterrupted().collect {
                if (it) {
                    _state.value = HomeState()
                }
            }
        }
        startTimer()
        onEvent(HomeEvent.ForceSync) // request sync once
    }

    fun onEvent(event: HomeEvent){
        when (event) {
            is HomeEvent.ResetRest -> {
                viewModelScope.launch {
                    _state.update { it.copy(restTimestamp = ZonedDateTime.now()) }
                }
            }
            is HomeEvent.ChangeReps -> {
                _state.update { it.copy(currentReps = state.value.currentReps + event.change) }
            }
            is HomeEvent.ChangeWeight -> {
                val deincrement = state.value.exerciseIncrement * event.change
                _state.update { it.copy(weight = state.value.weight + deincrement) }
            }
            is HomeEvent.CompleteSet -> {
                viewModelScope.launch {
                    val tare = if (state.value.equipment.lowercase().contains("barbell"))
                        state.value.barbellSizes[state.value.tareIndex]
                    else 0f
                    repository.completeSet(
                        state.value.exerciseName,
                        state.value.currentReps,
                        state.value.weight,
                        tare
                    )
                }

            }
            is HomeEvent.ForceSync -> {
                viewModelScope.launch {
                    repository.forceSync()
                }
            }
            is HomeEvent.ChangeTare -> {
                _state.update { it.copy(tareIndex = event.newIndex) }
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
                delay(100)
            }
        }.onEach {
            _state.update { it.copy(currentTime = ZonedDateTime.now()) }
        }.launchIn(viewModelScope)
    }
}
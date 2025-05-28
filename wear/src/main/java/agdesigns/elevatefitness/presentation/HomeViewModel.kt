package agdesigns.elevatefitness.presentation

import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.ZonedDateTime
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class HomeState(
    val exerciseName: String = "",
    val setsDone: Int = 0,
    val reps: List<Int> = emptyList(),
    val weight: Float = 0f,
    val rest: List<Int> = emptyList(),
    val restTimestampDec: Long? = null,  // This is different from restTimestamp in the app
    val note: String = "",
    val timeDec: Long? = null,
    val currentReps: Int = 0,
    val exerciseIncrement: Float = 0f,
    val nextExerciseName: String = "",
    val imageBitmap: Bitmap? = null
)

sealed class HomeEvent {
    data object ResetRest: HomeEvent()
    data class ChangeReps(val change: Int): HomeEvent()
    data class ChangeWeight(val change: Int): HomeEvent()
    data object CompleteSet: HomeEvent()
}


@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: WearRepository): ViewModel() {
    private val _state = mutableStateOf(HomeState())
    val state: State<HomeState> = _state
    private var timerJob: Job? = null
    private var syncTimerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeWearWorkout().collect {
                val setsDone = (it.setsDone ?: state.value.setsDone)
                val reps = it.reps ?: state.value.reps
                val currentReps = reps.getOrNull(setsDone) ?: state.value.currentReps

                _state.value = state.value.copy(
                    exerciseName = it.exerciseName ?: state.value.exerciseName,
                    setsDone = it.setsDone ?: state.value.setsDone,
                    reps = reps,
                    weight = it.weight ?: state.value.weight,
                    rest = it.rest ?: state.value.rest,
                    note = it.note ?: state.value.note,
                    restTimestampDec = it.restTimestamp?.div(100) ?: state.value.restTimestampDec,
                    currentReps = currentReps,
                    exerciseIncrement = it.exerciseIncrement ?: state.value.exerciseIncrement,
                    nextExerciseName = it.nextExerciseName ?: state.value.nextExerciseName, // FIXME: if no more exercises, this is null
                )
            }
        }
        viewModelScope.launch {
            repository.observeWearImage().collect {
                _state.value = state.value.copy(imageBitmap = it)
            }
        }
        _state.value = state.value.copy(timeDec = ZonedDateTime.now() .timeInMillis / 100)
        startTimer()
        startSyncTimer()
    }

    fun onEvent(event: HomeEvent){
        when (event) {
            is HomeEvent.ResetRest -> {
                viewModelScope.launch {
                    _state.value = state.value.copy(restTimestampDec = 0L)
                }
            }
            is HomeEvent.ChangeReps -> {
                _state.value = state.value.copy(currentReps = state.value.currentReps + event.change)
            }
            is HomeEvent.ChangeWeight -> {
                val deincrement = state.value.exerciseIncrement * event.change
                _state.value = state.value.copy(weight = state.value.weight + deincrement)
            }
            is HomeEvent.CompleteSet -> {
                viewModelScope.launch {
                    repository.completeSet(
                        state.value.exerciseName,
                        state.value.currentReps,
                        state.value.weight
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
                delay(100)
            }
        }.onEach {
            // FIXME: perhaps store ZonedDateTime instead of time millis
            _state.value = state.value.copy(timeDec = ZonedDateTime.now().toInstant().toEpochMilli() / 100)
        }.launchIn(viewModelScope)
    }

    // timer to force sync
    private fun startSyncTimer(){
        syncTimerJob?.cancel(CancellationException("Duplicate call"))
        syncTimerJob = flow {
            var counter = 0
            while (true) {
                emit(counter++)
                delay(1000)
            }
        }.onEach {
            if (state.value.exerciseName.isBlank()) {
                repository.forceSync()
            }
        }.launchIn(viewModelScope)
    }
}
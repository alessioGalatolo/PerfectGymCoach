package agdesigns.elevatefitness.presentation.screens.home

import agdesigns.elevatefitness.data.WearRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val workoutRunningFromPhone: Boolean = false,
)

sealed class HomeEvent {
    data object ForceSync: HomeEvent()
}


@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: WearRepository): ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    val activeWorkout = repository.activeWorkoutFlow
    val permissionStateDataStore = repository.permissionStateDataStore

    init {
        viewModelScope.launch {
            repository.observeWorkoutActive().collect { isActive ->
                _state.update { it.copy(workoutRunningFromPhone = isActive) }
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
        onEvent(HomeEvent.ForceSync) // request sync once
    }

    fun onEvent(event: HomeEvent){
        when (event) {
            is HomeEvent.ForceSync -> {
                viewModelScope.launch {
                    repository.forceSync()
                }
            }

        }

    }
}
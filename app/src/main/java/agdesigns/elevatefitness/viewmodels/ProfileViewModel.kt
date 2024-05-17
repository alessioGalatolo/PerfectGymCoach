package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.Sex
import agdesigns.elevatefitness.data.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val weight: Float = 0f,
    val userYear: Int = 0,
    val height: Float = 0f,
    val sex: Sex = Sex.OTHER,
    val theme: Theme = Theme.SYSTEM,
    val name: String = "",
    val imperialSystem: Boolean = false,
    val incrementBodyweight: Float = 0f,
    val incrementBarbell: Float = 0f,
    val incrementDumbbell: Float = 0f,
    val incrementMachine: Float = 0f,
    val incrementCable: Float = 0f,
)

sealed class ProfileEvent{
    data class UpdateWeight(val newWeight: Float): ProfileEvent()

    data class UpdateAgeYear(val newYear: Int): ProfileEvent()

    data class UpdateHeight(val newHeight: Float): ProfileEvent()

    data class UpdateName(val newName: String): ProfileEvent()

    data class UpdateSex(val newSex: Sex): ProfileEvent()

    data class UpdateTheme(val newTheme: Theme): ProfileEvent()

    data class UpdateIncrementBodyweight(val newIncrement: Float): ProfileEvent()

    data class UpdateIncrementBarbell(val newIncrement: Float): ProfileEvent()

    data class UpdateIncrementDumbbell(val newIncrement: Float): ProfileEvent()

    data class UpdateIncrementMachine(val newIncrement: Float): ProfileEvent()

    data class UpdateIncrementCable(val newIncrement: Float): ProfileEvent()

    data class SwitchImperialSystem(val newValue: Boolean): ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repository: Repository): ViewModel() {
    private val _state = mutableStateOf(ProfileState())
    val state: State<ProfileState> = _state

    init {
        viewModelScope.launch {
            repository.getUserWeight().collect {
                _state.value = state.value.copy(weight = it)
            }
        }
        viewModelScope.launch {
            repository.getUserHeight().collect {
                _state.value = state.value.copy(height = it)
            }
        }
        viewModelScope.launch {
            repository.getUserSex().collect {
                _state.value = state.value.copy(sex = it)
            }
        }
        viewModelScope.launch {
            repository.getUserName().collect {
                _state.value = state.value.copy(name = it)
            }
        }
        viewModelScope.launch {
            repository.getUserYear().collect {
                _state.value = state.value.copy(userYear = it)
            }
        }
        viewModelScope.launch {
            repository.getImperialSystem().collect {
                _state.value = state.value.copy(imperialSystem = it)
            }
        }
        viewModelScope.launch {
            repository.getTheme().collect {
                _state.value = state.value.copy(theme = it)
            }
        }
        viewModelScope.launch {
            repository.getBodyweightIncrement().collect {
                _state.value = state.value.copy(incrementBodyweight = it)
            }
        }
        viewModelScope.launch {
            repository.getBarbellIncrement().collect {
                _state.value = state.value.copy(incrementBarbell = it)
            }
        }
        viewModelScope.launch {
            repository.getDumbbellIncrement().collect {
                _state.value = state.value.copy(incrementDumbbell = it)
            }
        }
        viewModelScope.launch {
            repository.getMachineIncrement().collect {
                _state.value = state.value.copy(incrementMachine = it)
            }
        }
        viewModelScope.launch {
            repository.getCableIncrement().collect {
                _state.value = state.value.copy(incrementCable = it)
            }
        }
    }

    fun onEvent(event: ProfileEvent){
        when (event) {
            is ProfileEvent.UpdateName -> {
                viewModelScope.launch {
                    repository.setUserName(event.newName)
                }
            }
            is ProfileEvent.UpdateSex -> {
                viewModelScope.launch {
                    repository.setUserSex(event.newSex)
                }
            }
            is ProfileEvent.UpdateWeight -> {
                viewModelScope.launch {
                    repository.setUserWeight(event.newWeight)
                }
            }
            is ProfileEvent.UpdateHeight -> {
                viewModelScope.launch {
                    repository.setUserHeight(event.newHeight)
                }
            }
            is ProfileEvent.UpdateAgeYear -> {
                viewModelScope.launch {
                    repository.setUserYear(event.newYear)
                }
            }
            is ProfileEvent.SwitchImperialSystem -> {
                viewModelScope.launch {
                    repository.setImperialSystem(event.newValue)
                }
            }
            is ProfileEvent.UpdateTheme -> {
                viewModelScope.launch {
                    repository.setTheme(event.newTheme)
                }
            }
            is ProfileEvent.UpdateIncrementBarbell -> {
                viewModelScope.launch {
                    repository.setBarbellIncrement(event.newIncrement)
                }
            }
            is ProfileEvent.UpdateIncrementBodyweight -> {
                viewModelScope.launch {
                    repository.setBodyweightIncrement(event.newIncrement)
                }
            }
            is ProfileEvent.UpdateIncrementCable -> {
                viewModelScope.launch {
                    repository.setCableIncrement(event.newIncrement)
                }
            }
            is ProfileEvent.UpdateIncrementDumbbell -> {
                viewModelScope.launch {
                    repository.setDumbbellIncrement(event.newIncrement)
                }
            }
            is ProfileEvent.UpdateIncrementMachine -> {
                viewModelScope.launch {
                    repository.setMachineIncrement(event.newIncrement)
                }
            }
        }
    }

}

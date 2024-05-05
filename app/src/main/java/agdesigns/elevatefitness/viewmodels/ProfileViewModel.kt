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
    val imperialSystem: Boolean = false
)

sealed class ProfileEvent{
    data class UpdateWeight(val newWeight: Float): ProfileEvent()

    data class UpdateAgeYear(val newYear: Int): ProfileEvent()

    data class UpdateHeight(val newHeight: Float): ProfileEvent()

    data class UpdateName(val newName: String): ProfileEvent()

    data class UpdateSex(val newSex: Sex): ProfileEvent()

    data class UpdateTheme(val newTheme: Theme): ProfileEvent()

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
        }
    }

}

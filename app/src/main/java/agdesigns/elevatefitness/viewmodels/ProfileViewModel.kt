package agdesigns.elevatefitness.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.Sex
import agdesigns.elevatefitness.data.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getUserWeight(),
                repository.getUserHeight(),
                repository.getUserSex(),
                repository.getUserName(),
                repository.getUserYear(),
                repository.getImperialSystem(),
                repository.getTheme(),
                repository.getBodyweightIncrement(),
                repository.getBarbellIncrement(),
                repository.getDumbbellIncrement(),
                repository.getMachineIncrement(),
                repository.getCableIncrement()
            ) { values: Array<Any?> ->
                _state.update {
                    it.copy(
                        weight = values[0] as Float,
                        height = values[1] as Float,
                        sex = values[2] as Sex,
                        name = values[3] as String,
                        userYear = values[4] as Int,
                        imperialSystem = values[5] as Boolean,
                        theme = values[6] as Theme,
                        incrementBodyweight = values[7] as Float,
                        incrementBarbell = values[8] as Float,
                        incrementDumbbell = values[9] as Float,
                        incrementMachine = values[10] as Float,
                        incrementCable = values[11] as Float
                    )
                }
            }.collect()
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

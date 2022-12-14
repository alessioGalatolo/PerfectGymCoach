package com.anexus.perfectgymcoach.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anexus.perfectgymcoach.data.Repository
import com.ramcosta.composedestinations.annotation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val weight: Float = 0f,
    val userYear: Int = 0,
    val height: Float = 0f,
    val sex: String = "",
    val name: String = ""
)

sealed class ProfileEvent{
    data class UpdateWeight(val newWeight: Float): ProfileEvent()

    data class UpdateAgeYear(val newYear: Int): ProfileEvent()

    data class UpdateHeight(val newHeight: Float): ProfileEvent()

    data class UpdateName(val newName: String): ProfileEvent()

    data class UpdateSex(val newSex: String): ProfileEvent()
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
        }
    }

}

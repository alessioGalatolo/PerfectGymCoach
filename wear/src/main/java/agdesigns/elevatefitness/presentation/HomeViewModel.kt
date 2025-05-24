package agdesigns.elevatefitness.presentation

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val exerciseName: String = "",
    val reps: Int = 0,
    val weight: Float = 0f,
    val rest: Int = 0,
    val note: String = ""
)


@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: WearRepository): ViewModel() {
    private val _state = mutableStateOf(HomeState())
    val state: State<HomeState> = _state

    init {
        viewModelScope.launch {
            repository.incomingMessages.collect { json ->
                _state.value = state.value.copy(
                    exerciseName = json.getString("exerciseName"),
                    reps = json.getInt("reps"),
                    weight = json.getDouble("weight").toFloat(),
                    rest = json.getInt("rest"),
                    note = json.getString("note")
                )
            }
        }
    }
}
package agdesigns.elevatefitness.ui.screens.plans

import agdesigns.elevatefitness.data.PreferenceRepository
import agdesigns.elevatefitness.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramDays(
    val id: Long,
    val dayIndex: Int,
    val muscleOrdinals: Set<Int>
)

@HiltViewModel
class CustomizePlanViewModel @Inject constructor(
    private val preferences: PreferenceRepository,
    private val repository: Repository
) : ViewModel() {

    val excludedExerciseIds: StateFlow<Set<Long>> = preferences
        .getExcludedPlanExercises()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val hasPreviousWorkouts = repository.getWorkoutHistory().map {
        it.any { it.durationSeconds > 0L }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _customDays = MutableStateFlow<List<ProgramDays>>(listOf(
        ProgramDays(
            id = 0L,
            dayIndex = 0,
            muscleOrdinals = emptySet()
        )
    ))
    val customDays = _customDays.asStateFlow()

    fun saveExcludedExerciseIds(ids: Set<Long>) {
        viewModelScope.launch {
            preferences.setExcludedPlanExercises(ids)
        }
    }

    fun addExcludedExercise(id: Long) {
        viewModelScope.launch {
            preferences.addExcludedPlanExercise(id)
        }
    }

    fun addCustomDay() {
        _customDays.update { days ->
            val maxId = days.maxOfOrNull { it.id } ?: 0L
            days + listOf(
                ProgramDays(
                    id = maxId + 1,
                    dayIndex = days.size,
                    muscleOrdinals = emptySet()
                )
            )
        }
    }

    fun removeCustomDay(dayIndex: Int) {
        _customDays.update { days ->
            if (dayIndex in days.indices) {
                val updatedDays = days.toMutableList()
                updatedDays.removeAt(dayIndex)
                // update dayIndex for remaining days
                for (i in dayIndex until updatedDays.size) {
                    updatedDays[i] = updatedDays[i].copy(dayIndex = i)
                }
                updatedDays
            } else {
                days
            }
        }
    }

    fun toggleMuscleToDay(dayIndex: Int, muscleOrdinal: Int) {
        _customDays.update { days ->
            val updatedDays = days.toMutableList()
            while (updatedDays.size <= dayIndex) {
                addCustomDay()
            }
            val targetDay = updatedDays[dayIndex]
            if (muscleOrdinal in targetDay.muscleOrdinals) {
                updatedDays[dayIndex] = targetDay.copy(
                    muscleOrdinals = targetDay.muscleOrdinals - muscleOrdinal
                )
            } else {
                updatedDays[dayIndex] = targetDay.copy(
                    muscleOrdinals = targetDay.muscleOrdinals + muscleOrdinal
                )
            }
            updatedDays
        }
    }

    fun reorderDays(fromId: Long, toId: Long) {
        _customDays.update { days ->
            val updatedDays = days.toMutableList()
            val ids = updatedDays.map { it.id }
            if (fromId in ids && toId in ids) {
                val fromIndex = ids.indexOf(fromId)
                val toIndex = ids.indexOf(toId)
                val fromItem = updatedDays[fromIndex]
                val toItem = updatedDays[toIndex]
                updatedDays[fromIndex] = toItem.copy(dayIndex = fromIndex)
                updatedDays[toIndex] = fromItem.copy(dayIndex = toIndex)
            }
            updatedDays
        }
    }
}

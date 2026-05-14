package agdesigns.elevatefitness.ui.screens.plans

import agdesigns.elevatefitness.data.PreferenceRepository
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.SharableElement
import agdesigns.elevatefitness.data.SharedWorkoutPlanModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ReceivePlanState(
    val plan: SharedWorkoutPlanModel? = null,
    val parseError: Boolean = false,
    val imported: Boolean = false,
    val importing: Boolean = false,
    val newPlanId: Long? = null
)

@HiltViewModel
class ReceivePlanViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReceivePlanState())
    val state: StateFlow<ReceivePlanState> = _state.asStateFlow()

    fun parseSharedText(rawText: String) {
        try {
            _state.update {
                it.copy(plan = Json.decodeFromString(rawText))
            }
        } catch (_: Exception) {
            _state.update { it.copy(parseError = true) }
        }
    }

    fun importPlan() {
        val plan = _state.value.plan ?: return
        _state.update { it.copy(importing = true) }
        viewModelScope.launch {
            val planId = repository.importSharedPlan(plan)
            _state.update { it.copy(imported = true, importing = false, newPlanId = planId) }
            // if no current plan set, set it as this one
            preferences.setCurrentPlan(planId, overrideValue = false)
        }
    }
}

package agdesigns.elevatefitness.ui.screens.view_exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.SearchesRepository
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseReorder
import agdesigns.elevatefitness.data.db.entity.UpdateExerciseSuperset
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import agdesigns.elevatefitness.shared.Equipment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


enum class SearchField { Name, Variation, Equipment, Muscle, Tag }

data class FieldHighlight(
    val field: SearchField,
    val index: Int? = null, // e.g. variation index if field == Variation
    val ranges: List<IntRange>, // character ranges in ORIGINAL (non-normalized) text
    val reasonLabel: String // short label for UI chips (e.g. "Name", "Variation: Incline")
)

data class ExerciseSearchResult(
    val exercise: Exercise,
    val score: Int,
    val highlights: List<FieldHighlight>, // used by UI to highlight
    val reasons: List<String> // compact reason labels for chips
)


data class ExercisesState(
    val programExercisesAndInfo: List<ProgramExerciseAndInfo> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val equipToFiler: Equipment = Equipment.EVERYTHING,  // store request to filter
    val exercisesFilterEquip: List<Exercise>? = null,
    val searchQuery: String = "",  // store request to search
    @Deprecated("Use searchResults instead")
    val exercisesToDisplay: List<Exercise>? = null,
    val searchResults: List<ExerciseSearchResult>? = null,
    val recentSearchesAll: List<String> = emptyList(), // all
    val recentSearches: List<String> = emptyList()  // to display
)

sealed class ExercisesEvent{
    data class GetExercises(val muscle: Exercise.Muscle): ExercisesEvent()

    data class FilterExercise(val query: String, val forceRefilter: Boolean = false): ExercisesEvent()

    data class FilterExerciseEquipment(val query: Equipment): ExercisesEvent()

    data class AddRecentSearch(val search: String): ExercisesEvent()
}

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val repository: Repository,
    private val searchesRepository: SearchesRepository,
): ViewModel() {
    private val _state = MutableStateFlow(ExercisesState())
    val state: StateFlow<ExercisesState> = _state.asStateFlow()
    // having it here instead of inside composable creates less problems with collecting text changes
    val searchFieldState: TextFieldState = TextFieldState()

    private var getExercisesJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            searchesRepository.recent.collect { recent ->
                _state.update { state ->
                    state.copy(
                        recentSearchesAll = recent,
                        recentSearches = recent.filter {
                            it.contains(state.searchQuery, ignoreCase = true)
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            snapshotFlow { searchFieldState.text }
                .collectLatest { queryText ->
                    onEvent(ExercisesEvent.FilterExercise(queryText.toString()))
                }
        }
    }

    fun onEvent(event: ExercisesEvent){
        when (event) {
            is ExercisesEvent.GetExercises -> {
                getExercisesJob?.cancel()
                getExercisesJob = viewModelScope.launch {
                    repository.getExercises(event.muscle).collect { exs ->
                        val sorted = exs.sortedBy { ex ->
                            ex.name
                        }
                        _state.update { it.copy(
                            exercises = sorted
                        ) }
                        // got new exercises, now re-apply equip filter
                        onEvent(ExercisesEvent.FilterExerciseEquipment(state.value.equipToFiler))
                        // Don't call FilterExercise, it is already called from FilterExerciseEquipment
//                        onEvent(ExercisesEvent.FilterExercise(state.value.searchQuery))
                    }
                }
            }
            is ExercisesEvent.FilterExerciseEquipment -> {
                val filtered = state.value.exercises.filter {
                    event.query == Equipment.EVERYTHING || it.equipment == event.query
                }
                _state.update { it.copy(
                    exercisesFilterEquip = filtered,
                    equipToFiler = event.query
                ) }
                onEvent(
                    ExercisesEvent.FilterExercise(
                        state.value.searchQuery,
                        forceRefilter = true
                    )
                )
            }
            is ExercisesEvent.FilterExercise -> {
                // do not execute on main thread
                if (event.query == state.value.searchQuery && !event.forceRefilter)
                    return
                Log.d("ExercisesViewModel", "Filtering exercises for query ${event.query}")
                searchJob?.cancel()
                searchJob = viewModelScope.launch(Dispatchers.IO) {
                    // do it asap to avoid re-running this query
                    _state.update { it.copy(searchQuery = event.query) }
                    val all = _state.value.exercisesFilterEquip ?: emptyList()

                    val results = all.mapNotNull { ex -> matchExercise(ex, event.query) }
                        .sortedWith(
                            compareByDescending<ExerciseSearchResult> { it.score }
                                .thenBy { it.exercise.name.lowercase() }
                        )
                    Log.d("ExercisesViewModel", "Found ${results.size} results")
                    val recent = state.value.recentSearchesAll.filter { recent ->
                        recent.contains(event.query, ignoreCase = true)
                    }
                    _state.update {
                        it.copy(
                            searchResults = results,
                            recentSearches = recent
                        )
                    }
                    Log.d("ExercisesViewModel", "Filtered exercises for query ${event.query}")
                }
            }
            is ExercisesEvent.AddRecentSearch -> {
                viewModelScope.launch {
                    searchesRepository.push(event.search)
                }
            }
        }
    }
}

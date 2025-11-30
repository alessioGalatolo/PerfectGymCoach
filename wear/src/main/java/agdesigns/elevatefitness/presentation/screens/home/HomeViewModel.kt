package agdesigns.elevatefitness.presentation.screens.home

import agdesignes.elevatefitness.shared.grpc.MediaServiceGrpcKt
import agdesignes.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.data.WearRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoFlow
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val workoutRunningFromPhone: Boolean = false,
)

sealed class HomeEvent {
    data object ForceSync: HomeEvent()
}


@OptIn(ExperimentalHorologistApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WearRepository,
    private val registry: WearDataLayerRegistry
): ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    val activeWorkout = registry.protoFlow<Workout.WorkoutStaticData>(TargetNodeId.PairedPhone).map {
        it.activeWorkout
    }.distinctUntilChanged()
    val permissionStateDataStore = repository.permissionStateDataStore

    fun onEvent(event: HomeEvent){
        when (event) {
            // TODO: remove
            is HomeEvent.ForceSync -> {

            }

        }

    }
}
package agdesigns.elevatefitness.presentation.screens.home

import agdesigns.elevatefitness.shared.grpc.MediaServiceGrpcKt
import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.data.WearRepository
import agdesigns.elevatefitness.shared.grpc.Info
import agdesigns.elevatefitness.shared.grpc.PhoneInfoServiceGrpcKt
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoFlow
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.protobuf.Empty
import dagger.hilt.android.lifecycle.HiltViewModel
import io.grpc.StatusException
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
    val phoneVersionInfo: Info.VersionInfo? = null
)

sealed class HomeEvent {
    data object ForceSync: HomeEvent()
}


@OptIn(ExperimentalHorologistApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WearRepository,
    private val registry: WearDataLayerRegistry,
    private val phoneInfoService: PhoneInfoServiceGrpcKt.PhoneInfoServiceCoroutineStub
): ViewModel() {
    val hasExactAlarm = repository.hasExactAlarm
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    val activeWorkout = registry.protoFlow<Workout.WorkoutStaticData>(TargetNodeId.PairedPhone).map {
        it.activeWorkout
    }.distinctUntilChanged()
    val permissionStateDataStore = repository.permissionStateDataStore

    init {
        viewModelScope.launch {
            try {
                val versionInfo = phoneInfoService.versionInfo(Empty.newBuilder().build())
                _state.update {
                    it.copy(phoneVersionInfo = versionInfo)
                }
            } catch (e: StatusException) {
                Log.e("HomeViewModel", "Error getting version info with error: ${e.message}")
            }
        }
    }

    fun onEvent(event: HomeEvent){
        when (event) {
            // TODO: remove
            is HomeEvent.ForceSync -> {

            }

        }

    }
}
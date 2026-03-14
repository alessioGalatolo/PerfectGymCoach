package agdesigns.elevatefitness.presentation.screens.home

import agdesigns.elevatefitness.shared.grpc.MediaServiceGrpcKt
import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.data.WearRepository
import agdesigns.elevatefitness.shared.compareTo
import agdesigns.elevatefitness.shared.grpc.Info
import agdesigns.elevatefitness.shared.grpc.Info.VersionInfo
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
    val phoneVersionInfo: VersionInfo? = null,
    val incompatibleVersion: Boolean = false
)

sealed class HomeEvent {
    data object ForceSync: HomeEvent()
    data object RetryVersionCheck: HomeEvent()
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
        checkPhoneAppVersion()
    }

    fun onEvent(event: HomeEvent){
        when (event) {
            // TODO: remove
            is HomeEvent.ForceSync -> {

            }
            is HomeEvent.RetryVersionCheck -> {
                checkPhoneAppVersion()
            }
        }

    }

    fun checkPhoneAppVersion() {
        viewModelScope.launch {
            try {
                val versionInfo = phoneInfoService.versionInfo(Empty.newBuilder().build())
                _state.update {
                    it.copy(
                        phoneVersionInfo = versionInfo,
                        // whether this watch version is too updated for phone
                        incompatibleVersion = versionInfo.versionName <
                                Info.VersionName.newBuilder()
                                    .setMajor(0)
                                    .setMinor(0)
                                    .setPatch(8)
                                    .build()
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting version info: ${e.message}")
            }
        }
    }
}
package agdesigns.elevatefitness.ui.screens.profile

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.BackupRepository
import agdesigns.elevatefitness.data.HealthConnectRepository
import agdesigns.elevatefitness.data.PreferenceRepository
import agdesigns.elevatefitness.shared.grpc.WearInfoServiceGrpcKt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.db.entity.Sex
import agdesigns.elevatefitness.data.db.entity.Theme
import android.os.Build
import android.net.Uri
import android.util.Log
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import com.google.protobuf.Empty
import dagger.hilt.android.lifecycle.HiltViewModel
import io.grpc.StatusException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

data class ProfileState(
    val weight: Float = 0f,
    val userBirthday: ZonedDateTime = ZonedDateTime.now(),
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
    val language: String? = null,
    val isBackupLoading: Boolean = false,
    val isPreferencesBackupLoading: Boolean = false,
    val backupOutcomeResId: Int? = null,
    val lockHorizontalScroll: Boolean = false,
    val autoOpenWear: Boolean = false,
    val isHealthConnectAvailable: Boolean = false,
    val hasHealthConnectPermissions: Boolean = false,
    val hasSomeHealthConnectPermissions: Boolean = false,
    val suggestRepsWeight: Boolean = true,
    val suggestWorkoutModifications: Boolean = true,
    val inRestHints: Boolean = true,
    val tempoRomTracking: Boolean = false,
    val hasConnectedWatch: Boolean = false,
    val wearSupportsTempoRom: Boolean? = null,
)

sealed class ProfileEvent{
    data class UpdateWeight(val newWeight: Float): ProfileEvent()

    data class UpdateBirthday(val newBirthday: ZonedDateTime): ProfileEvent()

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

    data class ToggleLockHorizontalScroll(val newValue: Boolean): ProfileEvent()

    data class ToggleAutoOpenWear(val newValue: Boolean): ProfileEvent()

    data class ChangeLanguage(val newLanguage: String?): ProfileEvent()

    data class ExportDatabase(val fileUri: Uri): ProfileEvent()

    data class ImportDatabase(val fileUri: Uri): ProfileEvent()

    data class ExportPreferences(val fileUri: Uri): ProfileEvent()

    data class ImportPreferences(val fileUri: Uri): ProfileEvent()

    data object ResetOutcomeMessage: ProfileEvent()

    data object RefreshHealthConnectStatus : ProfileEvent()

    data class ToggleSuggestRepsWeight(val newValue: Boolean): ProfileEvent()

    data class ToggleSuggestWorkoutModifications(val newValue: Boolean): ProfileEvent()

    data class ToggleInRestHints(val newValue: Boolean): ProfileEvent()

    data class ToggleTempoRomTracking(val newValue: Boolean): ProfileEvent()
}

@OptIn(ExperimentalHorologistApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val preferences: PreferenceRepository,
    private val healthConnectRepository: HealthConnectRepository,
    private val phoneDataLayerAppHelper: PhoneDataLayerAppHelper,
    private val wearInfoService: WearInfoServiceGrpcKt.WearInfoServiceCoroutineStub,
): ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isHealthConnectAvailable = healthConnectRepository.isAvailable,
                    hasHealthConnectPermissions = healthConnectRepository.hasAllPermissions(),
                    hasSomeHealthConnectPermissions = healthConnectRepository.hasSomePermissions()
                )
            }
        }
        viewModelScope.launch {
            combine(
                preferences.getUserWeight(),
                preferences.getUserHeight(),
                preferences.getUserSex(),
                preferences.getUserName(),
                preferences.getUserBirthday(),
                preferences.getImperialSystem(),
                preferences.getTheme(),
                preferences.getBodyweightIncrement(),
                preferences.getBarbellIncrement(),
                preferences.getDumbbellIncrement(),
                preferences.getMachineIncrement(),
                preferences.getCableIncrement(),
                preferences.getLanguage(),
                preferences.getLockHorizontalScroll(),
                preferences.getAutoOpenWear(),
                preferences.getSuggestRepsWeight(),
                preferences.getSuggestWorkoutModifications(),
                preferences.getInRestHints(),
                preferences.getTempoRomTracking(),
            ) { values: Array<Any?> ->
                _state.update {
                    it.copy(
                        weight = values[0] as Float,
                        height = values[1] as Float,
                        sex = values[2] as Sex,
                        name = values[3] as String,
                        userBirthday = values[4] as ZonedDateTime,
                        imperialSystem = values[5] as Boolean,
                        theme = values[6] as Theme,
                        incrementBodyweight = values[7] as Float,
                        incrementBarbell = values[8] as Float,
                        incrementDumbbell = values[9] as Float,
                        incrementMachine = values[10] as Float,
                        incrementCable = values[11] as Float,
                        language = values[12] as String?,
                        lockHorizontalScroll = values[13] as Boolean,
                        autoOpenWear = values[14] as Boolean,
                        suggestRepsWeight = values[15] as Boolean,
                        suggestWorkoutModifications = values[16] as Boolean,
                        inRestHints = values[17] as Boolean,
                        tempoRomTracking = values[18] as Boolean,
                    )
                }
            }.collect()
        }
        viewModelScope.launch {
            val apiAvailable = phoneDataLayerAppHelper.isAvailable()

            if (apiAvailable) {
                try {
                    val hasDevice = phoneDataLayerAppHelper.connectedNodes().isNotEmpty()
                    _state.update { it.copy(hasConnectedWatch = hasDevice) }
                    val canTempoRomTrack = wearInfoService.getCapabilities(Empty.getDefaultInstance()).tempoRomTracking
                    _state.update {
                        it.copy(
                            wearSupportsTempoRom = canTempoRomTrack
                        )
                    }
                } catch (e: StatusException) {
                    Log.w("ProfileViewModel", "Watch capability check failed", e)
                }
            }
        }
        viewModelScope.launch {
            checkHealthConnectWeight()
        }
    }

    fun onEvent(event: ProfileEvent){
        when (event) {
            is ProfileEvent.UpdateName -> {
                viewModelScope.launch {
                    preferences.setUserName(event.newName)
                }
            }
            is ProfileEvent.UpdateSex -> {
                viewModelScope.launch {
                    preferences.setUserSex(event.newSex)
                }
            }
            is ProfileEvent.UpdateWeight -> {
                viewModelScope.launch {
                    preferences.setUserWeight(event.newWeight)
                    preferences.setWeightRecordDate(ZonedDateTime.now())
                    healthConnectRepository.writeWeight(event.newWeight.toDouble())
                }
            }
            is ProfileEvent.UpdateHeight -> {
                viewModelScope.launch {
                    preferences.setUserHeight(event.newHeight)
                }
            }
            is ProfileEvent.UpdateBirthday -> {
                viewModelScope.launch {
                    preferences.setUserBirthday(event.newBirthday)
                }
            }
            is ProfileEvent.SwitchImperialSystem -> {
                viewModelScope.launch {
                    preferences.setImperialSystem(event.newValue)
                }
            }
            is ProfileEvent.ToggleLockHorizontalScroll -> {
                viewModelScope.launch {
                    preferences.setLockHorizontalScroll(event.newValue)
                }
            }
            is ProfileEvent.ToggleAutoOpenWear -> {
                viewModelScope.launch {
                    preferences.setAutoOpenWear(event.newValue)
                }
            }
            is ProfileEvent.UpdateTheme -> {
                viewModelScope.launch {
                    preferences.setTheme(event.newTheme)
                }
            }
            is ProfileEvent.UpdateIncrementBarbell -> {
                viewModelScope.launch {
                    preferences.setBarbellIncrement(event.newIncrement)
                }
            }
            is ProfileEvent.UpdateIncrementBodyweight -> {
                viewModelScope.launch {
                    preferences.setBodyweightIncrement(event.newIncrement)
                }
            }
            is ProfileEvent.UpdateIncrementCable -> {
                viewModelScope.launch {
                    preferences.setCableIncrement(event.newIncrement)
                }
            }
            is ProfileEvent.UpdateIncrementDumbbell -> {
                viewModelScope.launch {
                    preferences.setDumbbellIncrement(event.newIncrement)
                }
            }
            is ProfileEvent.UpdateIncrementMachine -> {
                viewModelScope.launch {
                    preferences.setMachineIncrement(event.newIncrement)
                }
            }
            is ProfileEvent.ChangeLanguage -> {
                viewModelScope.launch {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (event.newLanguage != null) {
                            preferences.setLanguage(event.newLanguage)
                        } else {
                            preferences.resetLanguage()
                        }
                    }
                }
            }
            is ProfileEvent.ExportDatabase -> {
                viewModelScope.launch {
                    _state.update { it.copy(isBackupLoading = true) }
                    val r = backupRepository.backupDb(event.fileUri)
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    isBackupLoading = false,
                                    backupOutcomeResId = R.string.backup_success
                                )
                            }
                        }
                        .onFailure { error ->
                            _state.update {
                                it.copy(
                                    isBackupLoading = false,
                                    backupOutcomeResId = R.string.backup_error
                                )
                            }
                        }
                    Log.d("ProfileViewModel", "ExportDatabase: $r")
                }
            }
            is ProfileEvent.ImportDatabase -> {
                viewModelScope.launch {
                    _state.update { it.copy(isBackupLoading = true) }
                    val r = backupRepository.restoreDb(event.fileUri)
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    isBackupLoading = false,
                                    backupOutcomeResId = R.string.restore_success
                                )
                            }
                        }
                        .onFailure { error ->
                            _state.update {
                                it.copy(
                                    isBackupLoading = false,
                                    backupOutcomeResId = R.string.restore_error
                                )
                            }
                        }
                    Log.d("ProfileViewModel", "ImportDatabase: $r")
                }
            }
            is ProfileEvent.ExportPreferences -> {
                viewModelScope.launch {
                    _state.update { it.copy(isPreferencesBackupLoading = true) }
                    val r = backupRepository.backupPreferences(event.fileUri)
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    backupOutcomeResId = R.string.backup_success,
                                    isPreferencesBackupLoading = false
                                )
                            }
                        }
                        .onFailure {
                            _state.update {
                                it.copy(
                                    backupOutcomeResId = R.string.backup_error,
                                    isPreferencesBackupLoading = false
                                )
                            }
                        }
                    Log.d("ProfileViewModel", "ExportPreferences: $r")
                }
            }
            is ProfileEvent.ImportPreferences -> {
                viewModelScope.launch {
                    _state.update { it.copy(isPreferencesBackupLoading = true) }
                    val r = backupRepository.restorePreferences(event.fileUri)
                            .onSuccess {
                                _state.update {
                                    it.copy(
                                        backupOutcomeResId = R.string.restore_success,
                                        isPreferencesBackupLoading = false
                                    )
                                }
                            }
                            .onFailure {
                                _state.update {
                                    it.copy(
                                        backupOutcomeResId = R.string.restore_error,
                                        isPreferencesBackupLoading = false
                                    )
                                }
                            }
                    Log.d("ProfileViewModel", "ImportPreferences: $r")
                }
            }
            is ProfileEvent.ToggleSuggestRepsWeight -> {
                viewModelScope.launch {
                    preferences.setSuggestRepsWeight(event.newValue)
                }
            }
            is ProfileEvent.ToggleSuggestWorkoutModifications -> {
                viewModelScope.launch {
                    preferences.setSuggestWorkoutModifications(event.newValue)
                }
            }
            is ProfileEvent.ToggleInRestHints -> {
                viewModelScope.launch {
                    preferences.setInRestHints(event.newValue)
                }
            }
            is ProfileEvent.ToggleTempoRomTracking -> {
                viewModelScope.launch {
                    preferences.setTempoRomTracking(event.newValue)
                }
            }
            is ProfileEvent.ResetOutcomeMessage -> {
                _state.update { it.copy(backupOutcomeResId = null) }
            }
            is ProfileEvent.RefreshHealthConnectStatus -> {
                viewModelScope.launch {
                    val hasPermissions = healthConnectRepository.hasAllPermissions()
                    _state.update {
                        it.copy(
                            hasHealthConnectPermissions = hasPermissions,
                            hasSomeHealthConnectPermissions = healthConnectRepository.hasSomePermissions()
                        )
                    }
                    if (hasPermissions) {
                        // check/update weight
                        checkHealthConnectWeight()
                    }
                }
            }
        }
    }

    private suspend fun checkHealthConnectWeight() {
        // check if user has recorded a new weight on health connect
        val weight = healthConnectRepository.getWeight()
        // if is newer than what we have, override
        val ourDate = preferences.getWeightRecordDate().first()
        if (weight != null && weight.time.isAfter(ourDate.toInstant())) {
            Log.d("ProfileViewModel", "checkHealthConnectWeight: new weight")
            preferences.setUserWeight(weight.weight.inKilograms.toFloat())
            preferences.setWeightRecordDate(ZonedDateTime.ofInstant(weight.time, weight.zoneOffset))
        }
    }
}

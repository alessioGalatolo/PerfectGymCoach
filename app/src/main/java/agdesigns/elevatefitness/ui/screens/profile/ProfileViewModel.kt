package agdesigns.elevatefitness.ui.screens.profile

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.BackupRepository
import agdesigns.elevatefitness.data.PreferenceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import agdesigns.elevatefitness.data.db.entity.Sex
import agdesigns.elevatefitness.data.db.entity.Theme
import android.os.Build
import android.net.Uri
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
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
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val preferences: PreferenceRepository
): ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
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
                    )
                }
            }.collect()
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
            is ProfileEvent.ResetOutcomeMessage -> {
                _state.update { it.copy(backupOutcomeResId = null) }
            }
        }
    }

}

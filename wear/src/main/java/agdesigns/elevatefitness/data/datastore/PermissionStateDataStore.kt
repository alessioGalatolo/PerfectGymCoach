package agdesigns.elevatefitness.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PermissionStateDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        "permission_state_data_store",
    )

    /**
     * Returns whether the rationale for a permission has been shown to the user before.
     */
    fun hasPreviouslyShownRationale(permission: String): Flow<ShownRationaleStatus> =
        context.dataStore.data
            .map { preferences ->
                when (preferences[booleanPreferencesKey(permission)]) {
                    true -> ShownRationaleStatus.HAS_SHOWN
                    else -> ShownRationaleStatus.HAS_NOT_SHOWN
                }
            }

    /**
     * Updates whether the rationale for a permission has been shown to the user before.
     */
    suspend fun setHasPreviouslyShownRationale(hasShownRationale: ShownRationaleStatus, permission: String) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(permission)] =
                hasShownRationale == ShownRationaleStatus.HAS_SHOWN
        }
    }
}

enum class ShownRationaleStatus {
    HAS_SHOWN,
    HAS_NOT_SHOWN,
    UNKNOWN
}
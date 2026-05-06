package agdesigns.elevatefitness.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CalibrationDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val Context.calibDataStore: DataStore<Preferences> by preferencesDataStore("calibration_data_store")

    private val ACCEL_NOISE_X = floatPreferencesKey("accel_noise_x")
    private val ACCEL_NOISE_Y = floatPreferencesKey("accel_noise_y")
    private val ACCEL_NOISE_Z = floatPreferencesKey("accel_noise_z")
    private val IS_CALIBRATED = booleanPreferencesKey("is_calibrated")

    val isCalibrated: Flow<Boolean> = context.calibDataStore.data.map { it[IS_CALIBRATED] == true }
    val accelNoiseFloor: Flow<List<Float>?> = combine(
        context.calibDataStore.data.map { it[ACCEL_NOISE_X] },
        context.calibDataStore.data.map { it[ACCEL_NOISE_Y] },
        context.calibDataStore.data.map { it[ACCEL_NOISE_Z] }
    ) { x, y, z ->
        if (x == null || y == null || z == null) null
        else listOf(x, y, z)

    }

    suspend fun saveCalibration(noiseX: Float, noiseY: Float, noiseZ: Float) {
        context.calibDataStore.edit {
            it[ACCEL_NOISE_X] = noiseX
            it[ACCEL_NOISE_Y] = noiseY
            it[ACCEL_NOISE_Z] = noiseZ
            it[IS_CALIBRATED] = true
        }
    }
}
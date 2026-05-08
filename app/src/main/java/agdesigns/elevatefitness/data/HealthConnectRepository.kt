package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.readRecord
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.metadata.Metadata as HealthMetadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
            // TODO: re-enable and get google's approval
//            HealthPermission.getWritePermission(WeightRecord::class),
//            HealthPermission.getReadPermission(WeightRecord::class),
        )
    }

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val requiredPermissions get() = REQUIRED_PERMISSIONS

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable) return false
        return healthConnectClient.permissionController
            .getGrantedPermissions()
            .containsAll(requiredPermissions)
    }

    suspend fun hasSomePermissions(): Boolean {
        if (!isAvailable) return false
        return healthConnectClient.permissionController
            .getGrantedPermissions()
            .isNotEmpty()
    }


    suspend fun writeWorkout(workoutRecord: WorkoutRecord, programName: String): String? {
        if (!isAvailable || workoutRecord.startDate == null) {
            return null
        }
        Log.d("HealthConnectRepository", "Writing workout to Health Connect")
        val startTime = workoutRecord.startDate.toInstant()
        val endTime = startTime.plusSeconds(workoutRecord.durationSeconds)
        val zoneOffset = workoutRecord.startDate.offset
        val results = healthConnectClient.insertRecords(
            listOf<Record>(
                ExerciseSessionRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = endTime,
                    endZoneOffset = zoneOffset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
                    title = getProgramDisplayName(programName, context),
                    metadata = HealthMetadata.activelyRecorded(
                        device = Device(Device.TYPE_PHONE) // FIXME: change when using watch
                    )
                ),
                ActiveCaloriesBurnedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = endTime,
                    endZoneOffset = zoneOffset,
                    energy = Energy.kilocalories(workoutRecord.calories.toDouble()),
                    metadata = HealthMetadata.activelyRecorded(
                        device = Device(Device.TYPE_PHONE) // FIXME: change when using watch
                    )
                )
            )
        )
        return results.recordIdsList.getOrNull(0)
    }

    suspend fun getWeight(): WeightRecord? {
        return null
        // TODO: re-enable
//        return try {
//            if (!isAvailable) {
//                return null
//            }
//            // get last weight recorded
//            val records = healthConnectClient.readRecords(
//                ReadRecordsRequest<WeightRecord>(
//                    TimeRangeFilter.before(LocalDateTime.now()),
//                    ascendingOrder = false,
//                    pageSize = 1
//                )
//            ).records
//            records.getOrNull(0)
//        } catch (e: Exception) {
//            null
//        }
    }

    suspend fun writeWeight(weight: Double): Result<Unit> {
        return Result.success(Unit)
        // TODO: re-enable
//        return try {
//            if (!isAvailable) {
//                return Result.failure(Exception("Health Connect unavailable"))
//            }
//            val now = ZonedDateTime.now()
//            healthConnectClient.insertRecords(
//                listOf(
//                    WeightRecord(
//                        time = now.toInstant(),
//                        zoneOffset = now.offset,
//                        weight = Mass.kilograms(weight),
//                        metadata = HealthMetadata.manualEntry()
//                    )
//                )
//            )
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
    }


}

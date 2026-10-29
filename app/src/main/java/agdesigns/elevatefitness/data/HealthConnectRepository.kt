package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata as HealthMetadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

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
        private const val MAX_KCAL = 1_000_000.0
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


    suspend fun writeWorkout(
        workoutRecord: WorkoutRecord,
        programName: String,
        exerciseRecords: List<ExerciseRecordAndInfo>
    ): Result<String> {
        if (!isAvailable) {
            return Result.failure(IllegalStateException("Health Connect unavailable"))
        }
        if (workoutRecord.startDate == null) {
            return Result.failure(IllegalStateException("Workout has no start date"))
        }
        if (workoutRecord.durationSeconds <= 0) {
            return Result.failure(IllegalStateException("Workout has no duration"))
        }
        Log.d("HealthConnectRepository", "Writing workout to Health Connect")
        val startTime = workoutRecord.startDate.toInstant()
        val endTime = startTime.plusSeconds(workoutRecord.durationSeconds)
        val zoneOffset = workoutRecord.startDate.offset
        return try {
            val metadata = HealthMetadata.activelyRecorded(
                device = Device(Device.TYPE_PHONE) // FIXME: change when using watch
            )
            val title = programName.takeIf { it.isNotBlank() }
                ?.let { getProgramDisplayName(it, context) }
            val segments = buildSegments(startTime, endTime, exerciseRecords)
            val session = try {
                ExerciseSessionRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = endTime,
                    endZoneOffset = zoneOffset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
                    title = title,
                    metadata = metadata,
                    segments = segments
                )
            } catch (e: IllegalArgumentException) {
                // segments are a nice-to-have, don't lose the whole workout because of them
                Log.w("HealthConnectRepository", "Invalid segments, exporting session without them", e)
                ExerciseSessionRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = endTime,
                    endZoneOffset = zoneOffset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
                    title = title,
                    metadata = metadata
                )
            }
            val calories = workoutRecord.calories.toDouble()
                .takeIf { it.isFinite() }
                ?.coerceIn(0.0, MAX_KCAL)
                ?: 0.0
            val results = healthConnectClient.insertRecords(
                listOf<Record>(
                    session,
                    ActiveCaloriesBurnedRecord(
                        startTime = startTime,
                        startZoneOffset = zoneOffset,
                        endTime = endTime,
                        endZoneOffset = zoneOffset,
                        energy = Energy.kilocalories(calories),
                        metadata = metadata
                    )
                )
            )
            results.recordIdsList.firstOrNull()?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Health Connect returned no record id"))
        } catch (e: Exception) {
            Log.e("HealthConnectRepository", "Error writing workout to Health Connect", e)
            Result.failure(e)
        }
    }

    /**
     * Splits the session into one segment per set. Each exercise spans from its record date to the
     * start of the following exercise (or the end of the session), divided equally among its sets.
     * Everything is clamped to the session range and empty segments are dropped, so the result
     * always satisfies Health Connect's validation (no overlaps, within the session).
     */
    private fun buildSegments(
        sessionStart: Instant,
        sessionEnd: Instant,
        exerciseRecords: List<ExerciseRecordAndInfo>
    ): List<ExerciseSegment> = buildList {
        val records = exerciseRecords
            .filter { it.reps.isNotEmpty() }
            .sortedBy { it.date }
        records.forEachIndexed { i, record ->
            val exStart = maxOf(record.date.toInstant(), sessionStart)
            val exEnd = minOf(
                records.getOrNull(i + 1)?.date?.toInstant() ?: sessionEnd,
                sessionEnd
            )
            val millisPerSet = Duration.between(exStart, exEnd).toMillis() / record.reps.size
            if (millisPerSet <= 0) return@forEachIndexed
            record.reps.forEachIndexed { index, reps ->
                val segmentStart = exStart.plusMillis(index * millisPerSet)
                add(
                    ExerciseSegment(
                        startTime = segmentStart,
                        endTime = segmentStart.plusMillis(millisPerSet),
                        segmentType = record.healthExerciseSegmentType,
                        repetitions = max(0, reps),
                        weight = record.weights.getOrNull(index)?.let {
                            // weights are stored per side and without tare (e.g., the bar),
                            // export the total load. For bodyweight exercises the tare is the
                            // user's weight, only export the added weight.
                            // This needs to be non-negative but we allow that
                            val load = when (record.equipment) {
                                Equipment.BARBELL, Equipment.DUMBBELL -> it * 2 + record.tare
                                Equipment.BODY_WEIGHT -> it
                                else -> it + record.tare
                            }
                            Mass.kilograms(max(0.0, load.toDouble()))
                        },
                        setIndex = index
                    )
                )
            }
        }
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

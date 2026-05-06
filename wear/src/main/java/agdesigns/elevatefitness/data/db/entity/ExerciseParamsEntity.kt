package agdesigns.elevatefitness.data.db.entity

import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.utils.RepAndTempoCounter
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_params")
data class ExerciseParamsEntity(
    @PrimaryKey val exerciseId: Long,
    val accelPeakThreshold: Float,
    val velocityEnterThreshold: Float,
    val velocityExitThreshold: Float,
    val gyroActiveThreshold: Float,
    val minRepDurationMs: Long,
    val maxRepDurationMs: Long,
    val minPhaseDurationMs: Long,
    val maxPauseDurationMs: Long,
    val accelSmoothingAlpha: Float,
    val gyroSmoothingAlpha: Float,
    val velocityLeakPerSec: Float,
    val maxOrientationDriftRad: Float,
    val signatureWeight: Float,
    val signatureReferenceCount: Int,
    val tuningRate: Float,
    val firstPhaseIsConcentric: Boolean,
)

fun ExerciseParamsEntity.toExerciseParams(
    firstPhase: Workout.FirstPhase,
    rotationMovement: Boolean,
): RepAndTempoCounter.ExerciseParams = RepAndTempoCounter.ExerciseParams(
    exerciseId = exerciseId,
    accelPeakThreshold = accelPeakThreshold,
    velocityEnterThreshold = velocityEnterThreshold,
    velocityExitThreshold = velocityExitThreshold,
    gyroActiveThreshold = gyroActiveThreshold,
    minRepDurationMs = minRepDurationMs,
    maxRepDurationMs = maxRepDurationMs,
    minPhaseDurationMs = minPhaseDurationMs,
    maxPauseDurationMs = maxPauseDurationMs,
    accelSmoothingAlpha = accelSmoothingAlpha,
    gyroSmoothingAlpha = gyroSmoothingAlpha,
    velocityLeakPerSec = velocityLeakPerSec,
    maxOrientationDriftRad = maxOrientationDriftRad,
    signatureWeight = signatureWeight,
    signatureReferenceCount = signatureReferenceCount,
    tuningRate = tuningRate,
    firstPhaseIsConcentric = firstPhaseIsConcentric,
    firstPhase = firstPhase,
    rotationMovement = rotationMovement,
)

fun RepAndTempoCounter.ExerciseParams.toEntity(): ExerciseParamsEntity = ExerciseParamsEntity(
    exerciseId = exerciseId,
    accelPeakThreshold = accelPeakThreshold,
    velocityEnterThreshold = velocityEnterThreshold,
    velocityExitThreshold = velocityExitThreshold,
    gyroActiveThreshold = gyroActiveThreshold,
    minRepDurationMs = minRepDurationMs,
    maxRepDurationMs = maxRepDurationMs,
    minPhaseDurationMs = minPhaseDurationMs,
    maxPauseDurationMs = maxPauseDurationMs,
    accelSmoothingAlpha = accelSmoothingAlpha,
    gyroSmoothingAlpha = gyroSmoothingAlpha,
    velocityLeakPerSec = velocityLeakPerSec,
    maxOrientationDriftRad = maxOrientationDriftRad,
    signatureWeight = signatureWeight,
    signatureReferenceCount = signatureReferenceCount,
    tuningRate = tuningRate,
    firstPhaseIsConcentric = firstPhaseIsConcentric,
)
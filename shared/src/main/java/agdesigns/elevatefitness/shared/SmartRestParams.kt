package agdesigns.elevatefitness.shared

import agdesigns.elevatefitness.shared.grpc.Workout

// tunable parameters of the smart rest heart rate recovery estimation, editable from the
// phone's developer options and synced to the watch
data class SmartRestParams(
    // seconds before rest start that are considered when looking for the set's peak heart rate
    val peakLookbackSecs: Long = 30L,
    // seconds averaged (going back from the latest sample) to smooth sensor noise
    val smoothingWindowSecs: Long = 5L,
    // fraction of the heart rate elevation above baseline that has to be recovered
    val recoveryFraction: Float = 0.5f,
    // heart rate rarely goes back to resting mid-workout, never ask to go lower than this above baseline
    val minTargetAboveBaseline: Int = 15,
    // percentile of the workout heart rates used as baseline, more robust than the minimum to sensor glitches
    val baselinePercentile: Float = 0.05f,
) {
    fun toProto(): Workout.SmartRestParams = Workout.SmartRestParams.newBuilder()
        .setPeakLookbackSecs(peakLookbackSecs)
        .setSmoothingWindowSecs(smoothingWindowSecs)
        .setRecoveryFraction(recoveryFraction)
        .setMinTargetAboveBaseline(minTargetAboveBaseline)
        .setBaselinePercentile(baselinePercentile)
        .build()

    companion object {
        fun fromProto(proto: Workout.SmartRestParams) = SmartRestParams(
            peakLookbackSecs = proto.peakLookbackSecs,
            smoothingWindowSecs = proto.smoothingWindowSecs,
            recoveryFraction = proto.recoveryFraction,
            minTargetAboveBaseline = proto.minTargetAboveBaseline,
            baselinePercentile = proto.baselinePercentile,
        )
    }
}

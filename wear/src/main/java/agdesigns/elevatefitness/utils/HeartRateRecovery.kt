package agdesigns.elevatefitness.utils

import agdesigns.elevatefitness.shared.SmartRestParams
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Heart rate based estimation of the recovery between sets (smart rest).
 *
 * The heart rate is considered recovered once it dropped by [SmartRestParams.recoveryFraction] of
 * how much the set raised it above the baseline (a low percentile of the workout heart rates).
 * Heart rate maps are keyed by seconds on the workout heart rate timeline, see
 * WorkoutState.heartRateBySecond.
 */
class HeartRateRecovery(val params: SmartRestParams = SmartRestParams()) {

    fun baseline(heartRates: Collection<Int>): Int? {
        if (heartRates.isEmpty()) return null
        val sorted = heartRates.sorted()
        return sorted[((sorted.size - 1) * params.baselinePercentile.coerceIn(0f, 1f)).toInt()]
    }

    fun peak(heartRateBySecond: Map<Long, Int>, restStartSecs: Long, toSecs: Long): Int? =
        ((restStartSecs - params.peakLookbackSecs)..toSecs).mapNotNull { heartRateBySecond[it] }.maxOrNull()

    fun smoothed(heartRateBySecond: Map<Long, Int>, latestSecs: Long): Int? =
        ((latestSecs - max(params.smoothingWindowSecs, 1L) + 1)..latestSecs)
            .mapNotNull { heartRateBySecond[it] }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()

    // when the set barely raised the heart rate the target is >= peak, i.e., already recovered
    fun target(baseline: Int, peak: Int): Int = max(
        baseline + ((peak - baseline) * (1f - params.recoveryFraction)).roundToInt(),
        baseline + params.minTargetAboveBaseline
    )

    companion object {
        // 0 at the peak, 1 once the target is reached
        fun progress(peak: Int, target: Int, current: Int): Float {
            if (peak <= target) return 1f
            return ((peak - current).toFloat() / (peak - target).toFloat()).coerceIn(0f, 1f)
        }
    }
}

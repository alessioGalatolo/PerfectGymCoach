package agdesigns.elevatefitness.utils

import agdesigns.elevatefitness.shared.grpc.Workout
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Detects reps, range of motion and concentric/eccentric tempo from Wear OS sensor data.
 *
 * Expected sensor streams (raw values from Android `SensorEvent`):
 *  - Linear acceleration — `Sensor.TYPE_LINEAR_ACCELERATION` (m/s², gravity already removed).
 *    Main signal: projected onto the primary axis, then integrated once to obtain velocity
 *    and a second time (within each phase) to obtain displacement / range of motion.
 *  - Gyroscope — `Sensor.TYPE_GYROSCOPE` (rad/s). Gates transitions so pre-set fidgeting
 *    doesn't trigger, and its integrated magnitude forms part of the rep signature.
 *  - Gravity — `Sensor.TYPE_GRAVITY` (m/s²). Defines the primary axis of motion (the
 *    direction opposite to gravity) and — snapshotted at phase transitions — provides the
 *    orientation signature used to distinguish real reps from pre-set noise.
 *
 * A 5-state machine tracks the rep cycle:
 *
 *     IDLE ──► PHASE_A ──► PAUSE_AB ──► PHASE_B ──► PAUSE_BA ──► PHASE_A (next rep)
 *
 * By default PHASE_A is the concentric (against gravity) portion.
 *
 * ### Noise rejection
 *
 * Two layers of signature-based filtering sit on top of the raw threshold detection:
 *
 *  1. **Within-rep gate** (applied as each candidate closes): the gravity vector at rep
 *     start and rep end must lie within [ExerciseParams.maxOrientationDriftRad]; i.e. the
 *     watch must have returned to approximately its starting orientation.
 *  2. **Cross-rep signature matching** (applied in [reportActualReps]): a reference
 *     signature is built from the **last** K candidates — on the assumption that noise
 *     between the final rep and the user reporting the count is short — and all candidates
 *     are scored by similarity (gravity angles at start/mid/end, angular-path ratio). When
 *     the detector over-counts, the least similar candidates are dropped first. This is
 *     where pre-set fidgeting, grip adjustments, etc. get filtered out.
 *
 * After reconciling, detection thresholds are nudged toward values that would have yielded
 * the reported count; the tuned parameters can be retrieved via [getTunedParameters] and
 * persisted per exercise.
 */
class RepAndTempoCounter(
    initialParams: ExerciseParams,
    val accelOffset: List<Float>? = null
) {

    data class ExerciseParams(
        val exerciseId: Long,

        // Detection thresholds
        /** Minimum peak |projected acceleration| (m/s²) required to validate a rep. */
        val accelPeakThreshold: Float = 1.20f,
        /** |velocity| (m/s) that marks the start of an active phase. */
        val velocityEnterThreshold: Float = 0.18f,
        /** |velocity| (m/s) below which the active phase is considered finished. */
        val velocityExitThreshold: Float = 0.20f,
        /** Gyro magnitude (rad/s) above which the watch is considered actively moving. */
        val gyroActiveThreshold: Float = 0.50f,

        // Timing constraints (ms)
        val minRepDurationMs: Long = 700L,
        val maxRepDurationMs: Long = 10_000L,
        val minPhaseDurationMs: Long = 120L,
        val maxPauseDurationMs: Long = 6_500L,

        // Signal processing
        /** EMA factor for linear acceleration (0..1). Higher = less smoothing. */
        val accelSmoothingAlpha: Float = 0.25f,
        /** EMA factor for gyroscope (0..1). */
        val gyroSmoothingAlpha: Float = 0.35f,
        /** Leaky-integrator time constant (s⁻¹) used to fight accelerometer bias drift. */
        val velocityLeakPerSec: Float = 2.0f,  // disabled for now because accel should already do this

        // Signature / orientation gating
        /** Max angle (rad) between rep-start and rep-end gravity before it's rejected as noise. */
        val maxOrientationDriftRad: Float = 0f, // if we have a value, keep it, otherwise get a default
        /** Weight of signature similarity vs prominence when ranking candidates (0..1). */
        val signatureWeight: Float = 0.70f,
        /** Number of trailing candidates used to build the cross-rep reference signature. */
        val signatureReferenceCount: Int = 3,

        // Tuning
        /** How aggressively to move thresholds per set (0..1). */
        val tuningRate: Float = 0.15f,
        /** If true the first detected phase of a rep is concentric (e.g. squat, bench). */
        val firstPhaseIsConcentric: Boolean = true,  // TODO
        val firstPhase: Workout.FirstPhase = Workout.FirstPhase.ALONG_GRAVITY,
        // I.e., gyro position *won't* be the same at beginning and end of phase
        val rotationMovement: Boolean = false
    )

    data class RepMetrics(
        val index: Int,
        val concentricMs: Long,
        val eccentricMs: Long,
        val pauseTopMs: Long,
        val pauseBottomMs: Long,
        val rangeOfMotionM: Float,
        val peakVelocity: Float,
        val peakAcceleration: Float,
        /** Integrated |ω| during the rep (rad). Part of the rep signature. */
        val angularPathRad: Float,
        /** Similarity score to the reference signature, in 0..1. `1f` until reconciled. */
        val signatureScore: Float,
    )

    data class SetResult(
        val exerciseId: Long,
        val detectedReps: Int,
        val reportedReps: Int?,
        val reps: List<RepMetrics>,
        val avgConcentricMs: Long,
        val avgEccentricMs: Long,
        val avgRangeOfMotionM: Float,
        val totalDurationMs: Long,
    ) {
        fun toProto(): Workout.ProtoSetTrackingResult {
            return Workout.ProtoSetTrackingResult.newBuilder()
                .setDataAvailable(true)
                .setDetectedReps(this.detectedReps)
                .setReportedReps(this.reportedReps ?: this.detectedReps)
                .setAvgConcentricMs(this.avgConcentricMs)
                .setAvgEccentricMs(this.avgEccentricMs)
                .setAvgRangeOfMotionM(this.avgRangeOfMotionM)
                .addAllReps(this.reps.map { rep ->
                    Workout.ProtoRepMetrics.newBuilder()
                        .setIndex(rep.index)
                        .setConcentricMs(rep.concentricMs)
                        .setEccentricMs(rep.eccentricMs)
                        .setRangeOfMotionM(rep.rangeOfMotionM)
                        .setPeakVelocity(rep.peakVelocity)
                        .build()
                })
                .build()
        }

        companion object {
            val protoNoResult = Workout.ProtoSetTrackingResult.newBuilder()
                .setDataAvailable(false)
                .build()
        }
    }

    private enum class SensorType { GRAVITY, GYROSCOPE, LINEAR_ACCEL }

    /**
     * Single sensor event stored as three scalars to avoid per-sample FloatArray
     * heap allocations (kinder to the GC during a long set).
     */
    private data class SensorSample(
        val type: SensorType,
        val v0: Float, val v1: Float, val v2: Float,
        val timestampNs: Long,
    )

    /**
     * All sensor events received since the last [reset].
     * At ~50 Hz × 3 sensors a 10-minute set produces ≈90 k entries; each entry
     * is ~40 bytes on JVM, so peak heap cost is well under 4 MB.
     * Capped at [MAX_RECORDED_SAMPLES] as a safety net.
     */
    private val recordedSamples = mutableListOf<SensorSample>()

    /**
     * False in ephemeral replay instances created by [replayWith] so that those
     * instances don't recursively fill [recordedSamples].
     */
    private var isRecordingSession = true

    companion object {
        private const val MAX_RECORDED_SAMPLES = 200_000
        /** Number of binary-search iterations — 30 bisections give ~10⁻⁹ precision. */
        private const val BISECT_ITERS = 30
        /** Sensitivity range to search. 0.05 = very insensitive, 20 = very eager. */
        private const val SENSITIVITY_LO = 0.05f
        private const val SENSITIVITY_HI = 20.0f
    }
    private var params: ExerciseParams = if (initialParams.maxOrientationDriftRad == 0f) {
        Log.d("RepAndTempoCounter", "Rotation: ${initialParams.rotationMovement}, first phase: ${initialParams.firstPhase}")
        initialParams.copy(
            maxOrientationDriftRad = if (initialParams.rotationMovement) 1.5f else 0.9f
        )
    } else initialParams

    // Latest filtered sensor values
    private val gravityVec = FloatArray(3)
    private var hasGravity = false
    private val smoothedAccel = FloatArray(3)
    private val smoothedGyro = FloatArray(3)

    /** Unit vector opposite to gravity (i.e. "up"). */
    private val primaryAxis = FloatArray(3)

    // 1-D state along primaryAxis
    private var velocity: Float = 0f
    private var lastAccelNs: Long = 0L
    private var lastGyroNs: Long = 0L
    private var setStartNs: Long = 0L
    private var setEndNs: Long = 0L

    // Rep-detection state machine
    private enum class Phase { IDLE, PHASE_A, PAUSE_AB, PHASE_B, PAUSE_BA }
    private var phase: Phase = Phase.IDLE
    private var phaseStartNs: Long = 0L
    private var phaseDisplacement: Float = 0f
    private var phasePeakVelocity: Float = 0f
    private var phasePeakAccel: Float = 0f
    private var phaseDirectionSign: Int = 0

    // Rep-scoped gyro accumulation (reset at rep start)
    private var repAngularPath: Float = 0f
    private var repPeakAngularVel: Float = 0f

    // Horizontal plane state (used when firstPhaseDirection == PARALLEL)
    private val horizVel = FloatArray(3)
    private var horizontalSpeed: Float = 0f
    private val phaseStartHorizDir = FloatArray(3)
    private var phaseStartHorizSpeed: Float = 0f

    /** Mutable in-progress rep. Promoted to [candidates] when PHASE_B closes cleanly. */
    private data class Candidate(
        val startNs: Long,
        var concentricMs: Long = 0L,
        var eccentricMs: Long = 0L,
        var pauseTopMs: Long = 0L,
        var pauseBottomMs: Long = 0L,
        var concentricRomM: Float = 0f,
        var eccentricRomM: Float = 0f,
        var peakVelocity: Float = 0f,
        var peakAccel: Float = 0f,
        // Signature: gravity vector in device frame, snapshotted at phase transitions
        val gravityStart: FloatArray = FloatArray(3),
        val gravityMid: FloatArray = FloatArray(3),
        val gravityEnd: FloatArray = FloatArray(3),
        var angularPath: Float = 0f,
        var peakAngularVel: Float = 0f,
        /** Filled in [tailAnchoredSignatureScoring]. Default 1f = "match perfect / not scored". */
        var signatureScore: Float = 1f,
    ) {
        /** Raw detection strength, independent of the signature. */
        val prominence: Float
            get() = peakVelocity * 0.6f + peakAccel * 0.25f +
                    max(concentricRomM, eccentricRomM) * 2.0f

        /** Final ranking: prominence tempered by signature similarity. */
        fun rankingScore(signatureWeight: Float): Float {
            val w = signatureWeight.coerceIn(0f, 1f)
            return prominence * (1f - w + w * signatureScore)
        }
    }

    private var currentRep: Candidate? = null
    private val candidates = mutableListOf<Candidate>()
    private var reportedReps: Int? = null

    private val _repCountFlow = MutableStateFlow(0)
    val repCountFlow: StateFlow<Int> = _repCountFlow.asStateFlow()

    /** Clears all per-set state so a new set can be started. */
    fun reset() {
        velocity = 0f
        lastAccelNs = 0L
        lastGyroNs = 0L
        setStartNs = 0L
        setEndNs = 0L
        phase = Phase.IDLE
        phaseStartNs = 0L
        phaseDisplacement = 0f
        phasePeakVelocity = 0f
        phasePeakAccel = 0f
        phaseDirectionSign = 0
        repAngularPath = 0f
        repPeakAngularVel = 0f
        smoothedAccel.fill(0f)
        smoothedGyro.fill(0f)
        currentRep = null
        candidates.clear()
        reportedReps = null
        _repCountFlow.value = 0
        horizVel.fill(0f)
        horizontalSpeed = 0f
        phaseStartHorizSpeed = 0f
        recordedSamples.clear()
        isRecordingSession = true
    }

    private fun recordIfActive(type: SensorType, values: FloatArray, timestampNs: Long) {
        if (!isRecordingSession || recordedSamples.size >= MAX_RECORDED_SAMPLES) return
        recordedSamples.add(SensorSample(type, values[0], values[1], values[2], timestampNs))
    }

    fun onGravity(values: FloatArray, timestampNs: Long) {
        recordIfActive(SensorType.GRAVITY, values, timestampNs)

        val mag = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        if (mag <= 0.1f) return
        gravityVec[0] = values[0]
        gravityVec[1] = values[1]
        gravityVec[2] = values[2]
        primaryAxis[0] = -values[0] / mag
        primaryAxis[1] = -values[1] / mag
        primaryAxis[2] = -values[2] / mag
        hasGravity = true
    }

    fun onGyroscope(values: FloatArray, timestampNs: Long) {
        recordIfActive(SensorType.GYROSCOPE, values, timestampNs)

        val a = params.gyroSmoothingAlpha
        smoothedGyro[0] = smoothedGyro[0] * (1f - a) + values[0] * a
        smoothedGyro[1] = smoothedGyro[1] * (1f - a) + values[1] * a
        smoothedGyro[2] = smoothedGyro[2] * (1f - a) + values[2] * a

        // Integrate |ω| into the rep's angular-path signature, but only during active phases.
        val dt = if (lastGyroNs == 0L) 0f else (timestampNs - lastGyroNs) / 1_000_000_000f
        lastGyroNs = timestampNs
        if (phase == Phase.PHASE_A || phase == Phase.PHASE_B) {
            val mag = sqrt(
                smoothedGyro[0] * smoothedGyro[0] +
                        smoothedGyro[1] * smoothedGyro[1] +
                        smoothedGyro[2] * smoothedGyro[2]
            )
            if (dt in 0f..0.5f) repAngularPath += mag * dt
            if (mag > repPeakAngularVel) repPeakAngularVel = mag
        }
    }

    fun onLinearAcceleration(values: FloatArray, timestampNs: Long) {
        recordIfActive(SensorType.LINEAR_ACCEL, values, timestampNs)
        val accelX = accelOffset?.let { values[0] - it[0] } ?: values[0]
        val accelY = accelOffset?.let { values[1] - it[1] } ?: values[1]
        val accelZ = accelOffset?.let { values[2] - it[2] } ?: values[2]

        if (!hasGravity) return
        if (setStartNs == 0L) setStartNs = timestampNs

        val a = params.accelSmoothingAlpha
        smoothedAccel[0] = smoothedAccel[0] * (1f - a) + accelX * a
        smoothedAccel[1] = smoothedAccel[1] * (1f - a) + accelY * a
        smoothedAccel[2] = smoothedAccel[2] * (1f - a) + accelZ * a

        val ap = smoothedAccel[0] * primaryAxis[0] +
                smoothedAccel[1] * primaryAxis[1] +
                smoothedAccel[2] * primaryAxis[2]

        val dt = if (lastAccelNs == 0L) 0f else (timestampNs - lastAccelNs) / 1_000_000_000f
        // Horizontal acceleration = linear accel with the vertical component removed.
        val aHx = smoothedAccel[0] - ap * primaryAxis[0]
        val aHy = smoothedAccel[1] - ap * primaryAxis[1]
        val aHz = smoothedAccel[2] - ap * primaryAxis[2]

        if (dt in 0f..0.5f) {
            val leak = exp(-params.velocityLeakPerSec * dt)
            horizVel[0] = horizVel[0] * leak + aHx * dt
            horizVel[1] = horizVel[1] * leak + aHy * dt
            horizVel[2] = horizVel[2] * leak + aHz * dt
        }
        horizontalSpeed = sqrt(horizVel[0]*horizVel[0] + horizVel[1]*horizVel[1] + horizVel[2]*horizVel[2])
        lastAccelNs = timestampNs
        if (dt in 0f..0.5f) {
            velocity = velocity * (1f - params.velocityLeakPerSec * dt) + ap * dt
        }

        advance(ap, dt, timestampNs)
        setEndNs = timestampNs
    }

    /**
     * Report the ground-truth rep count from the user. Triggers:
     *  1. **Signature scoring** — all candidates are scored against a reference signature
     *     built from the last [ExerciseParams.signatureReferenceCount] candidates.
     *  2. **Parameter tuning** — thresholds move toward values that would have yielded
     *     `reportedReps`.
     *
     * The filtered rep list is emitted by [getResults].
     */
    fun reportActualReps(reportedReps: Int) {
        this.reportedReps = reportedReps
        tailAnchoredSignatureScoring(reportedReps)
        tuneParameters(detected = candidates.size, reported = reportedReps)
    }

    fun getResults(): SetResult {
        val reported = reportedReps
        val selected: List<Candidate> = when {
            reported == null -> candidates.toList()
            reported == 0 -> emptyList()
            candidates.size > reported ->
                candidates.sortedByDescending { it.rankingScore(params.signatureWeight) }
                    .take(reported)
                    .sortedBy { it.startNs }
            else -> candidates.toList()
        }

        val reps = selected.mapIndexed { i, c ->
            RepMetrics(
                index = i,
                concentricMs = c.concentricMs,
                eccentricMs = c.eccentricMs,
                pauseTopMs = c.pauseTopMs,
                pauseBottomMs = c.pauseBottomMs,
                rangeOfMotionM =
                    if (c.concentricRomM > 0f && c.eccentricRomM > 0f)
                        (c.concentricRomM + c.eccentricRomM) * 0.5f
                    else max(c.concentricRomM, c.eccentricRomM),
                peakVelocity = c.peakVelocity,
                peakAcceleration = c.peakAccel,
                angularPathRad = c.angularPath,
                signatureScore = c.signatureScore,
            )
        }

        val avgConc = if (reps.isEmpty()) 0L else reps.sumOf { it.concentricMs } / reps.size
        val avgEcc = if (reps.isEmpty()) 0L else reps.sumOf { it.eccentricMs } / reps.size
        val avgRom = if (reps.isEmpty()) 0f
        else reps.map { it.rangeOfMotionM.toDouble() }.average().toFloat()

        return SetResult(
            exerciseId = params.exerciseId,
            detectedReps = candidates.size,
            reportedReps = reported,
            reps = reps,
            avgConcentricMs = avgConc,
            avgEccentricMs = avgEcc,
            avgRangeOfMotionM = avgRom,
            totalDurationMs = (setEndNs - setStartNs) / 1_000_000L,
        )
    }

    fun getTunedParameters(): ExerciseParams = params

    /** Expected sign of `velocity` for PHASE_A. Zero means "use horizontal signal instead". */
    private fun expectedPhaseASign(): Int = when (params.firstPhase) {
        Workout.FirstPhase.AGAINST_GRAVITY -> +1
        Workout.FirstPhase.ALONG_GRAVITY -> -1
        Workout.FirstPhase.PARALLEL -> 0
        else -> 0
    }

    /** True if the current motion matches the expected direction for entering the given phase. */
    private fun directionGateOk(enteringPhaseA: Boolean, gyroMag: Float): Boolean {
//        if (gyroMag <= params.gyroActiveThreshold) {
//            return false
//        }
        val sign = expectedPhaseASign() * if (enteringPhaseA) 1 else -1
        val thr = params.velocityEnterThreshold
        return when (sign) {
            +1 -> velocity > thr
            -1 -> velocity < -thr
            else -> horizontalSpeed > thr && horizontalSpeed > abs(velocity) * 1.5f
        }
    }

    private fun effectivePhaseSign(enteringPhaseA: Boolean): Int {
        val s = expectedPhaseASign() * if (enteringPhaseA) 1 else -1
        // PARALLEL: we don't use the vertical sign; return 0 and let horizontal reversal check take over.
        return if (s == 0) 0 else s
    }

    private fun advance(accel: Float, dt: Float, nowNs: Long) {
        val gyroMag = sqrt(
            smoothedGyro[0] * smoothedGyro[0] +
                    smoothedGyro[1] * smoothedGyro[1] +
                    smoothedGyro[2] * smoothedGyro[2]
        )
        val velAbs: Float
        val reversalVel: Float  // signed magnitude along the phase-start direction
        if (params.firstPhase == Workout.FirstPhase.PARALLEL) {
            velAbs = horizontalSpeed
            reversalVel = if (phaseStartHorizSpeed > 0f)
                horizVel[0] * phaseStartHorizDir[0] +
                        horizVel[1] * phaseStartHorizDir[1] +
                        horizVel[2] * phaseStartHorizDir[2]
            else horizontalSpeed
        } else {
            velAbs = abs(velocity)
            reversalVel = velocity * phaseDirectionSign  // positive while moving in-phase, negative after reversal
        }

        val phaseMs: Long = if (phaseStartNs == 0L) 0L else (nowNs - phaseStartNs) / 1_000_000L

        when (phase) {
            Phase.IDLE -> {
                if (directionGateOk(enteringPhaseA = true, gyroMag = gyroMag)) {
                    beginNewRep(nowNs)
                    Log.d("RepAndTempoCounter", "IDLE: begin new rep")
                    startPhase(Phase.PHASE_A, sign = effectivePhaseSign(enteringPhaseA = true), nowNs = nowNs)
                }
            }

            Phase.PHASE_A -> {
                accumulatePhase(accel, dt)
                val reversed = reversalVel < -params.velocityEnterThreshold
                val stopped = velAbs < params.velocityExitThreshold
                if (phaseMs >= params.minPhaseDurationMs && (stopped || reversed)) {
                    Log.d("RepAndTempoCounter", "Phase A passed")
                    val rep = currentRep ?: return
                    gravityVec.copyInto(rep.gravityMid)
                    // Non-rotating exercises shouldn't see large orientation drift mid-phase either.
                    if (!params.rotationMovement) {
                        val midDrift = angleBetween(rep.gravityStart, rep.gravityMid)
                        if (midDrift.isFinite() && midDrift > params.maxOrientationDriftRad * 2f) {
                            Log.d("RepAndTempoCounter", "abandon rep due to rotation drift")
                            abandonRep()
                            return
                        }
                    }
                    val rom = abs(phaseDisplacement)
                    if (params.firstPhaseIsConcentric) {
                        rep.concentricMs = phaseMs
                        rep.concentricRomM = rom
                    } else {
                        rep.eccentricMs = phaseMs
                        rep.eccentricRomM = rom
                    }
                    rep.peakVelocity = max(rep.peakVelocity, phasePeakVelocity)
                    rep.peakAccel = max(rep.peakAccel, phasePeakAccel)
                    startPhase(Phase.PAUSE_AB, sign = 0, nowNs = nowNs)
                } else if (phaseMs > params.maxRepDurationMs) {
                    Log.d("RepAndTempoCounter", "Phase A: abandon rep due to duration")
                    abandonRep()
                }
            }

            Phase.PAUSE_AB -> {
                if (directionGateOk(enteringPhaseA = false, gyroMag = gyroMag)) {
                    Log.d("RepAndTempoCounter", "Phase AB passed")
                    currentRep?.let {
                        if (params.firstPhaseIsConcentric) it.pauseTopMs = phaseMs
                        else it.pauseBottomMs = phaseMs
                    }
                    startPhase(Phase.PHASE_B, sign = effectivePhaseSign(enteringPhaseA = false), nowNs = nowNs)
                } else if (phaseMs > params.maxPauseDurationMs) {
                    Log.d("RepAndTempoCounter", "Phase AB: abandon rep due to pause")
                    abandonRep()
                }
            }

            Phase.PHASE_B -> {
                accumulatePhase(accel, dt)
                val stopped = velAbs < params.velocityExitThreshold
                if (phaseMs >= params.minPhaseDurationMs && stopped) {
                    Log.d("RepAndTempoCounter", "Phase B")
                    val rep = currentRep ?: return
                    gravityVec.copyInto(rep.gravityEnd)
                    rep.angularPath = repAngularPath
                    rep.peakAngularVel = repPeakAngularVel

                    val rom = abs(phaseDisplacement)
                    if (params.firstPhaseIsConcentric) {
                        rep.eccentricMs = phaseMs
                        rep.eccentricRomM = rom
                    } else {
                        rep.concentricMs = phaseMs
                        rep.concentricRomM = rom
                    }
                    rep.peakVelocity = max(rep.peakVelocity, phasePeakVelocity)
                    rep.peakAccel = max(rep.peakAccel, phasePeakAccel)

                    val totalMs = rep.concentricMs + rep.eccentricMs + rep.pauseTopMs
                    val accelOk = rep.peakAccel >= params.accelPeakThreshold
                    val durationOk = totalMs in params.minRepDurationMs..params.maxRepDurationMs
                    // Within-rep orientation gate: the watch must return to approximately
                    // the same orientation it started the rep in. This alone kills most
                    // of the pre-set noise (scratches, grip changes, walking to the bar).
                    val drift = angleBetween(rep.gravityStart, rep.gravityEnd)
                    // this filters too many true positives, disable for now
                    val orientationOk = true // drift.isFinite() && drift <= params.maxOrientationDriftRad

                    if (accelOk && durationOk && orientationOk) {
                        candidates.add(rep)
                        _repCountFlow.value = candidates.size
                        Log.d("RepAndTempoCounter", "Reps success: ${candidates.size}")
                    } else {
                        Log.d("RepAndTempoCounter", "Reps fail: $accelOk, $durationOk, $orientationOk, drift was $drift")
                    }

                    currentRep = null
                    startPhase(Phase.PAUSE_BA, sign = 0, nowNs = nowNs)
                } else if (phaseMs > params.maxRepDurationMs) {
                    Log.d("RepAndTempoCounter", "Phase B: abandon rep due to duration")
                    abandonRep()
                }
            }

            Phase.PAUSE_BA -> {
                if (directionGateOk(enteringPhaseA = true, gyroMag = gyroMag)) {
                    candidates.lastOrNull()?.let {
                        if (params.firstPhaseIsConcentric) it.pauseBottomMs = phaseMs
                        else it.pauseTopMs = phaseMs
                    }
                    Log.d("RepAndTempoCounter", "Phase BA: begin new rep")
                    beginNewRep(nowNs)
                    startPhase(Phase.PHASE_A, sign = effectivePhaseSign(enteringPhaseA = true), nowNs = nowNs)
                } else if (phaseMs > params.maxPauseDurationMs) {
                    phase = Phase.IDLE
                    velocity = 0f
                }
            }

        }
    }

    private fun beginNewRep(nowNs: Long) {
        val rep = Candidate(startNs = nowNs)
        gravityVec.copyInto(rep.gravityStart)
        currentRep = rep
        repAngularPath = 0f
        repPeakAngularVel = 0f
    }

    private fun startPhase(newPhase: Phase, sign: Int, nowNs: Long) {
        phase = newPhase
        phaseStartNs = nowNs
        phaseDisplacement = 0f
        phasePeakVelocity = 0f
        phasePeakAccel = 0f
        phaseDirectionSign = sign

        if (params.firstPhase == Workout.FirstPhase.PARALLEL && horizontalSpeed > 1e-4f) {
            phaseStartHorizDir[0] = horizVel[0] / horizontalSpeed
            phaseStartHorizDir[1] = horizVel[1] / horizontalSpeed
            phaseStartHorizDir[2] = horizVel[2] / horizontalSpeed
            phaseStartHorizSpeed = horizontalSpeed
        } else {
            phaseStartHorizSpeed = 0f
        }
    }

    private fun accumulatePhase(accel: Float, dt: Float) {
        val signedSpeed = if (params.firstPhase == Workout.FirstPhase.PARALLEL) horizontalSpeed
        else velocity
        phaseDisplacement += signedSpeed * dt
        phasePeakVelocity = max(phasePeakVelocity, abs(signedSpeed))
        phasePeakAccel = max(phasePeakAccel, abs(accel))
    }

    private fun abandonRep() {
        currentRep = null
        velocity = 0f
        phase = Phase.IDLE
        horizVel.fill(0f)
    }

    /**
     * Builds a reference signature from the **tail** of the candidate list (last K reps)
     * and scores every candidate against it. Tail-anchoring exploits the fact that the
     * interval between the user's last rep and calling [reportActualReps] is typically
     * short — the tail is much more likely to be real than the head.
     */
    private fun tailAnchoredSignatureScoring(reported: Int) {
        if (candidates.isEmpty() || reported <= 0) {
            candidates.forEach { it.signatureScore = if (reported <= 0) 0f else 1f }
            return
        }

        val refCount = minOf(params.signatureReferenceCount, reported, candidates.size)
            .coerceAtLeast(1)
        val tail = candidates.takeLast(refCount)
        val refStart = averageDirection(tail.map { it.gravityStart })
        val refMid = averageDirection(tail.map { it.gravityMid })
        val refEnd = averageDirection(tail.map { it.gravityEnd })
        val refPath = tail.map { it.angularPath.toDouble() }.average().toFloat().coerceAtLeast(1e-3f)

        for (c in candidates) {
            c.signatureScore = signatureMatch(c, refStart, refMid, refEnd, refPath)
        }
    }

    /** Returns a similarity score in 0..1 between the candidate and the reference. */
    private fun signatureMatch(
        c: Candidate,
        refStart: FloatArray,
        refMid: FloatArray,
        refEnd: FloatArray,
        refPath: Float,
    ): Float {
        val dStart = angleToScore(angleBetween(c.gravityStart, refStart))
        val dMid = angleToScore(angleBetween(c.gravityMid, refMid))
        val dEnd = angleToScore(angleBetween(c.gravityEnd, refEnd))
        val orientationScore = (dStart + dMid + dEnd) / 3f
        val pathRatio = if (c.angularPath <= 0f) 0f
        else minOf(c.angularPath, refPath) / max(c.angularPath, refPath)

        // Rotation-heavy exercises: trust angular path more (distinctive per-rep rotation).
        // Non-rotating exercises: trust orientation triplet — the watch should barely tilt mid-rep.
        val (wOrient, wPath) = if (params.rotationMovement) 0.55f to 0.45f
        else 0.85f to 0.15f
        return (orientationScore * wOrient + pathRatio * wPath).coerceIn(0f, 1f)
    }

    /** Maps an angle (rad) to a similarity score: 0°→1.0, 90°→0.0, linearly. */
    private fun angleToScore(rad: Float): Float {
        if (!rad.isFinite()) return 0f
        return (1f - rad / (PI.toFloat() / 2f)).coerceIn(0f, 1f)
    }

    private fun angleBetween(a: FloatArray, b: FloatArray): Float {
        val magA = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
        val magB = sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2])
        if (magA < 1e-6f || magB < 1e-6f) return Float.POSITIVE_INFINITY
        val cos = ((a[0] * b[0] + a[1] * b[1] + a[2] * b[2]) / (magA * magB))
            .coerceIn(-1f, 1f)
        return acos(cos)
    }

    /** Mean of the input directions, renormalized. Good enough for tight angular clusters. */
    private fun averageDirection(vecs: List<FloatArray>): FloatArray {
        val out = FloatArray(3)
        for (v in vecs) { out[0] += v[0]; out[1] += v[1]; out[2] += v[2] }
        val mag = sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2])
        if (mag > 1e-6f) { out[0] /= mag; out[1] /= mag; out[2] /= mag }
        return out
    }

    /**
     * Replays all recorded sensor events through a temporary counter configured
     * with [testParams] and returns the number of candidates it produced.
     *
     * Because [isRecordingSession] is false in the temporary instance, no nested
     * recording occurs and the sample list is never modified.
     */
    private fun replayWith(testParams: ExerciseParams): Int {
        val temp = RepAndTempoCounter(testParams, accelOffset)
        temp.isRecordingSession = false          // prevent recursive recording
        val buf = FloatArray(3)
        for (s in recordedSamples) {
            buf[0] = s.v0; buf[1] = s.v1; buf[2] = s.v2
            when (s.type) {
                SensorType.GRAVITY -> temp.onGravity(buf, s.timestampNs)
                SensorType.GYROSCOPE -> temp.onGyroscope(buf, s.timestampNs)
                SensorType.LINEAR_ACCEL -> temp.onLinearAcceleration(buf, s.timestampNs)
            }
        }
        return temp.candidates.size
    }

    /**
     * Applies a sensitivity multiplier to the current params.
     *
     * The mapping is designed so that **higher sensitivity → more reps detected**:
     *
     * | Parameter              | Direction         | Rationale                                   |
     * |------------------------|-------------------|---------------------------------------------|
     * | accelPeakThreshold     | ÷ sensitivity     | lower bar → more candidates pass            |
     * | velocityEnterThreshold | ÷ sensitivity     | easier to enter a phase                     |
     * | velocityExitThreshold  | × sensitivity     | easier to *exit* a phase (higher ceiling)   |
     * | gyroActiveThreshold    | ÷ sensitivity     | easier to satisfy the motion gate           |
     */
    private fun sensitivityParams(sensitivity: Float): ExerciseParams = params.copy(
        accelPeakThreshold = (params.accelPeakThreshold / sensitivity).coerceIn(0.30f, 10.0f),
        velocityEnterThreshold = (params.velocityEnterThreshold / sensitivity).coerceIn(0.05f,  2.00f),
        velocityExitThreshold = (params.velocityExitThreshold * sensitivity).coerceIn(0.02f,  1.00f),
        gyroActiveThreshold = (params.gyroActiveThreshold / sensitivity).coerceIn(0.10f,  3.00f),
    )

    /**
     * Finds — via binary search over [recordedSamples] replays — the most conservative
     * sensitivity that still produces exactly [reported] reps, then commits the
     * resulting params.
     *
     * Binary search finds the boundary where count transitions from < reported to
     * >= reported.  The invariant is:
     *   replayWith(sensitivityParams(lo))  <  reported
     *   replayWith(sensitivityParams(hi)) >=  reported
     *
     * After [BISECT_ITERS] iterations lo and hi are indistinguishable in float32.
     * We commit `hi` — the smallest sensitivity (most conservative params) that
     * still reaches the target count.
     *
     * Edge cases:
     *  - If even SENSITIVITY_HI can't reach [reported], we commit the max-sensitivity
     *    params as the closest achievable (better than doing nothing).
     *  - If even SENSITIVITY_LO already exceeds [reported], we commit min-sensitivity
     *    params (most restrictive).
     *  - [reported] == 0 is handled upstream (not called here).
     */
    private fun tuneParameters(detected: Int, reported: Int) {
        if (reported <= 0 || recordedSamples.isEmpty()) return

        val maxSensParams = sensitivityParams(SENSITIVITY_HI)
        val minSensParams = sensitivityParams(SENSITIVITY_LO)

        // Fast-path: current params already correct.
        if (replayWith(params) == reported) {
            Log.d("RepAndTempoCounter", "Tuning: current params already correct ($reported reps)")
            return
        }

        // Clamp to achievable range.
        if (replayWith(maxSensParams) < reported) {
            Log.d("RepAndTempoCounter", "Tuning: target $reported unreachable even at max sensitivity; using max")
            params = maxSensParams
            return
        }
        if (replayWith(minSensParams) > reported) {
            Log.d("RepAndTempoCounter", "Tuning: target $reported unreachable even at min sensitivity; using min")
            params = minSensParams
            return
        }

        // Binary search: maintain  count(lo) < reported  ≤  count(hi)
        var lo = SENSITIVITY_LO
        var hi = SENSITIVITY_HI
        repeat(BISECT_ITERS) {
            val mid = (lo + hi) / 2f
            if (replayWith(sensitivityParams(mid)) >= reported) hi = mid else lo = mid
        }

        val tunedSensitivity = hi
        val tunedParams = sensitivityParams(tunedSensitivity)
        val achieved = replayWith(tunedParams)
        Log.d("RepAndTempoCounter",
            "Tuning done: sensitivity=%.3f, achieved=$achieved, target=$reported".format(tunedSensitivity))

        params = tunedParams
    }
}
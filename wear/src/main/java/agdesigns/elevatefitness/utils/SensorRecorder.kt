package agdesigns.elevatefitness.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.zip.GZIPOutputStream

/**
 * Records raw sensor streams for offline development of rep / ROM / tempo detection.
 *
 * No processing happens here on purpose: every event of every recorded sensor is written as is.
 * Recording is continuous while active and is cut into segments with [cut], called when a set
 * is completed, so each segment covers the rest before the set plus the set itself and carries
 * the user-reported values as ground truth.
 *
 * Each segment is a pair of files in [dir], sharing the same id:
 *  - `<id>.csv.gz`: `sensor,timestamp_ns,accuracy,v0,...,v5` (one row per [SensorEvent],
 *    timestamp is [SensorEvent.timestamp], values as many as the sensor reports)
 *  - `<id>.json`: metadata (set ground truth, markers, sensors info, clock references)
 * The json is written last, so its presence marks a complete segment.
 */
class SensorRecorder(
    private val sensorManager: SensorManager,
    private val dir: File,
) {
    private class Segment(
        val id: String,
        val file: File,
        val writer: BufferedWriter,
        val startElapsedNs: Long,
        val startWallMs: Long,
        val markers: JSONArray = JSONArray(),
        val counts: MutableMap<String, Int> = mutableMapOf(),
    )

    private val lock = Any()
    private var segment: Segment? = null
    private var thread: HandlerThread? = null
    private var sensors: List<Sensor> = emptyList()

    val isRecording: Boolean
        get() = synchronized(lock) { segment != null }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val name = SENSOR_NAMES[event.sensor.type] ?: return
            synchronized(lock) {
                val current = segment ?: return
                val line = StringBuilder(96)
                    .append(name).append(',')
                    .append(event.timestamp).append(',')
                    .append(event.accuracy)
                event.values.forEach { line.append(',').append(it) }
                current.writer.write(line.append('\n').toString())
                current.counts[name] = (current.counts[name] ?: 0) + 1
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            val name = SENSOR_NAMES[sensor.type] ?: return
            mark("accuracy_changed", JSONObject().put("sensor", name).put("accuracy", accuracy))
        }
    }

    fun start() {
        synchronized(lock) {
            if (segment != null) return
            dir.mkdirs()
            segment = newSegment()
        }
        sensors = SENSOR_NAMES.keys.mapNotNull { sensorManager.getDefaultSensor(it) }
        val handlerThread = HandlerThread("SensorRecorder").also { it.start() }
        thread = handlerThread
        val handler = Handler(handlerThread.looper)
        sensors.forEach {
            sensorManager.registerListener(listener, it, SAMPLING_PERIOD_US, handler)
        }
        Log.d(TAG, "Recording ${sensors.map { SENSOR_NAMES[it.type] }}")
    }

    /** Stops recording, discarding the current segment as it has no ground truth. */
    fun stop() {
        sensorManager.unregisterListener(listener)
        thread?.quitSafely()
        thread = null
        synchronized(lock) {
            segment?.let {
                runCatching { it.writer.close() }
                it.file.delete()
            }
            segment = null
        }
    }

    /** Timestamped annotation of the current segment, e.g. a UI event. */
    fun mark(name: String, extra: JSONObject? = null) {
        val marker = JSONObject()
            .put("name", name)
            .put("elapsed_ns", SystemClock.elapsedRealtimeNanos())
            .put("wall_ms", System.currentTimeMillis())
        extra?.let { marker.put("extra", it) }
        synchronized(lock) { segment?.markers?.put(marker) }
    }

    /**
     * Closes the current segment with [setInfo] as ground truth, then immediately starts a new
     * one so that no data is lost. Returns the id of the completed segment.
     */
    fun cut(setInfo: JSONObject): String? {
        val completed = synchronized(lock) {
            val current = segment ?: return null
            segment = newSegment()
            current.writer.close()
            current
        }
        val metadata = JSONObject()
            .put("format_version", FORMAT_VERSION)
            .put("id", completed.id)
            .put("set", setInfo)
            .put("start_elapsed_ns", completed.startElapsedNs)
            .put("start_wall_ms", completed.startWallMs)
            .put("end_elapsed_ns", SystemClock.elapsedRealtimeNanos())
            .put("end_wall_ms", System.currentTimeMillis())
            .put("sampling_period_us", SAMPLING_PERIOD_US)
            .put("sample_counts", JSONObject(completed.counts.toMap()))
            .put("markers", completed.markers)
            .put("sensors", JSONArray(sensors.map { it.toJson() }))
            .put("device", JSONObject()
                .put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL)
                .put("sdk", Build.VERSION.SDK_INT))
        File(dir, "${completed.id}.json").writeText(metadata.toString())
        return completed.id
    }

    private fun newSegment(): Segment {
        val id = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val file = File(dir, "$id.csv.gz")
        val writer = BufferedWriter(OutputStreamWriter(GZIPOutputStream(FileOutputStream(file))))
        writer.write("sensor,timestamp_ns,accuracy,v0,v1,v2,v3,v4,v5\n")
        return Segment(
            id = id,
            file = file,
            writer = writer,
            startElapsedNs = SystemClock.elapsedRealtimeNanos(),
            startWallMs = System.currentTimeMillis(),
        )
    }

    private fun Sensor.toJson() = JSONObject()
        .put("type", SENSOR_NAMES[type])
        .put("name", name)
        .put("vendor", vendor)
        .put("version", version)
        .put("resolution", resolution.toDouble())
        .put("max_range", maximumRange.toDouble())
        .put("min_delay_us", minDelay)
        .put("max_delay_us", maxDelay)
        .put("power_ma", power.toDouble())

    companion object {
        private const val TAG = "SensorRecorder"
        const val FORMAT_VERSION = 1

        fun recordingsDir(context: Context) = File(context.filesDir, "sensor_recordings")
        // 100 Hz, below the 200 Hz cap that needs HIGH_SAMPLING_RATE_SENSORS
        const val SAMPLING_PERIOD_US = 10_000

        // record both raw and fused streams so the algorithm can pick what it needs
        val SENSOR_NAMES = mapOf(
            Sensor.TYPE_ACCELEROMETER to "accelerometer",
            Sensor.TYPE_GYROSCOPE to "gyroscope",
            Sensor.TYPE_MAGNETIC_FIELD to "magnetic_field",
            Sensor.TYPE_LINEAR_ACCELERATION to "linear_acceleration",
            Sensor.TYPE_GRAVITY to "gravity",
            Sensor.TYPE_GAME_ROTATION_VECTOR to "game_rotation_vector",
            Sensor.TYPE_ROTATION_VECTOR to "rotation_vector",
            Sensor.TYPE_PRESSURE to "pressure",
        )
    }
}

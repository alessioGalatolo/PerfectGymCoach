package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.data.datastore.CalibrationDataStore
import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.data.datastore.PermissionStateDataStore
import agdesigns.elevatefitness.service.WorkoutService
import agdesigns.elevatefitness.shared.SENSOR_RECORDING_PATH
import agdesigns.elevatefitness.utils.RepAndTempoCounter
import agdesigns.elevatefitness.utils.SensorRecorder
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java

fun Context.exactAlarmPermissionFlow(): Flow<Boolean> = callbackFlow {
    // Only relevant on Android 12+
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        trySend(true) // Permission not required below S
        close()
        return@callbackFlow
    }

    val alarmManager = getSystemService(AlarmManager::class.java)

    // Emit the current state immediately
    trySend(alarmManager.canScheduleExactAlarms())

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                trySend(alarmManager.canScheduleExactAlarms())
            }
        }
    }

    val filter = IntentFilter(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    registerReceiver(receiver, filter)

    // Unregister when the flow is cancelled
    awaitClose {
        unregisterReceiver(receiver)
    }
}

@Singleton
class WearRepository @Inject constructor(
    val permissionStateDataStore: PermissionStateDataStore,
    val calibrationDataStore: CalibrationDataStore,
    @param:ApplicationContext private val context: Context
) {
    // WorkoutViewModel needs to register this (very suboptimal),
    // this is used when phone asks for Health data at the end of workout
    var getHealthData: () -> Workout.CompleteWorkout? = { null }

    var tempoRomTrackingEnabled = false
    /* Alarm stuff */
    private val _hintAlarmFiredFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hintAlarmFiredFlow: SharedFlow<Unit> = _hintAlarmFiredFlow

    val hasExactAlarm = context.exactAlarmPermissionFlow()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val restAlarmAction = "agdesigns.elevatefitness.REST_ALARM"
    private val restAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // notify sensors so we can collect set data
            startSetTracking()
            markSensorRecording("rest_alarm")

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // Vibrate pattern
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 800, 200, 800, 200, 800, 200, 1000),
                    -1
                )
            )
        }
    }

    private val exerciseTimerAlarmAction = "agdesigns.elevatefitness.EXERCISE_TIMER_ALARM"
    private val exerciseTimerAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // Vibrate pattern
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 800, 200, 800, 200, 800, 200, 1000),
                    -1
                )
            )
        }
    }

    fun vibrateForExercisePrep() {
        // vibrates for 3 seconds, simulating a 3, 2, 1, start timer.
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Vibrate pattern
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(250, 150, 850, 150, 850, 200),
                -1
            )
        )
    }

    private val hintAlarmAction = "agdesigns.elevatefitness.HINT_ALARM"
    private val hintAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // Vibrate pattern
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            )
            _hintAlarmFiredFlow.tryEmit(Unit)
        }
    }

    fun scheduleHintAlarm(durationMillis: Long = 2000) {
        cancelHintAlarm()
        val intent = Intent(hintAlarmAction).setPackage(context.packageName)
        scheduleAlarm(intent, durationMillis)
    }

    fun scheduleRestAlarm(durationMillis: Long) {
        cancelRestAlarm()
        val intent = Intent(restAlarmAction).setPackage(context.packageName)
        scheduleAlarm(intent, durationMillis)
    }

    fun scheduleExerciseTimerAlarm(durationMillis: Long) {
        cancelExerciseTimerAlarm()
        val intent = Intent(exerciseTimerAlarmAction).setPackage(context.packageName)
        scheduleAlarm(intent, durationMillis)
    }

    private fun scheduleAlarm(intent: Intent, durationMillis: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + durationMillis,
                pendingIntent
            )
        } else {
            Log.d("WearRepository", "Cannot schedule exact alarm")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + durationMillis,
                pendingIntent
            )
        }
    }

    fun cancelRestAlarm() {
        val intent = Intent(restAlarmAction).setPackage(context.packageName)
        cancelAlarm(intent)
    }

    fun cancelHintAlarm() {
        val intent = Intent(hintAlarmAction).setPackage(context.packageName)
        cancelAlarm(intent)
    }

    fun cancelExerciseTimerAlarm() {
        val intent = Intent(exerciseTimerAlarmAction).setPackage(context.packageName)
        cancelAlarm(intent)
    }

    private fun cancelAlarm(intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun registerAlarmReceivers() {
        val restAlarmFilter = IntentFilter(restAlarmAction)
        ContextCompat.registerReceiver(
            context,
            restAlarmReceiver,
            restAlarmFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val hintAlarmFilter = IntentFilter(hintAlarmAction)
        ContextCompat.registerReceiver(
            context,
            hintAlarmReceiver,
            hintAlarmFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val exerciseTimerAlarmFilter = IntentFilter(exerciseTimerAlarmAction)
        ContextCompat.registerReceiver(
            context,
            exerciseTimerAlarmReceiver,
            exerciseTimerAlarmFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun unregisterAlarmReceivers() {
        try {
            context.unregisterReceiver(restAlarmReceiver)
            context.unregisterReceiver(hintAlarmReceiver)
            context.unregisterReceiver(exerciseTimerAlarmReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("WearRepository", "Error unregistering alarm receivers", e)
        }
    }

    /* Stuff for tracking reps / tempo / rom */

    // Calls that wait for the service to be bound. They give up after a while and are cancelled
    // when a new workout starts, so a late call can't hit the next workout.
    private val serviceCallScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun withService(block: suspend (WorkoutService) -> Unit) {
        serviceCallScope.launch {
            val workoutService = withTimeoutOrNull(SERVICE_WAIT_MS) {
                service.filterNotNull().first()
            } ?: return@launch
            block(workoutService)
        }
    }

    fun stopSetTracking() {
        withService { it.stopSetTracking() }
    }

    fun initSetTracking(
        exerciseId: Long,
        wearRepTrackable: Workout.WearRepTrackable,
        firstPhase: Workout.FirstPhase
    ) {
        withService { it.initSetTracking(exerciseId, wearRepTrackable, firstPhase) }
    }

    fun startSetTracking() {
        if (!tempoRomTrackingEnabled) return
        withService { it.startSetTracking() }
    }

    fun setTrackingTruthAndGetResults(exerciseId: Long, groundTruthReps: Int): RepAndTempoCounter.SetResult? {
        return service.value?.setTrackingTruthAndGetResults(exerciseId, groundTruthReps)
    }

    /* Raw sensor recording (developer option) */
    private val sensorRecordingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sensorRecordingSendMutex = Mutex()

    fun setSensorRecording(enabled: Boolean) {
        sensorRecordingScope.launch {
            val workoutService = service.filterNotNull().first()
            if (enabled) {
                workoutService.startSensorRecording()
                // leftovers of a previous failed send
                sendSensorRecordings()
            } else {
                workoutService.stopSensorRecording()
            }
        }
    }

    fun markSensorRecording(name: String, extra: JSONObject? = null) {
        service.value?.markSensorRecording(name, extra)
    }

    /** Closes the current recording with [setInfo] as ground truth and sends it to the phone. */
    fun cutSensorRecording(setInfo: JSONObject) {
        service.value?.cutSensorRecording(setInfo) ?: return
        sensorRecordingScope.launch { sendSensorRecordings() }
    }

    /**
     * Sends every completed recording as a data item, which the data layer keeps (and delivers)
     * until the phone saves it and deletes it.
     */
    private suspend fun sendSensorRecordings() = sensorRecordingSendMutex.withLock {
        val dir = SensorRecorder.recordingsDir(context)
        val dataClient = Wearable.getDataClient(context)
        dir.listFiles { file -> file.name.endsWith(".json") }?.forEach { metadataFile ->
            val id = metadataFile.name.removeSuffix(".json")
            val samplesFile = File(dir, "$id.csv.gz")
            try {
                val request = PutDataMapRequest.create("$SENSOR_RECORDING_PATH/$id").apply {
                    dataMap.putString("id", id)
                    // as an asset as well, data items are limited to 100KB
                    dataMap.putAsset("metadata", Asset.createFromBytes(metadataFile.readBytes()))
                    dataMap.putAsset("samples", Asset.createFromBytes(samplesFile.readBytes()))
                }.asPutDataRequest().setUrgent()
                dataClient.putDataItem(request).await()
                metadataFile.delete()
                samplesFile.delete()
            } catch (e: Exception) {
                Log.e("WearRepository", "Failed to send sensor recording $id", e)
            }
        }
    }

    suspend fun runCalibration(durationMs: Long = 5000L) {
        val calibrationResult = service.filterNotNull().first().runCalibration(durationMs)
        if (calibrationResult.size != 3) return
        calibrationDataStore.saveCalibration(
            calibrationResult[0],
            calibrationResult[1],
            calibrationResult[2]
        )
    }

    private val _service = MutableStateFlow<WorkoutService?>(null)
    val service: StateFlow<WorkoutService?> = _service

    @OptIn(ExperimentalCoroutinesApi::class)
    val repCountFlow: Flow<Int?> = service.flatMapLatest { svc ->
        svc?.repCountFlow() ?: flowOf(null)
    }

    /* Other stuff */
    private var foregroundOnlyServiceBound = false

    // True from the moment bindService() is called until unbindService() is called, regardless
    // of whether onServiceConnected has fired yet. Prevents stopForegroundOnlyService() from
    // skipping unbindService() when the callback hasn't arrived yet (which leaves a dangling bind
    // that keeps the service alive but permanently silences future onServiceConnected calls).
    private var serviceBindRequested = false

    var foregroundOnlyWorkoutService: WorkoutService? = null
        private set

    // conflated: only the latest request matters, and it is kept until the ViewModel reads it
    val scrollToExerciseChannel = Channel<Int>(Channel.CONFLATED)
    val setRestChannel = Channel<Workout.RestPhone2Watch>(Channel.CONFLATED)

    fun handleScrollToExercise(exerciseIndex: Int) {
        scrollToExerciseChannel.trySend(exerciseIndex)
    }

    fun handleSetRest(rest: Workout.RestPhone2Watch) {
        setRestChannel.trySend(rest)
    }

    fun handleStopWorkout() {
        foregroundOnlyWorkoutService?.stopWorkout()
    }

    // A fresh ServiceConnection is created for each bind cycle. Reusing the same instance across
    // unbind→rebind can cause Android to silently skip onServiceConnected when the two calls race
    // (e.g. rapid onStop/onStart on Wear OS screen-off), leaving _service permanently null.
    private fun createConnection() = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as WorkoutService.LocalBinder
            foregroundOnlyWorkoutService = binder.workoutService
            foregroundOnlyServiceBound = true
            _service.update { foregroundOnlyWorkoutService }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyWorkoutService = null
            foregroundOnlyServiceBound = false
            _service.value = null
        }
    }

    private var connection: ServiceConnection = createConnection()

    suspend fun startWorkout() {
        // drop calls still pending from a previous workout
        serviceCallScope.coroutineContext[Job]?.children?.forEach { it.cancel() }
        service.filterNotNull().first().startWorkout()
        registerAlarmReceivers()
    }

    fun stopWorkout() {
        // run it right away when bound: the activity may unbind the service right after this
        service.value?.stopWorkout() ?: withService { it.stopWorkout() }
        unregisterAlarmReceivers()
        // phone requests meant for this workout must not reach the next one
        scrollToExerciseChannel.tryReceive()
        setRestChannel.tryReceive()
    }

    fun bindForegroundOnlyService() {
        if (serviceBindRequested) return
        serviceBindRequested = true
        val intent = Intent(context, WorkoutService::class.java)
        // If it's a foreground service that must actually run, start it as well:
        // ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun stopForegroundOnlyService() {
        if (serviceBindRequested) {
            serviceBindRequested = false
            val oldConnection = connection
            connection = createConnection() // fresh connection ensures onServiceConnected fires on next bind
            context.unbindService(oldConnection)
            foregroundOnlyServiceBound = false
            _service.value = null
        }
    }

    fun stringResToString(@StringRes id: Int): String = context.getString(id)

    companion object {
        private const val SERVICE_WAIT_MS = 10_000L

        // For Singleton instantiation
        @Volatile private var instance: WearRepository? = null

        fun getInstance(
            permissionStateDataStore: PermissionStateDataStore,
            calibrationDataStore: CalibrationDataStore,
            context: Context
        ) = instance ?: synchronized(this) {
            instance ?: WearRepository(
                permissionStateDataStore,
                calibrationDataStore,
                context
            ).also { instance = it }
        }
    }
}
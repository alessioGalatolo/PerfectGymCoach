package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.data.datastore.PermissionStateDataStore
import agdesigns.elevatefitness.service.WorkoutService
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java

private val _hintAlarmFiredFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

class RestAlarmReceiver : BroadcastReceiver() {
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


class HintAlarmReceiver : BroadcastReceiver() {
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
    @ApplicationContext private val context: Context
) {
    val hasExactAlarm = context.exactAlarmPermissionFlow()
    val hintAlarmFiredFlow: SharedFlow<Unit> = _hintAlarmFiredFlow
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleHintAlarm(durationMillis: Long = 2000) {
        cancelHintAlarm()
        val intent = Intent(context, HintAlarmReceiver::class.java)
        scheduleAlarm(intent, durationMillis)
    }

    fun scheduleRestAlarm(durationMillis: Long) {
        cancelRestAlarm()
        val intent = Intent(context, RestAlarmReceiver::class.java)
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
        val intent = Intent(context, RestAlarmReceiver::class.java)
        cancelAlarm(intent)
    }

    fun cancelHintAlarm() {
        val intent = Intent(context, HintAlarmReceiver::class.java)
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

    // The remaining variables are related to the binding/monitoring/interacting with the
    // service that gathers all the data to calculate walking points.
    private var foregroundOnlyServiceBound = false
    private val _service = MutableStateFlow<WorkoutService?>(null)
    val service: StateFlow<WorkoutService?> = _service

    var foregroundOnlyWalkingWorkoutService: WorkoutService? = null
        private set

    val scrollToExerciseChannel = Channel<Int>()
    val setRestChannel = Channel<Workout.RestPhone2Watch>()

    fun handleScrollToExercise(exerciseIndex: Int) {
        scrollToExerciseChannel.trySend(exerciseIndex)
    }

    fun handleSetRest(rest: Workout.RestPhone2Watch) {
        setRestChannel.trySend(rest)
    }

    fun handleStopWorkout() {
        foregroundOnlyWalkingWorkoutService?.stopWorkout()
    }


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as WorkoutService.LocalBinder
            foregroundOnlyWalkingWorkoutService = binder.workoutService
            foregroundOnlyServiceBound = true
            _service.value = foregroundOnlyWalkingWorkoutService
        }
        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyWalkingWorkoutService = null
            foregroundOnlyServiceBound = false
            _service.value = null
        }
    }

    suspend fun startWorkout() {
        service.filterNotNull().first().startWorkout()
    }

    fun stopWorkout() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            service.filterNotNull().first().stopWorkout()
        }
    }
    fun bindForegroundOnlyService() {
        val intent = Intent(context, WorkoutService::class.java)
        // If it's a foreground service that must actually run, start it as well:
        // ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun stopForegroundOnlyService() {
        if (foregroundOnlyServiceBound) {
            context.unbindService(connection)
            foregroundOnlyServiceBound = false
            _service.value = null
        }
    }

    fun stringResToString(@StringRes id: Int): String = context.getString(id)

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: WearRepository? = null

        fun getInstance(permissionStateDataStore: PermissionStateDataStore, context: Context) =
            instance ?: synchronized(this) {
                instance ?: WearRepository(permissionStateDataStore, context).also { instance = it }
            }
    }
}
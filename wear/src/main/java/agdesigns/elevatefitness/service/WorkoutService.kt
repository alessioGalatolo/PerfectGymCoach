package agdesigns.elevatefitness.service

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.WearActivity
import agdesigns.elevatefitness.data.WearRepository
import android.Manifest
import android.content.pm.PackageManager
import android.os.Binder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.content.Intent
import android.os.Build
import androidx.wear.ongoing.Status
import androidx.wear.ongoing.OngoingActivity
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
import android.health.connect.HealthPermissions
import android.os.IBinder
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.clearUpdateCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.NO_EXERCISE_IN_PROGRESS
import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.OTHER_APP_IN_PROGRESS
import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.OWNED_EXERCISE_IN_PROGRESS
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.WarmUpConfig
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.prepareExercise
import androidx.health.services.client.startExercise
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Duration
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class WorkoutService: LifecycleService() {
    @Inject lateinit var repository: WearRepository
    private lateinit var notificationManager: NotificationManager

    private lateinit var healthClient: HealthServicesClient
    private lateinit var exerciseClient: ExerciseClient

    /*
     * Checks whether the bound activity has really gone away (in which case a foreground service
     * with notification is created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private val serviceRunningInForeground: Boolean
        get() = this.foregroundServiceType != 0

    private var isStarted = false
    private var isBound = false

    private val localBinder = LocalBinder()

    private var workoutActive: Boolean = false

    private val _otherAppInProgress = MutableStateFlow(false)
    val otherAppInProgress: StateFlow<Boolean> = _otherAppInProgress.asStateFlow()

    // Saved for use when user confirms overriding another app's exercise
    private var pendingExerciseType: ExerciseType? = null
    private var pendingCanCollectCalories: Boolean = false
    private var pendingCanCollectHeartBeat: Boolean = false

    private fun canCollectHeartBeat(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            HealthPermissions.READ_HEART_RATE
        } else {
            Manifest.permission.BODY_SENSORS
        }
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun canCollectCalories(): Boolean {
        return checkSelfPermission(
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    data class ExerciseMetrics(
        val totalCalories: Double? = null,
        val heartRates: List<Pair<Duration, Double>> = emptyList(),
        val maxHeartRate: Double? = null,
        val averageHeartRate: Double? = null,
        val minHeartRate: Double? = null
    )

    fun exerciseMetricsFlow() = callbackFlow {
        val callback = object : ExerciseUpdateCallback {
            override fun onRegistered() {}

            override fun onRegistrationFailed(throwable: Throwable) {
                Log.e(TAG, "Exercise registration failed", throwable)
            }

            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {

                val latestMetrics = update.latestMetrics

                val heartRate = latestMetrics.getData(DataType.HEART_RATE_BPM).map {
                    it.timeDurationFromBoot to it.value
                }

                val caloriesTotal = latestMetrics.getData(DataType.CALORIES_TOTAL)?.total

                val heartBeatStats = latestMetrics.getData(DataType.HEART_RATE_BPM_STATS)

                trySendBlocking(
                    ExerciseMetrics(
                        totalCalories = caloriesTotal,
                        heartRates = heartRate,
                        maxHeartRate = heartBeatStats?.max,
                        averageHeartRate = heartBeatStats?.average,
                        minHeartRate = heartBeatStats?.min
                    )
                )
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}

            override fun onAvailabilityChanged(
                dataType: DataType<*, *>,
                availability: Availability
            ) {
                Log.d(TAG, "Availability changed: $dataType -> $availability")
            }
        }
        exerciseClient.setUpdateCallback(callback)

        awaitClose {
            Log.d(TAG, "Unregistering for data")
            runBlocking {
                exerciseClient.clearUpdateCallback(callback)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        healthClient = HealthServices.getClient(applicationContext)
        exerciseClient = healthClient.exerciseClient

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand()")

        if (!isStarted) {
            isStarted = true

            if (!isBound) {
                // We may have been restarted by the system. Manage our lifetime accordingly.
                stopSelfIfNotRunning()
            }
        }
        return START_STICKY
    }

    private fun stopSelfIfNotRunning() {
        lifecycleScope.launch {
            // We may have been restarted by the system. Check for an ongoing exercise.
            if (!workoutActive) {
                exerciseClient.endExercise()
                // We have nothing to do, so we can stop.
                stopSelf()
            }
        }
    }


    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)

        handleBind()

        return localBinder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)

        handleBind()
    }

    private fun handleBind() {
        if (!isBound) {
            isBound = true
            // Start ourself. This will begin collecting exercise state if we aren't already.
            startService(Intent(this, this::class.java))
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")
        isBound = false
        lifecycleScope.launch {
            // Client can unbind because it went through a configuration change, in which case it
            // will be recreated and bind again shortly. Wait a few seconds, and if still not bound,
            // manage our lifetime accordingly.
            delay(UNBIND_DELAY)
            if (!isBound) {
                stopSelfIfNotRunning()
            }
        }
        // Allow clients to re-bind. We will be informed of this in onRebind().
        return true
    }

    fun startWorkout() {
        Log.d(TAG, "startWorkout()")
        workoutActive = true
        if (!serviceRunningInForeground) {
            Log.d(TAG, "Posting ongoing activity notification")

            val notification = generateNotification(getString(R.string.workout_notification_started_text))
            val canCollectCalories = canCollectCalories()
            val canCollectHeartBeat = canCollectHeartBeat()
            lifecycleScope.launch {
                // let's check the best supported exercise
                var bestSupportedExercise: ExerciseType? = null
                val supportedExercises = exerciseClient.getCapabilities().supportedExerciseTypes
                val types2try = listOf(
                    ExerciseType.WORKOUT,
                    ExerciseType.STRENGTH_TRAINING,
                    ExerciseType.WEIGHTLIFTING
                )
                for (type in types2try) {
                    if (type in supportedExercises) {
                        bestSupportedExercise = type
                    }
                }
                if (bestSupportedExercise == null) {
                    Log.e(TAG, "No supported exercise type")
                    return@launch
                }
                if (!canCollectCalories && !canCollectHeartBeat) {
                    Log.d(TAG, "No supported data types were granted permission")
                    return@launch
                }
                val exerciseInfo = exerciseClient.getCurrentExerciseInfoAsync().await()
                when (exerciseInfo.exerciseTrackedStatus) {
                    OTHER_APP_IN_PROGRESS -> {
                        Log.d(TAG, "OTHER_APP_IN_PROGRESS — prompting user")
                        pendingExerciseType = bestSupportedExercise
                        pendingCanCollectCalories = canCollectCalories
                        pendingCanCollectHeartBeat = canCollectHeartBeat
                        _otherAppInProgress.value = true
                        return@launch
                    }
                    OWNED_EXERCISE_IN_PROGRESS -> {
                        Log.d(TAG, "OWNED_EXERCISE_IN_PROGRESS — ending previous and retrying")
                        exerciseClient.endExercise()
                        delay(1.seconds)
                    }
                    NO_EXERCISE_IN_PROGRESS -> Log.d(TAG, "NO_EXERCISE_IN_PROGRESS")
                }
                prepareAndStartExercise(bestSupportedExercise, canCollectCalories, canCollectHeartBeat)
            }

            startForeground(
                NOTIFICATION_ID,
                notification,
                if (
                    (canCollectHeartBeat || canCollectCalories) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                )
                    FOREGROUND_SERVICE_TYPE_HEALTH
                else
                    FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        }
    }

    private suspend fun prepareAndStartExercise(
        exerciseType: ExerciseType,
        canCollectCalories: Boolean,
        canCollectHeartBeat: Boolean
    ) {
        var deltaDataTypes: Set<DataType<*, *>> = buildSet {
            if (canCollectCalories) add(DataType.CALORIES)
            if (canCollectHeartBeat) add(DataType.HEART_RATE_BPM)
        }
        val capabilities = exerciseClient
            .getCapabilities()
            .getExerciseTypeCapabilities(exerciseType)
        deltaDataTypes = deltaDataTypes.intersect(capabilities.supportedDataTypes)
        val warmupConfig = WarmUpConfig(
            exerciseType,
            // this is unchecked but both CALORIES and HEART_RATE_BPM are DeltaDataType
            deltaDataTypes as Set<DeltaDataType<*, *>>
        )
        exerciseClient.prepareExercise(warmupConfig)

        // Now, we can add non-DeltaDataTypes
        val allDataTypes = buildSet {
            if (canCollectCalories) {
                add(DataType.CALORIES)
                add(DataType.CALORIES_TOTAL)
            }
            if (canCollectHeartBeat) {
                add(DataType.HEART_RATE_BPM)
                add(DataType.HEART_RATE_BPM_STATS)
            }
        }.intersect(capabilities.supportedDataTypes)
        val exerciseConfig = ExerciseConfig(
            exerciseType = exerciseType,
            dataTypes = allDataTypes,
            isAutoPauseAndResumeEnabled = false,
            isGpsEnabled = false,
        )
        exerciseClient.startExercise(exerciseConfig)
    }

    fun confirmKillOtherApp() {
        val exerciseType = pendingExerciseType ?: return
        _otherAppInProgress.value = false
        lifecycleScope.launch {
            prepareAndStartExercise(exerciseType, pendingCanCollectCalories, pendingCanCollectHeartBeat)
            pendingExerciseType = null
        }
    }

    fun stopWorkout() {
        Log.d(TAG, "stopWorkout()")
        if (serviceRunningInForeground) {
            Log.d(TAG, "Removing ongoing activity notification")
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }


    /*
     * Generates a BIG_TEXT_STYLE Notification that a workout is active.
     */
    private fun generateNotification(mainText: String): Notification {
        Log.d(TAG, "generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data (note, the main notification text comes from the parameter above).
        val titleText = getString(R.string.active_workout)

        // 1. Create Notification Channel.
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            titleText,
            NotificationManager.IMPORTANCE_DEFAULT,
        )

        // Adds NotificationChannel to system. Attempting to create an
        // existing notification channel with its original values performs
        // no operation, so it's safe to perform the below sequence.
        notificationManager.createNotificationChannel(notificationChannel)

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, WearActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val cancelIntent = Intent(this, WorkoutService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_WORKOUT_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchActivityIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        // 4. Build and issue the notification.
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        val notificationBuilder = notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainText)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // Makes Notification an Ongoing Notification (a Notification with a background task).
            .setOngoing(true)
            // For an Ongoing Activity, used to decide priority on the watch face.
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.mipmap.ic_launcher_foreground,
                getString(R.string.launch_activity),
                activityPendingIntent,
            )
            .addAction(
                R.mipmap.ic_launcher_foreground, // FIXME: Add icon
                getString(R.string.stop_workout_notification_text),
                servicePendingIntent,
            )

        val ongoingActivityStatus = Status.Builder()
            // Sets the text used across various surfaces.
            .addTemplate(mainText)
            .build()

        val ongoingActivity =
            OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notificationBuilder)
                // Sets icon that will appear on the watch face in active mode. If it isn't set,
                // the watch face will use the static icon in active mode.
//                .setAnimatedIcon(R.drawable.animated_walk)
                // Sets the icon that will appear on the watch face in ambient mode.
                // Falls back to Notification's smallIcon if not set. If neither is set,
                // an Exception is thrown.
                .setStaticIcon(R.mipmap.ic_launcher_foreground)
                // Sets the tap/touch event, so users can re-enter your app from the
                // other surfaces.
                // Falls back to Notification's contentIntent if not set. If neither is set,
                // an Exception is thrown.
                .setTouchIntent(activityPendingIntent)
                // In our case, sets the text used for the Ongoing Activity (more options are
                // available for timers and stop watches).
                .setStatus(ongoingActivityStatus)
                .build()

        // Applies any Ongoing Activity updates to the notification builder.
        // This method should always be called right before you build your notification,
        // since an Ongoing Activity doesn't hold references to the context.
        ongoingActivity.apply(applicationContext)

        return notificationBuilder.build()
    }


    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val workoutService: WorkoutService
            get() = this@WorkoutService
    }

    companion object {
        private const val TAG = "WorkoutService"

        private const val PACKAGE_NAME = "agdesigns.elevatefitness"

        private const val EXTRA_CANCEL_WORKOUT_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_SUBSCRIPTION_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private val UNBIND_DELAY = 3.seconds

        private const val NOTIFICATION_CHANNEL_ID = "workout_channel_01"
    }
}
package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.MainActivity
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.service.WorkoutForegroundService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton


data class WorkoutNotificationState(
    val setsPerExercise: List<Int> = emptyList(),
    val setsDonePerExercise: List<Int> = emptyList(),
    val currentExercise: Int = 0,
    val restTimeSecs: Long? = null,
    val restTimestamp: Long = 0L,
    val totalRest: Long? = null,
    val workoutStarted: Boolean = false,
)

fun getTintedIcon(context: Context, iconId: Int): Icon {
    // change icon tint depending on theme
    val tintColor = if (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
        Color.WHITE
    } else {
        Color.BLACK
    }

    val drawable = ContextCompat.getDrawable(context, iconId)?.mutate()
    drawable?.setTint(tintColor)
    return Icon.createWithBitmap(
        drawable?.toBitmapOrNull(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight
        )
    )
}

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val CHANNEL_NAME = context.getString(R.string.workout_notification_name)

    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_DEFAULT)

    private var foregroundStarted = false

    init {
        notificationManager.createNotificationChannel(channel)
    }

    fun startForegroundService() {
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_START
        }

        context.startForegroundService(intent)
        foregroundStarted = true
    }

    fun stopForegroundService() {
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun canPostPromotedNotifications() = notificationManager.canPostPromotedNotifications()

    fun buildBaseProgressStyle(state: WorkoutNotificationState): NotificationCompat.ProgressStyle {
        if (state.restTimeSecs != null && state.totalRest != null && state.restTimeSecs != 0L) {
            return NotificationCompat.ProgressStyle()
                .setProgress((state.restTimeSecs * 100 / state.totalRest).toInt())
        }

        val totalSets = state.setsPerExercise.sum()
        if (totalSets == 0)
            return NotificationCompat.ProgressStyle().setProgressIndeterminate(true)

        val pointColor = Color.valueOf(
            236f / 255f, // Normalize red value to be between 0.0 and 1.0
            183f / 255f, // Normalize green value to be between 0.0 and 1.0
            255f / 255f, // Normalize blue value to be between 0.0 and 1.0
            1f,
        ).toArgb()
        val segmentColor = Color.valueOf(
            134f / 255f, // Normalize red value to be between 0.0 and 1.0
            247f / 255f, // Normalize green value to be between 0.0 and 1.0
            250f / 255f, // Normalize blue value to be between 0.0 and 1.0
            1f,
        ).toArgb()
        // color for exercises with complete sets (greenish)
        val segmentFinishedColor = Color.GREEN
        // color for exercises missing at least a set
        val segmentUnfinishedColor = Color.YELLOW


        var progressStyle = NotificationCompat.ProgressStyle()
            // TODO: these don't work as intended. Only 4 points get drawn at wrong places
//                .setProgressPoints(
//                    notificationState.setsPerExercise.runningFold(
//                        initial = 0
//                    ) { acc, it ->
//                        acc + it
//                    }.map {
//                        NotificationCompat.ProgressStyle.Point(it * 100 / totalSets).setColor(pointColor)
//                    }
//                )
            .setProgressSegments(
                state.setsPerExercise.mapIndexed { index, it ->
                    val color = if (index <= state.currentExercise) {
                        if (state.setsDonePerExercise[index] == it)
                            segmentFinishedColor
                        else
                            segmentUnfinishedColor
                    } else {
                        segmentColor
                    }
                    NotificationCompat.ProgressStyle.Segment(it * 100 / totalSets).setColor(color)
                }
            )

        var progress = 0
        for (i in 0..state.currentExercise-1) {
            progress += state.setsPerExercise.getOrNull(i) ?: 0
        }
        progress += state.setsDonePerExercise.getOrNull(state.currentExercise) ?: 0
        return progressStyle.setProgress(progress * 100 / state.setsPerExercise.sum() )
            .setProgressTrackerIcon(
                IconCompat.createFromIcon(
                    context,
                    getTintedIcon(context, R.drawable.icon_dumbbell)
                )
            )
    }

    fun buildBaseNotification(): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            // No NEW_TASK / CLEAR_TASK flags — we want to resume!
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setSilent(true)  // TODO: maybe no silent when rest == 3,2,1
            .setContentIntent(pendingIntent)
            .setContentTitle(context.getString(R.string.ongoing_workout_notification_title))

        return notificationBuilder
    }



    fun updateNotification(state: WorkoutNotificationState) {
        if (!foregroundStarted)
            startForegroundService()

        val progressStyle = buildBaseProgressStyle(state)

        val notification = if (state.restTimeSecs != null && state.totalRest != null && state.restTimeSecs != 0L) {
            buildBaseNotification()
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentText(
                    context.getString(
                        R.string.remaining_rest_notification_content,
                        state.restTimeSecs
                    ))
                .setChronometerCountDown(true)
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(state.restTimestamp)
                .setStyle(progressStyle)
                .setLargeIcon(
                    getTintedIcon(context, R.drawable.timer_icon)
                )
                .build()
        } else {
            var contentText = context.getString(
                R.string.ongoing_workout_notification_content,
                state.setsDonePerExercise.sum(),
                state.setsPerExercise.sum()
            )
            if (state.restTimeSecs == 0L) {
                contentText = context.getString(
                    R.string.ongoing_workout_rest_finished_notification,
                    contentText
                )
            }
            buildBaseNotification()
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentText(contentText)
                .setStyle(progressStyle)
                .build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            Logger.getLogger("canPostPromotedNotifications")
                .log(
                    Level.INFO,
                    notificationManager.canPostPromotedNotifications().toString())
            Logger.getLogger("hasPromotableCharacteristics")
                .log(
                    Level.INFO,
                    notification.hasPromotableCharacteristics().toString())
        }

        Handler(Looper.getMainLooper()).post {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    fun stop() {
        stopForegroundService()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "live_updates_channel_id"
        const val NOTIFICATION_ID = 1234
    }
}

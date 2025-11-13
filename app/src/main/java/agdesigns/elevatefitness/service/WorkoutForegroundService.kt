package agdesigns.elevatefitness.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WorkoutForegroundService: Service() {

    @Inject
    lateinit var notificationService: NotificationService

    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Start foreground with initial notification
                val notification = notificationService.buildBaseNotification()
                    .build()

                notificationService.buildBaseNotification()

                startForeground(NotificationService.NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                stopForegroundService()
            }
        }
        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopForegroundService() {
        isServiceRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "ACTION_START_FOREGROUND_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_FOREGROUND_SERVICE"
    }

}

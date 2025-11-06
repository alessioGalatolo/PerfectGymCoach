package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.BuildConfig
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.genai.AppLifecycleProvider
import agdesigns.elevatefitness.genai.DownloadWorker
import agdesigns.elevatefitness.genai.KEY_MODEL_COMMIT_HASH
import agdesigns.elevatefitness.genai.KEY_MODEL_DOWNLOAD_ACCESS_TOKEN
import agdesigns.elevatefitness.genai.KEY_MODEL_DOWNLOAD_ERROR_MESSAGE
import agdesigns.elevatefitness.genai.KEY_MODEL_DOWNLOAD_FILE_NAME
import agdesigns.elevatefitness.genai.KEY_MODEL_DOWNLOAD_MODEL_DIR
import agdesigns.elevatefitness.genai.KEY_MODEL_DOWNLOAD_RATE
import agdesigns.elevatefitness.genai.KEY_MODEL_DOWNLOAD_RECEIVED_BYTES
import agdesigns.elevatefitness.genai.KEY_MODEL_DOWNLOAD_REMAINING_MS
import agdesigns.elevatefitness.genai.KEY_MODEL_NAME
import agdesigns.elevatefitness.genai.KEY_MODEL_TOTAL_BYTES
import agdesigns.elevatefitness.genai.KEY_MODEL_URL
import agdesigns.elevatefitness.genai.ModelDownloadStatus
import agdesigns.elevatefitness.genai.ModelDownloadStatusType
import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID

private const val TAG = "AGDownloadRepository"
private const val MODEL_NAME_TAG = "modelName"

/**
 * Repository for managing model downloads using WorkManager.
 *
 * This class provides methods to initiate model downloads, cancel downloads, observe download
 * progress, and retrieve information about enqueued or running download tasks. It utilizes
 * WorkManager to handle background download operations.
 */
class DownloadRepository(
    private val context: Context,
    private val lifecycleProvider: AppLifecycleProvider,
) {
    private val _downloadStatus = MutableStateFlow(ModelDownloadStatus(
            status = if (getModelPath().exists())
                ModelDownloadStatusType.SUCCEEDED
            else
                ModelDownloadStatusType.NOT_DOWNLOADED
        )
    )
    val downloadStatus: StateFlow<ModelDownloadStatus> = _downloadStatus.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    /**
     * Stores the start time of a model download.
     *
     * We use SharedPreferences to persist the download start times. This ensures that the data is
     * still available after the app restarts. The key is the model name and the value is the download
     * start time in milliseconds.
     */
    private val downloadStartTimeSharedPreferences =
        context.getSharedPreferences("download_start_time_ms", Context.MODE_PRIVATE)

    fun downloadModel() {
        _downloadStatus.update {
            it.copy(status = ModelDownloadStatusType.IN_PROGRESS)
        }

        // Create input data.
        val builder = Data.Builder()
        val inputDataBuilder =
            builder
                .putString(KEY_MODEL_NAME, modelName)
                .putString(KEY_MODEL_URL, fileUrl)
                .putString(KEY_MODEL_COMMIT_HASH, commitHash)
                .putString(KEY_MODEL_DOWNLOAD_MODEL_DIR, modelDir)
                .putString(KEY_MODEL_DOWNLOAD_FILE_NAME, fileName)
                .putLong(KEY_MODEL_TOTAL_BYTES, totalBytes)

        inputDataBuilder.putString(KEY_MODEL_DOWNLOAD_ACCESS_TOKEN, accessToken)
        val inputData = inputDataBuilder.build()

        // Create worker request.
        val downloadWorkRequest =
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(inputData)
                .addTag("$MODEL_NAME_TAG:$modelName")
                .build()

        val workerId = downloadWorkRequest.id

        // Start!
        workManager.enqueueUniqueWork(modelName, ExistingWorkPolicy.REPLACE, downloadWorkRequest)

        // Observe progress.
        observerWorkerProgress(
            workerId = workerId,
        )
    }

    fun cancelDownloadModel() {
        workManager.cancelAllWorkByTag("$MODEL_NAME_TAG:$modelName")
        _downloadStatus.update {
            it.copy(status = ModelDownloadStatusType.NOT_DOWNLOADED)
        }
    }

    fun observerWorkerProgress(
        workerId: UUID,
    ) {
        workManager.getWorkInfoByIdLiveData(workerId).observeForever { workInfo ->
            if (workInfo != null) {
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> {
                        downloadStartTimeSharedPreferences.edit {
                            putLong(modelName, System.currentTimeMillis())
                        }
                    }

                    WorkInfo.State.RUNNING -> {
                        val receivedBytes = workInfo.progress.getLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, 0L)
                        val downloadRate = workInfo.progress.getLong(KEY_MODEL_DOWNLOAD_RATE, 0L)
                        val remainingSeconds = workInfo.progress.getLong(KEY_MODEL_DOWNLOAD_REMAINING_MS, 0L)

                        if (receivedBytes != 0L) {
                            _downloadStatus.update {
                                ModelDownloadStatus(
                                    status = ModelDownloadStatusType.IN_PROGRESS,
                                    totalBytes = totalBytes,
                                    receivedBytes = receivedBytes,
                                    bytesPerSecond = downloadRate,
                                    remainingMs = remainingSeconds,
                                )
                            }
                        }
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        Log.d("repo", "worker %s success".format(workerId.toString()))
                        _downloadStatus.update {
                            it.copy(
                                status = ModelDownloadStatusType.SUCCEEDED,
                            )
                        }
                        sendNotification(
                            title = context.getString(R.string.download_notification_finished),
                            text = context.getString(
                                R.string.download_notification_finished_info,
                                modelName
                            ),
                            modelName = modelName,
                        )

                        val startTime = downloadStartTimeSharedPreferences.getLong(modelName, 0L)
                        val duration = System.currentTimeMillis() - startTime
                        downloadStartTimeSharedPreferences.edit { remove(modelName) }
                    }

                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED -> {
                        var status = ModelDownloadStatusType.FAILED
                        val errorMessage = workInfo.outputData.getString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE) ?: ""
                        Log.d(
                            "repo",
                            "worker %s FAILED or CANCELLED: %s".format(workerId.toString(), errorMessage),
                        )
                        if (workInfo.state == WorkInfo.State.CANCELLED) {
                            status = ModelDownloadStatusType.NOT_DOWNLOADED
                        } else {
                            sendNotification(
                                title = context.getString(R.string.download_notification_failed),
                                text = context.getString(
                                    R.string.download_notification_failed_info,
                                    modelName
                                ),
                                modelName = "",
                            )
                        }
                        _downloadStatus.update {
                            it.copy(
                                status = status,
                                errorMessage = errorMessage,
                            )
                        }

                        val startTime = downloadStartTimeSharedPreferences.getLong(modelName, 0L)
                        val duration = System.currentTimeMillis() - startTime
                        downloadStartTimeSharedPreferences.edit { remove(modelName) }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun sendNotification(title: String, text: String, modelName: String) {
        // Don't send notification if app is in foreground.
        if (lifecycleProvider.isAppInForeground) {
            return
        }

        val channelId = "download_notification"
        val channelName = context.getString(R.string.download_notification_channel)

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val intent: Intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!

        // Create a PendingIntent
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            if (
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, return or handle accordingly. In real app, request permission.
                return
            }
            notify(1, builder.build())
        }
    }

    fun deleteModel() {
        getModelPath().deleteRecursively()
        _downloadStatus.update {
            it.copy(
                status = ModelDownloadStatusType.NOT_DOWNLOADED,
            )
        }
    }

    private fun getModelPath(): File {
        return getModelPath(context)
    }

    fun isMemoryLow(): Boolean {
        return isMemoryLow(context)
    }

    companion object {
        private const val BYTES_IN_GB = 1024f * 1024 * 1024

        val commitHash = "42d538a932e8d5b12e6b3b455f5572560bd60b2c"
        val modelName = "Gemma3-1B-IT"
        val fileName = "gemma3-1b-it-int4.task"
        val fileUrl = "https://huggingface.co/litert-community/$modelName/resolve/$commitHash/$fileName?download=true"
        val totalBytes = 584417280L
        val minDeviceMemoryInGb = 6  // TODO
        val modelDir = "gemma3"
        val accessToken = BuildConfig.HUGGINGFACE_DOWNLOAD_API_KEY


        fun getModelPath(context: Context): File {
            return File(
                context.getExternalFilesDir(null),
                listOf(modelDir, fileName)
                    .joinToString(separator = File.separator),
            )
        }

        /** Checks if the device's memory is lower than the required minimum for the given model. */
        fun isMemoryLow(context: Context): Boolean {
            val activityManager =
                context.getSystemService(android.app.Activity.ACTIVITY_SERVICE) as? ActivityManager
            val minDeviceMemoryInGb = minDeviceMemoryInGb
            return if (activityManager != null) {
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                var deviceMemInGb = memoryInfo.totalMem / BYTES_IN_GB
                // API 34+ uses advertisedMem instead of totalMem for better accuracy.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    deviceMemInGb = memoryInfo.advertisedMem / BYTES_IN_GB
                }
                Log.d(
                    TAG,
                    "Device memory (GB): $deviceMemInGb. " +
                            "Model's required min device memory (GB): $minDeviceMemoryInGb.",
                )
                deviceMemInGb < minDeviceMemoryInGb
            } else {
                false
            }
        }
    }
}
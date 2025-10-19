package agdesigns.elevatefitness.genai

import agdesigns.elevatefitness.BuildConfig
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private const val TAG = "AGDownloadWorker"

data class UrlAndFileName(val url: String, val fileName: String)

private const val FOREGROUND_NOTIFICATION_CHANNEL_ID = "model_download_channel_foreground"
private var channelCreated = false

class DownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Unique notification id.
    private val notificationId: Int = params.id.hashCode()

    init {
        if (!channelCreated) {
            // Create a notification channel for showing notifications for model downloading progress.
            val channel =
                NotificationChannel(
                    FOREGROUND_NOTIFICATION_CHANNEL_ID,
                    "Model Downloading",
                    // Make it silent.
                    NotificationManager.IMPORTANCE_LOW,
                )
                    .apply { description = "Notifications for model downloading" }
            notificationManager.createNotificationChannel(channel)
            channelCreated = true
        }
    }

    override suspend fun doWork(): Result {
        val fileUrl = inputData.getString(KEY_MODEL_URL)
        val fileName = inputData.getString(KEY_MODEL_DOWNLOAD_FILE_NAME)
        val modelDir = inputData.getString(KEY_MODEL_DOWNLOAD_MODEL_DIR)!!
        val totalBytes = inputData.getLong(KEY_MODEL_TOTAL_BYTES, 0L)
        val accessToken = inputData.getString(KEY_MODEL_DOWNLOAD_ACCESS_TOKEN)!!


        return withContext(Dispatchers.IO) {
            if (fileUrl == null || fileName == null) {
                Result.failure()
            } else {
                return@withContext try {
                    // Set the worker as a foreground service immediately.
                    setForeground(createForegroundInfo(progress = 0))

                    // Collect data for all files.
                    val fileToDownload = UrlAndFileName(url = fileUrl, fileName = fileName)
                    Log.d(TAG, "About to download: $fileToDownload")

                    var downloadedBytes = 0L
                    val bytesReadSizeBuffer: MutableList<Long> = mutableListOf()
                    val bytesReadLatencyBuffer: MutableList<Long> = mutableListOf()
                    val url = URL(fileToDownload.url)

                    val connection = url.openConnection() as HttpURLConnection
                    Log.d(TAG, "Using access token: ${accessToken.subSequence(0, 10)}...")
                    connection.setRequestProperty("Authorization", "Bearer $accessToken")

                    // Prepare output file's dir.
                    val outputDir =
                        File(
                            applicationContext.getExternalFilesDir(null),
                            modelDir,
                        )
                    if (!outputDir.exists()) {
                        outputDir.mkdirs()
                    }

                    // Read the tmp file and see if it is partially downloaded.
                    val outputTmpFile =
                        File(
                            applicationContext.getExternalFilesDir(null),
                            listOf(modelDir, "${fileToDownload.fileName}.$TMP_FILE_EXT")
                                .joinToString(separator = File.separator),
                        )
                    val outputFileBytes = outputTmpFile.length()
                    if (outputFileBytes > 0) {
                        Log.d(
                            TAG,
                            "File '${outputTmpFile.name}' partial size: ${outputFileBytes}. Trying to resume download",
                        )
                        connection.setRequestProperty("Range", "bytes=${outputFileBytes}-")
                    }
                    connection.connect()
                    Log.d(TAG, "response code: ${connection.responseCode}")

                    if (
                        connection.responseCode == HttpURLConnection.HTTP_OK ||
                        connection.responseCode == HttpURLConnection.HTTP_PARTIAL
                    ) {
                        val contentRange = connection.getHeaderField("Content-Range")

                        if (contentRange != null) {
                            // Parse the Content-Range header
                            val rangeParts = contentRange.substringAfter("bytes ").split("/")
                            val byteRange = rangeParts[0].split("-")
                            val startByte = byteRange[0].toLong()
                            val endByte = byteRange[1].toLong()

                            Log.d(
                                TAG,
                                "Content-Range: $contentRange. Start bytes: ${startByte}, end bytes: $endByte",
                            )

                            downloadedBytes += startByte
                        } else {
                            Log.d(TAG, "Download starts from beginning.")
                        }
                    } else {
                        throw IOException("HTTP error code: ${connection.responseCode}")
                    }

                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(outputTmpFile, true /* append */)

                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    var lastSetProgressTs: Long = 0
                    var deltaBytes = 0L
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        deltaBytes += bytesRead

                        // Report progress every 200 ms.
                        val curTs = System.currentTimeMillis()
                        if (curTs - lastSetProgressTs > 200) {
                            // Calculate download rate.
                            var bytesPerMs = 0f
                            if (lastSetProgressTs != 0L) {
                                if (bytesReadSizeBuffer.size == 5) {
                                    bytesReadSizeBuffer.removeAt(0)
                                }
                                bytesReadSizeBuffer.add(deltaBytes)
                                if (bytesReadLatencyBuffer.size == 5) {
                                    bytesReadLatencyBuffer.removeAt(0)
                                }
                                bytesReadLatencyBuffer.add(curTs - lastSetProgressTs)
                                deltaBytes = 0L
                                bytesPerMs = bytesReadSizeBuffer.sum().toFloat() / bytesReadLatencyBuffer.sum()
                            }

                            // Calculate remaining seconds
                            var remainingMs = 0f
                            if (bytesPerMs > 0f && totalBytes > 0L) {
                                remainingMs = (totalBytes - downloadedBytes) / bytesPerMs
                            }

                            setProgress(
                                Data.Builder()
                                    .putLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, downloadedBytes)
                                    .putLong(KEY_MODEL_DOWNLOAD_RATE, (bytesPerMs * 1000).toLong())
                                    .putLong(KEY_MODEL_DOWNLOAD_REMAINING_MS, remainingMs.toLong())
                                    .build()
                            )
                            setForeground(
                                createForegroundInfo(
                                    progress = (downloadedBytes * 100 / totalBytes).toInt(),
                                )
                            )
                            Log.d(TAG, "downloadedBytes: $downloadedBytes")
                            lastSetProgressTs = curTs
                        }
                    }

                    outputStream.close()
                    inputStream.close()

                    // Rename the tmp file to the original file name by removing the tmp file ext.
                    val originalFilePath = outputTmpFile.absolutePath.replace(".$TMP_FILE_EXT", "")
                    val originalFile = File(originalFilePath)
                    if (originalFile.exists()) {
                        originalFile.delete()
                    }
                    outputTmpFile.renameTo(originalFile)
                    Log.d(TAG, "Download done")

                    Result.success()
                } catch (e: IOException) {
                    Result.failure(
                        Data.Builder().putString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE, e.message).build()
                    )
                }
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Initial progress is 0
        return createForegroundInfo(0)
    }

    /**
     * Creates a [ForegroundInfo] object for the download worker's ongoing notification. This
     * notification is used to keep the worker running in the foreground, indicating to the user that
     * an active download is in progress.
     */
    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        // Create a notification for the foreground service
        val title = "Downloading model"  // FIXME: localization
        val content = "Downloading in progress: $progress%"

        val intent =
            Intent(applicationContext, Class.forName("agdesigns.elevatefitness.MainActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat.Builder(applicationContext, FOREGROUND_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true) // Makes the notification non-dismissable
                .setProgress(100, progress, false) // Show progress
                .setContentIntent(pendingIntent)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(
                notificationId,
                notification
            )
        }
    }
}
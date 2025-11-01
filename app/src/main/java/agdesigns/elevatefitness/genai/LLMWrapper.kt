package agdesigns.elevatefitness.genai

import agdesigns.elevatefitness.data.DownloadRepository
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.concurrent.futures.await
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMWrapper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    @Volatile
    private var llm: LlmInference? = null

    // most code here should not run on main thread
    private val processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // if a generation request to LlmInference is made while another is running, it will not succeed
    // so we need to queue them internally
    private val requestQueue = Channel<GenerationRequest>(Channel.UNLIMITED)

    @Volatile
    private var currentTask: Job? = null

    private sealed class GenerationRequest {
        abstract val prompt: String
        abstract val completable: CompletableDeferred<String>
        abstract val cancellationToken: CancellationToken

        data class Sync(
            override val prompt: String,
            override val completable: CompletableDeferred<String>,
            override val cancellationToken: CancellationToken
        ) : GenerationRequest()

        data class Async(
            override val prompt: String,
            val resultListener: ProgressListener<String>,
            override val completable: CompletableDeferred<String>,
            override val cancellationToken: CancellationToken
        ) : GenerationRequest()
    }

    class CancellationToken {
        @Volatile
        var isCancelled = false
            private set

        fun cancel() {
            isCancelled = true
        }
    }

    // Model has been downloaded
    fun modelIsAvailable(): Boolean = DownloadRepository.getModelPath(context).exists()

    init {
        startQueueProcessor()
    }

    private fun startQueueProcessor() {
        processingScope.launch {
            for (request in requestQueue) {
                if (request.cancellationToken.isCancelled) {
                    request.completable.complete("")
                    continue
                }

                currentTask = launch {
                    try {
                        val result = when (request) {
                            is GenerationRequest.Sync -> {
                                processGeneration(request.prompt, request.cancellationToken)
                            }
                            is GenerationRequest.Async -> {
                                processGenerationAsync(
                                    request.prompt,
                                    request.resultListener,
                                    request.cancellationToken
                                )
                            }
                        }
                        request.completable.complete(result)
                    } catch (e: Exception) {
                        if (request.cancellationToken.isCancelled) {
                            request.completable.complete("")
                        } else {
                            e.printStackTrace()
                            request.completable.complete("")
                        }
                    }
                }
                currentTask?.join()
                currentTask = null
            }
        }
    }

    private suspend fun processGeneration(prompt: String, token: CancellationToken): String {
        if (!modelIsAvailable() || token.isCancelled)
            return ""
        if (llm == null)
            start()
        return try {
            if (token.isCancelled) {
                ""
            } else {
                llm?.generateResponse(prompt) ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private suspend fun processGenerationAsync(
        prompt: String,
        resultListener: ProgressListener<String>,
        token: CancellationToken
    ): String {
        if (!modelIsAvailable() || token.isCancelled)
            return ""
        if (llm == null)
            start()
        return try {
            if (token.isCancelled) {
                ""
            } else {
                // Wrap the listener to check cancellation
                val wrappedListener = ProgressListener<String> { partialResult, done ->
                    if (!token.isCancelled) {
                        resultListener.run(partialResult, done)
                    }
                }
                llm?.generateResponseAsync(prompt, wrappedListener)?.await() ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun start() {
        if (llm != null)
            return
        if (!modelIsAvailable())
            return
        processingScope.launch {
            // heuristic to establish if we can run on GPU
            val pm = context.packageManager
            val isOldApi = Build.VERSION.SDK_INT <= 28 // Oreo/Pie era
            val hasVulkan =
                pm.hasSystemFeature("android.hardware.vulkan.level") ||
                        pm.hasSystemFeature("android.hardware.vulkan.compute")
            val shouldUseGpu = !isOldApi && hasVulkan
            val backend = if (shouldUseGpu)
                LlmInference.Backend.GPU
            else
                LlmInference.Backend.CPU
            Log.d("LLMWrapper", "Backend: $backend")
            // Set the configuration options for the LLM Inference task
            val taskOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(DownloadRepository.getModelPath(context).absolutePath)
                .setMaxTopK(64)
                .setPreferredBackend(backend)
                .build()
            // Create an instance of the LLM Inference task
            llm = LlmInference.createFromOptions(context, taskOptions)
        }.join()
    }

    suspend fun generate(prompt: String, cancellationToken: CancellationToken? = null): String {
        val completable = CompletableDeferred<String>()
        val token = cancellationToken ?: CancellationToken()
        requestQueue.send(GenerationRequest.Sync(prompt, completable, token))
        return completable.await()
    }

    suspend fun generateAsync(
        prompt: String,
        resultListener: ProgressListener<String>,
        cancellationToken: CancellationToken? = null
    ): String {
        val completable = CompletableDeferred<String>()
        val token = cancellationToken ?: CancellationToken()
        requestQueue.send(GenerationRequest.Async(prompt, resultListener, completable, token))
        return completable.await()
    }

    fun cancelCurrentGeneration() {
        currentTask?.cancel()
        currentTask = null
    }

    fun cleanup() {
        processingScope.cancel()
        requestQueue.close()
    }
}
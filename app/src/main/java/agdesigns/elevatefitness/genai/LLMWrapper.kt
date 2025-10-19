package agdesigns.elevatefitness.genai

import agdesigns.elevatefitness.data.DownloadRepository
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.concurrent.futures.await
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var llm: LlmInference? = null

    fun modelIsAvailable(): Boolean = DownloadRepository.getModelPath(context).exists()

    // it's not a suspend fun but should not be run on main thread
    suspend fun start() {
        if (llm != null)
            return
        if (!modelIsAvailable())
            return

        // heuristic to establish if we can run on GPU
        val pm = context.packageManager
        val isMali = Build.HARDWARE.contains("mali", true)
        val isOldApi = Build.VERSION.SDK_INT <= 28 // Oreo/Pie era
        val hasVulkan =
            pm.hasSystemFeature("android.hardware.vulkan.level") ||
                    pm.hasSystemFeature("android.hardware.vulkan.compute")

        val shouldUseGpu = !isMali && !isOldApi && hasVulkan // keep it strict

        val backend = if (shouldUseGpu)
            LlmInference.Backend.GPU
        else
            LlmInference.Backend.CPU

        Log.d("LLMWrapper", "Backend: $backend")


        // Set the configuration options for the LLM Inference task
        val taskOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(DownloadRepository.getModelPath(context).absolutePath)
            .setMaxTopK(64)
            .setPreferredBackend(LlmInference.Backend.GPU)
            .build()

        // Create an instance of the LLM Inference task
        llm = LlmInference.createFromOptions(context, taskOptions)

    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        if (llm == null)
            start()

        llm?.generateResponse(prompt) ?: ""
    }

    suspend fun generateAsync(
        prompt: String,
        resultListener: ProgressListener<String>
    ): String = withContext(Dispatchers.IO) {
        if (llm == null)
            start()

        llm?.generateResponseAsync(
            prompt,
            resultListener
        )?.await() ?: ""
    }
}
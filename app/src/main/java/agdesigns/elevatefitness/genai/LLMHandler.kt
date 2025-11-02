package agdesigns.elevatefitness.genai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LLMState (
    val aiEnabled: Boolean = true,
    val aiGeneration: String = "",
    val aiGenerationFinished: Boolean = false
)

/*
    Interfaces with LLMWrapper so that it can be easily used in a ViewModel
 */
class LLMHandler (
    private val llmWrapper: LLMWrapper,
    private val scope: CoroutineScope
) {
    private var generateJob: Job? = null

    private var cancellationToken: LLMWrapper.CancellationToken? = null

    private val _state = MutableStateFlow(LLMState())
    val state: StateFlow<LLMState> = _state.asStateFlow()

    init {
        scope.launch {
            llmWrapper.modelIsAvailableFlow().collect {
                _state.update { it.copy(aiEnabled = it.aiEnabled) }
            }
        }
    }

    fun generate(prompt: String) {
        generateJob?.cancel()
        generateJob = scope.launch(Dispatchers.IO) {
            cancellationToken = LLMWrapper.CancellationToken()
            _state.update {
                it.copy(
                    aiGenerationFinished = false,
                    aiGeneration = ""
                )
            }
            val result = llmWrapper.generateAsync(
                prompt,
                { text, _ ->
                    _state.update {
                        it.copy(aiGeneration = it.aiGeneration + text)
                    }
                },
                cancellationToken
            )
            _state.update {
                it.copy(
                    aiGenerationFinished = true,
                    aiGeneration = result
                )
            }
            if (result.isEmpty()) {
                // TODO: error
            }
        }
    }


    fun interruptGeneration() {
        cancellationToken?.cancel()
        cancellationToken = null
        generateJob?.cancel()
        _state.update { it.copy(aiGenerationFinished = true) }
    }

    fun regenerate(prompt: String) {
        cancellationToken?.cancel()
        cancellationToken = null
        generateJob?.cancel()
        _state.update {
            it.copy(
                aiGenerationFinished = false,
                aiGeneration = ""
            )
        }
        generate(prompt)
    }
}
package agdesigns.elevatefitness.navigation

import androidx.lifecycle.ViewModel
import agdesigns.elevatefitness.genai.AppLifecycleProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class RootViewModel @Inject constructor(
    private val lifecycleProvider: AppLifecycleProvider
): ViewModel() {

    fun setAppInForeground(foreground: Boolean) {
        lifecycleProvider.isAppInForeground = foreground
    }
}

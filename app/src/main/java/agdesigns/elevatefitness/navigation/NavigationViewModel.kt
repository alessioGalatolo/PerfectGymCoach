package agdesigns.elevatefitness.navigation

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = NavigationViewModel.Factory::class)
class NavigationViewModel @AssistedInject constructor(
    @Assisted startDestination: TopLevelRoute
) : ViewModel() {

    val navigator = DestinationsNavigator(startDestination)

    // Survives config changes (dark/light mode, rotation) so we don't re-navigate
    // to the start destination on Activity recreation.
    private var initialized = false

    fun navigateToStart(destination: Route) {
        if (!initialized) {
            initialized = true
            navigator.navigate(destination)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(startDestination: TopLevelRoute): NavigationViewModel
    }
}

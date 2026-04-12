package agdesigns.elevatefitness.navigation

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = NavigationViewModel.Factory::class)
class NavigationViewModel @AssistedInject constructor(
    @Assisted startDestination: Any
) : ViewModel() {

    val navigator = DestinationsNavigator(startDestination)

    @AssistedFactory
    interface Factory {
        fun create(startDestination: Any): NavigationViewModel
    }
}

package agdesigns.elevatefitness.presentation

import agdesigns.elevatefitness.data.WearRepository
import agdesigns.elevatefitness.presentation.screens.home.Home
import agdesigns.elevatefitness.presentation.screens.select_values.SelectValuesScreen
import agdesigns.elevatefitness.presentation.screens.workout.Workout
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import agdesigns.elevatefitness.presentation.theme.PerfectGymCoachTheme
import android.util.Log
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.NavDeepLink
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
sealed interface Destination: NavKey {
    @Serializable
    data object HomeDestination: Destination

    @Serializable
    data object WorkoutDestination: Destination

    @Serializable
    data object SelectValuesDestination: Destination
}

@AndroidEntryPoint
class WearActivity : ComponentActivity() {

    @Inject
    lateinit var wearRepository: WearRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // only intent we have now is to autoopen workout "elevatefitnesswear://startworkout"
        val isOpenWorkoutIntent = intent.data?.let {
            it.scheme == "elevatefitnesswear" && it.authority == "startworkout"
        } ?: false

        val initialStack = if (isOpenWorkoutIntent) {
            listOf(Destination.HomeDestination, Destination.WorkoutDestination)
        } else {
            listOf(Destination.HomeDestination)
        }.toTypedArray()

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            val activityAmbientModeManager = rememberAmbientModeManager()
            CompositionLocalProvider(LocalAmbientModeManager provides activityAmbientModeManager) {
                PerfectGymCoachTheme {
                    ProvideVicoTheme(
                        rememberM3VicoTheme(
                            columnCartesianLayerColors = MaterialTheme.colorScheme.run { listOf(primary, secondary, tertiary) },
                            lineColor = MaterialTheme.colorScheme.outline,
                            textColor = MaterialTheme.colorScheme.onBackground,
                        )
                    ) {
                        val backStack = rememberNavBackStack(*initialStack)
                        val strategy = rememberSwipeDismissableSceneStrategy<NavKey>()
                        AppScaffold {
                            NavDisplay(
                                backStack = backStack,
                                sceneStrategies = listOf(strategy),
                                entryProvider = entryProvider {
                                    entry<Destination.HomeDestination> {
                                        Home(
                                            openWorkoutScreen = {
                                                backStack.add(Destination.WorkoutDestination)
                                            }
                                        )
                                    }
                                    entry<Destination.WorkoutDestination> {
                                        Workout(
                                            onBack = {
                                                backStack.removeAt(backStack.lastIndex)
                                            },
                                            navigateToSelectValues = {
                                                backStack.add(Destination.SelectValuesDestination)
                                            },
                                            terminate = {
                                                Log.d("WearActivity", "Terminating")
                                                this@WearActivity.finish()
                                            }
                                        )
                                    }
                                    entry<Destination.SelectValuesDestination> {
//                                        val parentEntry = remember(it) {
//                                            backStack.entryForKey(Destination.WorkoutDestination)
//                                        }
                                        val viewModel: WorkoutViewModel = hiltViewModel(/*parentEntry*/)
                                        SelectValuesScreen({
                                            backStack.removeAt(backStack.lastIndex)
                                        }, viewModel)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        wearRepository.bindForegroundOnlyService()
    }


    override fun onDestroy() {
        wearRepository.stopForegroundOnlyService()
        super.onDestroy()
    }
}

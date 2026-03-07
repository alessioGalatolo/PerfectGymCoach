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
import androidx.compose.runtime.remember
import androidx.navigation.NavDeepLink
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import javax.inject.Inject

@AndroidEntryPoint
class WearActivity : ComponentActivity() {

    @Inject
    lateinit var wearRepository: WearRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            PerfectGymCoachTheme {
                val navController = rememberSwipeDismissableNavController()
                AppScaffold() {
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable(route = "home") {
                            Home(
                                openWorkoutScreen = {
                                    navController.navigate("workout")
                                }
                            )
                        }
                        composable(
                            route = "workout",
                            deepLinks = listOf(NavDeepLink("elevatefitnesswear://startworkout"))
                        ) {
                            Workout(
                                onBack = {
                                    navController.navigateUp()
                                },
                                navigateToSelectValues = {
                                    navController.navigate("select-values")
                                },
                                terminate = {
                                    this@WearActivity.finish()
                                }
                            )
                        }
                        composable(route = "select-values") {
                            val parentEntry = remember(it) {
                                navController.getBackStackEntry("workout")
                            }
                            val viewModel: WorkoutViewModel = hiltViewModel(parentEntry)
                            SelectValuesScreen(navController, viewModel)
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

    override fun onStop() {
        wearRepository.stopForegroundOnlyService()
        super.onStop()
    }
}

package agdesigns.elevatefitness.presentation

import agdesigns.elevatefitness.data.WearRepository
import agdesigns.elevatefitness.presentation.screens.home.Home
import agdesigns.elevatefitness.presentation.screens.workout.Workout
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import agdesigns.elevatefitness.presentation.theme.PerfectGymCoachTheme

import androidx.navigation.NavDeepLink
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dagger.hilt.android.AndroidEntryPoint
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

    override fun onStop() {
        wearRepository.stopForegroundOnlyService()
        super.onStop()
    }
}

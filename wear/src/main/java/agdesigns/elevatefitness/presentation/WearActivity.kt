package agdesigns.elevatefitness.presentation

import agdesigns.elevatefitness.data.WearRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import agdesigns.elevatefitness.presentation.theme.PerfectGymCoachTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearActivity : ComponentActivity() {

    @Inject
    lateinit var wearRepository: WearRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            PerfectGymCoachTheme {
                val engine = rememberNavHostEngine()
                val navController = engine.rememberNavController()

                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    engine = engine,
                    navController = navController
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        wearRepository.bindForegroundOnlyService()
    }

    override fun onDestroy() {
        wearRepository.close()
        super.onDestroy()
    }

    override fun onStop() {
        wearRepository.stopForegroundOnlyService()
        wearRepository.close()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        wearRepository.reopen()
        // TODO: is this needed?
        wearRepository.forceSync()
    }
}

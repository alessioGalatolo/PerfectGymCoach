package agdesigns.elevatefitness

import agdesigns.elevatefitness.ui.theme.ElevateFitnessTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.Theme
import agdesigns.elevatefitness.ui.*
import agdesigns.elevatefitness.ui.screens.*
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: Repository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ComposeView(this).consumeWindowInsets = true

        setContent {
            val userPreference = repository.getTheme().collectAsState(initial = Theme.SYSTEM)
            val systemTheme = isSystemInDarkTheme()
            val darkTheme by remember { derivedStateOf {
                when (userPreference.value) {
                    Theme.SYSTEM -> systemTheme
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                }
            }}
            // navigation controller for everything (main screen)
            val engine = rememberNavHostEngine()
            val navController = engine.rememberNavController()

            ElevateFitnessTheme (darkTheme = darkTheme) {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    engine = engine,
                    navController = navController
                )
            }
        }
    }
}

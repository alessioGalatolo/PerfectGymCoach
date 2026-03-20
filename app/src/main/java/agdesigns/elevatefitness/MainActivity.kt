package agdesigns.elevatefitness

import agdesigns.elevatefitness.data.PreferenceRepository
import agdesigns.elevatefitness.ui.theme.ElevateFitnessTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import agdesigns.elevatefitness.data.Repository
import agdesigns.elevatefitness.data.db.entity.Theme
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalView
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: PreferenceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Call enableEdgeToEdge() BEFORE setContent, with a default style.
        enableEdgeToEdge()

        setContent {
            val userPreference = preferences.getTheme().collectAsState(initial = Theme.SYSTEM)
            val systemTheme = isSystemInDarkTheme()
            val darkTheme by remember {
                derivedStateOf {
                    when (userPreference.value) {
                        Theme.SYSTEM -> systemTheme
                        Theme.LIGHT -> false
                        Theme.DARK -> true
                    }
                }
            }

            // Re-call enableEdgeToEdge() reactively when the theme changes,
            // but keep the initial call above to avoid flicker on first frame.
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        ) { darkTheme }
                    )
                }
            }

            // navigation controller for everything (main screen)
            val engine = rememberNavHostEngine()
            val navController = engine.rememberNavController()

            ElevateFitnessTheme (darkTheme = darkTheme) {
                ProvideVicoTheme(rememberM3VicoTheme()) {
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        engine = engine,
                        navController = navController
                    )
                }
            }
        }
    }
}

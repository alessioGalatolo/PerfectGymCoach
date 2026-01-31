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
import agdesigns.elevatefitness.navigation.DeepLinkMatcher
import agdesigns.elevatefitness.navigation.HomeDestination
import agdesigns.elevatefitness.navigation.RootDestinationGraph
import android.net.Uri
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: PreferenceRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // simple deeplink parsing
        val uri: Uri? = intent.data
        val startDestination: Any = uri?.let {
            DeepLinkMatcher(it).match()
        } ?: HomeDestination // fallback if intent.uri is null or match is not found

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ComposeView(this).consumeWindowInsets = true

        setContent {
            val userPreference = preferences.getTheme().collectAsState(initial = Theme.SYSTEM)
            val systemTheme = isSystemInDarkTheme()
            val darkTheme by remember { derivedStateOf {
                when (userPreference.value) {
                    Theme.SYSTEM -> systemTheme
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                }
            }}
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme }
                )
                onDispose {}
            }

            ElevateFitnessTheme (darkTheme = darkTheme) {
                ProvideVicoTheme(rememberM3VicoTheme()) {
                    RootDestinationGraph(startDestination = startDestination)
                }
            }
        }
    }
}

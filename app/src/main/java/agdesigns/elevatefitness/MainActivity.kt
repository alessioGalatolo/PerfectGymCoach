package agdesigns.elevatefitness

import agdesigns.elevatefitness.data.ELEVATE_FITNESS_SHARE_EXTENSION
import agdesigns.elevatefitness.data.ELEVATE_FITNESS_SHARE_MIME_TYPE
import agdesigns.elevatefitness.data.PreferenceRepository
import agdesigns.elevatefitness.data.SharableElement
import agdesigns.elevatefitness.ui.navigation.ReceivePlanDestination
import android.provider.OpenableColumns
import agdesigns.elevatefitness.ui.theme.ElevateFitnessTheme
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.ui.navigation.DeepLinkMatcher
import agdesigns.elevatefitness.ui.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.navigation.HomeDestination
import agdesigns.elevatefitness.ui.navigation.NoDestination
import agdesigns.elevatefitness.ui.navigation.RootDestinationGraph
import agdesigns.elevatefitness.ui.navigation.Route
import android.net.Uri
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalView
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: PreferenceRepository

    @Inject
    lateinit var navigator: DestinationsNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Only handle deeplinks/shared files and set the start destination on a genuine
        // cold start. On a config-change recreation (e.g. rotation) the same intent is
        // still attached to the activity, and the navigator's backstack is already
        // retained (@ActivityRetainedScoped), so re-running this would incorrectly
        // force navigation back to the start destination.
        if (savedInstanceState == null) {
            // check if we're being opened from launcher shortcut with a valid uri

            // simple deeplink parsing, null if invalid or no uri
            val intentDeeplink = intent.data?.let { DeepLinkMatcher(it).match() }

            if (intentDeeplink is NoDestination) {
                finish() // no destination, just bring to foreground
            }

            // Handle incoming file imports
            val sharedElement: SharableElement? = run {
                if (intentDeeplink != null) return@run null
                val action = intent.action ?: return@run null
                if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return@run null

                val fileUri: Uri = (when (action) {
                    Intent.ACTION_VIEW -> intent.data
                    else -> @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }) ?: return@run null

                // Accept if MIME type matches, or if the file's display name / URI path ends with .efplan
                val mimeTypeOk = intent.type == ELEVATE_FITNESS_SHARE_MIME_TYPE
                val displayName: String? = runCatching {
                    contentResolver.query(
                        fileUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                }.getOrNull()
                val nameOk = displayName?.endsWith(ELEVATE_FITNESS_SHARE_EXTENSION, ignoreCase = true) == true
                    || fileUri.path?.endsWith(ELEVATE_FITNESS_SHARE_EXTENSION, ignoreCase = true) == true

                if (!mimeTypeOk && !nameOk) {
                    Toast.makeText(this, getString(R.string.invalid_file_type), Toast.LENGTH_SHORT).show()
                    finish()
                    return@run null
                }
                runCatching {
                    val json = contentResolver.openInputStream(fileUri)?.bufferedReader()?.readText()
                        ?: return@run null
                    Json.decodeFromString<SharableElement>(json)
                }.getOrNull()
            }

            val startDestination: Route = when {
                sharedElement != null -> {
                    when (sharedElement.type) {
                        SharableElement.Type.WORKOUT_PLAN -> {
                            ReceivePlanDestination(sharedElement.element)
                        }
                        // other sharable types...
                    }
                }
                else -> intentDeeplink ?: HomeDestination
            }
            navigator.navigate(startDestination)
        }

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

            ElevateFitnessTheme (darkTheme = darkTheme) {
                ProvideVicoTheme(rememberM3VicoTheme()) {
                    RootDestinationGraph(navigator = navigator)
                }
            }
        }
    }
}

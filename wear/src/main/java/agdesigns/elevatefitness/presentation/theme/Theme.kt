package agdesigns.elevatefitness.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

@Composable
fun PerfectGymCoachTheme(
    content: @Composable () -> Unit
) {
    /**
     * Empty theme to customize for your app.
     * See: https://developer.android.com/jetpack/compose/designsystems/custom
     */
    val context = LocalContext.current
    val dynamicColors = dynamicColorScheme(context)
    MaterialTheme(
        content = content,
        colorScheme = dynamicColors ?: MaterialTheme.colorScheme
    )
}
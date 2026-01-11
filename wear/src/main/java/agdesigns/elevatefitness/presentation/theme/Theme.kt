package agdesigns.elevatefitness.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme
import agdesigns.elevatefitness.shared.R as sharedR

@OptIn(ExperimentalTextApi::class)
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

    val fontFamily = FontFamily(
        Font(
            sharedR.font.google_sans_flex,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("ROND", 100f),
                FontVariation.weight(700)
            )
        )
    )
    MaterialTheme(
        content = content,
        colorScheme = dynamicColors ?: MaterialTheme.colorScheme,
        typography = MaterialTheme.typography.copy(
            numeralExtraSmall = MaterialTheme.typography.numeralExtraSmall.copy(
                fontFamily = fontFamily,
            ),
            numeralSmall = MaterialTheme.typography.numeralSmall.copy(
                fontFamily = fontFamily,
            ),
            numeralMedium = MaterialTheme.typography.numeralMedium.copy(
                fontFamily = fontFamily,
            ),
            numeralLarge = MaterialTheme.typography.numeralLarge.copy(
                fontFamily = fontFamily,
            ),
            numeralExtraLarge = MaterialTheme.typography.numeralExtraLarge.copy(
                fontFamily = fontFamily,
            ),
        )
    )
}
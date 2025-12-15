package agdesigns.elevatefitness.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

import agdesigns.elevatefitness.shared.R as sharedR


@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun customTypography(): Typography {
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
    val baseTypography = MaterialTheme.typography
    return baseTypography.copy(
        // replace all emphasized variants with new font family
        displayLargeEmphasized = baseTypography.displayLargeEmphasized.copy(fontFamily = fontFamily),
        displayMediumEmphasized = baseTypography.displayMediumEmphasized.copy(fontFamily = fontFamily),
        displaySmallEmphasized = baseTypography.displaySmallEmphasized.copy(fontFamily = fontFamily),
        headlineLargeEmphasized = baseTypography.headlineLargeEmphasized.copy(fontFamily = fontFamily),
        headlineMediumEmphasized = baseTypography.headlineMediumEmphasized.copy(fontFamily = fontFamily),
        headlineSmallEmphasized = baseTypography.headlineSmallEmphasized.copy(fontFamily = fontFamily),
        titleLargeEmphasized = baseTypography.titleLargeEmphasized.copy(fontFamily = fontFamily),
        titleMediumEmphasized = baseTypography.titleMediumEmphasized.copy(fontFamily = fontFamily),
        titleSmallEmphasized = baseTypography.titleSmallEmphasized.copy(fontFamily = fontFamily),
        bodyLargeEmphasized = baseTypography.bodyLargeEmphasized.copy(fontFamily = fontFamily),
        bodyMediumEmphasized = baseTypography.bodyMediumEmphasized.copy(fontFamily = fontFamily),
        bodySmallEmphasized = baseTypography.bodySmallEmphasized.copy(fontFamily = fontFamily),
        labelLargeEmphasized = baseTypography.labelLargeEmphasized.copy(fontFamily = fontFamily),
        labelMediumEmphasized = baseTypography.labelMediumEmphasized.copy(fontFamily = fontFamily),
        labelSmallEmphasized = baseTypography.labelSmallEmphasized.copy(fontFamily = fontFamily),
    )
}
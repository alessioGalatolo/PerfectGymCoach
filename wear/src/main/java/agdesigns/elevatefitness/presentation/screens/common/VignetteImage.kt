package agdesigns.elevatefitness.presentation.screens.common

import agdesigns.elevatefitness.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.MaterialTheme
import com.google.android.horologist.compose.ambient.AmbientAware

// Credits: Horologist library
@Composable
fun VignetteImage(
    imageBitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    alpha: Float = 0.4f,
    background: Color = MaterialTheme.colorScheme.background,
) {
    // Image with radial gradient
    val animatedBackgroundColor = animateColorAsState(
        targetValue = color,
        animationSpec = tween(450, 0, LinearEasing),
        label = "ColorBackground",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                // pre-compute your brush or shader once per size change
                val brush = Brush.radialGradient(
                    colors = listOf(
                        animatedBackgroundColor.value.copy(alpha = alpha),
                        background,
                    ),
                    center = size.center,
                    radius = size.minDimension / 2
                )
                onDrawWithContent {
                    drawContent()                // 1) draw children (your Image)
                    drawRect(brush = brush)     // 2) overlay the radial gradient
                }
            },
    ) {
        Image(
            imageBitmap,
            contentDescription = stringResource(R.string.exercise_image),
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
    }
}
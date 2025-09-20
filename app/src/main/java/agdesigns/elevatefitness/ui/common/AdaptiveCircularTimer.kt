package agdesigns.elevatefitness.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdaptiveCircularTimer(
    remainingTimeSecs: Long, // time in secs
    progress: Float,
    modifier: Modifier = Modifier,
    padding: Dp = 36.dp // empty space between text and ring
) {
    /*
    Shows a timer in mm:ss with a circular wavy progress indicator around
    Timer text is animated with a scale in/out animation
    Upon timer end, will display a short animation:
        (i) indicator will fill rapidly with a non-wavy line
        (ii) indicator will fade away
        (iii) a rounded, chunky tick will scale in
     */

    // FIXME? if first call is done with remainingTimeMillis = 0L, it still performs end animation
    // Is it desirable?
    val density = LocalDensity.current
    val thickStrokeWidth = with(LocalDensity.current) { 12.dp.toPx() }
    val thickStroke = remember(thickStrokeWidth) { Stroke(width = thickStrokeWidth, cap = StrokeCap.Round) }

    // Track completion state
    var hasCompleted by remember(remainingTimeSecs) { mutableStateOf(false) }
    var showCompletion by remember(remainingTimeSecs) { mutableStateOf(false) }

    // Detect when timer completes
    LaunchedEffect(remainingTimeSecs) {
        if (remainingTimeSecs == 0L && !hasCompleted) {
            hasCompleted = true
            // Start completion animation sequence
            showCompletion = true
        }
    }

    // Animated progress for completion
    var progressAnimationFinished by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (showCompletion) 1f else progress,
        animationSpec = if (showCompletion) {
            MotionScheme.expressive().defaultSpatialSpec<Float>()
//            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        } else {
            MotionScheme.expressive().fastSpatialSpec<Float>()
//            tween(durationMillis = 100)
        },
        finishedListener = { if (showCompletion) progressAnimationFinished = true },
        label = "progress_animation"
    )

    // Alpha animation for text and indicator fade out
    var alphaAnimationFinished by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (showCompletion && progressAnimationFinished) 0f else 1f,
        animationSpec =
            MotionScheme.expressive().defaultEffectsSpec<Float>(),
        finishedListener = { alphaAnimationFinished = true },
        label = "content_alpha"
    )

    // Scale animation for checkmark
    val checkmarkScale by animateFloatAsState(
        targetValue = if (showCompletion && alphaAnimationFinished) 1f else 0f,
        animationSpec =
            MotionScheme.expressive().slowSpatialSpec<Float>(),
        label = "checkmark_scale"
    )

    // we need to measure text size to adapt the ring size
    SubcomposeLayout(modifier = modifier) { constraints ->
        /* 1 Measure the TEXT first */
        val textPlaceable = subcompose("text") {
            AnimatedTimer(
                totalSeconds = remainingTimeSecs,
                textStyle = MaterialTheme.typography.displayMediumEmphasized,
                textColor = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(contentAlpha)
            )
        }.first().measure(constraints)

        /* 2 Decide how big the ring needs to be */
        val diameterPx =
            maxOf(textPlaceable.width, textPlaceable.height) +
                    (padding.roundToPx() * 2)

        /* 3 Measure the INDICATOR with that exact size */
        val indicatorPlaceable = subcompose("indicator") {
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                stroke = thickStroke,
                trackStroke = thickStroke,
                wavelength = 50.dp,
                waveSpeed = 50.dp,
                amplitude = { if (!hasCompleted) WavyProgressIndicatorDefaults.indicatorAmplitude(it) else 0f },
                modifier = Modifier
                    .size(with(density) { diameterPx.toDp() })
                    .alpha(contentAlpha)
            )
        }.first().measure(
            Constraints.fixed(diameterPx, diameterPx)
        )

        /* 4 Measure the CHECKMARK */
        val checkmarkPlaceable = subcompose("checkmark") {
            CompletionCheckmark(
                modifier = Modifier
                    .size(with(density) { (diameterPx * 0.6f).toDp() })
                    .scale(checkmarkScale)
            )
        }.first().measure(constraints)

        /* 5 Lay them out centered on top of each other */
        val layoutWidth  = maxOf(indicatorPlaceable.width, textPlaceable.width)
        val layoutHeight = maxOf(indicatorPlaceable.height, textPlaceable.height)

        layout(layoutWidth, layoutHeight) {
            indicatorPlaceable.placeRelative(
                (layoutWidth - indicatorPlaceable.width) / 2,
                (layoutHeight - indicatorPlaceable.height) / 2
            )
            textPlaceable.placeRelative(
                (layoutWidth - textPlaceable.width) / 2,
                (layoutHeight - textPlaceable.height) / 2
            )
            checkmarkPlaceable.placeRelative(
                (layoutWidth - checkmarkPlaceable.width) / 2,
                (layoutHeight - checkmarkPlaceable.height) / 2
            )
        }
    }
}

// by creating a custom checkmark, we can set custom stroke
@Composable
fun CompletionCheckmark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.1f
        val path = Path().apply {
            // Create checkmark path
            val centerX = size.width / 2
            val centerY = size.height / 2
            val checkSize = size.minDimension * 0.6f

            // Start point (left side of check)
            moveTo(centerX - checkSize * 0.3f, centerY)
            // Middle point (bottom of check)
            lineTo(centerX - checkSize * 0.1f, centerY + checkSize * 0.2f)
            // End point (right side of check)
            lineTo(centerX + checkSize * 0.4f, centerY - checkSize * 0.3f)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphDigit(
    digit: Char,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    textColor: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    AnimatedContent(
        targetState = digit,
        modifier = modifier,
        transitionSpec = {
            // Outgoing shrinks & fades, incoming grows & fades in.
            val scaleIn  = scaleIn(animationSpec = MotionScheme.expressive().defaultSpatialSpec(), initialScale = 0.6f)
            val scaleOut = scaleOut(animationSpec = MotionScheme.expressive().defaultSpatialSpec(), targetScale  = 0.4f)
            (scaleIn + fadeIn(MotionScheme.expressive().defaultSpatialSpec())).togetherWith(scaleOut + fadeOut(MotionScheme.expressive().defaultSpatialSpec()))
                .using(SizeTransform(clip = false))
        },
        label = "digit-morph"
    ) { char ->
        Text(
            char.toString(),
            style = textStyle,
            softWrap = false,              // keeps width constant
            textAlign = TextAlign.Center,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}

@Composable
fun AnimatedTimer(
    totalSeconds: Long,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(),
    textColor: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    val mm = totalSeconds / 60L
    val ss = totalSeconds.mod(60L)
    val formatted = "%02d:%02d".format(mm, ss)
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        formatted.forEach { ch ->
            when (ch) {
                ':' -> Text(":",
                    style = textStyle,
                    color = textColor,
                    fontWeight = fontWeight
                )          // colon doesn't animate
                else -> MorphDigit(
                    digit = ch,
                    textStyle = textStyle,
                    textColor = textColor,
                    fontWeight = fontWeight,
                    modifier = Modifier
                        .width(IntrinsicSize.Max)            // prevents jitter
                )
            }
        }
    }
}
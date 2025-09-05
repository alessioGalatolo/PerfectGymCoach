package agdesigns.elevatefitness.ui.screens.workout.components

import agdesigns.elevatefitness.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterIntensityAndFinishDialog(
    dialogIsOpen: Boolean,
    lastIntensity: Float?,
    dismissDialog: () -> Unit,
    completeWorkout: (Float) -> Unit
) {
    var wrapUp: Boolean by rememberSaveable { mutableStateOf(false) }
    val sliderState = rememberSliderState(
        value = 50f,
        steps = 0,
        valueRange = 0f..100f,
        onValueChangeFinished = {
            wrapUp = true
        },
    )
    LaunchedEffect(wrapUp) {
        if (wrapUp) {
            dismissDialog()
            completeWorkout(sliderState.value)
        }
    }

    if (dialogIsOpen) {
        AlertDialog(
            onDismissRequest = {
                dismissDialog()
            },
            title = { Text(text = stringResource(R.string.how_intense_was_this_workout)) },
            text = { Column {
                var supportText = stringResource(R.string.workout_intensity_info)
                if (lastIntensity != null) {
                    supportText += stringResource(R.string.workout_intensity_intensity_info)
                }
                Text(text = supportText)
                SliderWithIndicator(
                    sliderState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    indicatorPosition = lastIntensity
                )
            } },
            confirmButton = {
                // no confirm button, user needs to proceed by dragging the slider
                // FIXME what if an external bug prevents the slider dragged callback?
            },
            dismissButton = {
                // no dismiss button, use can go back by tapping outside the dialog
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SliderWithIndicator(
    sliderState: SliderState,
    indicatorPosition: Float?, // null or 0-100f
    modifier: Modifier = Modifier,
) {
    val indicatorSize: Dp = 100.dp
    val thumbRadius: Dp = 2.dp
    val layoutDir = LocalLayoutDirection.current
    BoxWithConstraints(
        modifier = modifier
    ) {
        var xOffsetPx = 0
        if (indicatorPosition != null) {
            val maxW = this.maxWidth
            val density = LocalDensity.current

            // fraction of the track (0..1) for current value
            val start = sliderState.valueRange.start
            val end = sliderState.valueRange.endInclusive
            val rawFraction = ((indicatorPosition - start) / (end - start)).coerceIn(0f, 1f)
            val fraction = if (layoutDir == LayoutDirection.Rtl) 1f - rawFraction else rawFraction

            // Compute horizontal offset in px so the icon’s center matches the thumb center
            xOffsetPx = with(density) {
                val boxWidthPx = maxW.toPx()
                val thumbRPx = thumbRadius.toPx()
                val indicatorW = indicatorSize.toPx()
                val trackWidthPx = (boxWidthPx - 2f * thumbRPx).coerceAtLeast(0f)

                // thumb center moves from [thumbR, boxWidth - thumbR]
                val thumbCenterPx = thumbRPx + fraction * trackWidthPx
                (thumbCenterPx - indicatorW / 2f).roundToInt()
            }
        }
        // Stack: icon on top, slider at the bottom
        Box(Modifier.fillMaxWidth()) {
            if (indicatorPosition != null) {
                // The pointing icon (e.g., a downward arrow) sitting above the slider
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown, // points down at the slider
                    contentDescription = null,
                    modifier = Modifier
                        .size(indicatorSize)
                        .offset { IntOffset(x = xOffsetPx, y = 0) }
                        .align(Alignment.TopStart)
                )
            }

            // Give the icon a little breathing room above the slider
            Slider(
                state = sliderState,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(top = 70.dp) // space so the icon doesn't overlap thumb
            )
        }
    }
}

package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutState
import agdesigns.elevatefitness.shared.grpc.Workout
import agdesigns.elevatefitness.utils.RepAndTempoCounter
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material3.AnimatedText
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberAnimatedTextFontRegistry
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.fillMaxRectangle
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun RepsTempoPage(
    state: WorkoutState,
    pagerState: PagerState,
    setResult: RepAndTempoCounter.SetResult? = null,
) {
    val localView = LocalView.current
    val haptics = LocalHapticFeedback.current

    DisposableEffect(Unit) {
        localView.keepScreenOn = true

        onDispose {
            localView.keepScreenOn = false
        }
    }
    val isRest = remember(state.ongoingRestSecs) {
        state.ongoingRestSecs != null && state.ongoingRestSecs > 0L
    }

    PlayerScreen(
        mediaDisplay = {
            if (!isRest) {
                Text(stringResource(R.string.counted_reps))
            } else if (setResult != null && setResult.reps.isNotEmpty()) {
                Text(stringResource(R.string.last_set_data))
            }
        },
        controlButtons = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.currentExercise?.wearRepTrackable == Workout.WearRepTrackable.NOT_TRACKABLE) {
                    Text(stringResource(R.string.tracking_not_supported))
                } else if (isRest) {
                    if (setResult != null && setResult.reps.isNotEmpty()) {
                        PaceChart(setResult)
                    }
                } else {
                    // count reps
                    val textStyle = MaterialTheme.typography.numeralExtraLarge
                    val animatedTextFontRegistry =
                        rememberAnimatedTextFontRegistry(
                            // Variation axes at the start of the animation, width 10, weight 200
                            startFontVariationSettings =
                                FontVariation.Settings(FontVariation.weight(
                                    textStyle.fontWeight?.weight ?: 700
                                )),
                            // Variation axes at the end of the animation, width 100, weight 500
                            endFontVariationSettings =
                                FontVariation.Settings(
                                    FontVariation.weight(
                                        (textStyle.fontWeight?.weight ?: 700).times(1.5f).toInt()
                                    )
                                ),
                            startFontSize = textStyle.fontSize,
                            endFontSize = textStyle.fontSize,
                            textStyle = textStyle.copy(
                                color = MaterialTheme.colorScheme.primary
                            ),
                        )
                    val textAnimatable = remember { Animatable(0f) }
                    val reps = state.autoRepsCount
                    LaunchedEffect(reps) {
                        if (reps == 0)
                            return@LaunchedEffect
                        textAnimatable.animateTo(1f)
                        textAnimatable.animateTo(0f)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        AnimatedText(
                            text = state.autoRepsCount?.toString() ?: "0",
                            fontRegistry = animatedTextFontRegistry,
                            progressFraction = { textAnimatable.value }
                        )
                    } else {
                        Text(
                            text = state.autoRepsCount?.toString() ?: "0",
                            style = textStyle,
                        )
                    }
                }
            }
        },
        buttons = {

        },
    )
}

@Composable
fun PaceChart(result: RepAndTempoCounter.SetResult) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(result) {
        val romValues = result.reps.map { it.rangeOfMotionM * 100.0 }
        val durationValues = result.reps.map { (it.concentricMs + it.eccentricMs) }
        modelProducer.runTransaction {
            columnSeries { series(romValues) }
            lineSeries { series(durationValues) }
        }
    }
    val repFormatter = CartesianValueFormatter { _, x, _ -> "R${(x.toInt() + 1)}" }
    val romFormatter = CartesianValueFormatter { _, y, _ -> "${"%.0f".format(y)}" }
    val durFormatter = CartesianValueFormatter { _, y, _ -> "${"%.1f".format(y / 1000)}" }
    Column(Modifier.fillMaxRectangle()) {
//        Text(
//            "Last set tracking",
//            style = MaterialTheme.typography.titleMedium,
//            textAlign = TextAlign.Center,
//            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
//        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(Modifier
                    .size(10.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.shapes.extraSmall
                    ))
                Text(stringResource(R.string.rom_cm))//, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.extraSmall))
                Text(stringResource(R.string.duration_s))//, style = MaterialTheme.typography.labelSmall)
            }
        }
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider =
                        ColumnCartesianLayer.ColumnProvider.series(
                            listOf(
                                rememberLineComponent(
                                    Fill(MaterialTheme.colorScheme.secondary),
                                    25.dp,
                                    shape = MaterialTheme.shapes.small
                                )
                            )
                        ),
                    columnCollectionSpacing = 4.dp,
                    verticalAxisPosition = Axis.Position.Vertical.Start
                ),
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        listOf(
                            LineCartesianLayer.rememberLine(LineCartesianLayer.LineFill.single(Fill(
                                MaterialTheme.colorScheme.tertiary
                            )))
                        )
                    ),
                    verticalAxisPosition = Axis.Position.Vertical.End
                ),
                startAxis = VerticalAxis.rememberStart(
                    line = rememberLineComponent(Fill.Transparent),
                    tick = rememberLineComponent(Fill.Transparent),
                    valueFormatter = romFormatter,
                    itemPlacer = VerticalAxis.ItemPlacer.step(step = { 5.0 })
                ),
                endAxis = VerticalAxis.rememberEnd(
                    line = rememberLineComponent(Fill.Transparent),
                    tick = rememberLineComponent(Fill.Transparent),
                    valueFormatter = durFormatter,
                    guideline = null,
                    itemPlacer = VerticalAxis.ItemPlacer.step(step = { 5.0 })
                ),
//                bottomAxis = HorizontalAxis.rememberBottom(
//                    tick = rememberLineComponent(Fill.Transparent),
//                    guideline = rememberLineComponent(Fill.Transparent),
//                    valueFormatter = repFormatter,
//                ),
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(false),
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
            modifier = Modifier,
        )
    }
}
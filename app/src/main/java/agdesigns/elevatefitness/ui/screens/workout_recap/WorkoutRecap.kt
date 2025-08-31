package agdesigns.elevatefitness.ui.screens.workout_recap

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.navigation.FullscreenDialogTransition
import agdesigns.elevatefitness.ui.common.InfoDialog
import com.agdesignes.shared.maybeKgToLb
import androidx.compose.foundation.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import agdesigns.elevatefitness.navigation.WorkoutOnlyGraph
import agdesigns.elevatefitness.ui.common.CurrentColumnKey
import agdesigns.elevatefitness.ui.common.ExerciseRecordsList
import agdesigns.elevatefitness.ui.common.columnProviderWithHighlight
import agdesigns.elevatefitness.ui.common.highlightSeriesKey
import agdesigns.elevatefitness.ui.common.lineProviderWithHighlight
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.rememberHorizontalLegend
import com.patrykandpatrick.vico.compose.common.rememberVerticalLegend
import com.patrykandpatrick.vico.compose.common.shape.toVicoShape
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.LegendItem
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.HistoryDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.ExerciseStatsDestination
import java.time.format.DateTimeFormatter


@Destination<WorkoutOnlyGraph>(style = FullscreenDialogTransition::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun WorkoutRecap(
    navigator: DestinationsNavigator,
    workoutId: Long,
    viewModel: RecapViewModel = hiltViewModel()
) {
    val recapState by viewModel.state.collectAsState()
    viewModel.onEvent(RecapEvent.SetWorkoutId(workoutId))
    val volumeDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    val calorieDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    InfoDialog(dialogueIsOpen = volumeDialogIsOpen.value,
        toggleDialogue = { volumeDialogIsOpen.value = !volumeDialogIsOpen.value })
    {
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = LocalContentColor.current)) {
                append(stringResource(R.string.volume_info))
            }
            withLink(
                LinkAnnotation.Url(
                    url = "https://doi.org/10.1007/s40279-017-0793-0",
                    styles = TextLinkStyles(
                        style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                    )
                )
            ) {
                append(stringResource(R.string.learn_more))
            }
        }

        Text(
            text = annotatedText
        )
    }
    InfoDialog(dialogueIsOpen = calorieDialogIsOpen.value,
        toggleDialogue = { calorieDialogIsOpen.value = !calorieDialogIsOpen.value })
    {
        Text(stringResource(R.string.calories_info))
    }
    if (
        recapState.workoutId != 0L &&
        recapState.workoutRecord != null
    ){
        val records = recapState.olderRecords
        val legend = listOf (
            listOf(
                "${stringResource(R.string.volume)} (${if (recapState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg)})",
                stringResource(R.string.this_workout)
            ),
            listOf(
                "${stringResource(R.string.calories)} (kcal)",
                stringResource(R.string.this_workout)
            ),
            listOf(
                "${stringResource(R.string.workout_time)} (s)",
                "${stringResource(R.string.workout_active_time)} (s)",
                stringResource(R.string.this_workout),
            )
        )
        Scaffold(topBar = {
            TopAppBar (title = {
                Text(stringResource(R.string.workout_recap))
            }, navigationIcon = {
                IconButton(onClick = {
                    navigator.navigateUp()
                    navigator.navigateUp()
                    navigator.navigate(
                        HistoryDestination()
                    )
                }) {
                    Icon(Icons.Default.Close, stringResource(R.string.close_icon))
                }
            })
        }) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                item {
                    Text(
                        stringResource(R.string.workout_recap_praise),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
                item{
                    val pagerState = rememberPagerState(pageCount = { 3 })
                    if (records.size > 1){
                        HorizontalPager(state = pagerState) { page ->
                            ElevatedCard(Modifier.padding(horizontal = dimensionResource(R.dimen.card_outside_padding))) {
                                val formatter = DateTimeFormatter.ofPattern("d MMM")
                                val legendItemLabelComponent = rememberTextComponent(
                                    vicoTheme.textColor
                                )
                                val colors = vicoTheme.lineCartesianLayerColors.drop(1)
                                val primaryColor = vicoTheme.lineCartesianLayerColors[0]
                                val cartesianLayer = when (page) {
                                    // calories chart is column, others are lines
                                    1 -> rememberColumnCartesianLayer(
                                        columnProvider = columnProviderWithHighlight(
                                            baseShape = MaterialTheme.shapes.extraSmall,
                                            baseColor = MaterialTheme.colorScheme.secondary,
                                            highlightKey = CurrentColumnKey
                                        )
                                    )
                                    else -> rememberLineCartesianLayer(
                                        lineProviderWithHighlight()
                                    )
                                }
                                CartesianChartHost(
                                    chart = rememberCartesianChart(
                                        cartesianLayer,
                                        startAxis = VerticalAxis.rememberStart(),
                                        bottomAxis = HorizontalAxis.rememberBottom(
                                            valueFormatter = CartesianValueFormatter { _, value, _ ->
                                                recapState.index2date[value.toInt()]?.format(formatter) ?: ""
                                            }
                                        ),
                                        legend =
                                            rememberHorizontalLegend(
                                                items = { extraStore ->
                                                    legend[page].forEachIndexed { index, label ->
                                                        val highlights = extraStore.getOrNull(
                                                            highlightSeriesKey
                                                        ) ?: emptyList()
                                                        val color = if (highlights.contains(index)) {
                                                            primaryColor
                                                        } else {
                                                            colors[index % colors.size]
                                                        }
                                                        val shape = if (
                                                            // calories chart has opposite shapes
                                                            (page == 1 && !highlights.contains(index))
                                                            || (page != 1 && highlights.contains(index))
                                                        ) {
                                                            // FIXME: should use material extraSmall but doesn't work
                                                            CorneredShape.rounded(2f)
                                                        } else
                                                            CorneredShape.Pill
                                                        add(
                                                            LegendItem(
                                                                shapeComponent(
                                                                    fill = fill(color),
                                                                    shape = shape
                                                                ),
                                                                legendItemLabelComponent,
                                                                label,
                                                            )
                                                        )
                                                    }
                                                },
                                                padding = insets(top = 16.dp),
                                            ),
                                    ),
                                    modelProducer = when (page) {
                                        0 -> recapState.volumeChartProducer
                                        1 -> recapState.caloriesChartProducer
                                        2 -> recapState.timeChartProducer
                                        else -> recapState.volumeChartProducer // should not happen
                                    },
                                    modifier = Modifier.padding(8.dp),
                                    scrollState = rememberVicoScrollState(
                                        scrollEnabled = false, // TODO: should not enable scroll inside a horizontal pager
                                        initialScroll = Scroll.Absolute.End // FIXME: scroll to current workout instead
                                    ),
                                )
                            }
                        }
                        Column(Modifier.fillMaxWidth()) {
                            HorizontalPagerIndicator(
                                pagerState = pagerState,
                                pageCount = pagerState.pageCount,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.no_analytics),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 128.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                if (recapState.workoutRecord != null) {
                    item {
                        OutlinedCard(Modifier.padding(horizontal = dimensionResource(R.dimen.card_outside_padding))) {
                            Column(Modifier.padding(dimensionResource(R.dimen.card_outside_padding))) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Outlined.LocalFireDepartment,
                                            stringResource(R.string.calories_burned),
                                            Modifier.size(50.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(
                                                R.string.calorie_consumption_i_kcal,
                                                recapState.workoutRecord!!.calories.toInt()
                                            )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    IconButton(
                                        onClick = { calorieDialogIsOpen.value = true },
                                        modifier = Modifier.weight(0.1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.HelpOutline,
                                            stringResource(R.string.help_icon_info)
                                        )
                                    }
                                }
//                                HorizontalDivider()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.weight_icon),
                                            stringResource(R.string.volume_lifted),
                                            Modifier.size(50.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.total_volume) +
                                                    ": %.2f ".format(maybeKgToLb(
                                                        recapState.workoutRecord!!.volume.toFloat(),
                                                        recapState.imperialSystem
                                                    )) + if (recapState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    IconButton(
                                        onClick = { volumeDialogIsOpen.value = true },
                                        modifier = Modifier.weight(0.1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.help_icon_info))
                                    }
                                }
//                                HorizontalDivider()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        Icons.Outlined.Schedule, stringResource(R.string.workout_time),
                                        Modifier.size(50.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.total_time) +
                                                DateUtils.formatElapsedTime(
                                                    recapState.workoutRecord!!.durationSeconds
                                                )
                                    )
                                }
//                                HorizontalDivider()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        Icons.Outlined.PendingActions, stringResource(R.string.workout_active_time),
                                        Modifier.size(50.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.workout_active_time) + ": " +
                                                DateUtils.formatElapsedTime(
                                                    recapState.workoutRecord!!.activeTimeSeconds
                                                )
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.workout_history),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .padding(horizontal = 16.dp))
                }
                ExerciseRecordsList(
                    recapState.imperialSystem,
                    exerciseRecordsWithImage = recapState.exerciseRecords,
                    onRecordClick = { recordId ->
                        val exerciseId = recapState.exerciseRecords.find { it.recordId == recordId }?.extExerciseId
                        if (exerciseId != null) {
                            navigator.navigate(
                                ExerciseStatsDestination(exerciseId = exerciseId)
                            )
                        }
                    }
                )
            }
        }
    }
}

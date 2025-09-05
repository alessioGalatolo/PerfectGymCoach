package agdesigns.elevatefitness.ui.screens.statistics

import agdesigns.elevatefitness.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.ui.common.ExerciseRecordsList
import agdesigns.elevatefitness.ui.screens.statistics.ExerciseStatsEvent.ChangeOneRepMaxFormula
import agdesigns.elevatefitness.utils.getStickyHeader
import agdesigns.elevatefitness.ui.common.MeanLineKey
import agdesigns.elevatefitness.ui.common.PillChart
import agdesigns.elevatefitness.ui.common.rememberHorizontalLine
import agdesigns.elevatefitness.utils.OneRepMaxFormula
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

@Destination<ChangePlanGraph>(style = SlideTransition::class)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExerciseStats(
    navigator: DestinationsNavigator,
    exerciseId: Long,
    viewModel: ExerciseStatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    viewModel.onEvent(
        ExerciseStatsEvent.StartRetrievingData(
            exerciseId
        )
    )

    var title by remember { mutableStateOf("") }
    val unit = if (state.imperialSystem) stringResource(R.string.lb) else stringResource(
        R.string.kg
    )
    LaunchedEffect(state.exercise) {
        if (state.exercise != null) {
            title = state.exercise!!.name
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
                        )
                    }
                },
            )
        }, content = { innerPadding ->
            if (state.exercise != null) {
                val listState = rememberLazyListState()

                // (key, title) -> we start from MAX_VALUE to avoid conflicts with auto assigned keys
                val headers = listOf(
                    title.ifEmpty { stringResource(R.string.exercise_statistics) },
                    stringResource(R.string.es_header0_volume_progression),
                    stringResource(R.string.es_header1_volume_progression_month),
                    stringResource(R.string.es_header2_max_weight_lifted),
                    stringResource(R.string.es_header3_average_weight_lifted),
                    stringResource(R.string.es_header4_max_reps_done),
                    stringResource(R.string.es_header5_average_reps_done),
                    stringResource(R.string.es_header6_one_rep_max),
                    stringResource(R.string.es_header7_history)
                )
                val stickyHeaders2Id = headers.mapIndexed { index, header ->
                    Pair(header, Int.MAX_VALUE - index)
                }.toMap()
                val id2StickyHeader = stickyHeaders2Id.entries.associate { (k, v) -> v to k }
                var lastVisibleKey by remember { mutableIntStateOf(Int.MAX_VALUE) }
                // Monitor visibility changes
                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo }
                        .collect { layoutInfo ->
                            getStickyHeader(
                                layoutInfo = layoutInfo,
                                id2StickyHeader = id2StickyHeader,
                                lastVisibleKey = lastVisibleKey
                            ).also {
                                title = it.first ?: title
                                lastVisibleKey = it.second
                            }
                        }
                }
                LazyColumn(
                    state = listState,
                    contentPadding = innerPadding,
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AsyncImage(
                            state.exercise!!.image,
                            stringResource(R.string.exercise_image),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(AbsoluteRoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                        )
                    }
                    if (state.exerciseRecords.isNotEmpty()) {
                        val defaultValueFormatter = CartesianValueFormatter { _, value, _ ->
                            val index =
                                value.toInt().coerceIn(0, state.indices2dates.keys.max())
                            state.indices2dates[index]?.format(
                                DateTimeFormatter.ofPattern("MMM dd")
                            ) ?: value.toString() // fallback to value, empty string creates problems
                        }
                        if (state.indices2dates.isNotEmpty()) {
                            // Volume progression
                            PlotPillChart(
                                headers[1],
                                stickyHeaders2Id[headers[1]]!!,
                                state.volumeProgressionAllProducer,
                                xValueFormatter = defaultValueFormatter,
                                markerDecimalFormat = DecimalFormat(
                                    "#.## $unit"
                                ),
                                scrollable = true,
                                meanAvailable = true,
                            )
                        }
                        if (state.volumeMonthIndex2Date.isNotEmpty()) {
                            // Volume progression month
                            PlotPillChart(
                                headers[2],
                                stickyHeaders2Id[headers[2]]!!,
                                state.volumeProgressionMonthProducer,
                                xValueFormatter = CartesianValueFormatter { _, value, _ ->
                                    val index =
                                        value.toInt()
                                            .coerceIn(0, state.volumeMonthIndex2Date.keys.maxOrNull())
                                    state.volumeMonthIndex2Date[index]?.format(
                                        DateTimeFormatter.ofPattern("MMM dd")
                                    ) ?: value.toString() // fallback to value, empty string creates problems
                                },
                                markerDecimalFormat = DecimalFormat(
                                    "#.## $unit"
                                ),
                                scrollable = false
                            )
                        }
                        if (state.indices2dates.isNotEmpty()) {
                            // max weights lifted
                            PlotPillChart(
                                headers[3],
                                stickyHeaders2Id[headers[3]]!!,
                                state.maxWeightsProducer,
                                xValueFormatter = defaultValueFormatter,
                                markerDecimalFormat = DecimalFormat(
                                    "#.## $unit"
                                ),
                                scrollable = true
                            )

                            // average weights lifted
                            PlotPillChart(
                                headers[4],
                                stickyHeaders2Id[headers[4]]!!,
                                state.avgWeightProducer,
                                xValueFormatter = defaultValueFormatter,
                                markerDecimalFormat = DecimalFormat(
                                    "#.## $unit"
                                ),
                                scrollable = true
                            )
                            // max reps done
                            PlotPillChart(
                                headers[5],
                                stickyHeaders2Id[headers[5]]!!,
                                state.maxRepsProducer,
                                xValueFormatter = defaultValueFormatter,
                                markerDecimalFormat = DecimalFormat(
                                    "#.##"
                                ),
                                scrollable = true
                            )
                            // average reps done
                            PlotPillChart(
                                headers[6],
                                stickyHeaders2Id[headers[6]]!!,
                                state.avgRepsProducer,
                                xValueFormatter = defaultValueFormatter,
                                markerDecimalFormat = DecimalFormat(
                                    "#.##"
                                ),
                                scrollable = true
                            )

                            // one rep max
                            PlotPillChart(
                                headers[7],
                                stickyHeaders2Id[headers[7]]!!,
                                state.oneRepMaxsProducer,
                                xValueFormatter = defaultValueFormatter,
                                markerDecimalFormat = DecimalFormat(
                                    "#.## $unit"
                                ),
                                scrollable = true
                            )
                            item {
                                var expanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it }) {
                                    OutlinedTextField(
                                        value = state.oneRepMaxFormula.displayName,
                                        onValueChange = { },
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = expanded
                                            )
                                        },
                                        label = { Text(stringResource(R.string._1rm_calculation_method)) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier = Modifier.menuAnchor(
                                            ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                            true
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }) {
                                        OneRepMaxFormula.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        option.displayName,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                },
                                                onClick = {
                                                    expanded = false
                                                    viewModel.onEvent(ChangeOneRepMaxFormula(option))
                                                },
                                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // History
                        item (key = stickyHeaders2Id[headers[8]]!!) {
                            Text(
                                headers[8],
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                            )
                        }
                        ExerciseRecordsList(state.imperialSystem, state.exerciseRecords)
                    } else {
                        item {
                            Text(
                                stringResource(R.string.empty_history),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

fun LazyListScope.PlotPillChart(
    name: String,
    stickyKey: Int,
    modelProducer: CartesianChartModelProducer,
    xValueFormatter: CartesianValueFormatter,
    markerDecimalFormat: DecimalFormat,
    scrollable: Boolean,
    meanAvailable: Boolean = false,
) {
    item(key = stickyKey) {
        Text(
            name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .fillMaxWidth()
        )
    }
    item {
        val decorations = if (meanAvailable) listOf(
            rememberHorizontalLine(
                MeanLineKey,
                stringResource(R.string.average)
            )
        ) else emptyList()
        ElevatedCard (
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            PillChart(
                modelProducer,
                markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
                    markerDecimalFormat
                ),
                xValueFormatter = xValueFormatter,
                decorations = decorations,
                modifier = Modifier.padding(8.dp),
                scrollable = scrollable
            )
        }
    }
}
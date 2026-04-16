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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.screens.statistics.ExerciseStatsEvent.ChangeOneRepMaxFormula
import agdesigns.elevatefitness.utils.getStickyHeader
import agdesigns.elevatefitness.ui.common.MeanLineKey
import agdesigns.elevatefitness.ui.common.PillChart
import agdesigns.elevatefitness.ui.common.rememberHorizontalLine
import agdesigns.elevatefitness.ui.screens.workout.components.HistoricRecord
import agdesigns.elevatefitness.utils.OneRepMaxFormula
import agdesigns.elevatefitness.utils.plus
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import kotlinx.coroutines.launch
import java.text.DecimalFormat


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun ExerciseStats(
    navigator: DestinationsNavigator,
    exerciseId: Long,
    viewModel: ExerciseStatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(exerciseId) {
        viewModel.onEvent(
            ExerciseStatsEvent.StartRetrievingData(
                exerciseId
            )
        )
    }

    var title by remember { mutableStateOf("") }
    val unit = if (state.imperialSystem) stringResource(R.string.lb) else stringResource(
        R.string.kg
    )
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
                        )
                    }
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
        }, content = { innerPadding ->
            if (state.exercise != null) {

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
                    contentPadding = innerPadding + PaddingValues(
                        bottom = dimensionResource(R.dimen.screen_edge_padding)
                    ),
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AsyncImage(
                            state.exercise!!.image,
                            stringResource(R.string.exercise_image),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .clip(AbsoluteRoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                        )
                    }
                    item {
                        Text(
                            state.exercise!!.name,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineLargeEmphasized,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                    }
                    if (state.exerciseRecords.isNotEmpty()) {
                        // Summary stats row
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ExerciseStatCard(
                                        label = stringResource(R.string.es_sessions),
                                        value = state.totalSessions.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    ExerciseStatCard(
                                        label = stringResource(R.string.es_personal_best),
                                        value = "%.1f %s".format(state.personalBestWeight, unit),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ExerciseStatCard(
                                        label = stringResource(R.string.es_total_volume),
                                        value = "%.0f %s".format(state.totalVolume, unit),
                                        modifier = Modifier.weight(1f)
                                    )
                                    ExerciseStatCard(
                                        label = stringResource(R.string.es_best_1rm),
                                        value = "%.1f %s".format(state.bestOneRepMax, unit),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // X-axis formatter for volume charts (one point per month)
                        val defaultValueFormatter = CartesianValueFormatter { _, value, _ ->
                            val index =
                                value.toInt().coerceIn(0, state.indices2dates.keys.max())
                            state.indices2dates[index] ?: value.toString()
                        }
                        // X-axis formatter for per-session charts (one point per workout session)
                        val sessionValueFormatter = CartesianValueFormatter { _, value, _ ->
                            val index =
                                value.toInt().coerceIn(0, state.perSessionIndex2Date.keys.maxOrNull() ?: 0)
                            state.perSessionIndex2Date[index] ?: value.toString()
                        }
                        if (state.indices2dates.isNotEmpty()) {
                            // Volume progression
                            PlotPillChart(
                                headers[1],
                                stickyHeaders2Id[headers[1]]!!,
                                state.volumeProgressionAllProducer,
                                xValueFormatter = defaultValueFormatter,
                                markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
                                    decimalCount = 2,
                                    suffix = unit
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
                                xValueFormatter = { _, value, _ ->
                                    val index =
                                        value.toInt()
                                            .coerceIn(0, state.volumeMonthIndex2Date.keys.maxOrNull())
                                    state.volumeMonthIndex2Date[index] ?: value.toString()
                                },
                                markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
                                    decimalCount = 2,
                                    suffix = unit
                                ),
                                scrollable = false
                            )
                        }
                        if (state.perSessionIndex2Date.isNotEmpty()) {
                            // max weights lifted
                            PlotPillChart(
                                headers[3],
                                stickyHeaders2Id[headers[3]]!!,
                                state.maxWeightsProducer,
                                xValueFormatter = sessionValueFormatter,
                                markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
                                    decimalCount = 2,
                                    suffix = unit
                                ),
                                scrollable = true
                            )

                            // average weights lifted
                            PlotPillChart(
                                headers[4],
                                stickyHeaders2Id[headers[4]]!!,
                                state.avgWeightProducer,
                                xValueFormatter = sessionValueFormatter,
                                markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
                                    decimalCount = 2,
                                    suffix = unit
                                ),
                                scrollable = true
                            )
                            // max reps done
                            PlotPillChart(
                                headers[5],
                                stickyHeaders2Id[headers[5]]!!,
                                state.maxRepsProducer,
                                xValueFormatter = sessionValueFormatter,
                                markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
                                    decimalCount = 2,
                                ),
                                scrollable = true
                            )
                            // average reps done
                            PlotPillChart(
                                headers[6],
                                stickyHeaders2Id[headers[6]]!!,
                                state.avgRepsProducer,
                                xValueFormatter = sessionValueFormatter,
                                markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
                                    decimalCount = 2,
                                ),
                                scrollable = true
                            )

                            // one rep max
                            PlotPillChart(
                                headers[7],
                                stickyHeaders2Id[headers[7]]!!,
                                state.oneRepMaxsProducer,
                                xValueFormatter = sessionValueFormatter,
                                markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
                                    decimalCount = 2,
                                    suffix = unit
                                ),
                                scrollable = true
                            )
                            item {
                                var expanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
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
                        items(state.exerciseRecords) { record ->
                            HistoricRecord(
                                record,
                                state.imperialSystem,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
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

@Composable
private fun ExerciseStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(72.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

fun LazyListScope.PlotPillChart(
    name: String,
    stickyKey: Int,
    modelProducer: CartesianChartModelProducer,
    xValueFormatter: CartesianValueFormatter,
    markerValueFormatter: DefaultCartesianMarker.ValueFormatter,
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
                markerValueFormatter = markerValueFormatter,
                xValueFormatter = xValueFormatter,
                decorations = decorations,
                modifier = Modifier.padding(8.dp),
                scrollable = scrollable
            )
        }
    }
}
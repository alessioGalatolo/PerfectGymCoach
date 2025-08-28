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
import agdesigns.elevatefitness.utils.OneRepMaxFormula
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.ui.components.ExerciseRecordsList
import agdesigns.elevatefitness.utils.getStickyHeader
import agdesigns.elevatefitness.ui.screens.statistics.ExerciseStatsEvent.ChangeOneRepMaxFormula
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import com.jaikeerthick.composable_graphs.composables.line.LineGraph
import com.jaikeerthick.composable_graphs.composables.line.model.LineData
import com.jaikeerthick.composable_graphs.composables.line.style.LineGraphStyle
import com.jaikeerthick.composable_graphs.composables.line.style.LineGraphVisibility
import com.jaikeerthick.composable_graphs.style.LabelPosition
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

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
                    stringResource(R.string.es_header0_volume_progression),
                    stringResource(R.string.es_header1_max_weight_lifted),
                    stringResource(R.string.es_header2_average_weight_lifted),
                    stringResource(R.string.es_header3_max_reps_done),
                    stringResource(R.string.es_header4_average_reps_done),
                    stringResource(R.string.es_header5_one_rep_max),
                    stringResource(R.string.es_header6_history)
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
                        // Volume progression
                        PlotStat(
                            headers[0],
                            stickyHeaders2Id[headers[0]]!!,
                            state.volumeProgression
                        )
                        // max weights lifted
                        PlotStat(
                            headers[1],
                            stickyHeaders2Id[headers[1]]!!,
                            state.maxWeights.zip(state.volumeProgression) { maxWeight, lineData ->
                                LineData(
                                    lineData.x,
                                    maxWeight
                                )
                            })
                        // average weights lifted
                        PlotStat(
                            headers[2],
                            stickyHeaders2Id[headers[2]]!!,
                            state.avgWeight.zip(state.volumeProgression) { maxRep, lineData ->
                                LineData(
                                    lineData.x,
                                    maxRep
                                )
                            })
                        // max reps done
                        PlotStat(
                            headers[3],
                            stickyHeaders2Id[headers[3]]!!,
                            state.maxReps.zip(state.volumeProgression) { maxRep, lineData ->
                                LineData(
                                    lineData.x,
                                    maxRep
                                )
                            })
                        // average reps done
                        PlotStat(
                            headers[4],
                            stickyHeaders2Id[headers[4]]!!,
                            state.avgReps.zip(state.volumeProgression) { maxRep, lineData ->
                                LineData(
                                    lineData.x,
                                    maxRep
                                )
                            })
                        if (state.oneRepMaxs.isNotEmpty()) {
                            // one rep max
                            item (key = stickyHeaders2Id[headers[5]]!!) {
                                Text(
                                    headers[5],
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
                                var expanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it }) {
                                    TextField(
                                        value = state.oneRepMaxFormula.displayName,
                                        onValueChange = { },
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = expanded
                                            )
                                        },
                                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                        modifier = Modifier.menuAnchor(
                                            MenuAnchorType.PrimaryNotEditable,
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
                            item {
                                var selectedValue by remember { mutableStateOf("") }
                                ElevatedCard(
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    if (selectedValue.isNotEmpty()) {
                                        Text(
                                            stringResource(R.string.selected_value_i, selectedValue),
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                    if (state.oneRepMaxs.size > 1) {
                                        LineGraph(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .padding(16.dp),
                                            data = state.oneRepMaxs.zip(state.volumeProgression)
                                                .map { (oneRepMax, lineData) ->
                                                    LineData(
                                                        lineData.x,
                                                        oneRepMax
                                                    )
                                                },
                                            style = LineGraphStyle(
                                                visibility = LineGraphVisibility(
                                                    isYAxisLabelVisible = true
                                                ),
                                                yAxisLabelPosition = LabelPosition.LEFT
                                            ),
                                            onPointClick = { selectedValue = it.y.toString() }
                                        )
                                    } else {
                                        Box(Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                stringResource(R.string.too_few_records_for_stats),
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(16.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // FIXME: history does not stick
                        item (key = stickyHeaders2Id[headers[6]]!!) {
                            Text(
                                headers[6],
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

fun LazyListScope.PlotStat(name: String, stickyKey: Int, data: List<LineData>) {
    if (data.isNotEmpty()) {
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
            var selectedValue by remember { mutableStateOf("") }
            ElevatedCard (
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (selectedValue.isNotEmpty()) {
                    Text(
                        stringResource(R.string.selected_value_i, selectedValue),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                if (data.size > 1) {
                    LineGraph(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        data = data,
                        style = LineGraphStyle(
                            visibility = LineGraphVisibility(
                                isYAxisLabelVisible = true
                            ),
                            yAxisLabelPosition = LabelPosition.LEFT
                        ),
                        onPointClick = { selectedValue = it.y.toString() }
                    )
                } else {
                    Box(Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.too_few_records_for_stats),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
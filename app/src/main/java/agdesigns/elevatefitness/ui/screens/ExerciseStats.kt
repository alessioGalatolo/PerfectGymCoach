package agdesigns.elevatefitness.ui.screens

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
import agdesigns.elevatefitness.ui.ChangePlanGraph
import agdesigns.elevatefitness.ui.OneRepMaxFormula
import agdesigns.elevatefitness.ui.SlideTransition
import agdesigns.elevatefitness.ui.components.ExerciseRecordsList
import agdesigns.elevatefitness.ui.getStickyHeader
import agdesigns.elevatefitness.viewmodels.ExerciseStatsEvent
import agdesigns.elevatefitness.viewmodels.ExerciseStatsEvent.ChangeOneRepMaxFormula
import agdesigns.elevatefitness.viewmodels.ExerciseStatsViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
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
                            contentDescription = "Go back"
                        )
                    }
                },
            )
        }, content = { innerPadding ->
            if (state.exercise != null) {
                val listState = rememberLazyListState()

                // (key, title) -> we start from MAX_VALUE to avoid conflicts with auto assigned keys
                val stickyHeaders2Id = mapOf(
                    Pair(state.exercise!!.name, Int.MAX_VALUE),
                    Pair("Volume Progression", Int.MAX_VALUE - 1),
                    Pair("Max Weight Lifted", Int.MAX_VALUE - 2),
                    Pair("Average Weight Lifted", Int.MAX_VALUE - 3),
                    Pair("Max Reps Done", Int.MAX_VALUE - 4),
                    Pair("Average Reps Done", Int.MAX_VALUE - 5),
                    Pair("One Rep Max", Int.MAX_VALUE - 6),
                    Pair("History", Int.MAX_VALUE - 7)
                )
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
                            "Exercise image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(AbsoluteRoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                        )
                    }
                    if (state.exerciseRecords.isNotEmpty()) {
                        PlotStat(
                            "Volume Progression",
                            stickyHeaders2Id["Volume Progression"]!!,
                            state.volumeProgression
                        )
                        PlotStat(
                            "Max Weight Lifted",
                            stickyHeaders2Id["Max Weight Lifted"]!!,
                            state.maxWeights.zip(state.volumeProgression) { maxWeight, lineData ->
                                LineData(
                                    lineData.x,
                                    maxWeight
                                )
                            })
                        PlotStat(
                            "Average Weight Lifted",
                            stickyHeaders2Id["Average Weight Lifted"]!!,
                            state.avgWeight.zip(state.volumeProgression) { maxRep, lineData ->
                                LineData(
                                    lineData.x,
                                    maxRep
                                )
                            })
                        PlotStat(
                            "Max Reps Done",
                            stickyHeaders2Id["Max Reps Done"]!!,
                            state.maxReps.zip(state.volumeProgression) { maxRep, lineData ->
                                LineData(
                                    lineData.x,
                                    maxRep
                                )
                            })
                        PlotStat(
                            "Average Reps Done",
                            stickyHeaders2Id["Average Reps Done"]!!,
                            state.avgReps.zip(state.volumeProgression) { maxRep, lineData ->
                                LineData(
                                    lineData.x,
                                    maxRep
                                )
                            })
                        if (state.oneRepMaxs.isNotEmpty()) {
                            item (key = stickyHeaders2Id["One Rep Max"]!!) {
                                Text(
                                    "One Rep Max",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp).fillMaxWidth()
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
                                            "Selected value: $selectedValue",
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
                                                "Keep going! Once you have more than one record, you will see key statistics here",
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
                        item (key = stickyHeaders2Id["History"]!!) {
                            Text(
                                "History",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp).fillMaxWidth()
                            )
                        }
                        ExerciseRecordsList(state.imperialSystem, state.exerciseRecords)
                    } else {
                        item {
                            Text(
                                "No records found",
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
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp).fillMaxWidth()
            )
        }
        item {
            var selectedValue by remember { mutableStateOf("") }
            ElevatedCard (
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (selectedValue.isNotEmpty()) {
                    Text(
                        "Selected value: $selectedValue",
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
                            "Keep going! Once you have more than one record, you will see key statistics here",
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
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
import agdesigns.elevatefitness.viewmodels.ExerciseStatsEvent
import agdesigns.elevatefitness.viewmodels.ExerciseStatsEvent.ChangeOneRepMaxFormula
import agdesigns.elevatefitness.viewmodels.ExerciseStatsViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListScope
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.exercise?.name ?: "") },
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
                // FIXME: currently ignoring innerPadding and using appbar height and statusbar
                // as offset, otherwise stickyHeader goes behind status bar.
                LazyColumn(
//                    contentPadding = innerPadding,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = TopAppBarDefaults.MediumAppBarCollapsedHeight)
                        .padding(WindowInsets.statusBars.asPaddingValues()),
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
                        PlotStat("Volume Progression", state.volumeProgression)
                        PlotStat(
                            "Max Weight Lifted",
                            state.maxWeights.zip(state.volumeProgression) { maxWeight, lineData ->
                                LineData(
                                    lineData.x,
                                    maxWeight
                                )
                            })
                        PlotStat(
                            "Average Weight Lifted",
                            state.avgWeight.zip(state.volumeProgression) { maxRep, lineData ->
                                LineData(
                                    lineData.x,
                                    maxRep
                                )
                            })
                        PlotStat(
                            "Max Reps Done",
                            state.maxReps.zip(state.volumeProgression) { maxRep, lineData ->
                                LineData(
                                    lineData.x,
                                    maxRep
                                )
                            })
                        PlotStat(
                            "Average Reps Done",
                            state.avgReps.zip(state.volumeProgression) { maxRep, lineData ->
                                LineData(
                                    lineData.x,
                                    maxRep
                                )
                            })
                        if (state.oneRepMaxs.isNotEmpty()) {
                            stickyHeader {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth().background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                    )
                                ) {
                                    Text(
                                        "One Rep Max",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
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
                        stickyHeader {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth().background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                )
                            ) {
                                Text(
                                    "History",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
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
                    // FIXME: remove once the stickyHeaders have been properly fixed and innerPadding is actually used
                    item {
                        Spacer(Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
                    }
                }
            }
        }
    )
}

fun LazyListScope.PlotStat(name: String, data: List<LineData>) {
    if (data.isNotEmpty()) {
        stickyHeader {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )) {
                Text(
                    name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }
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
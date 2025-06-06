package agdesigns.elevatefitness.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.workout_record.WorkoutRecordAndName
import agdesigns.elevatefitness.ui.BottomNavigationGraph
import agdesigns.elevatefitness.ui.FadeTransition
import agdesigns.elevatefitness.ui.maybeKgToLb
import agdesigns.elevatefitness.viewmodels.HistoryViewModel
import androidx.compose.runtime.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.WorkoutRecapDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale


@Composable
fun WorkoutCalendarCards(recordsMap: Map<Int, List<WorkoutRecordAndName>>, listState: LazyListState) {
    if (recordsMap.isNotEmpty()) {
        val weekField = WeekFields.of(Locale.getDefault()).weekOfYear()
        val currentWeek = ZonedDateTime.now().get(weekField)
        val scope = rememberCoroutineScope()
        Column(Modifier.fillMaxWidth()) {
            LazyRow(
                Modifier
                    .fillMaxWidth(),
                reverseLayout = true,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(id = R.dimen.card_space_between)
                )
            ){
                item {
                    Spacer(Modifier.width(8.dp))  // FIXME: centralize the value
                }
                for (week in currentWeek downTo 1) {
                    // FIXME: what if it's the beginning of the year?
                    item {
                        val weekRecords = recordsMap[week] ?: emptyList()
                        if (weekRecords.isEmpty()) {
                            OutlinedCard(Modifier.padding(dimensionResource(R.dimen.card_space_between) / 2)) {
                                Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                                    Text(
                                        "Week $week",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Icon(
                                        Icons.Filled.SentimentNeutral,
                                        contentDescription = "Neutral emotion",
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Icon(
                                        Icons.Filled.Remove,
                                        contentDescription = "No workout",
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        } else {
                            ElevatedCard(
                                Modifier
                                    .padding(dimensionResource(R.dimen.card_space_between)/2)
                                    .clickable {
                                        scope.launch {
                                            listState.animateScrollToItem(index =
                                                recordsMap.toSortedMap().tailMap(week).keys.size // week headers
                                                // FIXME: does this work? not very well.
                                            )
                                        }
                                    }
                            ) {
                                Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                                    Text(
                                        "Week $week",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(Modifier.height(4.dp))

                                    val icon = if (weekRecords.size > 2) listOf(
                                        Icons.Filled.Rocket,
                                        Icons.Filled.RocketLaunch,
                                        Icons.Filled.Whatshot,
                                        Icons.Filled.SelfImprovement
                                    ).random() else Icons.Filled.SentimentVerySatisfied
                                    Icon(
                                        icon,
                                        contentDescription = "Let's go!",
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly){
                                        for (i in 1..weekRecords.size) {
                                            Icon(Icons.Filled.FitnessCenter, contentDescription = "Workout $i")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    }
}


@Destination<BottomNavigationGraph>(style = FadeTransition::class)
@Composable
fun History(
    navigator: DestinationsNavigator,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyState by viewModel.state.collectAsState()
    val recordsMapMap = historyState.workoutRecords
    if (recordsMapMap.isEmpty())
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "",
                modifier = Modifier.size(160.dp)
            )
            Text(
                stringResource(id = R.string.empty_history),
                modifier = Modifier.padding(16.dp)
            )
        }
    else {
        val currentYear = ZonedDateTime.now().year
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                WorkoutCalendarCards(
                    recordsMapMap[currentYear] ?: emptyMap(),
                    listState,
                )
            }
            var yearIteration = currentYear
            for (recordMap in recordsMapMap.toSortedMap(compareByDescending { it })) {
                if (recordMap.key != yearIteration) {
                    item {
                        Text(
                            recordMap.key.toString(),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    yearIteration = recordMap.key
                }
                var weekIteration = ZonedDateTime.now().year
                item {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        for (record in recordMap.value.toSortedMap(compareByDescending { it })) {
                            if (record.key != weekIteration) {
                                Text(
                                    "Week ${record.key}",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                                Spacer(Modifier.height(8.dp))
                                weekIteration = record.key
                            }
                            var isFirst = true
                            val sortedRecords = record.value.sortedByDescending { it.startDate }
                            for (workout in sortedRecords) {
                                if (isFirst)
                                    isFirst = false
                                else
                                    HorizontalDivider()
                                Column(modifier = Modifier.fillMaxWidth().clickable{
                                    navigator.navigate(
                                        WorkoutRecapDestination(
                                            workoutId = workout.workoutId
                                        )
                                    )
                                }) {
                                    val formatter = DateTimeFormatter.ofPattern("d MMM (yyyy) - HH:mm")
                                    val date = workout.startDate!!.format(formatter)
                                    Row {
                                        Text(
                                            workout.name,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                    Row {
                                        Text(
                                            "Volume: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic
                                        )
                                        Text(
                                            "${maybeKgToLb(
                                                workout.volume,
                                                historyState.useImperialSystem
                                            )} ${if (historyState.useImperialSystem) "lb" else "kg"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row {
                                        Text(
                                            "Calories: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic
                                        )
                                        Text(
                                            "${workout.calories.toInt()} kcal",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row {
                                        Text(
                                            "Duration: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic
                                        )
                                        Text(
                                            "${(workout.durationSeconds / 60).toInt()}m",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Text(
                                        date.replace("-", "at"),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .statusBarsPadding()
        ) {
            // Section header with improved styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Calendar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                reverseLayout = true,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                for (week in currentWeek downTo 1) {
                    item {
                        val weekRecords = recordsMap[week] ?: emptyList()
                        if (weekRecords.isEmpty()) {
                            // Empty week card with subtle styling
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(140.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Week $week",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "No workouts",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Rest week",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            // Active week card with enhanced styling
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(140.dp)
                                    .clickable {
                                        scope.launch {
                                            listState.animateScrollToItem(
                                                index = recordsMap.toSortedMap().tailMap(week).keys.size
                                            )
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 6.dp,
                                    hoveredElevation = 8.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Header
                                    Text(
                                        text = "Week $week",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    // Achievement icon
                                    val (icon, iconColor) = when {
                                        weekRecords.size >= 5 -> Icons.Default.Whatshot to MaterialTheme.colorScheme.onPrimaryContainer
                                        weekRecords.size >= 3 -> Icons.Default.RocketLaunch to MaterialTheme.colorScheme.onPrimaryContainer
                                        weekRecords.size >= 2 -> Icons.Default.SelfImprovement to MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> Icons.Default.SentimentVerySatisfied to MaterialTheme.colorScheme.onPrimaryContainer
                                    }

                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Achievement",
                                        tint = iconColor,
                                        modifier = Modifier.size(28.dp)
                                    )

                                    // Workout indicators
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        repeat(minOf(weekRecords.size, 7)) { index ->
                                            Icon(
                                                imageVector = Icons.Default.FitnessCenter,
                                                contentDescription = "Workout ${index + 1}",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            if (index < minOf(weekRecords.size, 7) - 1) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }
                                        }
                                    }

                                    // Workout count
                                    Text(
                                        text = "${weekRecords.size} workout${if (weekRecords.size != 1) "s" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
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

    if (recordsMapMap.isEmpty()) {
        // Empty state with improved styling
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Empty history",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(24.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.empty_history),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Start your fitness journey today!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        val currentYear = ZonedDateTime.now().year
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                WorkoutCalendarCards(
                    recordsMapMap[currentYear] ?: emptyMap(),
                    listState
                )
            }

            var yearIteration = currentYear
            for (recordMap in recordsMapMap.toSortedMap(compareByDescending { it })) {
                if (recordMap.key != yearIteration) {
                    item {
                        // Year header with enhanced styling
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Year",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = recordMap.key.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    yearIteration = recordMap.key
                }

                var weekIteration = ZonedDateTime.now().year
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        for (record in recordMap.value.toSortedMap(compareByDescending { it })) {
                            if (record.key != weekIteration) {
                                // Week header with subtle styling
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Week",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Week ${record.key}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                                weekIteration = record.key
                            }

                            // Workout cards with enhanced styling
                            val sortedRecords = record.value.sortedByDescending { it.startDate }
                            sortedRecords.forEachIndexed { index, workout ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            navigator.navigate(
                                                WorkoutRecapDestination(workoutId = workout.workoutId)
                                            )
                                        },
//                                    colors = CardDefaults.cardColors(
//                                        containerColor = MaterialTheme.colorScheme.surface
//                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        // Workout name and date
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = workout.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )

                                            val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
                                            val date = workout.startDate!!.format(formatter)
                                            Text(
                                                text = date,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Workout stats in a grid
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Volume stat
                                            StatCard(
                                                icon = Icons.Default.FitnessCenter,
                                                label = "Volume",
                                                value = "${maybeKgToLb(workout.volume, historyState.useImperialSystem)} ${if (historyState.useImperialSystem) "lb" else "kg"}",
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Calories stat
                                            StatCard(
                                                icon = Icons.Default.LocalFireDepartment,
                                                label = "Calories",
                                                value = "${workout.calories.toInt()} kcal",
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Duration stat
                                            StatCard(
                                                icon = Icons.Default.Schedule,
                                                label = "Duration",
                                                value = "${(workout.durationSeconds / 60).toInt()}m",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                if (index < sortedRecords.size - 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
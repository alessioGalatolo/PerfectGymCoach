package agdesigns.elevatefitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import agdesigns.elevatefitness.ui.BottomNavigationGraph
import agdesigns.elevatefitness.ui.FadeTransition
import agdesigns.elevatefitness.viewmodels.ExerciseStats
import agdesigns.elevatefitness.viewmodels.PersonalRecord
import agdesigns.elevatefitness.viewmodels.StatisticsEvent
import agdesigns.elevatefitness.viewmodels.StatisticsViewModel
import agdesigns.elevatefitness.viewmodels.TimeFrame
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jaikeerthick.composable_graphs.composables.bar.BarGraph
import com.jaikeerthick.composable_graphs.composables.bar.model.BarData
import com.jaikeerthick.composable_graphs.composables.bar.style.BarGraphStyle
import com.jaikeerthick.composable_graphs.composables.bar.style.BarGraphVisibility
import com.jaikeerthick.composable_graphs.composables.donut.DonutChart
import com.jaikeerthick.composable_graphs.composables.donut.style.DonutChartStyle
import com.jaikeerthick.composable_graphs.composables.donut.style.DonutChartType
import com.jaikeerthick.composable_graphs.composables.donut.style.DonutSliceType
import com.jaikeerthick.composable_graphs.composables.line.LineGraph
import com.jaikeerthick.composable_graphs.composables.line.style.LineGraphStyle
import com.jaikeerthick.composable_graphs.composables.line.style.LineGraphVisibility
import com.jaikeerthick.composable_graphs.composables.pie.PieChart
import com.jaikeerthick.composable_graphs.composables.pie.style.PieChartStyle
import com.jaikeerthick.composable_graphs.style.LabelPosition
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ExerciseStatsDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Destination<BottomNavigationGraph>(style = FadeTransition::class)
@Composable
fun Statistics(
    destinationsNavigator: DestinationsNavigator,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()  // TODO: remove and add topappbar
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Time Frame Selector
        Box(Modifier.fillMaxWidth().background(Color.Transparent), contentAlignment = Alignment.Center) {
            TimeFrameSelector(
                selectedTimeFrame = state.selectedTimeFrame,
                onTimeFrameSelected = { timeFrame ->
                    viewModel.onEvent(StatisticsEvent.OnTimeFrameChanged(timeFrame))
                }
            )
        }
        if (state.isLoading) {
            Column (
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(state.progressText)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Cards
                OverviewCards(
                    totalWorkouts = state.totalWorkouts,
                    totalVolume = state.totalVolume,
                    avgDuration = state.avgWorkoutDuration,
                    totalCalories = state.totalCalories,
                    avgCalories = state.avgCalories,
                    currentStreak = state.currentStreak,
                    longestStreak = state.longestStreak,
                    useImperial = state.useImperialSystem
                )

                // Volume Progress Chart
                if (state.weeklyVolume.isNotEmpty()) {
                    stickyHeader {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )) {
                            Text(
                                "Volume Progress",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    item {
                        var selectedValue by remember { mutableStateOf("") }
                        ElevatedCard (
//                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            if (selectedValue.isNotEmpty()) {
                                Text(
                                    "Selected value: $selectedValue",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            LineGraph(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(16.dp),
                                data = state.weeklyVolume,
                                style = LineGraphStyle(
                                    visibility = LineGraphVisibility(
                                        isYAxisLabelVisible = true
                                    ),
                                    yAxisLabelPosition = LabelPosition.LEFT
                                ),
                                onPointClick = { selectedValue = it.y.toString() }
                            )
                        }
                    }
                }

                // Workout Frequency Chart
                if (state.monthlyWorkouts.isNotEmpty()) {
                    stickyHeader {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )) {
                            Text(
                                "Workout Frequency",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp)
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
                            BarGraph(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(16.dp),
                                data = state.monthlyWorkouts.map {
                                    BarData(it.x, it.y)
                                },
                                onBarClick = { selectedValue = it.y.toInt().toString() },
                                // FIXME: cannot show y labels as it shows one label per point
                                // resulting in repeated entries e.g., 0 1 1 2 3 3 4
//                                style = BarGraphStyle(
//                                    visibility = BarGraphVisibility(
//                                        isYAxisLabelVisible = true
//                                    ),
//                                    yAxisLabelPosition = LabelPosition.LEFT
//                                )
                            )
                        }
                    }
                }

                // Muscle Group Distribution
                if (state.muscleGroupDistribution.isNotEmpty()) {
                    stickyHeader {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )) {
                            Text(
                                "Muscle Group Distribution",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    item {
                        ElevatedCard (
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row (Modifier.padding(16.dp)) {
                                PieChart(
                                    modifier = Modifier
                                        .height(150.dp)
                                        .wrapContentWidth()
                                        .weight(1f)
                                        .padding(8.dp),
                                    data = state.muscleGroupDistribution
                                )
                                Spacer(Modifier.width(32.dp))
                                Column {
                                    for (data in state.muscleGroupDistribution) {
                                        Row {
                                            Icon(
                                                Icons.Default.Circle,
                                                "A ${data.color} circle.",
                                                tint = data.color
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("${data.label}: ${data.value.toInt()}%")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Top Exercises
                if (state.topExercises.isNotEmpty()) {
                    stickyHeader {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )) {
                            Text(
                                text = "Top Exercises",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            state.topExercises.forEach { exercise ->
                                ExerciseStatItem(
                                    navigator = destinationsNavigator,
                                    exercise = exercise,
                                    useImperial = state.useImperialSystem
                                )
                            }
                        }
                    }
                }

                // Recent Personal Records
                if (state.recentPRs.isNotEmpty()) {
                    stickyHeader {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )) {
                            Text(
                                "Recent Personal Records",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.recentPRs.forEach { pr ->
                                PersonalRecordItem(
                                    pr = pr,
                                    useImperial = state.useImperialSystem,
                                    navigator = destinationsNavigator
                                )
                            }
                        }
                    }
                }

                // Equipment Usage
                if (state.equipmentUsage.isNotEmpty()) {
                    stickyHeader {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )) {
                            Text(
                                "Equipment Usage",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    item {
                        ElevatedCard (
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Row (
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ){
                                DonutChart(
                                    modifier = Modifier
//                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .weight(1f)
                                        .align(Alignment.CenterVertically),
                                    data = state.equipmentUsage.map { it.second },
                                    type = DonutChartType.Normal,
                                    style = DonutChartStyle(sliceType = DonutSliceType.Rounded),
                                )
                                Spacer(Modifier.width(32.dp))
                                Column(Modifier.align(Alignment.CenterVertically)) {
                                    for (nameDataPair in state.equipmentUsage) {
                                        Row {
                                            Icon(
                                                Icons.Default.Circle,
                                                "A ${nameDataPair.second.color} circle.",
                                                tint = nameDataPair.second.color
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "${nameDataPair.first}: ${nameDataPair.second.value.toInt()}",
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeFrameSelector(
    selectedTimeFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit
) {
    MultiChoiceSegmentedButtonRow {
        TimeFrame.entries.toTypedArray().forEachIndexed { index, timeFrame ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeFrame.entries.size),
                onCheckedChange = { onTimeFrameSelected(timeFrame) },
                checked = selectedTimeFrame == timeFrame,
            ) {
                Text(timeFrame.displayName)
            }
        }
    }
}


private fun LazyListScope.OverviewCards(
    totalWorkouts: Int,
    totalVolume: Double,
    avgDuration: Long,
    totalCalories: Double,
    avgCalories: Double,
    currentStreak: Int,
    longestStreak: Int,
    useImperial: Boolean
) {
    stickyHeader {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().background(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )) {
            Text(
                "Highlights",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
    item {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCard(
                title = "Total Workouts",
                value = totalWorkouts.toString(),
                icon = Icons.Default.FitnessCenter,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)

            )
            Spacer(Modifier.width(16.dp))
            MetricCard(
                title = "Total Volume",
                value = "${totalVolume.toInt()} ${if (useImperial) "lbs" else "kg"}",
                icon = Icons.Default.Scale,
                iconColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
    item {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCard(
                title = "Avg Duration",
                value = "${avgDuration / 60}m",
                icon = Icons.Default.Timer,
                iconColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(16.dp))
            MetricCard(
                title = "Calories",
                value = "${totalCalories.toInt()}",
                icon = Icons.Default.LocalFireDepartment,
                iconColor = Color(0xFFFF6B35),
                modifier = Modifier.weight(1f)
            )
        }
    }
    item {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCard(
                title = "Avg Calories",
                value = "${avgCalories.toInt()}",
                icon = Icons.Default.LocalFireDepartment,
                iconColor = Color(0xFFFF6B35),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(16.dp))
            MetricCard(
                title = "Current Streak",
                value = "${currentStreak}d",
                icon = Icons.Default.Whatshot,
                iconColor = Color(0xFFFF9500),
                modifier = Modifier.weight(1f)
            )
        }
    }
    item {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCard(
                title = "Best Streak",
                value = "${longestStreak}d",
                icon = Icons.Default.EmojiEvents,
                iconColor = Color(0xFFFFD700),
                modifier = Modifier.weight(1f)
            )
//            Spacer(Modifier.width(16.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier.height(100.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExerciseStatItem(
    navigator: DestinationsNavigator,
    exercise: ExerciseStats,
    useImperial: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        onClick = {
            navigator.navigate(ExerciseStatsDestination(exercise.exerciseId))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${exercise.timesPerformed} sets • ${exercise.totalVolume.toInt()} ${if (useImperial) "lbs" else "kg"} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${exercise.maxWeight.toInt()} ${if (useImperial) "lbs" else "kg"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PersonalRecordItem(
    pr: PersonalRecord,
    useImperial: Boolean,
    navigator: DestinationsNavigator
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            navigator.navigate(ExerciseStatsDestination(pr.exerciseId))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pr.exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pr.date.format(DateTimeFormatter.ofPattern("MMM dd")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${pr.weight.toInt()} ${if (useImperial) "lbs" else "kg"} × ${pr.reps}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
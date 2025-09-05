package agdesigns.elevatefitness.ui.screens.statistics

import agdesigns.elevatefitness.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import agdesigns.elevatefitness.navigation.BottomNavigationGraph
import agdesigns.elevatefitness.navigation.FadeTransition
import agdesigns.elevatefitness.ui.common.GroupedCard
import agdesigns.elevatefitness.ui.common.MeanLineKey
import agdesigns.elevatefitness.ui.common.PillChart
import agdesigns.elevatefitness.ui.common.rememberHorizontalLine
import agdesigns.elevatefitness.utils.getStickyHeader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jaikeerthick.composable_graphs.composables.donut.DonutChart
import com.jaikeerthick.composable_graphs.composables.donut.style.DonutChartStyle
import com.jaikeerthick.composable_graphs.composables.donut.style.DonutChartType
import com.jaikeerthick.composable_graphs.composables.donut.style.DonutSliceType
import com.jaikeerthick.composable_graphs.composables.pie.PieChart
import com.jaikeerthick.composable_graphs.composables.pie.model.PieData
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ExerciseStatsDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Destination<BottomNavigationGraph>(style = FadeTransition::class)
@Composable
fun Statistics(
    navigator: DestinationsNavigator,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var titleText by remember { mutableStateOf("Highlights") }

    // we want to have headers as top app bar titles, need listState
    val listState = rememberLazyListState()
    // (key, title) -> we start from MAX_VALUE to avoid conflicts with auto assigned keys
    val headers = listOf(
        stringResource(R.string.s_header0_statistics),
        stringResource(R.string.s_header1_highlights),
        stringResource(R.string.s_header2_volume_progress),
        stringResource(R.string.s_header3_workout_frequency),
        stringResource(R.string.s_header4_muscle_group_distribution),
        stringResource(R.string.s_header5_top_exercises),
        stringResource(R.string.s_header6_recent_personal_records),
        stringResource(R.string.s_header7_equipment_usage)
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
                    titleText = it.first ?: titleText
                    lastVisibleKey = it.second
                }
            }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleText
                    )
                },

            )
        }
    ) { innerPadding ->
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time Frame Selector
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimeFrameSelector(
                    selectedTimeFrame = state.selectedTimeFrame,
                    onTimeFrameSelected = { timeFrame ->
                        titleText = headers[0]
                        viewModel.onEvent(StatisticsEvent.OnTimeFrameChanged(timeFrame))
                    }
                )
            }
            if (state.isLoading) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ContainedLoadingIndicator()
                    Text(stringResource(state.progressTextRes))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Overview Cards
                    item(stickyHeaders2Id[headers[1]]) {
                        Text(
                            headers[1],
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
                    overviewCards(
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
                    item(stickyHeaders2Id[headers[2]]) {
                        Text(
                            headers[2],
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
                        ElevatedCard(
//                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            if (selectedValue.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.selected_value_i, selectedValue),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            // TODO: add volume by muscle group
                            PillChart(
                                modelProducer = state.volumeChartProducer,
                                markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
                                    DecimalFormat("#.## ${if (state.useImperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg)}")
                                ),
                                xValueFormatter = CartesianValueFormatter { _, value, _ ->
                                    val index = value.toInt().coerceIn(0, state.volumeIndex2Date.keys.maxOrNull())
                                    state.volumeIndex2Date[index]?.format(
                                        DateTimeFormatter.ofPattern("MMM dd")
                                    ) ?: value.toString() // fall back to value otherwise empty string will crash stuff
                                },
                                decorations = listOf(
                                    rememberHorizontalLine(
                                        MeanLineKey,
                                        stringResource(R.string.average)
                                    )
                                ),
                                modifier = Modifier.padding(8.dp),
                                scrollable = state.selectedTimeFrame == TimeFrame.ALL_TIME
                            )
                        }
                    }

                    // Workout Frequency Chart
                    item(stickyHeaders2Id[headers[3]]) {
                        Text(
                            headers[3],
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
                        ElevatedCard(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            PillChart(
                                state.frequencyChartProducer,
                                baseShape = MaterialTheme.shapes.small,
                                baseColor = MaterialTheme.colorScheme.tertiary,
                                xValueFormatter = CartesianValueFormatter { _, value, _ ->
                                    Instant
                                        .ofEpochMilli(value.toLong())
                                        .atZone(ZoneId.systemDefault()).format(
                                            DateTimeFormatter.ofPattern("MMM yyyy")
                                        )
                                },
                                scrollable = true,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Muscle Group Distribution
                    if (state.muscleGroupDistribution.isNotEmpty()) {
                        item(stickyHeaders2Id[headers[4]]) {
                            Text(
                                headers[4],
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
                            ElevatedCard(
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                val pieData = state.muscleGroupDistribution.map { pair ->
                                    PieData(
                                        label = stringResource(pair.first),
                                        value = pair.second
                                    )
                                }
                                Row(Modifier.padding(16.dp)) {
                                    PieChart(
                                        modifier = Modifier
                                            .height(150.dp)
                                            .wrapContentWidth()
                                            .weight(1f)
                                            .padding(8.dp),
                                        data = pieData
                                    )
                                    Spacer(Modifier.width(32.dp))
                                    Column {
                                        for (data in pieData) {
                                            Row {
                                                Icon(
                                                    Icons.Default.Circle,
                                                    stringResource(
                                                        R.string.circle_icon_colored,
                                                        data.color
                                                    ),
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
                        item(stickyHeaders2Id[headers[5]]) {
                            Text(
                                text = headers[5],
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
                            GroupedCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                onClicks = state.topExercises.map { exercise ->
                                    {
                                        navigator.navigate(ExerciseStatsDestination(exercise.exerciseId))
                                    }
                                },
                                items = state.topExercises.map { exercise ->
                                    {
                                        ExerciseStatItem(
                                            exercise = exercise,
                                            useImperial = state.useImperialSystem
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // Recent Personal Records
                    if (state.recentPRs.isNotEmpty()) {
                        item(stickyHeaders2Id[headers[6]]) {
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
                        item {
                            GroupedCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                items = state.recentPRs.map { pr ->
                                    {
                                        PersonalRecordItem(
                                            pr = pr,
                                            useImperial = state.useImperialSystem
                                        )
                                    }
                                },
                                onClicks = state.recentPRs.map {
                                    {
                                        navigator.navigate(ExerciseStatsDestination(it.exerciseId))
                                    }
                                }
                            )
                        }
                    }

                    // Equipment Usage
                    if (state.equipmentUsage.isNotEmpty()) {
                        item(stickyHeaders2Id[headers[7]]) {
                            Text(
                                headers[7],
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
                            ElevatedCard(
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
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
                                                    stringResource(R.string.circle_icon_colored, nameDataPair.second.color),
                                                    tint = nameDataPair.second.color
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "${stringResource(nameDataPair.first)}: ${nameDataPair.second.value.toInt()}",
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimeFrameSelector(
    selectedTimeFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit
) {
    // TODO: this feels like will be getting a wrapper in compose eventually...
    Row(
        Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        TimeFrame.entries.forEachIndexed { index, timeFrame ->
            val modifier = if (timeFrame == selectedTimeFrame)
                Modifier.weight(1f + ButtonGroupDefaults.ExpandedRatio) // expanded
            else Modifier.weight(1f)

            ToggleButton(
                checked = timeFrame == selectedTimeFrame,
                onCheckedChange = { onTimeFrameSelected(timeFrame) },
                modifier = modifier,
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        TimeFrame.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                Text(stringResource(timeFrame.displayResource), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun LazyListScope.overviewCards(
    totalWorkouts: Int,
    totalVolume: Double,
    avgDuration: Long,
    totalCalories: Double,
    avgCalories: Double,
    currentStreak: Int,
    longestStreak: Int,
    useImperial: Boolean
) {
    item {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCard(
                title = stringResource(R.string.total_workouts),
                value = totalWorkouts.toString(),
                icon = Icons.Default.FitnessCenter,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)

            )
            Spacer(Modifier.width(8.dp))
            MetricCard(
                title = stringResource(R.string.total_volume),
                value = "${totalVolume.toInt()} ${if (useImperial) stringResource(R.string.lb) else stringResource(R.string.kg)}",
                icon = Icons.Default.Scale,
                iconColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
    item {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCard(
                title = stringResource(R.string.avg_duration),
                value = "${avgDuration / 60}m",
                icon = Icons.Default.Timer,
                iconColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            MetricCard(
                title = stringResource(R.string.calories),
                value = "${totalCalories.toInt()} kcal",
                icon = Icons.Default.LocalFireDepartment,
                iconColor = Color(0xFFFF6B35),
                modifier = Modifier.weight(1f)
            )
        }
    }
    item {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCard(
                title = stringResource(R.string.avg_calories),
                value = "${avgCalories.toInt()}",
                icon = Icons.Default.LocalFireDepartment,
                iconColor = Color(0xFFFF6B35),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            MetricCard(
                title = stringResource(R.string.current_streak),
                value = stringResource(R.string.streak_in_days, currentStreak),
                icon = Icons.Default.Whatshot,
                iconColor = Color(0xFFFF9500),
                modifier = Modifier.weight(1f)
            )
        }
    }
    item {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCard(
                title = stringResource(R.string.best_streak),
                value = stringResource(R.string.streak_in_days, longestStreak),
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
    exercise: ExerciseStats,
    useImperial: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
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
                text = stringResource(
                    R.string.sets_total,
                    exercise.timesPerformed,
                    exercise.totalVolume.toInt(),
                    if (useImperial) stringResource(R.string.lb) else stringResource(R.string.kg)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${exercise.maxWeight.toInt()} ${if (useImperial) stringResource(R.string.lb) else stringResource(R.string.kg)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PersonalRecordItem(
    pr: PersonalRecord,
    useImperial: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
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
            text = "${pr.weight.toInt()} ${if (useImperial) stringResource(R.string.lb) else stringResource(R.string.kg)} × ${pr.reps}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
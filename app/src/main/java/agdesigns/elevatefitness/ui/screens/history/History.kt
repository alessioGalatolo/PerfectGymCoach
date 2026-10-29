package agdesigns.elevatefitness.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.getPlanDisplayName
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.ui.navigation.AddProgramDestination
import agdesigns.elevatefitness.ui.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.navigation.HistoryDestination
import agdesigns.elevatefitness.ui.navigation.WorkoutRecapDestination
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.screens.history.components.CompactHeatmapOverlap
import agdesigns.elevatefitness.ui.screens.history.components.CompactWorkoutHeatmap
import agdesigns.elevatefitness.ui.screens.history.components.WorkoutHeatmap
import agdesigns.elevatefitness.shared.maybeKgToLb
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharedFlow
import java.time.LocalDate
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// items of the history list placed before the workouts (i.e., the heatmap)
private const val HEADER_ITEMS = 1

@Composable
fun History(
    navigator: DestinationsNavigator,
    refreshContentRequest: SharedFlow<Any>,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // keep the target below the compact heatmap, which is always shown after jumping
    val overlapPx = with(LocalDensity.current) { CompactHeatmapOverlap.roundToPx() }
    val scrollToWorkout: (Int) -> Unit = { index ->
        if (index != -1) scope.launch {
            // TODO: it would be nice to *animate* the scroll but it always results in an off
            //  scroll due to a bug?
            listState.scrollToItem(index + HEADER_ITEMS, scrollOffset = -overlapPx)
        }
    }
    val onWeekOpen: (LocalDate) -> Unit = { week ->
        scrollToWorkout(state.mainList.indexOfFirst { it.weekStart == week })
    }
    val showCompactHeatmap by remember { derivedStateOf { listState.firstVisibleItemIndex >= HEADER_ITEMS } }
    // week of the first workout not hidden by the compact heatmap
    val currentWeek by remember(state.mainList) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index >= HEADER_ITEMS && it.offset + it.size > overlapPx }
                ?.let { state.mainList.getOrNull(it.index - HEADER_ITEMS)?.weekStart }
        }
    }
    LaunchedEffect(refreshContentRequest) {
        refreshContentRequest.collect {
            if (it == HistoryDestination) {
                // this refresh is for us
                listState.animateScrollToItem(0)
            }
        }
    }
    if (state.mainList.isEmpty()) {
        EmptyScreenInfo(
            icon = Icons.Default.History,
            iconDescriptionRes = R.string.empty_history_icon,
            titleRes = R.string.empty_history,
            subtitleRes = R.string.start_your_fitness_journey_today
        )
    } else {
        Scaffold { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = innerPadding,
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        WorkoutHeatmap(
                            heatmap = state.heatmap,
                            maxWeeklyWorkouts = state.maxWeeklyWorkouts,
                            weekWorkouts = state.weekWorkouts,
                            onWeekOpen = onWeekOpen,
                            onWorkoutOpen = { workoutId ->
                                scrollToWorkout(state.mainList.indexOfFirst { it.workout.workoutId == workoutId })
                            }
                        )
                    }
                    items(
                        items = state.mainList,
                        key = { "workout_${it.workout.workoutId}" }
                    ) { listItem ->
                        listItem.yearHeader?.let { year ->
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
                                        contentDescription = stringResource(R.string.year_icon),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = year.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        listItem.monthHeader?.let { summary ->
                            MonthHeader(summary, state.useImperialSystem)
                        }
                        OutlinedCard(
                            shape = MaterialTheme.shapes.large,
                            onClick = {
                                navigator.navigate(
                                    WorkoutRecapDestination(workoutId = listItem.workout.workoutId)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
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
                                        text = getProgramDisplayName(listItem.workout.name),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )

                                    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, HH:mm")
                                    val date = listItem.workout.startDate?.format(formatter) ?: ""
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
                                        label = stringResource(R.string.volume),
                                        value = "${
                                            maybeKgToLb(
                                                listItem.workout.volume,
                                                state.useImperialSystem
                                            ).roundToInt()
                                        } ${
                                            if (state.useImperialSystem) stringResource(R.string.lb) else stringResource(
                                                R.string.kg
                                            )
                                        }",
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Sets stat
                                    StatCard(
                                        icon = Icons.Default.Repeat,
                                        label = stringResource(R.string.sets),
                                        value = listItem.workout.totalSets.toString(),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Calories stat
                                    StatCard(
                                        icon = Icons.Default.LocalFireDepartment,
                                        label = stringResource(R.string.calories),
                                        value = stringResource(
                                            R.string.calories_kcal,
                                            listItem.workout.calories.toInt()
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Duration stat
                                    StatCard(
                                        icon = Icons.Default.Schedule,
                                        label = stringResource(R.string.duration),
                                        value = stringResource(
                                            R.string.i_minutes,
                                            (listItem.workout.durationSeconds / 60).toInt()
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        listItem.planChange?.let { planChange ->
                            Spacer(modifier = Modifier.height(8.dp))
                            PlanChangeMarker(planChange) { planId ->
                                navigator.navigate(AddProgramDestination(planId))
                            }
                        }
                        listItem.breakDays?.let { days ->
                            Spacer(modifier = Modifier.height(8.dp))
                            BreakMarker(days)
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
                AnimatedVisibility(
                    visible = showCompactHeatmap,
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    CompactWorkoutHeatmap(
                        heatmap = state.heatmap,
                        maxWeeklyWorkouts = state.maxWeeklyWorkouts,
                        currentWeek = currentWeek,
                        onWeekOpen = onWeekOpen,
                        onTitleClick = { scope.launch { listState.animateScrollToItem(0) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(summary: MonthSummary, useImperialSystem: Boolean) {
    val monthName = remember(summary.month) {
        summary.month.format(DateTimeFormatter.ofPattern("LLLL"))
            .replaceFirstChar { it.titlecase() }
    }
    val volume = NumberFormat.getIntegerInstance()
        .format(maybeKgToLb(summary.volume, useImperialSystem).roundToInt())
    val unit = stringResource(if (useImperialSystem) R.string.lb else R.string.kg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = monthName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(
                R.string.month_summary,
                pluralStringResource(R.plurals.workout_count, summary.workouts, summary.workouts),
                "$volume $unit"
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// shown below the first workout after a long period without workouts
@Composable
private fun BreakMarker(days: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Surface(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pluralStringResource(R.plurals.days_off, days.toInt(), days.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

// shown below the first workout of the new plan, i.e., between the two plans' workouts
@Composable
private fun PlanChangeMarker(planChange: PlanChange, onPlanClick: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Surface(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(max = 280.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                PlanChangeRow(
                    icon = Icons.Default.ArrowUpward,
                    text = stringResource(
                        R.string.plan_change_new,
                        getPlanDisplayName(planChange.toPlanName)
                    ),
                    onClick = { onPlanClick(planChange.toPlanId) }
                )
                PlanChangeRow(
                    icon = Icons.Default.ArrowDownward,
                    text = stringResource(
                        R.string.plan_change_old,
                        getPlanDisplayName(planChange.fromPlanName)
                    ),
                    onClick = { onPlanClick(planChange.fromPlanId) }
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun PlanChangeRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        shape = MaterialTheme.shapes.extraLarge
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
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
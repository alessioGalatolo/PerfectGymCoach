package agdesigns.elevatefitness.ui.screens.history.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.ui.screens.history.HistoryScreenCalendarItem
import agdesigns.elevatefitness.ui.screens.history.HistoryScreenListItem
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutCalendarCards(
    calendarList: List<HistoryScreenCalendarItem>,
    mainList: List<HistoryScreenListItem>,
    listState: LazyListState
) {
    if (calendarList.isNotEmpty()) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current

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
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = stringResource(R.string.calendar),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.weekly_overview),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                reverseLayout = true,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(calendarList) {
                    if (it.showYearHeader) {
                        Card(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .width(120.dp)
                                .height(140.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent
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
                                    text = it.year.toString(),
                                    style = MaterialTheme.typography.headlineMediumEmphasized,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (it.workouts == 0) {
                        NoWorkoutWeekCard(it.week)
                    } else {
                        WorkoutWeekCard(it.week, it.workouts) {
                            scope.launch {
                                // Find the first item index for this week in the main list
                                val targetIndex = mainList.indexOfFirst { item ->
                                    item.year == it.year && item.week == it.week
                                }
                                if (targetIndex != -1) {
                                    // TODO: it would be nice to *anitmate* scroll to item but it always
                                    //  results in off scroll due to a bug?
                                    listState.scrollToItem(
                                        targetIndex + 1,
                                        scrollOffset = with(density) {
                                            // FIXME: do not hardcode
                                            -40.dp.roundToPx()  // offset given by week header
                                        }
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

@Composable
fun NoWorkoutWeekCard(
    week: Int
) {
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
                text = stringResource(R.string.week_i, week),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.no_workouts),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.rest_week),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun WorkoutWeekCard(
    week: Int,
    workouts: Int,
    onClick: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(140.dp),
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
                text = stringResource(R.string.week_i, week),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )

            // Achievement icon
            val (icon, iconColor) = when {
                workouts >= 5 -> Icons.Default.Whatshot to MaterialTheme.colorScheme.onPrimaryContainer
                workouts >= 3 -> Icons.Default.RocketLaunch to MaterialTheme.colorScheme.onPrimaryContainer
                workouts >= 2 -> Icons.Default.SelfImprovement to MaterialTheme.colorScheme.onPrimaryContainer
                else -> Icons.Default.SentimentVerySatisfied to MaterialTheme.colorScheme.onPrimaryContainer
            }

            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.achievement_icon),
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )

            // Workout indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(minOf(workouts, 7)) { index ->
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = stringResource(
                            R.string.workout_i_icon,
                            index + 1
                        ),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    if (index < minOf(workouts, 7) - 1) {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
            }

            // Workout count
            Text(
                text = pluralStringResource(
                    R.plurals.workout_count,
                    workouts,
                    workouts
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
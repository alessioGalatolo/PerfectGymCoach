package agdesigns.elevatefitness.ui.screens.history.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.WorkoutRecordAndName
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.ranges.downTo


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
                        } else {
                            // Active week card with enhanced styling
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(140.dp)
                                    .clickable {
                                        scope.launch {
                                            listState.animateScrollToItem(
                                                index = recordsMap.toSortedMap()
                                                    .tailMap(week).keys.size
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
                                        text = stringResource(R.string.week_i, week),
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
                                        contentDescription = stringResource(R.string.achievement_icon),
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
                                                contentDescription = stringResource(
                                                    R.string.workout_i_icon,
                                                    index + 1
                                                ),
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
                                        text = stringResource(
                                            R.string.workout_count,
                                            weekRecords.size,
                                            if (weekRecords.size != 1) "s" else ""
                                        ),
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

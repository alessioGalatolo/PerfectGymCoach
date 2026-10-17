package agdesigns.elevatefitness.ui.screens.history.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.ui.screens.history.HeatmapWorkout
import agdesigns.elevatefitness.ui.screens.history.HeatmapWeek
import agdesigns.elevatefitness.ui.screens.history.HeatmapYear
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.ceil
import androidx.compose.ui.platform.LocalLocale

private const val HEATMAP_LEVELS = 4
private const val MAX_WEEKS_PER_MONTH = 5

/**
 * GitHub-style contribution grid aggregated by week, one year per pager page. Each year is a block
 * of 12 columns (months), each holding the weeks that start in that month. Darker cells mean more workouts that week.
 * Tapping a week expands its workouts below the grid; tapping it again calls [onWeekOpen].
 */
@Composable
fun WorkoutHeatmap(
    heatmap: List<HeatmapYear>,
    maxWeeklyWorkouts: Int,
    weekWorkouts: Map<LocalDate, List<HeatmapWorkout>>,
    onWeekOpen: (LocalDate) -> Unit,
    onWorkoutOpen: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (heatmap.isEmpty()) return
    var selectedWeek by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    // page 0 is the current year; reversed layout so older years are to the left
    val pagerState = rememberPagerState { heatmap.size }
    val scope = rememberCoroutineScope()
    val currentYear = heatmap.getOrNull(pagerState.currentPage) ?: heatmap.first()
    val selected = selectedWeek?.let { week ->
        currentYear.months.firstNotNullOfOrNull { month -> month.firstOrNull { it.weekStart == week } }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.workout_activity),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.shapes.large
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    enabled = pagerState.currentPage < heatmap.lastIndex
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_year)
                    )
                }
                Text(
                    text = currentYear.year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_year)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                reverseLayout = true,
                pageSpacing = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                HeatmapYearBlock(
                    year = heatmap[page],
                    maxWeeklyWorkouts = maxWeeklyWorkouts,
                    selectedWeek = selectedWeek,
                    onWeekClick = { week ->
                        when {
                            selectedWeek != week.weekStart -> selectedWeek = week.weekStart
                            week.workouts > 0 -> onWeekOpen(week.weekStart)
                            // nothing to show in the list for a rest week, just collapse it
                            else -> selectedWeek = null
                        }
                    }
                )
            }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selected != null && selected.workouts > 0) {
                        Icon(
                            imageVector = achievementIcon(selected.workouts),
                            contentDescription = stringResource(R.string.achievement_icon),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = selected?.let {
                            stringResource(
                                R.string.week_of_date,
                                it.weekStart.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                            ) + " · " + pluralStringResource(R.plurals.workout_count, it.workouts, it.workouts)
                        } ?: stringResource(R.string.tap_a_week_for_details),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    HeatmapLegend()
                    if (selected != null) {
                        IconButton(onClick = { selectedWeek = null }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.hide_week_details)
                            )
                        }
                    }
                }
                AnimatedContent(
                    targetState = selected,
                    contentKey = { it?.weekStart },
                    transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = true) },
                    label = "weekDetails"
                ) { week ->
                    if (week != null)
                        WeekDetails(weekWorkouts[week.weekStart].orEmpty(), onWorkoutOpen)
                    else
                        Spacer(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun HeatmapYearBlock(
    year: HeatmapYear,
    maxWeeklyWorkouts: Int,
    selectedWeek: LocalDate?,
    onWeekClick: (HeatmapWeek) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        year.months.forEachIndexed { monthIndex, weeks ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Month.of(monthIndex + 1)
                        .getDisplayName(TextStyle.NARROW_STANDALONE, LocalLocale.current.platformLocale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                weeks.forEach { week ->
                    HeatmapCell(
                        week = week,
                        maxWeeklyWorkouts = maxWeeklyWorkouts,
                        selected = week.weekStart == selectedWeek,
                        onClick = { onWeekClick(week) }
                    )
                }
                // keep all months the same height
                repeat(MAX_WEEKS_PER_MONTH - weeks.size) {
                    Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    week: HeatmapWeek,
    maxWeeklyWorkouts: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val color = heatmapColor(heatmapLevel(week.workouts, maxWeeklyWorkouts))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(if (week.isFuture) color.copy(alpha = 0.35f) else color, shape)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape)
                else Modifier
            )
            .then(if (week.isFuture) Modifier else Modifier.clickable(onClick = onClick))
    )
}

@Composable
private fun WeekDetails(workouts: List<HeatmapWorkout>, onWorkoutClick: (Long) -> Unit) {
    val locale = LocalLocale.current.platformLocale
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE d", locale) }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (workouts.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.rest_week),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }
        workouts.forEach { workout ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWorkoutClick(workout.workoutId) }
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getProgramDisplayName(workout.name),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = workout.date.format(dayFormatter) + " · " + workout.date.format(timeFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.i_minutes, (workout.durationSeconds / 60).toInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.tap_week_again_to_show_in_history),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
        )
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = stringResource(R.string.heatmap_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        for (level in 0..HEATMAP_LEVELS) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(heatmapColor(level), RoundedCornerShape(2.dp))
            )
        }
        Text(
            text = stringResource(R.string.heatmap_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal fun heatmapLevel(workouts: Int, maxWeeklyWorkouts: Int): Int =
    if (workouts <= 0 || maxWeeklyWorkouts <= 0) 0
    else ceil(HEATMAP_LEVELS * workouts / maxWeeklyWorkouts.toFloat()).toInt()
        .coerceIn(1, HEATMAP_LEVELS)

@Composable
internal fun heatmapColor(level: Int): Color =
    if (level == 0) MaterialTheme.colorScheme.surfaceContainerHighest
    else lerp(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.primary,
        (level - 1) / (HEATMAP_LEVELS - 1).toFloat()
    )

private fun achievementIcon(workouts: Int): ImageVector = when {
    workouts >= 5 -> Icons.Default.Whatshot
    workouts >= 3 -> Icons.Default.RocketLaunch
    workouts >= 2 -> Icons.Default.SelfImprovement
    else -> Icons.Default.SentimentVerySatisfied
}

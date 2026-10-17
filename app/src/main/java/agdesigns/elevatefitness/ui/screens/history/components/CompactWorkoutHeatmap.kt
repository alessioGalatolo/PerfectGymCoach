package agdesigns.elevatefitness.ui.screens.history.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.ui.screens.history.HeatmapWeek
import agdesigns.elevatefitness.ui.screens.history.HeatmapYear
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

private val CompactHeatmapTopPadding = 8.dp
private val CompactHeatmapHeight = 56.dp

// how much of the top of the list is covered by the compact heatmap (plus a small gap)
val CompactHeatmapOverlap: Dp = CompactHeatmapTopPadding + CompactHeatmapHeight + 8.dp

/**
 * Slim version of [WorkoutHeatmap], meant to be pinned on top of the history list once the full
 * heatmap is scrolled away. Shows the weeks of the month containing [currentWeek]; tapping a week
 * calls [onWeekOpen], the arrows open the closest older/newer month with workouts.
 */
@Composable
fun CompactWorkoutHeatmap(
    heatmap: List<HeatmapYear>,
    maxWeeklyWorkouts: Int,
    currentWeek: LocalDate?,
    onWeekOpen: (LocalDate) -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // all the months of the heatmap, oldest first
    val months = remember(heatmap) {
        heatmap.reversed().flatMap { year ->
            year.months.mapIndexed { index, weeks -> YearMonth.of(year.year, index + 1) to weeks }
        }
    }
    val shownMonth = YearMonth.from(currentWeek ?: LocalDate.now())
    val monthIndex = months.indexOfFirst { it.first == shownMonth }
    if (monthIndex == -1) return

    fun latestWorkoutWeek(indices: IntProgression): LocalDate? = indices
        .firstNotNullOfOrNull { i -> months[i].second.lastOrNull { it.workouts > 0 } }
        ?.weekStart
    val previousWeek = latestWorkoutWeek(monthIndex - 1 downTo 0)
    val nextWeek = latestWorkoutWeek(monthIndex + 1..months.lastIndex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = CompactHeatmapTopPadding)
            .height(CompactHeatmapHeight),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { previousWeek?.let(onWeekOpen) },
                enabled = previousWeek != null
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_month)
                )
            }
            Text(
                text = shownMonth.month.getDisplayName(
                    TextStyle.SHORT_STANDALONE,
                    LocalLocale.current.platformLocale
                ) + " " + shownMonth.year,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(
                        onClickLabel = stringResource(R.string.show_workout_activity),
                        onClick = onTitleClick
                    )
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                months[monthIndex].second.forEach { week ->
                    CompactHeatmapCell(
                        week = week,
                        maxWeeklyWorkouts = maxWeeklyWorkouts,
                        current = week.weekStart == currentWeek,
                        onClick = { onWeekOpen(week.weekStart) }
                    )
                }
            }
            IconButton(
                onClick = { nextWeek?.let(onWeekOpen) },
                enabled = nextWeek != null
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month)
                )
            }
        }
    }
}

@Composable
private fun CompactHeatmapCell(
    week: HeatmapWeek,
    maxWeeklyWorkouts: Int,
    current: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    val color = heatmapColor(heatmapLevel(week.workouts, maxWeeklyWorkouts))
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(shape)
            .background(if (week.isFuture) color.copy(alpha = 0.35f) else color)
            .then(
                if (current) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape)
                else Modifier
            )
            // rest weeks have nothing to show in the list
            .then(if (week.workouts > 0) Modifier.clickable(onClick = onClick) else Modifier)
    )
}

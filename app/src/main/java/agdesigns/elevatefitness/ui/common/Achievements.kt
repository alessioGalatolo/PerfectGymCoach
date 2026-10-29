package agdesigns.elevatefitness.ui.common

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.AchievementType
import agdesigns.elevatefitness.data.db.entity.WorkoutAchievementAndInfo
import agdesigns.elevatefitness.shared.maybeKgToLb
import android.text.format.DateUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// delay between the appearance of consecutive achievements in the recap
private const val ACHIEVEMENT_STAGGER_MILLIS = 350L
// time for the trophy to land before the first achievement appears
private const val HEADER_LANDING_MILLIS = 500L
private const val COUNT_UP_MILLIS = 900
private const val TROPHY_TURN_MILLIS = 12_000
private const val BADGE_SHINE_MILLIS = 900
private const val CONFETTI_PARTICLES = 64
private const val CONFETTI_SECONDS = 1.8f
private const val CONFETTI_GRAVITY = 600f  // dp/s²
private const val CONFETTI_DRAG = 2.5f  // 1/s
private val HEADER_PADDING = 12.dp
private val TROPHY_SIZE = 48.dp
private val ROW_SLIDE_DISTANCE = 24.dp

@Composable
private fun formatWeight(kg: Double, useImperialSystem: Boolean, fractionDigits: Int = 1): String {
    val value = NumberFormat.getNumberInstance().apply { maximumFractionDigits = fractionDigits }
        .format(maybeKgToLb(kg, useImperialSystem))
    return "$value ${stringResource(if (useImperialSystem) R.string.lb else R.string.kg)}"
}

@Composable
private fun repsText(reps: Int, weightKg: Double?, useImperialSystem: Boolean): String =
    if (weightKg == null || weightKg <= 0.0)
        pluralStringResource(R.plurals.achievement_reps, reps, reps)
    else
        pluralStringResource(
            R.plurals.achievement_reps_with_weight, reps, reps, formatWeight(weightKg, useImperialSystem)
        )

/** What was achieved, e.g., "100 kg × 5" for a weight record. */
@Composable
fun achievementValueText(achievement: WorkoutAchievementAndInfo, useImperialSystem: Boolean): String {
    val value = achievement.value
    return when (achievement.type) {
        AchievementType.HEAVIEST_WEIGHT -> achievement.secondaryValue?.let {
            stringResource(R.string.achievement_weight_reps, formatWeight(value, useImperialSystem), it.toInt())
        } ?: formatWeight(value, useImperialSystem)
        AchievementType.ESTIMATED_ONE_REP_MAX -> formatWeight(value, useImperialSystem)
        // volumes are shown without decimals, like in the rest of the app
        AchievementType.EXERCISE_VOLUME,
        AchievementType.WORKOUT_VOLUME,
        AchievementType.PROGRAM_VOLUME -> formatWeight(value, useImperialSystem, fractionDigits = 0)
        AchievementType.REP_RECORD -> repsText(value.toInt(), achievement.secondaryValue, useImperialSystem)
        AchievementType.LONGEST_DURATION -> DateUtils.formatElapsedTime(value.toLong())
        AchievementType.WORKOUT_COUNT ->
            pluralStringResource(R.plurals.achievement_workouts_completed, value.toInt(), value.toInt())
        AchievementType.LIFETIME_VOLUME ->
            stringResource(R.string.achievement_lifted_in_total, formatWeight(value, useImperialSystem))
        AchievementType.LONGEST_STREAK ->
            pluralStringResource(R.plurals.achievement_weeks_in_a_row, value.toInt(), value.toInt())
    }
}

/** The previous best, null if there is nothing meaningful to show. */
@Composable
fun achievementPreviousText(achievement: WorkoutAchievementAndInfo, useImperialSystem: Boolean): String? {
    val previous = achievement.previousValue?.takeIf { it > 0.0 } ?: return null
    return when (achievement.type) {
        AchievementType.HEAVIEST_WEIGHT,
        AchievementType.ESTIMATED_ONE_REP_MAX -> formatWeight(previous, useImperialSystem)
        AchievementType.EXERCISE_VOLUME,
        AchievementType.WORKOUT_VOLUME,
        AchievementType.PROGRAM_VOLUME -> formatWeight(previous, useImperialSystem, fractionDigits = 0)
        AchievementType.REP_RECORD -> pluralStringResource(R.plurals.achievement_reps, previous.toInt(), previous.toInt())
        AchievementType.LONGEST_DURATION -> DateUtils.formatElapsedTime(previous.toLong())
        AchievementType.LONGEST_STREAK ->
            pluralStringResource(R.plurals.achievement_weeks_in_a_row, previous.toInt(), previous.toInt())
        AchievementType.WORKOUT_COUNT, AchievementType.LIFETIME_VOLUME -> null
    }
}

/**
 * Card listing the achievements unlocked by a workout, shown in the recap.
 *
 * Unless [animate] is false, it celebrates them: confetti from the trophy, then each achievement
 * appears in turn with its value counting up from the previous best. [onAnimationStarted] is called
 * when the celebration starts, so that the caller can avoid replaying it (e.g., after scrolling back).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AchievementsCard(
    achievements: List<WorkoutAchievementAndInfo>,
    useImperialSystem: Boolean,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    onAnimationStarted: () -> Unit = {}
) {
    // decided once: the caller flips [animate] as soon as the animation starts
    val play = remember { animate }
    val haptics = LocalHapticFeedback.current
    var revealed by remember { mutableIntStateOf(if (play) 0 else achievements.size) }
    // once confetti and the trophy are done, rows can start
    var headerLanded by remember { mutableStateOf(!play) }
    LaunchedEffect(Unit) {
        if (!play) return@LaunchedEffect
        onAnimationStarted()
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        delay(HEADER_LANDING_MILLIS)
        headerLanded = true
        while (revealed < achievements.size) {
            revealed++
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            delay(ACHIEVEMENT_STAGGER_MILLIS)
        }
    }
    LaunchedEffect(achievements.size) {
        // achievements may be added after the animation (e.g., the flow emits again)
        if (headerLanded) revealed = achievements.size
    }

    Box(modifier = modifier) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(HEADER_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrophyBadge(play)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.achievements),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // counts up as achievements are revealed
                    AnimatedContent(
                        targetState = revealed,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()) togetherWith
                                (slideOutVertically { -it } + fadeOut())
                        },
                        label = "achievements count"
                    ) { count ->
                        Text(
                            pluralStringResource(R.plurals.achievements_unlocked, count, count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            achievements.forEachIndexed { index, achievement ->
                AnimatedVisibility(
                    visible = index < revealed,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                ) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        AchievementRow(achievement, useImperialSystem, play)
                    }
                }
            }
        }
        if (play)
            ConfettiBurst(
                // from the center of the trophy
                origin = DpOffset(HEADER_PADDING + TROPHY_SIZE / 2, HEADER_PADDING + TROPHY_SIZE / 2),
                modifier = Modifier.matchParentSize()
            )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrophyBadge(play: Boolean) {
    val scale = remember { Animatable(if (play) 0f else 1f) }
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!play) return@LaunchedEffect
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
        wiggle.animateTo(0f, keyframes {
            durationMillis = 600
            -18f at 100
            14f at 220
            -9f at 340
            5f at 460
        })
    }
    // the cookie keeps slowly turning behind the trophy
    val rotation by rememberInfiniteTransition(label = "trophy").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(TROPHY_TURN_MILLIS, easing = LinearEasing)),
        label = "trophy rotation"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TROPHY_SIZE)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation }
                .background(MaterialTheme.colorScheme.tertiary, MaterialShapes.Cookie9Sided.toShape())
        )
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer { rotationZ = wiggle.value }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AchievementRow(
    achievement: WorkoutAchievementAndInfo,
    useImperialSystem: Boolean,
    play: Boolean
) {
    val isRecord = achievement.type.isPersonalRecord
    val slide = remember { Animatable(if (play) 0f else 1f) }
    val morph = remember { Animatable(if (play) 0f else 1f) }
    // fraction of the way from the previous best (or 0) to the new value
    val count = remember { Animatable(if (play) 0f else 1f) }
    var showDelta by remember { mutableStateOf(!play) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        if (!play) return@LaunchedEffect
        launch { slide.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) }
        launch { morph.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow)) }
        count.animateTo(1f, tween(COUNT_UP_MILLIS, easing = FastOutSlowInEasing))
        showDelta = true
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    val shapeMorph = remember(isRecord) {
        Morph(MaterialShapes.Circle, if (isRecord) MaterialShapes.Sunny else MaterialShapes.Cookie6Sided)
    }
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = slide.value.coerceIn(0f, 1f)
                translationY = with(density) { ROW_SLIDE_DISTANCE.toPx() } * (1f - slide.value)
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    // spins into place while morphing from a circle
                    rotationZ = -120f * (1f - morph.value)
                    val scale = 0.6f + 0.4f * morph.value
                    scaleX = scale
                    scaleY = scale
                }
                .background(
                    if (isRecord) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                    MorphShape(shapeMorph, morph.value)
                )
        ) {
            Icon(
                achievement.type.icon,
                contentDescription = null,
                tint = if (isRecord) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .size(20.dp)
                    // keep the icon upright
                    .graphicsLayer { rotationZ = 120f * (1f - morph.value) }
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.name?.let { stringResource(achievement.type.titleResource) + " • " + it }
                    ?: stringResource(achievement.type.titleResource),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val start = achievement.previousValue?.takeIf { it > 0.0 } ?: 0.0
                val shown = if (count.value >= 1f) achievement
                    else achievement.copy(value = start + (achievement.value - start) * count.value)
                Text(
                    achievementValueText(shown, useImperialSystem),
                    // tabular digits, the text should not jump around while counting
                    style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                achievementDeltaText(achievement, useImperialSystem)?.let { delta ->
                    AnimatedVisibility(
                        visible = showDelta,
                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioHighBouncy)) + fadeIn()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                delta,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            achievementPreviousText(achievement, useImperialSystem)?.let {
                AnimatedVisibility(visible = showDelta, enter = fadeIn() + expandVertically()) {
                    Text(
                        stringResource(R.string.achievement_previous, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/** Improvement over the previous best, e.g., "+5 kg", null if there is none to show. */
@Composable
private fun achievementDeltaText(achievement: WorkoutAchievementAndInfo, useImperialSystem: Boolean): String? {
    val previous = achievement.previousValue?.takeIf { it > 0.0 } ?: return null
    val delta = achievement.value - previous
    if (delta <= 0.0) return null
    return "+" + when (achievement.type) {
        AchievementType.HEAVIEST_WEIGHT,
        AchievementType.ESTIMATED_ONE_REP_MAX -> formatWeight(delta, useImperialSystem)
        AchievementType.EXERCISE_VOLUME,
        AchievementType.WORKOUT_VOLUME,
        AchievementType.PROGRAM_VOLUME -> formatWeight(delta, useImperialSystem, fractionDigits = 0)
        AchievementType.REP_RECORD -> pluralStringResource(R.plurals.achievement_reps, delta.toInt(), delta.toInt())
        AchievementType.LONGEST_DURATION -> "${delta.toInt()} ${stringResource(R.string.sec)}"
        AchievementType.LONGEST_STREAK ->
            pluralStringResource(R.plurals.achievement_weeks_in_a_row, delta.toInt(), delta.toInt())
        AchievementType.WORKOUT_COUNT, AchievementType.LIFETIME_VOLUME -> return null
    }
}

/** Shape of [morph] at [progress]; the polygons are expected to be normalized to a unit square. */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        morph.asCubics(progress).forEachIndexed { i, cubic ->
            if (i == 0) path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            path.cubicTo(
                cubic.control0X, cubic.control0Y,
                cubic.control1X, cubic.control1Y,
                cubic.anchor1X, cubic.anchor1Y
            )
        }
        path.close()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

private class ConfettiParticle(
    val angle: Float,  // radians, direction of the initial velocity
    val speed: Float,  // dp per second
    val size: Float,  // dp
    val spin: Float,  // degrees per second
    val isCircle: Boolean,
    val color: Color
)

/** One burst of confetti from [origin], falling down with gravity and fading out. */
@Composable
private fun ConfettiBurst(origin: DpOffset, modifier: Modifier = Modifier) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
    )
    val particles = remember {
        List(CONFETTI_PARTICLES) {
            ConfettiParticle(
                // mostly upwards and to the right, away from the card's edge
                angle = Math.toRadians(Random.nextDouble(-160.0, 10.0)).toFloat(),
                speed = Random.nextFloat() * 400f + 200f,
                size = Random.nextFloat() * 5f + 4f,
                spin = Random.nextFloat() * 720f - 360f,
                isCircle = Random.nextBoolean(),
                color = colors.random()
            )
        }
    }
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        time.animateTo(CONFETTI_SECONDS, tween((CONFETTI_SECONDS * 1000).toInt(), easing = LinearEasing))
    }
    if (time.value >= CONFETTI_SECONDS) return
    Canvas(modifier) {
        val t = time.value
        val alpha = (1f - (t / CONFETTI_SECONDS).pow(2)).coerceIn(0f, 1f)
        val ox = origin.x.toPx()
        val oy = origin.y.toPx()
        particles.forEach { p ->
            // drag slows the burst down, gravity pulls it down
            val travelled = p.speed * (1f - exp(-CONFETTI_DRAG * t)) / CONFETTI_DRAG
            val x = ox + cos(p.angle) * travelled.dp.toPx()
            val y = oy + sin(p.angle) * travelled.dp.toPx() + (0.5f * CONFETTI_GRAVITY * t * t).dp.toPx()
            val size = p.size.dp.toPx()
            rotate(p.spin * t, pivot = Offset(x, y)) {
                if (p.isCircle)
                    drawCircle(p.color, radius = size / 2, center = Offset(x, y), alpha = alpha)
                else
                    drawRect(
                        p.color,
                        topLeft = Offset(x - size / 2, y - size / 4),
                        size = Size(size, size / 2),
                        alpha = alpha
                    )
            }
        }
    }
}

/** Small badges with the records of an exercise, shown in its recap card. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseAchievementBadges(
    achievements: List<WorkoutAchievementAndInfo>,
    modifier: Modifier = Modifier
) {
    if (achievements.isEmpty()) return
    // pop in, then a shine sweeps across them
    val scale = remember { Animatable(0.4f) }
    val shine = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        shine.animateTo(1f, tween(BADGE_SHINE_MILLIS, easing = FastOutSlowInEasing))
    }
    val shineColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    FlowRow(
        modifier = modifier
            .padding(bottom = 8.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        achievements.forEach { achievement ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .drawWithContent {
                            drawContent()
                            if (shine.value in 0.001f..0.999f) {
                                // diagonal band going from left to right, clipped by the Surface
                                val bandWidth = size.width / 2
                                val x = -bandWidth + (size.width + 2 * bandWidth) * shine.value
                                drawRect(
                                    Brush.linearGradient(
                                        listOf(Color.Transparent, shineColor, Color.Transparent),
                                        start = Offset(x - bandWidth / 2, 0f),
                                        end = Offset(x + bandWidth / 2, size.height)
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        achievement.type.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(achievement.type.titleResource),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/** One line summary of the achievements of a workout, shown in the history. */
@Composable
fun AchievementsNote(
    achievements: List<WorkoutAchievementAndInfo>,
    useImperialSystem: Boolean,
    modifier: Modifier = Modifier
) {
    if (achievements.isEmpty()) return
    val recordExercises = achievements
        .filter { it.type.isPersonalRecord }
        .mapNotNull { it.name }
        .distinct()
    val parts = buildList {
        if (recordExercises.isNotEmpty())
            add(pluralStringResource(
                R.plurals.achievement_new_prs,
                recordExercises.size,
                recordExercises.size,
                recordExercises.joinToString(", ")
            ))
        achievements.filterNot { it.type.isPersonalRecord }.forEach {
            add(when (it.type) {
                AchievementType.WORKOUT_VOLUME, AchievementType.PROGRAM_VOLUME ->
                    stringResource(it.type.titleResource)
                else -> achievementValueText(it, useImperialSystem)
            })
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = stringResource(R.string.achievements),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = parts.joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

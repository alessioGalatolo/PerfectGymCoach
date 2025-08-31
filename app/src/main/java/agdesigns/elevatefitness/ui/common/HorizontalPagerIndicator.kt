package agdesigns.elevatefitness.ui.common


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.abs


@Composable
fun HorizontalPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageCount: Int = pagerState.pageCount,
    dotSize: Dp = 8.dp,                 // diameter of a circle
    rectWidth: Dp = 24.dp,               // width of the active rounded rectangle
    spacing: Dp = 6.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = activeColor.copy(alpha = 0.35f),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(pageCount) { page ->
            val selection = 1f - pageOffsetFraction(pagerState, page).coerceIn(0f, 1f)
            // selection: 0f -> circle (inactive), 1f -> rounded rectangle (active)
            val width = lerp(dotSize, rectWidth, selection)
            val height = dotSize
            val color = colorLerp(inactiveColor, activeColor, selection)

            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(percent = 50)) // capsule; circle when width == height
                    .background(color)
            )
        }
    }
}

/**
 * Returns how far [page] is from the current page, factoring in the current swipe offset.
 * 0f means the page is the current one; 1f means it's one full page away, etc.
 */
private fun pageOffsetFraction(pagerState: PagerState, page: Int): Float {
    // This formula is robust to sign/normalisation differences across pager versions.
    val current = pagerState.currentPage
    val offset = pagerState.currentPageOffsetFraction
    return abs((page - current) - offset)
}

private fun lerp(start: Dp, end: Dp, fraction: Float): Dp {
    val f = fraction.coerceIn(0f, 1f)
    return start + (end - start) * f
}

private fun colorLerp(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f,
    )
}

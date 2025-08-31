package agdesigns.elevatefitness.ui.common

import android.text.Layout
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.shape.toVicoShape
import com.patrykandpatrick.vico.core.cartesian.FadingEdges
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.Position
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape


val MeanLineKey = ExtraStore.Key<Double>()
val BestColumnKey = ExtraStore.Key<Double>()

/**
 * Draws an horizontal line for e.g., the mean value
 */
@Composable
fun rememberHorizontalLine(extraKey: ExtraStore.Key<Double>, lineText: String): HorizontalLine {
    val fillColor = fill(MaterialTheme.colorScheme.primaryContainer)
    val line = rememberLineComponent(fill = fillColor, thickness = 2.dp)
    val labelComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        margins = insets(start = 6.dp),
        padding = insets(start = 8.dp, end = 8.dp, bottom = 2.dp),
        background =
            shapeComponent(
                fillColor,
                CorneredShape.rounded(topLeftDp = 4f, topRightDp = 4f)
            ),
    )
    return remember {
        HorizontalLine(
            y = { extra -> extra.getOrNull(extraKey) ?: 0.0  },
            line = line,
            labelComponent = labelComponent,
            label = { lineText },
            verticalLabelPosition = Position.Vertical.Top,
        )
    }
}

/**
 * Pill column with top indicator
 */
class ColumnWithTopIndicatorShape(
    private val baseColumnShape: CornerBasedShape,  // Material shape
    private val indicatorType: IndicatorType = IndicatorType.CIRCLE,
    private val indicatorSizeRatio: Float = 0.5f, // Indicator size as ratio of column width
) : Shape {

    enum class IndicatorType {
        CIRCLE, SQUARE, DIAMOND, CHECKMARK
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val columnWidth = size.width
            val columnHeight = size.height
            val spacing = 4f

            // Calculate indicator size based on column width
            val calculatedIndicatorSize = columnWidth * indicatorSizeRatio
            val indicatorSize = calculatedIndicatorSize//.coerceIn(minIndicatorSize, maxIndicatorSize)

            // Calculate positions
            val indicatorCenterX = columnWidth / 2f
            val indicatorTop = 2f
            val columnTop = indicatorTop + indicatorSize + spacing

            // FIXME: what if topStart is different from other corners?
            val columnCornerRadius = baseColumnShape.topStart.toPx(size, density)
            // Draw main column
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = columnTop,
                    right = columnWidth,
                    bottom = columnHeight,
                    radiusX = columnCornerRadius,
                    radiusY = columnCornerRadius
                )
            )

            // Draw top indicator based on type
            when (indicatorType) {
                IndicatorType.CIRCLE -> {
                    addOval(
                        oval = Rect(
                            left = indicatorCenterX - indicatorSize/2f,
                            top = indicatorTop,
                            right = indicatorCenterX + indicatorSize/2f,
                            bottom = indicatorTop + indicatorSize
                        )
                    )
                }

                IndicatorType.SQUARE -> {
                    addRect(
                        Rect(
                            left = indicatorCenterX - indicatorSize/2f,
                            top = indicatorTop,
                            right = indicatorCenterX + indicatorSize/2f,
                            bottom = indicatorTop + indicatorSize
                        )
                    )
                }

                IndicatorType.DIAMOND -> {
                    val halfSize = indicatorSize / 2f
                    val centerY = indicatorTop + halfSize

                    moveTo(indicatorCenterX, indicatorTop) // Top
                    lineTo(indicatorCenterX + halfSize, centerY) // Right
                    lineTo(indicatorCenterX, indicatorTop + indicatorSize) // Bottom
                    lineTo(indicatorCenterX - halfSize, centerY) // Left
                    close()
                }

                IndicatorType.CHECKMARK -> {
                    // Simple checkmark path
                    val checkWidth = indicatorSize * 0.8f
                    val checkHeight = indicatorSize * 0.6f
                    val startX = indicatorCenterX - checkWidth/2f
                    val startY = indicatorTop + (indicatorSize - checkHeight)/2f

                    moveTo(startX, startY + checkHeight * 0.5f)
                    lineTo(startX + checkWidth * 0.4f, startY + checkHeight)
                    lineTo(startX + checkWidth, startY)
                }
            }
        }

        return Outline.Generic(path)
    }
}

/**
 * Provides normal column for all except "highest" which is takes from extra "BestColumnKey"
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun columnProviderWithBest(baseShape: CornerBasedShape, baseColor: Color, thickness: Dp) =
    object : ColumnCartesianLayer.ColumnProvider {
        val normal: LineComponent = rememberLineComponent(
            fill = fill(baseColor),
            thickness = thickness,
            shape = baseShape.toVicoShape()
        )
        val best: LineComponent = rememberLineComponent(
            fill = fill(MaterialTheme.colorScheme.primary),
            thickness = thickness,
            shape = ColumnWithTopIndicatorShape(
                baseColumnShape = MaterialTheme.shapes.extraSmall,
                indicatorType = ColumnWithTopIndicatorShape.IndicatorType.CIRCLE
            ).toVicoShape()
        )

        override fun getColumn(
            entry: ColumnCartesianLayerModel.Entry,
            seriesIndex: Int,
            extraStore: ExtraStore,
        ) = if (entry.y >= (extraStore.getOrNull(BestColumnKey) ?: Double.MAX_VALUE)) best else normal

        override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore) = normal
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PillChart(
    modelProducer: CartesianChartModelProducer,
    baseShape: CornerBasedShape = MaterialTheme.shapes.extraExtraLarge,
    baseColor: Color = MaterialTheme.colorScheme.secondary,
    thickness: Dp = 25.dp,
    columnCollectionSpacing: Dp = 4.dp,
    xValueFormatter: CartesianValueFormatter = CartesianValueFormatter.Default,
    yValueFormatter: CartesianValueFormatter = CartesianValueFormatter.Default,
    markerValueFormatter: DefaultCartesianMarker.ValueFormatter = DefaultCartesianMarker.ValueFormatter.default(),
    decorations: List<HorizontalLine> = emptyList(),
    scrollable: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val labelBackground = rememberShapeComponent(
        fill = fill(MaterialTheme.colorScheme.background),
        shape = MaterialTheme.shapes.medium.toVicoShape(),
    )
    val markerLabel = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        textAlignment = Layout.Alignment.ALIGN_CENTER,
        padding = insets(8.dp, 4.dp),
        background = labelBackground,
    )
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                rangeProvider = CartesianLayerRangeProvider.auto(),
                columnProvider = columnProviderWithBest(baseShape, baseColor, thickness),
                columnCollectionSpacing = columnCollectionSpacing
            ),
            fadingEdges = if (scrollable)
                FadingEdges(widthDp = 64f)
            else null,
            marker = rememberDefaultCartesianMarker(
                label = markerLabel,
                valueFormatter = markerValueFormatter
            ),
            startAxis = VerticalAxis.rememberStart(
                line = rememberLineComponent(Fill.Transparent),
                tick = rememberLineComponent(Fill.Transparent),
                valueFormatter = yValueFormatter
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                tick = rememberLineComponent(Fill.Transparent),
                guideline = rememberLineComponent(Fill.Transparent),
                valueFormatter = xValueFormatter
            ),
            decorations = decorations
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollable, initialScroll = Scroll.Absolute.End),
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        modifier = modifier,
    )
}
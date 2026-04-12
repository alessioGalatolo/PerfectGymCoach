package agdesigns.elevatefitness.ui.common

import android.text.Layout
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.compose.cartesian.FadingEdges
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

// Used to set the average value line in chart
val MeanLineKey = ExtraStore.Key<Double>()
// index of best column so that it can be highlighted
val BestColumnKey = ExtraStore.Key<Int>()
// index of "current" column (e.g., current workout) so that it can be highlighted
val CurrentColumnKey = ExtraStore.Key<Int>()
// same as above but for lines and is a list
val highlightSeriesKey = ExtraStore.Key<List<Int>>()
// key to get labels for the indices of workout frequency
val WorkoutFrequencyLabelsKey = ExtraStore.Key<List<String>>()

val chartColors = listOf(
    Color(0xFF7F77DD),
    Color(0xFF1D9E75),
    Color(0xFFD85A30),
    Color(0xFFD4537E),
    Color(0xFF378ADD),
    Color(0xFFEF9F27),
    Color(0xFF639922),
    Color(0xFFE24B4A),
    Color(0xFF888780),
    Color(0xFF185FA5),
)

/**
 * Draws an horizontal line for e.g., the mean value
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberHorizontalLine(extraKey: ExtraStore.Key<Double>, lineText: String): HorizontalLine {
    val fillColor = Fill(MaterialTheme.colorScheme.primaryContainer)
    val line = rememberLineComponent(fill = fillColor, thickness = 2.dp)
    val labelComponent = rememberTextComponent(
        style = TextStyle(MaterialTheme.colorScheme.onPrimaryContainer),
        margins = Insets(start = 6.dp),
        padding = Insets(start = 8.dp, end = 8.dp, bottom = 2.dp),
        background =
            ShapeComponent(
                fillColor,
                MaterialTheme.shapes.extraSmall.copy(bottomStart = CornerSize(0f), bottomEnd = CornerSize(0f))
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
fun columnProviderWithHighlight(
    baseShape: CornerBasedShape,
    baseColor: Color,
    thickness: Dp = 25.dp,
    highlightKey: ExtraStore.Key<Int> = BestColumnKey
) =
    object : ColumnCartesianLayer.ColumnProvider {
        val normal: LineComponent = rememberLineComponent(
            fill = Fill(baseColor),
            thickness = thickness,
            shape = baseShape
        )
        val best: LineComponent = rememberLineComponent(
            fill = Fill(MaterialTheme.colorScheme.primary),
            thickness = thickness,
            shape = ColumnWithTopIndicatorShape(
                baseColumnShape = MaterialTheme.shapes.extraSmall,
                indicatorType = ColumnWithTopIndicatorShape.IndicatorType.CIRCLE
            )
        )

        override fun getColumn(
            entry: ColumnCartesianLayerModel.Entry,
            seriesIndex: Int,
            extraStore: ExtraStore,
        ) = if (entry.x.toInt() == (extraStore.getOrNull(highlightKey) ?: -1)) best else normal

        override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore) = normal
    }

/**
 * Provides normal lines for all except series index in "highlightSeriesKey" which is special line and point
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun lineProviderWithHighlight(
    seriesKey: ExtraStore.Key<List<Int>> = highlightSeriesKey,
) =
    object : LineCartesianLayer.LineProvider {
        val primaryColor = vicoTheme.lineCartesianLayerColors[0]
        val secondaryColor = vicoTheme.lineCartesianLayerColors[1]
        val tertiaryColor = vicoTheme.lineCartesianLayerColors[2]


        val highlight: LineCartesianLayer.Line = LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(primaryColor)),
            areaFill = null,
            pointProvider =
                LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(rememberShapeComponent(
                        Fill(primaryColor),
                        MaterialTheme.shapes.extraSmall
                    ), size = 16.dp)  // double default size
                ),
        )

        val mainLine = LineCartesianLayer.rememberLine(
                fill = LineCartesianLayer.LineFill.single(Fill(secondaryColor)),
                areaFill = null,
                pointProvider =
                    LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.Point(
                            rememberShapeComponent(
                                Fill(secondaryColor),
                                MaterialTheme.shapes.extraExtraLarge
                            )
                        )
                    ),
            )

        val otherLine = LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(tertiaryColor)),
            areaFill = null,
            pointProvider =
                LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        rememberShapeComponent(
                            Fill(tertiaryColor),
                            MaterialTheme.shapes.extraExtraLarge
                        )
                    )
                ),
        )

        override fun getLine(
            seriesIndex: Int,
            extraStore: ExtraStore
        ): LineCartesianLayer.Line {
            val highlightIndices = extraStore.getOrNull(seriesKey) ?: emptyList()
            if (highlightIndices.contains(seriesIndex)) {
                return highlight
            }
            // FIXME: wrong behaviour if series is: [(0, main), (1, highlight), (2, otherLine) <-- this gets mainLine]
            return listOf(mainLine, otherLine)[seriesIndex % 2]
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PillChart(
    modelProducer: CartesianChartModelProducer,
    baseShape: CornerBasedShape = MaterialTheme.shapes.extraExtraLarge,
    baseColor: Color = MaterialTheme.colorScheme.secondary,
    thickness: Dp = 25.dp,
    columnCollectionSpacing: Dp = 4.dp,
    xValueFormatter: CartesianValueFormatter = CartesianValueFormatter.decimal(),
    yValueFormatter: CartesianValueFormatter = CartesianValueFormatter.decimal(),
    markerValueFormatter: DefaultCartesianMarker.ValueFormatter = DefaultCartesianMarker.ValueFormatter.default(),
    decorations: List<HorizontalLine> = emptyList(),
    scrollable: Boolean = false,
    itemPlacer: VerticalAxis.ItemPlacer = remember { VerticalAxis.ItemPlacer.step() },
    modifier: Modifier = Modifier,
) {
    val labelBackground = rememberShapeComponent(
        fill = Fill(MaterialTheme.colorScheme.background),
        shape = MaterialTheme.shapes.medium,
    )
    val markerLabel = rememberTextComponent(
        style = TextStyle(
            MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        padding = Insets(8.dp, 4.dp),
        background = labelBackground,
    )
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                rangeProvider = CartesianLayerRangeProvider.auto(),
                columnProvider = columnProviderWithHighlight(baseShape, baseColor, thickness),
                columnCollectionSpacing = columnCollectionSpacing
            ),
            fadingEdges = if (scrollable)
                FadingEdges(width = 64.dp)
            else null,
            marker = rememberDefaultCartesianMarker(
                label = markerLabel,
                valueFormatter = markerValueFormatter
            ),
            startAxis = VerticalAxis.rememberStart(
                line = rememberLineComponent(Fill.Transparent),
                tick = rememberLineComponent(Fill.Transparent),
                valueFormatter = yValueFormatter,
                itemPlacer = itemPlacer
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
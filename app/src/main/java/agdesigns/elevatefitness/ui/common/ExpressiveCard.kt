package agdesigns.elevatefitness.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

@ExperimentalMaterial3ExpressiveApi
object SelectableCardDefaults {
    @Composable
    fun shapes(): SelectableCardShapes = SelectableCardShapes(
        shape = MaterialTheme.shapes.medium,
        pressedShape = MaterialTheme.shapes.extraLarge,
    )

    @Composable
    fun shape(pressed: Boolean): CornerBasedShape = if (pressed)
        MaterialTheme.shapes.extraLarge
    else MaterialTheme.shapes.medium
}

@ExperimentalMaterial3ExpressiveApi
class SelectableCardShapes(val shape: CornerBasedShape, val pressedShape: CornerBasedShape = shape) {
    fun copy(shape: CornerBasedShape? = this.shape, pressedShape: CornerBasedShape? = this.pressedShape) =
        SelectableCardShapes(
            shape = shape.takeOrElse { this.shape },
            pressedShape = pressedShape.takeOrElse { this.pressedShape },
        )

    internal fun CornerBasedShape?.takeOrElse(block: () -> CornerBasedShape): CornerBasedShape = this ?: block()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SelectableCardShapes) return false

        if (shape != other.shape) return false
        if (pressedShape != other.pressedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + pressedShape.hashCode()

        return result
    }
}

/**
 * A card that can has a selectable state (selected/not selected)
 * When pressed, the card will change shape based on material expressive
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SelectableCard(
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: SelectableCardShapes = SelectableCardDefaults.shapes(),
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val size = Size.Zero
    val transition = updateTransition(selected, label = "Card selected")
    val animatedShape by transition.animateValue(
        transitionSpec = { MotionScheme.standard().fastSpatialSpec() },
        typeConverter = TwoWayConverter(
            convertToVector = { shape ->
                AnimationVector4D(
                    shape.topStart.toPx(size, density),
                    shape.topEnd.toPx(size, density),
                    shape.bottomEnd.toPx(size, density),
                    shape.bottomStart.toPx(size, density)
                )
            },
            convertFromVector = { vector ->
                RoundedCornerShape(
                    topStart = vector.v1,
                    topEnd = vector.v2,
                    bottomEnd = vector.v3,
                    bottomStart = vector.v4,
                )
            }
        )
    ) {
        if (it && shapes.shape != shapes.pressedShape) shapes.pressedShape else shapes.shape
    }
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        if (selected) {
            scope.launch {
                scale.animateTo(
                    targetValue = 1.05f,
                    animationSpec = spring(
                        0.6f,
                        800.0f * 4f
                    )
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        0.6f,
                        800.0f * 4f
                    )
                )
            }
        }
    }
    Card(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        enabled = enabled,
        shape = animatedShape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
        content = content,
    )
}
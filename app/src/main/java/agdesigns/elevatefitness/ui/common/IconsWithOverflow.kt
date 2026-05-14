package agdesigns.elevatefitness.ui.common

import agdesigns.elevatefitness.R
import android.R.attr.checked
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.roundToInt

data class IconAndLabel(val icon: ImageVector, val label: String, val onClick: () -> Unit)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconsWithOverflow(
    modifier: Modifier = Modifier,
    spacing: Dp = 0.dp,
    maxVisibleItems: Int = Int.MAX_VALUE,
    contents: List<IconAndLabel>
) {
    val spacingPx = with(LocalDensity.current) { spacing.toPx().roundToInt() }
    var expanded by remember { mutableStateOf(false) }

    val overflowContent: @Composable (List<IconAndLabel>) -> Unit = { items ->
        val groupInteractionSource = remember { MutableInteractionSource() }

        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(stringResource(R.string.morevert_icon_options)) } },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, null)
                }
            }
            DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(0, 1),
                    interactionSource = groupInteractionSource,
                ) {
                    items.fastForEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.label) },
                            leadingIcon = {
                                Icon(
                                    item.icon,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            },
                            onClick = item.onClick,
                        )
                    }
                }
            }
        }
    }

    SubcomposeLayout(modifier) { constraints ->
        val itemPlaceables = contents.mapIndexed { idx, item ->
            subcompose("items$idx") {
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(item.label) } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(onClick = item.onClick) {
                        Icon(item.icon, contentDescription = null)
                    }
                }
            }.first().measure(constraints.copy(minWidth = 0))
        }

        var usedWidth = 0
        val visible = mutableListOf<Placeable>()
        val hiddenIndices = mutableListOf<Int>()

        // Measure overflow button width for space reservation (hiddenIndices still empty here,
        // but we only need the button's own width — item list is irrelevant for sizing).
        val overflowPlaceable = subcompose("overflow") {
            overflowContent(emptyList())
        }.first().measure(constraints.copy(minWidth = 0))
        val overflowWidth = overflowPlaceable.width + spacingPx

        itemPlaceables.forEachIndexed { index, placeable ->
            val spaceNeeded = placeable.width + if (visible.isEmpty()) 0 else spacingPx
            val remainingWidth = constraints.maxWidth - usedWidth
            // Reserve overflow space unless this is the last item and nothing is hidden yet.
            val wouldNeedOverflow = index < itemPlaceables.lastIndex || hiddenIndices.isNotEmpty()
            val effectiveRemaining = if (wouldNeedOverflow) remainingWidth - overflowWidth else remainingWidth

            if (spaceNeeded <= effectiveRemaining && visible.size < maxVisibleItems) {
                visible.add(placeable)
                usedWidth += spaceNeeded
            } else {
                hiddenIndices.add(index)
            }
        }

        // Re-subcompose overflow with the real hidden item list now that indices are final.
        val finalOverflow = if (hiddenIndices.isNotEmpty()) {
            subcompose("overflow_final") {
                overflowContent(contents.slice(hiddenIndices))
            }.first().measure(constraints.copy(minWidth = 0))
        } else null

        val height = (visible.map { it.height } + listOfNotNull(finalOverflow?.height)).maxOrNull() ?: 0

        // Report actual content width so the parent can arrange/align correctly.
        // constraints.maxWidth is only used internally to decide which items fit.
        val contentWidth = when {
            finalOverflow != null && visible.isNotEmpty() -> usedWidth + spacingPx + finalOverflow.width
            finalOverflow != null -> finalOverflow.width
            else -> usedWidth
        }

        layout(contentWidth, height) {
            var x = 0
            visible.forEach { p ->
                p.placeRelative(x, (height - p.height) / 2)
                x += p.width + spacingPx
            }
            finalOverflow?.placeRelative(x, (height - finalOverflow.height) / 2)
        }
    }
}
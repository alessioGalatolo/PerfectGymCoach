package agdesigns.elevatefitness.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

class GroupedCardScope {
    internal val items = mutableListOf<Pair<@Composable () -> Unit, () -> Unit>>()

    fun subCard(
        onClick: () -> Unit = {},
        content: @Composable () -> Unit
    ) {
        items.add(content to onClick)
    }
}

/*
A single card but split between items (similar to android's grouped notifications)
 */
@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    content: GroupedCardScope.() -> Unit
) {
    val scope = remember(content) { GroupedCardScope().apply(content) }
    val items = scope.items

    Column(modifier) {
        items.forEachIndexed { index, (item, onClick) ->
            val defaultShape: RoundedCornerShape = when (CardDefaults.shape) {
                is RoundedCornerShape -> CardDefaults.shape as RoundedCornerShape
                else -> RoundedCornerShape(16.dp)
            }
            val defaultOtherCorner: Dp = 4.dp
            val currentItemShape = if (items.size == 1)
                defaultShape
            else when (index) {
                0 -> defaultShape.copy(
                    bottomStart = CornerSize(defaultOtherCorner),
                    bottomEnd = CornerSize(defaultOtherCorner)
                )
                items.lastIndex -> defaultShape.copy(
                    topStart = CornerSize(defaultOtherCorner),
                    topEnd = CornerSize(defaultOtherCorner)
                )
                else -> RoundedCornerShape(defaultOtherCorner)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = currentItemShape,
                colors = colors,
                onClick = onClick
            ) {
                Column(Modifier.padding(16.dp)) {
                    item()
                }
            }

            if (index != items.lastIndex) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

// ---- LazyList variant ----

class LazyGroupedCardScope {
    internal data class SubcardDetails(
        val content: @Composable () -> Unit,
        val onClick: (() -> Unit)? = null,
        val key: Any? = null,
        val reorderableLazyListState: ReorderableLazyListState? = null,
        val modifier: Modifier = Modifier
    )

    internal val subcards = mutableListOf<SubcardDetails>()

    fun subCard(
        onClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        subcards.add(
            SubcardDetails(
                content = content,
                onClick = onClick,
                modifier = modifier
            )
        )
    }

    fun reorderableSubCard(
        key: Any,
        reorderableLazyListState: ReorderableLazyListState,
        onClick: (() -> Unit),
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        subcards.add(
            SubcardDetails(
                key = key,
                reorderableLazyListState = reorderableLazyListState,
                content = content,
                onClick = onClick,
                modifier = modifier
            )
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.lazyGroupedCard(
    colors: CardColors? = null,
    innerCardPadding: Dp = 16.dp,
    content: LazyGroupedCardScope.() -> Unit
) {
    val scope = LazyGroupedCardScope().apply(content)
    val subcards = scope.subcards
    val lastIndex = subcards.lastIndex

    subcards.forEachIndexed { index, subcardDetails ->
        item(key = subcardDetails.key) {
            val haptics = LocalHapticFeedback.current
            val defaultShape: RoundedCornerShape = when (CardDefaults.shape) {
                is RoundedCornerShape -> CardDefaults.shape as RoundedCornerShape
                else -> RoundedCornerShape(16.dp)
            }
            val defaultOtherCorner: Dp = 4.dp
            val currentItemShape = if (subcards.size == 1)
                defaultShape
            else when (index) {
                0 -> defaultShape.copy(
                    bottomStart = CornerSize(defaultOtherCorner),
                    bottomEnd = CornerSize(defaultOtherCorner)
                )
                lastIndex -> defaultShape.copy(
                    topStart = CornerSize(defaultOtherCorner),
                    topEnd = CornerSize(defaultOtherCorner)
                )
                else -> RoundedCornerShape(defaultOtherCorner)
            }
            if (subcardDetails.reorderableLazyListState != null && subcardDetails.key != null) {
                val interactionSource = remember { MutableInteractionSource() }
                ReorderableItem(
                    subcardDetails.reorderableLazyListState,
                    key = subcardDetails.key,
                ) {
                    InnerCard(
                        subcardDetails = subcardDetails,
                        currentItemShape = currentItemShape,
                        colors = colors,
                        innerCardPadding = innerCardPadding,
                        interactionSource = interactionSource,
                        modifier = Modifier.longPressDraggableHandle(
                            interactionSource = interactionSource,
                            onDragStarted = {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            },
                            onDragStopped = {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            },
                        )
                    )
                }
            } else {
                InnerCard(
                    subcardDetails = subcardDetails,
                    currentItemShape = currentItemShape,
                    colors = colors,
                    innerCardPadding = innerCardPadding
                )
            }
            if (index != lastIndex) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun LazyItemScope.InnerCard(
    subcardDetails: LazyGroupedCardScope.SubcardDetails,
    currentItemShape: RoundedCornerShape,
    colors: CardColors?,
    innerCardPadding: Dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    modifier: Modifier = Modifier
) {
    // if no onClick, we can use the normal card that allows clickable in modifier
    if (subcardDetails.onClick != null) {
        Card(
            modifier = subcardDetails.modifier
                .then(modifier)
                .fillMaxWidth()
                .animateItem(
                    fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                ),
            shape = currentItemShape,
            colors = colors ?: CardDefaults.cardColors(),
            onClick = subcardDetails.onClick,
            interactionSource = interactionSource
        ) {
            Column(Modifier.padding(innerCardPadding)) {
                subcardDetails.content()
            }
        }
    } else {
        Card(
            modifier = subcardDetails.modifier
                .fillMaxWidth()
                .animateItem(
                    fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                ),
            shape = currentItemShape,
            colors = colors ?: CardDefaults.cardColors(),
        ) {
            Column(Modifier.padding(innerCardPadding)) {
                subcardDetails.content()
            }
        }
    }
}


// ---- Previews ----

@Preview
@Composable
fun GroupedCardPreview() {
    GroupedCard {
        subCard(onClick = { /* handle click */ }) { Text("Item 1") }
        subCard(onClick = { /* handle click */ }) { Text("Item 2") }
        subCard { Text("Item 3") }
    }
}

@Preview
@Composable
fun LazyGroupedCardPreview() {
    LazyColumn {
        lazyGroupedCard {
            subCard(onClick = { /* handle click */ }) { Text("Item 1") }
            subCard(onClick = { /* handle click */ }) { Text("Item 2") }
            subCard { Text("Item 3") }
        }
        // Other lazy items can be freely mixed in
        item { Spacer(Modifier.height(8.dp)) }
        lazyGroupedCard {
            subCard { Text("Standalone item") }
        }
    }
}
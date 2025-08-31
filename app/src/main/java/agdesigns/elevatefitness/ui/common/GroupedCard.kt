package agdesigns.elevatefitness.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    items: List<@Composable () -> Unit>,
    onClicks: List<() -> Unit> = List(items.size) { {} }
) {
    /*
    A single card but split between items (similar to android's grouped notifications)
     */
    Column(modifier) {
        items.forEachIndexed { index, item ->
            val defaultShape: RoundedCornerShape = when (CardDefaults.shape) {
                is RoundedCornerShape -> CardDefaults.shape as RoundedCornerShape
                else -> RoundedCornerShape(16.dp) // fallback
            }
            // the corner radius between items
            val defaultOtherCorner: Dp = 4.dp
            val currentItemShape = if (items.size == 1)
                defaultShape
            else
                when (index) {
                0 -> defaultShape.copy(
                    bottomStart = CornerSize(defaultOtherCorner),
                    bottomEnd = CornerSize(defaultOtherCorner)
                )

                items.lastIndex -> defaultShape.copy(
                    topStart = CornerSize(defaultOtherCorner),
                    topEnd = CornerSize(defaultOtherCorner),
                )

                else -> RoundedCornerShape(
                    topStart = defaultOtherCorner,
                    topEnd = defaultOtherCorner,
                    bottomStart = defaultOtherCorner,
                    bottomEnd = defaultOtherCorner
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = currentItemShape,
                colors = colors,
                onClick = onClicks[index]
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

@Preview
@Composable
fun GroupedCardPreview() {
    GroupedCard(items = listOf(
        { Text("Item 1") },
        { Text("Item 2") },
        { Text("Item 3") },
    ))
}
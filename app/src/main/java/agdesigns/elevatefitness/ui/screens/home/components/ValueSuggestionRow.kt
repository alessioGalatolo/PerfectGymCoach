package agdesigns.elevatefitness.ui.screens.home.components

import agdesigns.elevatefitness.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun <T> ValueSuggestionRow(
    shouldBeShown: Boolean,
    options: List<T>,
    onClick: (T) -> Unit,
    valueIsSelected: (T) -> Boolean,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(shouldBeShown) {
        if (shouldBeShown) {
            val indexOfSelected = options.indexOfFirst(valueIsSelected)
            if (indexOfSelected != -1) {
                listState.animateScrollToItem(indexOfSelected+1)
            }
        }
    }
    AnimatedVisibility(shouldBeShown) {
        LazyRow(state = listState) {
            item {
                Spacer(Modifier.width(4.dp))
            }
            options.forEach { option ->
                item {
                    ElevatedSuggestionChip(
                        onClick = { onClick(option) },
                        colors = if (valueIsSelected(option))
                            SuggestionChipDefaults.elevatedSuggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) else SuggestionChipDefaults.elevatedSuggestionChipColors(),
                        label = {
                            Row (verticalAlignment = CenterVertically) {
                                if (valueIsSelected(option)) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        stringResource(
                                            R.string.selected_value_i,
                                            option.toString()
                                        )
                                    )
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                }
                                Text(option.toString())
                            }
                        }, modifier = Modifier.padding(4.dp)
                    )
                }
            }
            item {
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
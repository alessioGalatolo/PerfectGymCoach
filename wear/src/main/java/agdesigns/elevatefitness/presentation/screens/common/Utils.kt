package agdesigns.elevatefitness.presentation.screens.common

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.util.isLargeScreen

/**
 * Mostly copy-pasted from horologist "TextMediaDisplay" with the addition of Marquee
 */
@OptIn(ExperimentalHorologistApi::class)
@Composable
fun TextHeaderWithMarquee(
    title: String,
    subtitle: String,
    ambientMode: AmbientMode,
    modifier: Modifier = Modifier,
    titleIcon: ImageVector? = null,
) {
    val marqueeModifier = if (ambientMode is AmbientMode.Interactive) {
        Modifier.basicMarquee()
    } else Modifier
    val isLargeScreen = LocalConfiguration.current.isLargeScreen
    val titleSidePadding = (0.063f * LocalConfiguration.current.screenWidthDp).dp

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val textStyle = MaterialTheme.typography.button
        val text = buildAnnotatedString {
            if (titleIcon != null) {
                appendInlineContent(id = "iconSlot")
                append(" ")
            }
            append(title)
        }
        val inlineContent = if (titleIcon != null) {
            mapOf(
                "iconSlot" to InlineTextContent(
                    Placeholder(textStyle.fontSize, textStyle.fontSize, PlaceholderVerticalAlign.TextCenter),
                ) {
                    Icon(
                        titleIcon,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onBackground,
                    )
                },
            )
        } else {
            emptyMap()
        }
        Text(
            text = text,
            inlineContent = inlineContent,
            modifier = marqueeModifier
                .padding(
                    top = if (isLargeScreen) 0.dp else 2.dp,
                    bottom = if (isLargeScreen) 3.dp else 1.dp,
                    start = titleSidePadding,
                    end = titleSidePadding,
                ),
            color = MaterialTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = textStyle,
        )
        Text(
            text = subtitle,
            modifier = marqueeModifier
                .fillMaxWidth()
                .padding(top = 1.dp, bottom = .6.dp),
            color = MaterialTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.body2,
        )
    }
}
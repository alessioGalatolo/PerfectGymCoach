package agdesigns.elevatefitness.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AICard(
    text: String,
    aiEnabled: Boolean,
    generationFinished: Boolean,
    interruptGeneration: () -> Unit,
    regenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Text(
            "✨ AI Coach ✨",
            style = MaterialTheme.typography.titleLarge.copy(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Blue, Color.Green)
                )
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        )
        if (aiEnabled) {
            if (text.isEmpty() && !generationFinished) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ContainedLoadingIndicator()
                }
            } else {
                CompositionLocalProvider {
                    ProvideTextStyle(
                        value = TextStyle(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.3,
                        )
                    ) {
                        RichText(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style =
                                RichTextStyle(
                                    codeBlockStyle =
                                        CodeBlockStyle(
                                            textStyle =
                                                TextStyle(
                                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                                    fontFamily = FontFamily.Monospace,
                                                )
                                        ),
                                    stringStyle =
                                        RichTextStringStyle(
                                            linkStyle =
                                                TextLinkStyles(
                                                    style = SpanStyle(
                                                        color = Color.Cyan
                                                    )
                                                )
                                        ),
                                ),
                        ) {
                            Markdown(
                                content = text
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                IconButton(
                    if (generationFinished) regenerate else interruptGeneration,
                    shapes = IconButtonDefaults.shapes(
                        shape = MaterialTheme.shapes.medium,
                        pressedShape = MaterialTheme.shapes.extraExtraLarge
                    ),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Icon(
                        if (generationFinished)
                            Icons.Default.Refresh
                        else
                            Icons.Default.Stop,
                        ""
                    )
                }
            }
        } else {
            Text(
                "AI is not enabled. You can change this setting from the Profile tab.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
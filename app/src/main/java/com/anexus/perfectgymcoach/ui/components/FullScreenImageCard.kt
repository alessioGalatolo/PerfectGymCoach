package com.anexus.perfectgymcoach.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.anexus.perfectgymcoach.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageCard(
    topAppBarNavigationIcon: @Composable (Boolean) -> Unit,
    topAppBarActions: @Composable RowScope.(Boolean) -> Unit,
    title: @Composable () -> Unit,
    image: @Composable BoxScope.() -> Unit,
    imageHeight: Dp,
    brightImage: Boolean,
    content: @Composable () -> Unit,
    bottomBar: @Composable (PaddingValues, @Composable (@Composable () -> Unit) -> Unit) -> Unit
) {
    val deviceCornerRadius = 12.dp // TODO: should be same as device (waiting compose API)

    val localDensity = LocalDensity.current
    val statusBarsHeight = WindowInsets.Companion.statusBars.asPaddingValues().calculateTopPadding()
    val contentBelowImage by remember {
        derivedStateOf{
            max(
                0.dp,
                /*with(localDensity) { imageHeight.toDp() }*/ imageHeight - statusBarsHeight - 64.dp
            ) // top app bar height FIXME: should not be hardcoded
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState()
    )
    val s = scrollBehavior.state
    val belowImageFloat by remember { derivedStateOf { with(localDensity) { contentBelowImage.toPx() }}}
    val transition by remember { derivedStateOf { 1 - ((s.heightOffsetLimit - s.contentOffset - belowImageFloat).coerceIn(
        minimumValue = s.heightOffsetLimit,
        maximumValue = 0f
    ) / s.heightOffsetLimit) }}

    // make status bar transparent to see image behind
    val sysUiController = rememberSystemUiController()
    val transitionStarted = transition > 0.0
    val darkTheme = isSystemInDarkTheme()
    LaunchedEffect(transitionStarted, brightImage, darkTheme) {
        sysUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = (brightImage && !transitionStarted) || (transitionStarted && !darkTheme)
        )
    }

    Box (contentAlignment = TopCenter,
        modifier = Modifier
            .background(Color.Transparent)
            .fillMaxSize()){
        image()

        Scaffold (
            Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                // FIXME: low level transition needed because compose likes to hide its functions
                val transparentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                val tonedColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    BottomAppBarDefaults.ContainerElevation
                )
                val backgroundColor by remember { derivedStateOf { lerp(
                        transparentColor,
                        tonedColor,
                        FastOutLinearInEasing.transform(transition)
                    )}}
                TopAppBar(
                    title = {
                        // animate text alpha with scrolling
                        ProvideTextStyle(value = MaterialTheme.typography.titleLarge) {
                            CompositionLocalProvider(
                                LocalContentColor provides AlertDialogDefaults.titleContentColor.copy(alpha = transition),
                                content = title
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        scrolledContainerColor = backgroundColor
                    ),
                    navigationIcon = { topAppBarNavigationIcon(transitionStarted) },
                    actions = { topAppBarActions(transitionStarted) },
//                    modifier = Modifier.statusBarsPadding()
                )

            }, content = {
                Box(
                    Modifier
                        .padding(it)
                        .fillMaxSize()) {

                    // puts background in the whole screen
                    Surface(
                        Modifier
                            .fillMaxSize()
                            .padding(top = contentBelowImage)) {}
                    Column(
                        Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                    ) {
                        // space the same as the image height
                        Spacer(modifier = Modifier
                            .height(contentBelowImage)
                            .zIndex(1f)) // FIXME: necessary?
                        Surface(
                            shape = ReversedCornersShape(deviceCornerRadius),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            content()
                        }
                    }

                    // bottom bar

                }
            }, bottomBar = { bottomBar(
                WindowInsets.ime.add(WindowInsets.navigationBars).asPaddingValues()
            ) { bottomBarContent ->
                Surface(
                    tonalElevation = NavigationBarDefaults.Elevation,
                    modifier = Modifier
                        .background(NavigationBarDefaults.containerColor)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .wrapContentHeight(Alignment.CenterVertically)
                ) {
                    bottomBarContent()
                }
            }
            }
        )
    }
}

class ReversedCornersShape(private val radius: Dp) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            // Draw your custom path here
            path = reverseRoundedCorners(size = size, radius = with(density) { radius.toPx() })
        )
    }

    private fun reverseRoundedCorners(size: Size, radius: Float): Path {
        return Path().apply {
            val rect = size.toRect()
            addRect(rect)
            val outerCornerDiameter = radius * 2
            val cornerSize = Size(outerCornerDiameter, outerCornerDiameter)
            val cornerOffset = Offset(0f, -outerCornerDiameter)
            val cornerYOffset = Offset(-outerCornerDiameter, -outerCornerDiameter)
            addArc(
                Rect(
                    offset = rect.topLeft + cornerOffset,
                    size = cornerSize
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
            )
            lineTo(rect.topLeft.x, rect.topLeft.y)

            addArc(
                Rect(
                    offset = rect.topRight + cornerYOffset,
                    size = cornerSize
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -90f,
            )
            lineTo(rect.topRight.x, rect.topRight.y)
        }
    }
}
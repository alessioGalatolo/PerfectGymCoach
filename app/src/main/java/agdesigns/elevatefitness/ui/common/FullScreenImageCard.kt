package agdesigns.elevatefitness.ui.common

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SharedTransitionScope.FullScreenImageCard(
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedState: SharedContentState,
    topAppBarNavigationIcon: @Composable (Boolean) -> Unit,
    topAppBarActions: @Composable RowScope.(Boolean) -> Unit,
    bottomBar: @Composable () -> Unit,
    title: @Composable () -> Unit,
    image: @Composable BoxScope.() -> Unit,
    snackbarHost: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    imageHeight: Dp,
    brightImage: Boolean,
    darkTheme: Boolean,
    cardShape: RoundedCornerShape,
    scrollState: ScrollState,
    content: @Composable (Dp) -> Unit
) {
    val cornerRadius = cardShape.topStart

    val localDensity = LocalDensity.current
    val statusBarsHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val contentBelowImage = max(
        0.dp, imageHeight - statusBarsHeight - TopAppBarDefaults.TopAppBarExpandedHeight
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState()
    )
    val s = scrollBehavior.state
    val belowImageFloat = with(localDensity) { contentBelowImage.toPx() }
    // without this, the topappbar becomes opaque when content goes under it
    // but this will create a weird effect for the image corners which are above the content
    val cornersOffset = with(localDensity) { 20.dp.toPx() }
    val transition by remember(belowImageFloat) { derivedStateOf {
            1 - ((s.heightOffsetLimit + scrollState.value - belowImageFloat + cornersOffset).coerceIn(
            minimumValue = s.heightOffsetLimit,
            maximumValue = 0f
        ) / s.heightOffsetLimit) }
    }

    // make status bar transparent to see image behind
    // This is an approximation of what happened in accompanist systemUiController
    // but it is not deprecated :(
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: LocalActivity.current?.window
    val view = LocalView.current
    val transitionStarted = transition > 0.0
    DisposableEffect(transitionStarted, brightImage, darkTheme) {
        window?.let {
            WindowCompat.getInsetsController(it, view)
        }?.let {
            it.isAppearanceLightStatusBars = (brightImage && !transitionStarted) || (transitionStarted && !darkTheme)
        }
        onDispose {
            // revert icon colors
            window?.let {
                WindowCompat.getInsetsController(it, view)
            }?.let {
                it.isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    Box (contentAlignment = TopCenter,
        modifier = Modifier
            .background(Color.Transparent)
            .fillMaxSize()
    ){
        image()

        Scaffold (
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            topBar = {
                val transparentColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
                val tonedColor = MaterialTheme.colorScheme.surfaceContainer
                val backgroundColor = lerp(
                    transparentColor, // start from base color e.g., white to remove transparency instantly
                    tonedColor,  // transition to right color slowly together with text
                    FastOutLinearInEasing.transform(transition)
                )
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
                        // transition instantly from transparent
                        containerColor = if (transitionStarted) backgroundColor else Color.Transparent,
                        scrolledContainerColor = if (transitionStarted) backgroundColor else Color.Transparent
                    ),
                    navigationIcon = { topAppBarNavigationIcon(transitionStarted) },
                    actions = { topAppBarActions(transitionStarted) },
//                    modifier = Modifier.statusBarsPadding()
                )

            }, content = { innerPadding ->
                // top padding can be applied here
                val topPadding = innerPadding.calculateTopPadding()
                // bottom padding should be applied by content
                val bottomPadding = innerPadding.calculateBottomPadding()
                Box(
                    Modifier
                        .padding(top = topPadding)
                        .fillMaxSize()
                        .sharedBounds(
                            sharedContentState = sharedState,
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                MotionScheme.expressive().slowSpatialSpec()
                            }
                        )) {

                    // puts background in the whole screen
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = contentBelowImage)) {}
                    Column(
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .verticalScroll(scrollState)
                    ) {
                        // space the same as the image height
                        Spacer(modifier = Modifier.height(contentBelowImage))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = ReversedCornersShape(cornerRadius),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            content(bottomPadding)
                        }
                    }
                }
            },
            floatingActionButton = { floatingActionButton() },
            bottomBar = bottomBar
        )
    }
}

enum class ReversedCorner { TopStart, TopEnd, BottomStart, BottomEnd }

class ReversedCornersShape(
    private val cornerSize: CornerSize,
    private val corners: Set<ReversedCorner> = setOf(ReversedCorner.TopStart, ReversedCorner.TopEnd)
) : BaseReversedCornersShape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = reverseRoundedCorners(size = size, radius = cornerSize.toPx(size, density), corners = corners)
        )
    }
}

interface BaseReversedCornersShape : Shape {

    fun reverseRoundedCorners(
        size: Size,
        radius: Float,
        corners: Set<ReversedCorner> = setOf(ReversedCorner.TopStart, ReversedCorner.TopEnd)
    ): Path {
        return Path().apply {
            val rect = size.toRect()
            addRect(rect)
            val outerCornerDiameter = radius * 2
            val cornerSize = Size(outerCornerDiameter, outerCornerDiameter)

            if (ReversedCorner.TopStart in corners) {
                addArc(
                    Rect(offset = rect.topLeft + Offset(0f, -outerCornerDiameter), size = cornerSize),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                )
                lineTo(rect.topLeft.x, rect.topLeft.y)
            }

            if (ReversedCorner.TopEnd in corners) {
                addArc(
                    Rect(offset = rect.topRight + Offset(-outerCornerDiameter, -outerCornerDiameter), size = cornerSize),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = -90f,
                )
                lineTo(rect.topRight.x, rect.topRight.y)
            }

            if (ReversedCorner.BottomStart in corners) {
                addArc(
                    Rect(offset = rect.bottomLeft, size = cornerSize),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = -90f,
                )
                lineTo(rect.bottomLeft.x, rect.bottomLeft.y)
            }

            if (ReversedCorner.BottomEnd in corners) {
                addArc(
                    Rect(offset = rect.bottomRight + Offset(-outerCornerDiameter, 0f), size = cornerSize),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                )
                lineTo(rect.bottomRight.x, rect.bottomRight.y)
            }
        }
    }
}
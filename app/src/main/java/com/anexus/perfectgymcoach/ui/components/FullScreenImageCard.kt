package com.anexus.perfectgymcoach.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.anexus.perfectgymcoach.ui.ExerciseSettingsMenu
import com.anexus.perfectgymcoach.ui.TextFieldWithButtons
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageCard(
    topAppBarNavigationIcon: @Composable () -> Unit,
    topAppBarActions: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
    image: @Composable BoxScope.(Modifier) -> Unit,
    content: @Composable () -> Unit,
    bottomBar: @Composable (PaddingValues) -> Unit
) {
    val deviceCornerRadius = 12.dp // TODO: should be same as device (waiting compose API)

    // make status bar transparent to see image behind
    val sysUiController = rememberSystemUiController()
    SideEffect {
        sysUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = true
        )
    }
    val localDensity = LocalDensity.current
    var imageHeight by remember { // store image height to make content start at the right point
        mutableStateOf(0)
    }
    val imageHeightDp = with(localDensity) { imageHeight.toDp() }
    val statusBarsHeight = WindowInsets.Companion.statusBars.asPaddingValues().calculateTopPadding()
    val contentBelowImage = max(0.dp, imageHeightDp - statusBarsHeight - 64.dp) // top app bar height FIXME: should not be hardcoded
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState()
    )

    Box (contentAlignment = TopCenter,
        modifier = Modifier.background(Color.Transparent).fillMaxSize()){
        image(
            Modifier
                .fillMaxWidth()
//                .clip(AbsoluteRoundedCornerShape(deviceCornerRadius))
                .onGloballyPositioned { coordinates ->
                    imageHeight = coordinates.size.height
                }
        )

        Scaffold (
            Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
//            floatingActionButton = ,
            topBar = {
                val backgroundColors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = TopAppBarDefaults.smallTopAppBarColors().containerColor(
                        colorTransitionFraction = 1f
                    ).value)
                val s = scrollBehavior.state
                val belowImageFloat = with(localDensity) { contentBelowImage.toPx() }
                val transition = 1 - ((s.heightOffsetLimit - s.contentOffset - belowImageFloat).coerceIn(
                    minimumValue = s.heightOffsetLimit,
                    maximumValue = 0f
                ) / s.heightOffsetLimit)
                val backgroundColor = backgroundColors.containerColor(
                    colorTransitionFraction = transition
                ).value
                Box(Modifier.background(backgroundColor)) {
                    SmallTopAppBar(
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
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        ),
                        navigationIcon = topAppBarNavigationIcon,
                        actions = topAppBarActions,
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            }, content = {
                Box(
                    Modifier
                        .padding(it)
                        .fillMaxSize()) {
                    var bottomBarHeight by remember { mutableStateOf(0) }

                    // puts background in the whole screen
                    Surface(Modifier
                        .fillMaxSize()
                        .padding(top = contentBelowImage,
                            bottom = with(localDensity) { bottomBarHeight.toDp() })) {}
                    Column(
                        Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = with(localDensity) { bottomBarHeight.toDp() })
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
                    Surface (tonalElevation = NavigationBarDefaults.Elevation,
                        modifier = Modifier
                            .background(NavigationBarDefaults.containerColor)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .onGloballyPositioned { coord -> bottomBarHeight = coord.size.height }){
                        bottomBar(WindowInsets.ime.add(WindowInsets.navigationBars).asPaddingValues())
                    }
                }
            })
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
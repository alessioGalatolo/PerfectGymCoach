package agdesigns.elevatefitness.ui.navigation

import androidx.compose.animation.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.utils.largeLandscapeDirective
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberSupportingPaneSceneStrategy
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.collections.get


data class PrimaryActionContent(
    val icon: ImageVector,
    val labelId: Int,
    val showLabel: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun RootDestinationGraph(
    navigator: DestinationsNavigator
) {
    val state = rememberNavigationSuiteScaffoldState()

    // history is the only bottom bar route which is allowed to be in a pane,
    // if it happens remove the nav bar
    val showNavBar by remember {
        derivedStateOf {
            navigator.topLevelKey in BOTTOM_BAR_ROUTES
                && navigator.backStack.none { it is WorkoutRecapDestination }
        }
    }
    LaunchedEffect(showNavBar) {
        if (showNavBar) state.show() else state.hide()
    }
    val screenToPrimaryActionContent = remember { mutableStateMapOf<Route, PrimaryActionContent>() }
    val navSuiteType =
        with(currentWindowAdaptiveInfoV2()) {
            when {
                windowSizeClass.minWidthDp == 0 -> NavigationSuiteType.ShortNavigationBarCompact
                windowSizeClass.minHeightDp == 0 -> NavigationSuiteType.ShortNavigationBarMedium
                windowSizeClass.minWidthDp == WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                    || windowSizeClass.minHeightDp >= windowSizeClass.minWidthDp ->
                        NavigationSuiteType.WideNavigationRailCollapsed
                else -> NavigationSuiteType.WideNavigationRailExpanded
            }
        }
    val currentScreen = navigator.backStack.lastOrNull()
    val primaryActionContent = screenToPrimaryActionContent[currentScreen]
    // Snapshot holds the last non-null content so the exit animation renders
    // the FAB icon while it fades out (after navigating away from a FAB screen).
    val snapshotContent = remember { mutableStateOf<PrimaryActionContent?>(null) }
    primaryActionContent?.let { snapshotContent.value = it }

    // FAB overlaps content only for bottom-bar nav types; rail types place it outside content area.
    val fabOverlapHeight = { when (navSuiteType) {
        NavigationSuiteType.NavigationBar,
        NavigationSuiteType.ShortNavigationBarCompact,
        NavigationSuiteType.ShortNavigationBarMedium -> if (snapshotContent.value?.showLabel == true) 56.dp + 16.dp else 80.dp + 16.dp
        else -> 0.dp
    } }

    val innerListDetail = rememberListDetailSceneStrategy<Any>(
        backNavigationBehavior = BackNavigationBehavior.PopUntilContentChange,
        directive = largeLandscapeDirective(currentWindowAdaptiveInfoV2())
    )
    // Only activate the list-detail two-pane layout when a detailPane entry (marked with
    // DETAIL_PANE_METADATA_KEY) is actually in the back stack. Without this guard, any listPane
    // entry shown alone would split the screen with an empty pane on wide layouts.
    val listDetailStrategy = remember(innerListDetail) {
        SceneStrategy { entries: List<NavEntry<Any>> ->
            if (entries.none { it.metadata[DETAIL_PANE_METADATA_KEY] == true }) null
            else with(innerListDetail) { calculateScene(entries) }
        }
    }
    val supportingPaneSceneStrategy = rememberSupportingPaneSceneStrategy<Any>(
        directive = largeLandscapeDirective(currentWindowAdaptiveInfoV2())
    )
    val refreshContentFlow = remember { MutableSharedFlow<Any>(
        replay = 0,      // Number of values replayed to new subscribers
        extraBufferCapacity = 10  // Buffer for slow subscribers
    ) }

    val activity = LocalActivity.current

    NavigationSuiteScaffold(
        state = state,
        navigationSuiteType = navSuiteType,
        navigationItems = {
            BOTTOM_BAR_ROUTES.forEach { destination ->
                val selected = destination == navigator.topLevelKey
                NavigationSuiteItem(
                    navigationSuiteType = navSuiteType,
                    icon = {
                        if (selected)
                            Icon(
                                destination.iconSelected,
                                contentDescription = stringResource(destination.label) +
                                        stringResource(R.string.bottom_bar_current)
                            )
                        else
                            Icon(
                                destination.icon,
                                contentDescription = stringResource(destination.label)
                            )
                    },
                    label = { Text(stringResource(destination.label)) },
                    selected = selected,
                    onClick = {
                        if (selected) {
                            navigator.navigateUpTo(destination)
                        } else {
                            navigator.navigate(destination)
                        }
                        if (navigator.backStack.lastOrNull() == destination) {
                            refreshContentFlow.tryEmit(destination)
                        }
                    }
                )
            }
        },
        primaryActionContent = {
            AnimatedVisibility(
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec())
                        + scaleIn(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                        scaleOut(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                visible = primaryActionContent != null
            ) {
                val content = snapshotContent.value ?: return@AnimatedVisibility
                when (navSuiteType) {
                    NavigationSuiteType.NavigationBar,
                    NavigationSuiteType.ShortNavigationBarCompact,
                    NavigationSuiteType.ShortNavigationBarMedium -> {
                        if (!content.showLabel) {
                            MediumFloatingActionButton(
                                onClick = { content.onClick() },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    imageVector = content.icon,
                                    contentDescription = stringResource(content.labelId),
                                    modifier = Modifier.size(
                                        FloatingActionButtonDefaults.MediumIconSize
                                    )
                                )
                            }
                        } else {
                            ExtendedFloatingActionButton(
                                text = { Text(stringResource(content.labelId)) },
                                icon = {
                                    Icon(
                                        imageVector = content.icon,
                                        contentDescription = stringResource(content.labelId),
                                        modifier = Modifier.size(
                                            FloatingActionButtonDefaults.MediumIconSize
                                        )
                                    )
                                },
                                expanded = true,
                                onClick = { content.onClick() },
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    NavigationSuiteType.WideNavigationRailCollapsed -> {
                        // Bigger fabs go out of bounds
                        FloatingActionButton(
                            onClick = { content.onClick() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.navigation_rail_item_padding))
                        ) {
                            Icon(
                                imageVector = content.icon,
                                contentDescription = stringResource(content.labelId),
                                modifier = Modifier.size(
                                    FloatingActionButtonDefaults.MediumIconSize
                                )
                            )
                            if (content.showLabel) {
                                Text(stringResource(content.labelId))
                            }
                        }
                    }
                    NavigationSuiteType.WideNavigationRailExpanded -> {
                        ExtendedFloatingActionButton(
                            onClick = { content.onClick() },
                            icon = {
                                Icon(
                                    imageVector = content.icon,
                                    contentDescription = null,
                                )
                            },
                            text = { Text(stringResource(content.labelId)) },
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.navigation_rail_item_padding))
                        )
                    }
                    else -> {}
                }
            }
        },
        primaryActionContentHorizontalAlignment = Alignment.End
    ) {
        SharedTransitionLayout {
            NavDisplay(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
                backStack = navigator.backStack,
                onBack = { if (navigator.navigateUp()) activity?.finish() },
                sceneStrategies = listOf(
                    supportingPaneSceneStrategy,
                    listDetailStrategy
                ),
                entryProvider = entryProvider {
                    bottomBarEntryBuilder(
                        navigator,
                        setPrimaryAction = { source, content ->
                            screenToPrimaryActionContent[source] = content
                        },
                        refreshContentFlow = refreshContentFlow,
                        fabOverlapHeight = fabOverlapHeight,
                    )
                    deepScreensEntryBuilder(
                        navigator,
                        setPrimaryAction = { source, content ->
                            screenToPrimaryActionContent[source] = content
                        },
                        fabOverlapHeight = fabOverlapHeight,
                    )
                    workoutScreenEntryBuilder(navigator)
                },
                entryDecorators = listOf(
                    // Add the default decorators for managing scenes and saving state
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // Then add the view model store decorator
                    rememberViewModelStoreNavEntryDecorator()
                ),
                sharedTransitionScope = this,
                transitionSpec = {
                    fadeIn(MotionScheme.expressive().slowEffectsSpec()) togetherWith
                            ExitTransition.None
                },
                popTransitionSpec = {
                    EnterTransition.None togetherWith fadeOut(
                        MotionScheme.expressive().slowEffectsSpec()
                    )
                },
                predictivePopTransitionSpec = {
                    EnterTransition.None togetherWith fadeOut(
                        MotionScheme.expressive().slowEffectsSpec()
                    )
                }
            )
        }
    }
}
package agdesigns.elevatefitness.navigation

import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
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
import kotlinx.coroutines.flow.MutableSharedFlow


data class PrimaryActionContent(
    val icon: ImageVector,
    val labelId: Int,
    val onClick: () -> Unit
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun RootDestinationGraph(startDestination: Any) {
    val state = rememberNavigationSuiteScaffoldState()
    val navigationViewModel = hiltViewModel<NavigationViewModel, NavigationViewModel.Factory>(
        creationCallback = { factory -> factory.create(startDestination) }
    )
    val destinationsNavigator = navigationViewModel.navigator
    // history is the only bottom bar route which is allowed to be in a pane,
    // if it happens remove the nav bar
    val showNavBar by remember {
        derivedStateOf {
            destinationsNavigator.topLevelKey in BOTTOM_BAR_ROUTES
                && destinationsNavigator.backStack.none { it is WorkoutRecapDestination }
        }
    }
    LaunchedEffect(showNavBar) {
        if (showNavBar) state.show() else state.hide()
    }
    val primaryActionContent = remember { mutableStateOf<PrimaryActionContent?>(null) }
    // whose action it is
    val primaryActionOrigin = remember { mutableStateOf<Any?>(null) }
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
    NavigationSuiteScaffold(
        state = state,
        navigationSuiteType = navSuiteType,
        navigationItems = {
            BOTTOM_BAR_ROUTES.forEach { destination ->
                val selected = destination == destinationsNavigator.topLevelKey
                NavigationSuiteItem(
                    navigationSuiteType = navSuiteType,
                    icon = {
                        if (selected)
                            Icon(
                                destination.iconSelected,
                                contentDescription = stringResource(destination.label) + " (current)" // FIXME: locale
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
                        destinationsNavigator.navigateUpTo(destination)
                        if (destinationsNavigator.backStack.last() == destination) {
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
                visible = primaryActionOrigin.value == destinationsNavigator.topLevelKey
            ) {
                if (primaryActionContent.value == null) {
                    return@AnimatedVisibility
                }
                when (navSuiteType) {
                    NavigationSuiteType.NavigationBar,
                    NavigationSuiteType.ShortNavigationBarCompact,
                    NavigationSuiteType.ShortNavigationBarMedium -> {
                        MediumFloatingActionButton(
                            onClick = { primaryActionContent.value?.onClick() },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = primaryActionContent.value?.icon ?: Icons.Default.AcUnit,
                                contentDescription = primaryActionContent.value?.labelId?.let {
                                    stringResource(it)
                                },
                                modifier = Modifier.size(
                                    FloatingActionButtonDefaults.MediumIconSize
                                )
                            )
                        }
                    }
                    NavigationSuiteType.WideNavigationRailCollapsed -> {
                        // Bigger fabs go out of bounds
                        FloatingActionButton(
                            onClick = { primaryActionContent.value?.onClick() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.navigation_rail_item_padding))
                        ) {
                            Icon(
                                imageVector = primaryActionContent.value?.icon
                                    ?: Icons.Default.AcUnit,
                                contentDescription = primaryActionContent.value?.labelId?.let {
                                    stringResource(it)
                                },
                                modifier = Modifier.size(
                                    FloatingActionButtonDefaults.MediumIconSize
                                )
                            )
                        }
                    }
                    NavigationSuiteType.WideNavigationRailExpanded -> {
                        ExtendedFloatingActionButton(
                            onClick = { primaryActionContent.value?.onClick() },
                            icon = {
                                Icon(
                                    imageVector = primaryActionContent.value?.icon ?: Icons.Default.AcUnit,
                                    contentDescription = null,
                                )
                            },
                            text = {
                                if (primaryActionContent.value != null) {
                                    Text(
                                        stringResource(
                                            primaryActionContent.value?.labelId ?: R.string.app_name
                                        )
                                    )
                                }
                            },
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
                backStack = destinationsNavigator.backStack,
                onBack = { destinationsNavigator.navigateUp() },
                sceneStrategies = listOf(
                    supportingPaneSceneStrategy,
                    listDetailStrategy
                ),
                entryProvider = entryProvider {
                    bottomBarEntryBuilder(destinationsNavigator, primaryActionOrigin, primaryActionContent, refreshContentFlow)
                    deepScreensEntryBuilder(destinationsNavigator)
                    workoutScreenEntryBuilder(destinationsNavigator)
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
package agdesigns.elevatefitness.navigation

import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.ui.screens.workout.Workout
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass


class DestinationsNavigator(startKey: Any) {

    // Maintain a stack for each top level route
    private var topLevelStacks : LinkedHashMap<Any, SnapshotStateList<Any>> = linkedMapOf(
        startKey to mutableStateListOf(startKey)
    )

    // Expose the current top level route for consumers
    var topLevelKey by mutableStateOf(startKey)
        private set

    // Expose the back stack so it can be rendered by the NavDisplay
    val backStack = mutableStateListOf(startKey)

    private fun updateBackStack() =
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }

    private fun addTopLevel(key: Any){
        // If the top level doesn't exist, add it
        if (topLevelStacks[key] == null){
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            // Otherwise just move it to the end of the stacks
            topLevelStacks.apply {
                remove(key)?.let {
                    put(key, it)
                }
            }
        }
        topLevelKey = key
        updateBackStack()
    }

    fun navigate(key: Any){
        if (key is TopLevelRoute) {
            addTopLevel(key)
            return
        }
        topLevelStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    fun navigateUp(){
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        // If the removed key was a top level key, remove the associated top level stack
        topLevelStacks.remove(removedKey)
        topLevelKey = topLevelStacks.keys.last()
        updateBackStack()
    }
}

data class PrimaryActionContent(
    val icon: ImageVector,
    val labelId: Int,
    val onClick: () -> Unit
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun RootDestinationGraph(startDestination: Any) {
    val state = rememberNavigationSuiteScaffoldState()
    val destinationsNavigator = remember { DestinationsNavigator(startDestination) }
    LaunchedEffect(destinationsNavigator.topLevelKey) {
        if (destinationsNavigator.topLevelKey in BOTTOM_BAR_ROUTES)
            state.show()
        else
            state.hide()
    }
    val primaryActionContent = remember { mutableStateOf<PrimaryActionContent?>(null) }
    // whose action it is
    val primaryActionOrigin = remember { mutableStateOf<Any?>(null) }
    val navSuiteType =
        with(currentWindowAdaptiveInfo()) {
            Log.d("RootGraph", "Window class is $windowSizeClass")
            when {
                windowSizeClass.minWidthDp == 0 -> NavigationSuiteType.ShortNavigationBarCompact
                windowSizeClass.minHeightDp == 0 -> NavigationSuiteType.ShortNavigationBarMedium
                windowSizeClass.minWidthDp == WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                    || windowSizeClass.minHeightDp >= windowSizeClass.minWidthDp ->
                        NavigationSuiteType.WideNavigationRailCollapsed
                else -> NavigationSuiteType.WideNavigationRailExpanded
            }
        }
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
                        destinationsNavigator.navigate(destination)
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
                backStack = destinationsNavigator.backStack,
                onBack = { destinationsNavigator.navigateUp() },
                entryProvider = entryProvider {
                    bottomBarEntryBuilder(destinationsNavigator, primaryActionOrigin, primaryActionContent)
                    deepScreensEntryBuilder(destinationsNavigator)
                    workoutScreenEntryBuilder(destinationsNavigator)
                },
                entryDecorators = listOf(
                    // Add the default decorators for managing scenes and saving state
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // Then add the view model store decorator
                    rememberViewModelStoreNavEntryDecorator()
                ),
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
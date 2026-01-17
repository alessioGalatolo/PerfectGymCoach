package agdesigns.elevatefitness.navigation

import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.ui.screens.history.History
import agdesigns.elevatefitness.ui.screens.home.Home
import agdesigns.elevatefitness.ui.screens.profile.Profile
import agdesigns.elevatefitness.ui.screens.statistics.Statistics
import agdesigns.elevatefitness.ui.screens.workout.Workout
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

private sealed interface TopLevelRoute {
//    val label: Int
//    val icon: ImageVector
//    val iconSelected: ImageVector
}

private sealed interface BottomBarDestination: TopLevelRoute {
    val label: Int
    val icon: ImageVector
    val iconSelected: ImageVector
}


private data object Home: BottomBarDestination {
    override val label = R.string.home
    override val icon = Icons.Outlined.Home
    override val iconSelected = Icons.Filled.Home
}

private data object History: BottomBarDestination {
    override val label = R.string.history
    override val icon = Icons.Outlined.History
    override val iconSelected = Icons.Filled.History
}

private data object Statistics: BottomBarDestination {
    override val label = R.string.statistics
    override val icon = Icons.Outlined.Analytics
    override val iconSelected = Icons.Filled.Analytics
}

private data object Profile: BottomBarDestination {
    override val label = R.string.profile
    override val icon = Icons.Outlined.Person
    override val iconSelected = Icons.Filled.Person
}

data class WorkoutRoute (
    val programId: Long,
    val previewExercise: ProgramExerciseAndInfo? = null,
    val quickStart: Boolean = false,
    val resumeWorkout: Boolean = false
): TopLevelRoute

private val BOTTOM_BAR_ROUTES : List<BottomBarDestination> = listOf(
    Home,
    History,
    Statistics,
    Profile
)


class TopLevelBackStack<T: Any>(startKey: T) {

    // Maintain a stack for each top level route
    private var topLevelStacks : LinkedHashMap<T, SnapshotStateList<T>> = linkedMapOf(
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

    private fun addTopLevel(key: T){
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

    fun add(key: T){
        if (key is TopLevelRoute) {
            addTopLevel(key)
            return
        }
        topLevelStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    fun removeLast(){
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        // If the removed key was a top level key, remove the associated top level stack
        topLevelStacks.remove(removedKey)
        topLevelKey = topLevelStacks.keys.last()
        updateBackStack()
    }
}

@Destination<RootGraph>(start=true, style = FadeTransition::class)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun RootDestinationGraph(){

    val navSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    val state = rememberNavigationSuiteScaffoldState()
    val topLevelBackStack = remember { TopLevelBackStack<Any>(Home) }
    LaunchedEffect(topLevelBackStack.topLevelKey) {
        if (topLevelBackStack.topLevelKey in BOTTOM_BAR_ROUTES)
            state.show()
        else
            state.hide()
    }
    var primaryActionContent by remember { mutableStateOf<(@Composable () -> Unit)>({ }) }
    NavigationSuiteScaffold(
        state = state,
        navigationItems = {
            BOTTOM_BAR_ROUTES.forEach { destination ->
                val selected = destination == topLevelBackStack.topLevelKey
                NavigationSuiteItem(
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
                        topLevelBackStack.add(destination)
                    }
                )
            }
        },
        primaryActionContent = primaryActionContent
    ) {
        SharedTransitionLayout {
            NavDisplay(
                backStack = topLevelBackStack.backStack,
                onBack = { topLevelBackStack.removeLast() },
                entryProvider = entryProvider {
                    entry<Home> {
                        Home(
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                            backStack = topLevelBackStack,
                            changePrimaryAction = { primaryActionContent = it }
                        )
                    }
                    entry<History> {
                        History(
                            backStack = topLevelBackStack
                        )
                    }
                    entry<Statistics> {
                        Statistics(
                            backStack = topLevelBackStack
                        )
                    }
                    entry<Profile> {
                        Profile(
                            backStack = topLevelBackStack
                        )
                    }
                    entry<WorkoutRoute> {
                        Workout(
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                            backStack = topLevelBackStack,
                            programId = it.programId,
                            previewExercise = it.previewExercise,
                            quickStart = it.quickStart,
                            resumeWorkout = it.resumeWorkout
                        )
                    }
                }
            )
        }
    }
}
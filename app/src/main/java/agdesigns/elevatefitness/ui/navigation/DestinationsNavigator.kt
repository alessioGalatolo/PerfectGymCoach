package agdesigns.elevatefitness.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.MutableStateFlow

// used when navigating back with a result
class ResultKey<T>(val id: String)

open class DestinationsNavigator(private val startKey: TopLevelRoute) {

    private val _results = mutableMapOf<String, MutableStateFlow<Any?>>()

    // Maintain a stack for each top level route
    private var topLevelStacks : LinkedHashMap<TopLevelRoute, SnapshotStateList<Route>> = linkedMapOf(
        startKey to mutableStateListOf(startKey)
    )

    // Expose the current top level route for consumers
    var topLevelKey by mutableStateOf(startKey)
        private set

    // All the entries of every top level stack, used to retain the state of stacks that are not
    // currently visible (e.g. other bottom bar destinations)
    val backStack: SnapshotStateList<Route> = mutableStateListOf(startKey)

    // The entries that navigateUp would actually go through, i.e. what the NavDisplay should show
    // (and preview on predictive back). Popping the root of a bottom bar destination always goes
    // back to the startKey, so stacks sitting in between are not part of it.
    val visibleBackStack: SnapshotStateList<Route> = mutableStateListOf(startKey)

    private fun updateBackStack() {
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }
        visibleBackStack.apply {
            clear()
            addAll(computeVisibleBackStack())
        }
    }

    // Mirrors the behaviour of navigateUp
    private fun computeVisibleBackStack(): List<Route> {
        val keys = topLevelStacks.keys.toList()
        val visible = ArrayDeque<Route>()
        for (key in keys.asReversed()) {
            visible.addAll(0, topLevelStacks[key].orEmpty())
            if (key == startKey) break
            if (key is BottomBarDestination) {
                // navigateUp re-creates the start stack if it is missing
                visible.addAll(0, topLevelStacks[startKey] ?: listOf(startKey))
                break
            }
        }
        return visible
    }

    private fun addTopLevel(key: TopLevelRoute){
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

    open fun navigate(key: Route){
        if (key is NoDestination) {
            return
        }
        if (key is TopLevelRoute) {
            addTopLevel(key)
            return
        }
        val stack = topLevelStacks[topLevelKey] ?: return
        if (stack.lastOrNull()?.let { it::class == key::class } == true) {
            stack[stack.lastIndex] = key
        } else {
            stack.add(key)
        }
        updateBackStack()
    }

    open fun navigateUp(): Boolean {
        val currentStack = topLevelStacks[topLevelKey]
        if (topLevelKey == startKey && (currentStack == null || currentStack.size <= 1)) {
            return true // caller should finish the activity
        }
        val removedKey = currentStack?.removeLastOrNull()
        // If the removed key was a top level key, remove the associated top level stack
        if (removedKey is TopLevelRoute) {
            topLevelStacks.remove(removedKey)
        }
        if (removedKey is BottomBarDestination) {
            // go back to startKey, if available
            // in order to keep the right sorting we need to pop and re-enter the startKey
            addTopLevel(startKey)
        }
        topLevelKey = topLevelStacks.keys.lastOrNull() ?: return false
        updateBackStack()
        return false
    }

    /**
     * Pops the current top-level stack (e.g. WorkoutDestination) and navigates to a bottom bar
     * destination, resetting that destination's back stack.
     */
    fun popAndNavigateToBottomBar(destination: BottomBarDestination) {
        topLevelStacks.remove(topLevelKey)
        topLevelStacks.remove(destination)
        addTopLevel(destination)
    }

    /**
     * Goes back in the back stack until the given `destination` is found
     *
     * @param override: if true it will override the existing destination with the given one
     */
    fun navigateUpTo(destination: Route, override: Boolean = false) {
        if (destination is TopLevelRoute) {
            topLevelStacks.remove(destination)
            navigate(destination)
            return
        }
        val stack = topLevelStacks[topLevelKey]
        val index = stack?.indexOfLast { it::class == destination::class } ?: -1
        if (stack == null || index == -1) {
            // not in the stack: don't pop anything (it would also pop the top-level root)
            navigate(destination)
            return
        }
        while (stack.lastIndex > index) {
            stack.removeAt(stack.lastIndex)
        }
        if (override) {
            // replaces the entry since it has the same class
            navigate(destination)
        } else {
            updateBackStack()
        }
    }

    fun <T> navigateUpToWithResult(
        destination: Route,
        resultKey: ResultKey<T>,
        result: T,
        override: Boolean = false
    ) {
        _results.getOrPut(resultKey.id) { MutableStateFlow(null) }.value = result
        navigateUpTo(destination, override)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> observeResult(resultKey: ResultKey<T>): MutableStateFlow<T?> =
        _results.getOrPut(resultKey.id) { MutableStateFlow(null) } as MutableStateFlow<T?>

    @Suppress("UNCHECKED_CAST")
    fun <T> consumeResult(resultKey: ResultKey<T>): T? =
        _results.remove(resultKey.id)?.value as T?

}
package agdesigns.elevatefitness.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.MutableStateFlow

// used when navigating back with a result
class ResultKey<T>(val id: String)

open class DestinationsNavigator(startKey: Route) {

    private val _results = mutableMapOf<String, MutableStateFlow<Any?>>()

    // Maintain a stack for each top level route
    private var topLevelStacks : LinkedHashMap<Route, SnapshotStateList<Route>> = linkedMapOf(
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

    private fun addTopLevel(key: Route){
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

    open fun navigateUp() {
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        // If the removed key was a top level key, remove the associated top level stack
        topLevelStacks.remove(removedKey)
        topLevelKey = topLevelStacks.keys.lastOrNull() ?: return
        updateBackStack()
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
        var removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        while (removedKey != null && removedKey::class != destination::class) {
            removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        }
        if (removedKey != null && !override) {
            navigate(removedKey)
        } else {
            navigate(destination)
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
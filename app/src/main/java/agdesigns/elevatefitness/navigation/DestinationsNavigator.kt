package agdesigns.elevatefitness.navigation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

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

    fun navigateUp() {
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        // If the removed key was a top level key, remove the associated top level stack
        topLevelStacks.remove(removedKey)
        // FIXME: NoSuchElementException: Collection is empty. when closing workout recap after workout
        topLevelKey = topLevelStacks.keys.last()
        updateBackStack()
    }

    fun navigateUpTo(destination: BottomBarDestination) {
        topLevelStacks.remove(destination)
        navigate(destination)
    }
}
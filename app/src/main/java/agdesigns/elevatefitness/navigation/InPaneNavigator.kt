package agdesigns.elevatefitness.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * A navigator used inside DoublePaneWorkout's right pane. Destinations that belong in the pane
 * (ExercisesByMuscle, ViewExercises, AddExerciseDialog, CreateExerciseDialog) are pushed onto
 * [rightPaneStack] instead of the global back stack. All other destinations (e.g. ExerciseStats)
 * are forwarded to [realNavigator] and open full-screen as normal.
 *
 * [navigateVersion] increments on every [navigate] call so callers can use it as a ViewModel key
 * discriminator to force a fresh ViewModel for each new in-pane navigation.
 */
class InPaneNavigator(
    private val rightPaneStack: SnapshotStateList<Any>,
    private val realNavigator: DestinationsNavigator,
) : DestinationsNavigator(HomeDestination) {

    var navigateVersion by mutableIntStateOf(0)
        private set

    private fun isInPaneDestination(key: Any) =
        key is ExercisesByMuscleDestination
                || key is ViewExercisesDestination
                || key is AddExerciseDialogDestination
                || key is CreateExerciseDialogDestination
                || key is ExerciseStatsDestination

    override fun navigate(key: Route) {
        navigateVersion++
        if (isInPaneDestination(key)) {
            rightPaneStack.add(key)
        } else {
            realNavigator.navigate(key)
        }
    }

    override fun navigateUp() {
        if (rightPaneStack.isNotEmpty()) {
            rightPaneStack.removeAt(rightPaneStack.lastIndex)
        }
        // If the stack is empty the BackHandler in DoublePaneWorkout is disabled,
        // so the system back press will be handled by the real navigator.
    }

    fun popAllRightPanes() {
        while (rightPaneStack.isNotEmpty()) {
            rightPaneStack.removeAt(rightPaneStack.lastIndex)
        }
    }
}

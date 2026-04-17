package agdesigns.elevatefitness.navigation

import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.ui.screens.workout.Workout
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope

sealed interface Route

sealed interface TopLevelRoute: Route

data class WorkoutDestination (
    val programId: Long,
    val previewExercise: ProgramExerciseAndInfo? = null,
    val quickStart: Boolean = false,
    val resumeWorkout: Boolean = false
): TopLevelRoute

context(sharedTransitionScope: SharedTransitionScope)
fun EntryProviderScope<Any>.workoutScreenEntryBuilder(
    navigator: DestinationsNavigator
) {
    with (sharedTransitionScope) {
        entry<WorkoutDestination>(metadata = FadeTransition) {
            // TODO: this is the suggested approach but doesn't seem necessary?
//            val viewModel = hiltViewModel<WorkoutViewModel, WorkoutViewModel.Factory>(
//                // Note: We need a new ViewModel for every new RouteB instance. Usually
//                // we would need to supply a `key` String that is unique to the
//                // instance, however, the ViewModelStoreNavEntryDecorator (supplied
//                // above) does this for us, using `NavEntry.contentKey` to uniquely
//                // identify the viewModel.
//                //
//                // tl;dr: Make sure you use rememberViewModelStoreNavEntryDecorator()
//                // if you want a new ViewModel for each new navigation key instance.
//                creationCallback = { factory ->
//                    factory.create(it)
//                }
//            )
            Workout(
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                navigator = navigator,
                programId = it.programId,
                previewExercise = it.previewExercise,
                quickStart = it.quickStart,
                resumeWorkout = it.resumeWorkout,
//                viewModel = viewModel
            )
        }
    }
}

package agdesigns.elevatefitness.navigation

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanGoal
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanSplit
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.screens.add_exercise.AddExerciseDialog
import agdesigns.elevatefitness.ui.screens.create_exercise.CreateExerciseDialog
import agdesigns.elevatefitness.ui.screens.plans.AddWorkoutPlan
import agdesigns.elevatefitness.ui.screens.plans.ArchivedPlans
import agdesigns.elevatefitness.ui.screens.plans.CustomizePlanGeneration
import agdesigns.elevatefitness.ui.screens.plans.ViewGeneratedPlan
import agdesigns.elevatefitness.ui.screens.program_exercises.AddProgramExercise
import agdesigns.elevatefitness.ui.screens.programs.AddProgram
import agdesigns.elevatefitness.ui.screens.statistics.ExerciseStats
import agdesigns.elevatefitness.ui.screens.view_exercises.ExercisesByMuscle
import agdesigns.elevatefitness.ui.screens.view_exercises.ViewExercises
import agdesigns.elevatefitness.ui.screens.workout_recap.WorkoutRecap
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope

data class AddProgramDestination(
    val planId: Long,
    val openDialogNow: Boolean = false
): TopLevelRoute

data class AddWorkoutPlanDestination(
    val openDialogNow: Boolean = false,
)


data object CustomizePlanGenerationDestination

data class AddProgramExerciseDestination(
    val programName: String,
    val programId: Long
)

data class ExerciseStatsDestination(
    val exerciseId: Long
)

data class ViewExercisesDestination(
    val programId: Long = 0L,
    val workoutId: Long = 0L,
    val muscleOrdinal: Int,
    val focusSearch: Boolean = false,
    val programName: String = "",
    val returnAfterAdding: Boolean = false
)

data class AddExerciseDialogDestination(
    val previewExercise: Exercise,
    val programId: Long = 0L, // programId != 0L means we are adding an exercise to a program (and maybe a current workout)
    val workoutId: Long = 0L, // workoutId != 0L we're adding to a ongoing workout (and maybe a program)
    val programExerciseId: Long = 0L,  // != 0L if we are changing an existing exercise
    val programName: String = "",
    val returnAfterAdding: Boolean = false,  // if adding a single exercise to workout, return to workout instead of program
    val continueAdding: Boolean = true,
)

data class CreateExerciseDialogDestination(
    val muscleOrdinal: Int = 0,
    val filterEquipment: Equipment? = null,
)

data class ExercisesByMuscleDestination(
    val programName: String,
    val programId: Long = 0,
    val workoutId: Long = 0,
    val successfulAddExercise: Boolean = false,
    val returnAfterAdding: Boolean = false
)

data class ViewGeneratedPlanDestination(
    val goalChoice: WorkoutPlanGoal,
    val expertiseLevel: WorkoutPlanDifficulty,
    val workoutSplit: WorkoutPlanSplit,
)

data object ArchivedPlansDestination

data class WorkoutRecapDestination(
    val workoutId: Long
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
context(sharedTransitionScope: SharedTransitionScope)
fun EntryProviderScope<Any>.deepScreensEntryBuilder(
    navigator: DestinationsNavigator
) {
    with (sharedTransitionScope) {
        entry<AddProgramDestination>(metadata = SlideTransition + ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                EmptyScreenInfo(
                    Icons.Default.Description,
                    R.string.no_programs_selected,
                    titleRes = R.string.no_programs_selected,
                    subtitleRes = R.string.no_programs_selected_info
                )
            }
        )) {
            AddProgram(
                navigator = navigator,
                planId = it.planId,
                openDialogNow = it.openDialogNow
            )
        }
        entry<AddWorkoutPlanDestination>(metadata = SlideTransition) {
            AddWorkoutPlan(
                navigator = navigator,
                openDialogNow = it.openDialogNow
            )
        }
        entry<CustomizePlanGenerationDestination>(metadata = FullscreenDialogTransition) {
            CustomizePlanGeneration(
                navigator = navigator
            )
        }
        entry<AddProgramExerciseDestination>(metadata = SlideTransition + ListDetailSceneStrategy.detailPane()) {
            AddProgramExercise(
                navigator = navigator,
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                programName = it.programName,
                programId = it.programId
            )
        }
        entry<ExerciseStatsDestination>(metadata = SlideTransition) {
            ExerciseStats(
                navigator = navigator,
                exerciseId = it.exerciseId
            )
        }
        entry<ViewExercisesDestination>(metadata = SlideTransition + ListDetailSceneStrategy.extraPane()) {
            ViewExercises(
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                navigator = navigator,
                programId = it.programId,
                workoutId = it.workoutId,
                muscleOrdinal = it.muscleOrdinal,
                focusSearch = it.focusSearch,
                programName = it.programName,
                returnAfterAdding = it.returnAfterAdding
            )
        }
        entry<AddExerciseDialogDestination>(metadata = ListDetailSceneStrategy.extraPane()) {
            AddExerciseDialog(
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                navigator = navigator,
                previewExercise = it.previewExercise,
                programId = it.programId,
                workoutId = it.workoutId,
                programExerciseId = it.programExerciseId,
                programName = it.programName,
                returnAfterAdding = it.returnAfterAdding,
                continueAdding = it.continueAdding
            )
        }
        entry<CreateExerciseDialogDestination>(metadata = FullscreenDialogTransition + ListDetailSceneStrategy.extraPane()) {
            CreateExerciseDialog(
                navigator = navigator,
                muscleOrdinal = it.muscleOrdinal,
                filterEquipment = it.filterEquipment
            )
        }
        entry<ExercisesByMuscleDestination>(metadata = SlideTransition + ListDetailSceneStrategy.extraPane()) {
            ExercisesByMuscle(
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                navigator = navigator,
                programName = it.programName,
                programId = it.programId,
                workoutId = it.workoutId,
                successfulAddExercise = it.successfulAddExercise,
                returnAfterAdding = it.returnAfterAdding
            )
        }
        entry<ViewGeneratedPlanDestination>(metadata = SlideTransition) {
            ViewGeneratedPlan(
                navigator = navigator,
                goalChoice = it.goalChoice,
                expertiseLevel = it.expertiseLevel,
                workoutSplit = it.workoutSplit
            )
        }
        entry<ArchivedPlansDestination>(metadata = SlideTransition) {
            ArchivedPlans(
                navigator = navigator
            )
        }
        entry<WorkoutRecapDestination>(metadata = FullscreenDialogTransition) {
            WorkoutRecap(
                navigator = navigator,
                workoutId = it.workoutId
            )
        }
    }
}
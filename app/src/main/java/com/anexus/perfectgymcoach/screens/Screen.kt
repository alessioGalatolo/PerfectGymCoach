package com.anexus.perfectgymcoach.screens

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.anexus.perfectgymcoach.R

sealed interface Screen{
    val route: String
    val resourceId: Int
}

sealed class MainScreen(override val route: String,
                        @StringRes override val resourceId: Int) : Screen {
    object Main : MainScreen("main", R.string.main)
    object Workout : MainScreen("workout", R.string.workout)
    object ChangePlan : MainScreen("change_plan", R.string.change_plan)
    object AddProgram : MainScreen("add_program", R.string.add_program)
    object AddExercise : MainScreen("add_exercise", R.string.add_exercise)
    object ExercisesByMuscle : MainScreen("exercises_by_muscle", R.string.exercises_by_muscle)
    object ViewExercises : MainScreen("view_exercises", R.string.view_exercises)
}

sealed class NavigationScreen(
    override val route: String,
    @StringRes override val resourceId: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector) : Screen{
    // TODO: use R8 / ProGuard to remove unused icons from your application.

    object Home : NavigationScreen("home", R.string.home, Icons.Outlined.Home, Icons.Filled.Home)
    object History : NavigationScreen("history", R.string.history, Icons.Outlined.History, Icons.Filled.History)
    object Statistics : NavigationScreen("statistics", R.string.statistics, Icons.Outlined.Analytics, Icons.Filled.Analytics)
    object Profile : NavigationScreen("profile", R.string.profile, Icons.Outlined.Person, Icons.Filled.Person)
}
package com.anexus.perfectgymcoach.screens

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
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
    object Program : MainScreen("program", R.string.program)
    object ChangePlan : MainScreen("change_plan", R.string.change_plan)
}

sealed class NavigationScreen(
    override val route: String,
    @StringRes override val resourceId: Int,
    val icon: ImageVector) : Screen{
    // TODO: use R8 / ProGuard to remove unused icons from your application.

    object Home : NavigationScreen("home", R.string.home, Icons.Outlined.Home)
    object History : NavigationScreen("history", R.string.history, Icons.Outlined.History)
    object Statistics : NavigationScreen("statistics", R.string.statistics, Icons.Outlined.Analytics)
    object Profile : NavigationScreen("profile", R.string.profile, Icons.Outlined.Person)
}
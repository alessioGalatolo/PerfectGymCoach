package com.anexus.perfectgymcoach.ui

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
import com.anexus.perfectgymcoach.ui.screens.destinations.HistoryDestination
import com.anexus.perfectgymcoach.ui.screens.destinations.HomeDestination
import com.anexus.perfectgymcoach.ui.screens.destinations.ProfileDestination
import com.anexus.perfectgymcoach.ui.screens.destinations.StatisticsDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class BottomBarDestination(
    val direction: DirectionDestinationSpec,
    @StringRes val label: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector
) {
    Home(HomeDestination, R.string.home, Icons.Outlined.Home, Icons.Filled.Home),
    History(HistoryDestination, R.string.history, Icons.Outlined.History, Icons.Filled.History),
    Statistics(StatisticsDestination, R.string.statistics, Icons.Outlined.Analytics, Icons.Filled.Analytics),
    Profile(ProfileDestination, R.string.profile, Icons.Outlined.Person, Icons.Filled.Person)
}
package agdesigns.elevatefitness.navigation

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.ui.screens.history.History
import agdesigns.elevatefitness.ui.screens.home.Home
import agdesigns.elevatefitness.ui.screens.profile.Profile
import agdesigns.elevatefitness.ui.screens.statistics.Statistics
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope

sealed interface BottomBarDestination: TopLevelRoute {
    val label: Int
    val icon: ImageVector
    val iconSelected: ImageVector
}


data object HomeDestination: BottomBarDestination {
    override val label = R.string.home
    override val icon = Icons.Outlined.Home
    override val iconSelected = Icons.Filled.Home
}

data object HistoryDestination: BottomBarDestination {
    override val label = R.string.history
    override val icon = Icons.Outlined.History
    override val iconSelected = Icons.Filled.History
}

data object StatisticsDestination: BottomBarDestination {
    override val label = R.string.statistics
    override val icon = Icons.Outlined.Analytics
    override val iconSelected = Icons.Filled.Analytics
}

data object ProfileDestination: BottomBarDestination {
    override val label = R.string.profile
    override val icon = Icons.Outlined.Person
    override val iconSelected = Icons.Filled.Person
}

context(sharedTransitionScope: SharedTransitionScope)
fun EntryProviderScope< Any>.bottomBarEntryBuilder(
    destinationsNavigator: DestinationsNavigator,
    primaryActionOrigin: MutableState<Any?>,
    primaryActionContent: MutableState<PrimaryActionContent?>,
) {
    with (sharedTransitionScope) {
        entry<HomeDestination>(metadata = FadeTransition) {
            Home(
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                navigator = destinationsNavigator,
                changePrimaryActionContent = {
                    primaryActionOrigin.value = HomeDestination
                    primaryActionContent.value = it
                }
            )
        }
        entry<HistoryDestination>(metadata = FadeTransition) {
            History(
                navigator = destinationsNavigator
            )
        }
        entry<StatisticsDestination>(metadata = FadeTransition) {
            Statistics(
                navigator = destinationsNavigator
            )
        }
        entry<ProfileDestination>(metadata = FadeTransition) {
            Profile(
                navigator = destinationsNavigator
            )
        }
    }
}

val BOTTOM_BAR_ROUTES : List<BottomBarDestination> = listOf(
    HomeDestination,
    HistoryDestination,
    StatisticsDestination,
    ProfileDestination
)
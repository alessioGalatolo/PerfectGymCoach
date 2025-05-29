package agdesigns.elevatefitness.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationStyle

object FadeTransition : DestinationStyle.Animated() {

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        fadeIn()
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        fadeOut()
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        fadeIn()
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        fadeOut()
    }
}

object FullscreenDialogTransition : DestinationStyle.Animated() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn()
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut()
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn()
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut()
    }
}

object SlideTransition : DestinationStyle.Animated() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn()
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        fadeOut()
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        fadeIn()
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut()
    }
}


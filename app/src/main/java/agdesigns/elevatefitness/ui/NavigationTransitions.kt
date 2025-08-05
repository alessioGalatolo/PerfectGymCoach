package agdesigns.elevatefitness.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object FadeTransition : DestinationStyle.Animated() {

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        fadeIn(MotionScheme.expressive().defaultEffectsSpec())
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        fadeOut(MotionScheme.expressive().defaultEffectsSpec())
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        fadeIn(MotionScheme.expressive().defaultEffectsSpec())
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        fadeOut(MotionScheme.expressive().defaultEffectsSpec())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object FullscreenDialogTransition : DestinationStyle.Animated() {

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Up,
            MotionScheme.expressive().defaultSpatialSpec()
        ) + fadeIn(MotionScheme.expressive().defaultEffectsSpec())
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Down,
            MotionScheme.expressive().defaultSpatialSpec()
        ) + fadeOut(MotionScheme.expressive().defaultEffectsSpec())
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Up,
            MotionScheme.expressive().defaultSpatialSpec()
        ) + fadeIn(MotionScheme.expressive().defaultEffectsSpec())
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Down,
            MotionScheme.expressive().defaultSpatialSpec()
        ) + fadeOut(MotionScheme.expressive().defaultEffectsSpec())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object SlideTransition : DestinationStyle.Animated() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            MotionScheme.expressive().defaultSpatialSpec()
        ) + fadeIn(MotionScheme.expressive().defaultEffectsSpec())
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        fadeOut(MotionScheme.expressive().defaultEffectsSpec())
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        fadeIn(MotionScheme.expressive().defaultEffectsSpec())
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
            MotionScheme.expressive().defaultSpatialSpec()
        ) + fadeOut(MotionScheme.expressive().defaultEffectsSpec())
    }
}


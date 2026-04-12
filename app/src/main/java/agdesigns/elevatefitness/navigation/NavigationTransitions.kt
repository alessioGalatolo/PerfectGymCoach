package agdesigns.elevatefitness.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.navigation3.ui.NavDisplay


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val SlideTransition = NavDisplay.transitionSpec {
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        MotionScheme.expressive().slowSpatialSpec()
    ) + fadeIn(
        MotionScheme.expressive().slowEffectsSpec()
    ) togetherWith
            ExitTransition.None
} + NavDisplay.popTransitionSpec {
    EnterTransition.None togetherWith
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                MotionScheme.expressive().slowSpatialSpec()
            ) + fadeOut(
        MotionScheme.expressive().slowEffectsSpec()
    )
} + NavDisplay.predictivePopTransitionSpec {
    EnterTransition.None togetherWith
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                MotionScheme.expressive().slowSpatialSpec()
            ) + fadeOut(
        MotionScheme.expressive().slowEffectsSpec()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val FadeTransition = NavDisplay.transitionSpec {
    fadeIn(MotionScheme.expressive().slowEffectsSpec()) togetherWith
            ExitTransition.None
} + NavDisplay.popTransitionSpec {
    EnterTransition.None togetherWith
            fadeOut(MotionScheme.expressive().slowEffectsSpec())
} + NavDisplay.predictivePopTransitionSpec {
    EnterTransition.None togetherWith
            fadeOut(MotionScheme.expressive().slowEffectsSpec())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val FullscreenDialogTransition = NavDisplay.transitionSpec {
    slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Up,
            MotionScheme.expressive().slowSpatialSpec()
        ) + fadeIn(
            MotionScheme.expressive().slowEffectsSpec()
        ) togetherWith
            ExitTransition.None
} + NavDisplay.popTransitionSpec {
    EnterTransition.None togetherWith
        slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Down,
                MotionScheme.expressive().slowSpatialSpec()
        ) + fadeOut(
            MotionScheme.expressive().slowEffectsSpec()
        )
} + NavDisplay.predictivePopTransitionSpec {
    EnterTransition.None togetherWith
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Down,
                MotionScheme.expressive().slowSpatialSpec()
            ) + fadeOut(
        MotionScheme.expressive().slowEffectsSpec()
    )
}
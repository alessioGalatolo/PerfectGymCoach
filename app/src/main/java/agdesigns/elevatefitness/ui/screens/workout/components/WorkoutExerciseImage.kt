package agdesigns.elevatefitness.ui.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.ui.common.HorizontalPagerIndicator
import agdesigns.elevatefitness.ui.screens.workout.CurrentExerciseState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap


@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SharedTransitionScope.WorkoutExerciseImage(
    animatedVisibilityScope: AnimatedVisibilityScope,
    imageId: Int,
    sharedStateImg: SharedTransitionScope.SharedContentState,
    imageCorners: CornerBasedShape,
    previewImage: Int?,
    previewImageShouldDisappear: Boolean,
    canShowActualImage: Boolean,
    showPagerIndicator: Boolean,
    pagerState: PagerState,
    setImageIsBright: (Boolean) -> Unit,
    setImageHeight: (Dp) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // preview image is placed below (z-wise) the actual image
    // actual image will fade in and cover the preview image
    Box(
        Modifier
            .wrapContentHeight(Top), contentAlignment = TopCenter
    ) {
        // we need to fade preview image, otherwise it will be visible everytime a new image buffers
        AnimatedVisibility(
            !previewImageShouldDisappear,
            enter = EnterTransition.None,
            exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec())
        ) {
            AsyncImage(
                ImageRequest.Builder(context)
                    .data(previewImage ?: R.drawable.finish_workout)
                    .crossfade(true)
                    .build(),
                stringResource(R.string.exercise_image),
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .graphicsLayer(
                        shape = imageCorners,
                        clip = true
                    )
                    .sharedBounds(
                        sharedStateImg,
                        animatedVisibilityScope,
                        clipInOverlayDuringTransition = OverlayClip(imageCorners),
                        boundsTransform = { _, _ ->
                            MotionScheme.expressive().slowSpatialSpec()
                        }
                    )
                    .graphicsLayer(
                        shape = imageCorners,
                        clip = true
                    ).onGloballyPositioned {
                        setImageHeight(with(density) { it.size.height.toDp() })
                    },
                contentScale = ContentScale.Crop
            )
        }
        AnimatedVisibility(
            visible = canShowActualImage,
            enter = EnterTransition.None,
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
        ) {
            AsyncImage(
                ImageRequest.Builder(context)
                    .allowHardware(false) // pixel access is not supported on Config#HARDWARE bitmaps
                    .data(imageId)
                    // .crossfade(true)
                    .listener { _, result ->
                        val image = result.image.toBitmap()
                        Palette.from(image).maximumColorCount(3)
                            .clearFilters()
                            .setRegion(0, 0, image.width, 50)
                            .generate {
                                setImageIsBright((ColorUtils.calculateLuminance(
                                    it?.getDominantColor(Color.Black.toArgb()) ?: 0
                                )) > 0.5)
                            }
                    }
                    .build(),
                stringResource(R.string.exercise_image),
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .sharedBounds(
                        sharedStateImg,
                        animatedVisibilityScope,
                        clipInOverlayDuringTransition = OverlayClip(imageCorners),
                        boundsTransform = { _, _ ->
                            MotionScheme.expressive().slowSpatialSpec()
                        }
                    )
                    .graphicsLayer(
                        shape = imageCorners,
                        clip = true
                    ),
                contentScale = ContentScale.Crop
            )
        }
        AnimatedVisibility(
            visible = canShowActualImage && showPagerIndicator,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            HorizontalPagerIndicator(
                pagerState = pagerState,
            )
        }
    }
}
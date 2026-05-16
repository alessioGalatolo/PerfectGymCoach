package agdesigns.elevatefitness.ui.common

import agdesigns.elevatefitness.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.AddProgramExerciseDestination
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.navigation.WorkoutDestination
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutCard(
    navigator: DestinationsNavigator,
    program: WorkoutProgram,
    exercises: List<ProgramExerciseAndInfo>,
    onCardClick: (ProgramExerciseAndInfo?) -> Unit,
    cardModifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    exerciseModifier: Modifier = Modifier,
    showCompact: Boolean = false,
    cardShape: Shape = MaterialTheme.shapes.medium,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cardElevation: Dp = 1.dp,
    trailingIcons: @Composable () -> Unit = {},
){
    val pagerState = rememberPagerState(pageCount = { exercises.size })
    ElevatedCard(
        interactionSource = interactionSource,
        onClick = {
            onCardClick(
                exercises.getOrNull(pagerState.currentPage)
            )
        },
        shape = cardShape,
        modifier = cardModifier
            .fillMaxWidth()
    ) {
        Column {
            AnimatedVisibility(
                exercises.isNotEmpty() && !showCompact
            ) {
                Box(
                    Modifier.wrapContentHeight(Alignment.Top),
                    contentAlignment = Alignment.TopCenter
                ) {
                    HorizontalPager(state = pagerState,
                        modifier = Modifier
                            .graphicsLayer {
                                shape = cardShape
                                clip = true
                            }
                            .then(imageModifier)
                    ) { page ->
                        Box (Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = exercises[page].image,
                                contentDescription = stringResource(R.string.exercise_image),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 2f)
                                    .align(Alignment.TopCenter)
                            )
                        }
                    }
                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        pageCount = exercises.size,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            }
            Row{
                Text(
                    text = getProgramDisplayName(program.name),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedVisibility(
                !showCompact
            ) {
                Column {
                    exercises.forEachIndexed { index, it ->
                        // breaks if more than one exercise has the same name
                        val modifier = if (index == pagerState.currentPage)
                            exerciseModifier
                        else Modifier
                        val variation = if (it.variation.isNotBlank()) " (${it.variation})" else ""
                        val exerciseText = it.name + variation
                        Text(
                            text = exerciseText,
                            // exerciseModifier needs to go after because we're adding padding
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .then(modifier)
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(stringResource(R.string.sets))
                                    append(": ")
                                }
                                append(it.reps.size.toString())
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(" • ")
                                    if (it.overriddenDurationBased) {
                                        append(stringResource(R.string.exercise_hold))
                                        append(" (${stringResource(R.string.seconds_unit)})")
                                    } else
                                        append(stringResource(R.string.reps))
                                    append(": ")
                                }
                                if (it.reps.all { rep -> rep == it.reps[0] })
                                    append(it.reps[0].toString())
                                else
                                    append(it.reps.joinToString(", "))
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(" • ")
                                    append(stringResource(R.string.rest))
                                    append(": ")
                                }
                                if (it.rest.all { rest -> rest == it.rest[0] })
                                    append(it.rest[0].toString() + stringResource(R.string.seconds_unit))
                                else
                                    append(it.rest.joinToString("${stringResource(R.string.seconds_unit)}, ") + stringResource(R.string.seconds_unit))
                                if (it.note.isNotBlank()) {
                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                        append(" • ")
                                        append(stringResource(R.string.note))
                                    }
                                    append(it.note)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (exercises.isNotEmpty()) {
                            Button(
                                shapes = ButtonDefaults.shapes(),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                onClick = {
                                    navigator.navigate(
                                        WorkoutDestination(
                                            programId = program.programId,
                                            quickStart = true,
                                            previewExercise = exercises.getOrNull(pagerState.currentPage)
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.RocketLaunch,
                                    stringResource(R.string.quick_start_icon)
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.quick_start))
                            }
                        }
                        trailingIcons()
                    }
                }
            }
        }
    }
}
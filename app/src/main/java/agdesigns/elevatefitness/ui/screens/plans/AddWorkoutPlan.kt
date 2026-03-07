package agdesigns.elevatefitness.ui.screens.plans

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.WorkoutPlan
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanRename
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.getPlanDisplayName
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.common.InsertNameDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramDestination
import com.ramcosta.composedestinations.generated.destinations.ArchivedPlansDestination
import com.ramcosta.composedestinations.generated.destinations.CustomizePlanGenerationDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.abs

@Destination<ChangePlanGraph>(start=true, style = SlideTransition::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AddWorkoutPlan(
    navigator: DestinationsNavigator,
    openDialogNow: Boolean = false,
    viewModel: PlansViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    // rename plan
    InsertNameDialog(
        prompt = stringResource(R.string.rename_plan),
        dialogueIsOpen = state.openChangeNameDialog,
        toggleDialog = { viewModel.onEvent(PlansEvent.ToggleChangeNameDialog(null)) },
        oldName = state.workoutPlanMapPrograms.firstOrNull {
            it.first.planId == state.planToBeRenamed
        }?.first?.name?.let { getPlanDisplayName(it) },
        insertName = {
            if (state.planToBeRenamed != null) {
                viewModel.onEvent(
                    PlansEvent.RenameProgram(
                        WorkoutPlanRename(
                            planId = state.planToBeRenamed!!,
                            name = it
                        )
                    )
                )
            }
        }
    )
    InsertNameDialog(
        prompt = stringResource(R.string.new_plan_prompt),
        dialogueIsOpen = state.openAddPlanDialogue,
        toggleDialog = { viewModel.onEvent(PlansEvent.TogglePlanDialogue) },
        insertName = {
            planName -> viewModel.onEvent(
            PlansEvent.AddPlan(
                WorkoutPlan(
                    name = planName,
                    creationDate = ZonedDateTime.now(),
                )
            ))
        }
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val cantSwipeErrorString = stringResource(R.string.archive_plan_swipe_error)
    val scope = rememberCoroutineScope()
    val openDialog = rememberSaveable { mutableStateOf(openDialogNow) }
    LaunchedEffect(openDialog.value) {
        if (openDialog.value) {
            awaitFrame()
            awaitFrame()
            viewModel.onEvent(PlansEvent.TogglePlanDialogue)
            openDialog.value = false
        }
    }
    Scaffold (
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(stringResource(R.string.manage_workout_plans)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }, floatingActionButton = {
            MediumFloatingActionButton (
                onClick = {
                    viewModel.onEvent(PlansEvent.TogglePlanDialogue)
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_icon_workout_plan),
                    modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize),
                )
            }
        }) { innerPadding ->
        if (state.workoutPlanMapPrograms.isEmpty() && state.mainPlanMapPrograms == null) {
            // if you have no plans
            EmptyScreenInfo(
                icon = Icons.Default.ContentPaste,
                iconDescriptionRes = R.string.empty_home,
                titleRes = R.string.empty_home,
                subtitleRes = R.string.empty_plans
            ) {
                GeneratePlanButton(navigator)
            }
        } else {
            // if you have some plans
            val planArchivedString = stringResource(R.string.plan_archived)
            val undoString = stringResource(R.string.undo)
            val planSetAsCurrentString = stringResource(R.string.plan_set_as_current)
            val positionalThresholdFun = SwipeToDismissBoxDefaults.positionalThreshold
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.grouped_cards_between_cards)
                ),
            ) {
                if (state.mainPlanMapPrograms != null) {
                    item(key = "currentPlanHeader") {
                        Text(
                            stringResource(R.string.current_plan),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                vertical = dimensionResource(R.dimen.header_to_content_padding)
                            ).padding(horizontal = dimensionResource(R.dimen.screen_edge_padding))
                        )
                    }
                    item(key = state.mainPlanMapPrograms!!.first.planId) {
                        // these should not be null but you never know
                        val plan = state.mainPlanMapPrograms?.first ?: WorkoutPlan(
                            name = "",
                            creationDate = ZonedDateTime.now()
                        )
                        val programs = state.mainPlanMapPrograms?.second ?: emptyList()

                        val swipeToDismissBoxState = remember(plan.planId, plan.archived) {
                            SwipeToDismissBoxState(
                                initialValue = SwipeToDismissBoxValue.Settled,
                                positionalThreshold = {
                                    positionalThresholdFun(it * 3)
                                }
                            )
                        }

                        PlanCard(
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.screen_edge_padding)
                            ).padding(bottom = 8.dp),
                            secondaryCardPosition = null,
                            navigator = navigator,
                            plan = plan,
                            programs = programs,
                            canBeSwiped = false,
                            swipeToDismissBoxState = swipeToDismissBoxState,
                            showCantSwipeError = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        cantSwipeErrorString
                                    )
                                }
                            },
                            onSwipe = {
                                viewModel.onEvent(PlansEvent.ArchivePlan(plan.planId))
                                scope.launch {
                                    val snackbarResult = snackbarHostState.showSnackbar(
                                        planArchivedString,
                                        actionLabel = undoString,
                                        duration = SnackbarDuration.Short
                                    )
                                    when (snackbarResult) {
                                        SnackbarResult.ActionPerformed -> {
                                            viewModel.onEvent(PlansEvent.UnarchivePlan(plan.planId))
                                        }

                                        SnackbarResult.Dismissed -> {
                                            /* Handle snackbar dismissed */
                                        }
                                    }
                                }
                            },
                            swipeIcon = Icons.Default.Archive,
                            swipeDescription = stringResource(R.string.archive_plan_action),
                            swipeBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                            primaryActionIcon = Icons.Default.Favorite,
                            primaryActionDescription = stringResource(R.string.current_plan),
                            onPrimaryAction = {
                                viewModel.onEvent(PlansEvent.SetCurrentPlan(plan.planId))
                                scope.launch {
                                    snackbarHostState.showSnackbar(planSetAsCurrentString)
                                }
                            },
                            trailingActions = listOf({ close ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.rename)) },
                                    onClick = {
                                        viewModel.onEvent(PlansEvent.ToggleChangeNameDialog(plan.planId))
                                        close()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    }
                                )
                            }, { close ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.archive_plan_action)) },
                                    onClick = {
                                        scope.launch {
                                            close()
                                            swipeToDismissBoxState.dismiss(SwipeToDismissBoxValue.StartToEnd)
                                            viewModel.onEvent(PlansEvent.ArchivePlan(plan.planId))
                                        }
                                    },
                                    enabled = false,
                                    leadingIcon = {
                                        Icon(Icons.Default.Archive, contentDescription = null)
                                    }
                                )
                            }
                            )
                        )
                    }
                }
                item(key = "generatePlanButton") {
                    Column(Modifier.fillMaxWidth()) {
                        GeneratePlanButton(navigator)
                    }
                }
                if (state.workoutPlanMapPrograms.isNotEmpty()) {
                    item(key = "otherPlansHeader") {
                        Text(
                            stringResource(R.string.other_plans),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                vertical = dimensionResource(R.dimen.header_to_content_padding)
                            ).padding(horizontal = dimensionResource(R.dimen.screen_edge_padding))
                        )
                    }
                }
                itemsIndexed(
                    items = state.workoutPlanMapPrograms,
                    key = { _, it -> it.first.planId }
                ) { index, plan ->

                    val swipeToDismissBoxState = remember(plan.first.planId, plan.first.archived) {
                        SwipeToDismissBoxState(
                            initialValue = SwipeToDismissBoxValue.Settled,
                            positionalThreshold = positionalThresholdFun
                        )
                    }

                    val cardPosition =
                        if (state.workoutPlanMapPrograms.size == 1)
                            CardPositionInGroup.ONLY_ONE
                        else
                            when (index) {
                                0 -> CardPositionInGroup.FIRST
                                state.workoutPlanMapPrograms.size - 1 -> CardPositionInGroup.LAST
                                else -> CardPositionInGroup.MIDDLE
                            }
                    PlanCard(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        secondaryCardPosition = cardPosition,
                        navigator = navigator,
                        plan = plan.first,
                        programs = plan.second,
                        canBeSwiped = true,
                        swipeToDismissBoxState = swipeToDismissBoxState,
                        showCantSwipeError = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    cantSwipeErrorString
                                )
                            }
                        },
                        onSwipe = {
                            viewModel.onEvent(PlansEvent.ArchivePlan(plan.first.planId))
                            scope.launch {
                                val snackbarResult = snackbarHostState.showSnackbar(
                                    planArchivedString,
                                    actionLabel = undoString,
                                    duration = SnackbarDuration.Short
                                )
                                when (snackbarResult) {
                                    SnackbarResult.ActionPerformed -> {
                                        viewModel.onEvent(PlansEvent.UnarchivePlan(plan.first.planId))
                                    }

                                    SnackbarResult.Dismissed -> {
                                        /* Handle snackbar dismissed */
                                    }
                                }
                            }
                        },
                        swipeIcon = Icons.Default.Archive,
                        swipeDescription = stringResource(R.string.archive_plan_action),
                        swipeBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                        primaryActionIcon = Icons.Default.FavoriteBorder,
                        primaryActionDescription = stringResource(
                            R.string.set_as_current_plan
                        ),
                        onPrimaryAction = {
                            viewModel.onEvent(PlansEvent.SetCurrentPlan(plan.first.planId))
                            scope.launch {
                                snackbarHostState.showSnackbar(planSetAsCurrentString)
                            }
                        },
                        trailingActions = listOf({ close ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename)) },
                                onClick = {
                                    viewModel.onEvent(PlansEvent.ToggleChangeNameDialog(plan.first.planId))
                                    close()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                        }, { close ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.archive_plan_action)) },
                                onClick = {
                                    scope.launch {
                                        close()
                                        swipeToDismissBoxState.dismiss(SwipeToDismissBoxValue.StartToEnd)
                                        viewModel.onEvent(PlansEvent.ArchivePlan(plan.first.planId))
                                    }
                                },
                                enabled = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Archive, contentDescription = null)
                                }
                            )
                        }
                        )
                    )
                }
                if (state.archivedPlans.isNotEmpty()) {
                    item(key = "archivedPlans") {
                        if (state.workoutPlanMapPrograms.size <= 1) {
                            Column (Modifier.fillMaxWidth()){
                                Text(
                                    stringResource(R.string.other_plans),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        top = dimensionResource(R.dimen.header_to_content_padding)
                                    ).padding(horizontal = dimensionResource(R.dimen.screen_edge_padding))
                                )
                            }
                        } else {
                            Spacer(
                                Modifier.height(
                                    dimensionResource(R.dimen.header_to_content_padding)
                                )
                            )
                        }
                        // Archived chat card
                        Card(
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.screen_edge_padding)
                            ).padding(top = 8.dp),
//                            colors = CardDefaults.cardColors(
//                                containerColor = MaterialTheme.colorScheme.surface
//                            ),
                            onClick = {
                                navigator.navigate(ArchivedPlansDestination)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(R.dimen.card_inner_padding)),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = "")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.archived_plans_title), style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
                item(key = "bottomSpacers"){
                    var finalSpacerSize = 80.dp + 16.dp // large fab size + its padding FIXME: not hardcode
                    finalSpacerSize += 16.dp
                    Spacer(Modifier.height(finalSpacerSize))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LazyItemScope.PlanCard(
    navigator: DestinationsNavigator,
    plan: WorkoutPlan,
    programs: List<WorkoutProgram>,
    secondaryCardPosition: CardPositionInGroup? = null,
    canBeSwiped: Boolean = true,
    showCantSwipeError: () -> Unit = {},
    onSwipe: (Long) -> Unit = {},
    swipeBackgroundColor: Color = Color.White,
    swipeIcon: ImageVector? = null,
    swipeDescription: String? = null,
    swipeToDismissBoxState: SwipeToDismissBoxState? = null,
    primaryActionIcon: ImageVector? = null,
    primaryActionDescription: String? = null,
    onPrimaryAction: (() -> Unit) = {},
    modifier: Modifier = Modifier,
    trailingActions: List<(@Composable ColumnScope.(() -> Unit) -> Unit)> = emptyList()
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val positionalThresholdFun = SwipeToDismissBoxDefaults.positionalThreshold
    // NOTE: we need these two keys otherwise when undoing archivePlan, the state would be recycled
    val dismissState = swipeToDismissBoxState
        ?: remember(plan.planId, plan.archived) {
         SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            positionalThreshold = if (canBeSwiped) {
                positionalThresholdFun
            } else { it ->
                positionalThresholdFun(it * 3)
            }
        )
    }
    val density = LocalDensity.current
    val swipeWidthDp by remember {
        derivedStateOf {
            try {
                with(density) { abs(dismissState.requireOffset()).toDp() }
            } catch (e: IllegalStateException) {
                0.dp
            }
        }
    }
    SwipeToDismissBox(
        modifier = modifier.animateItem(),
        state = dismissState,
        onDismiss = { direction ->
            if (direction != SwipeToDismissBoxValue.Settled) {
                if (canBeSwiped) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipe(plan.planId)
                } else {
                    scope.launch {
                        dismissState.reset()
                        showCantSwipeError()
                    }
                }
            } else {
                scope.launch {
                    dismissState.reset()
                }
            }
        },
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val defaultColors = CardDefaults.cardColors()
            val dismissColors by animateColorAsState(
                when (dismissState.targetValue) {  // pastel red
                    SwipeToDismissBoxValue.StartToEnd -> swipeBackgroundColor
                    SwipeToDismissBoxValue.EndToStart -> swipeBackgroundColor
                    SwipeToDismissBoxValue.Settled -> if (canBeSwiped) defaultColors.containerColor else swipeBackgroundColor
                }, label = "Dismiss box anim color"
            )

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.Start
                SwipeToDismissBoxValue.EndToStart -> Alignment.End
                SwipeToDismissBoxValue.Settled -> Alignment.CenterHorizontally
            }
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled || !canBeSwiped) 1f else 1.5f,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "Dismiss box anim"
            )
            val targetHeight = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled &&
                dismissState.progress > 0.85f && canBeSwiped) 1f - dismissState.progress else 1f
            val animatedHeight by animateFloatAsState(
                targetValue = targetHeight,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "heightAnim"
            )
            Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                Card(
                    shape = MaterialTheme.shapes.extraExtraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = dismissColors
                    ),
                    modifier = Modifier
                        .width(swipeWidthDp)
                        .fillMaxHeight(animatedHeight)
                        .clipToBounds()
                ) {
                    if (swipeIcon != null) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                swipeIcon,
                                contentDescription = swipeDescription,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .scale(scale)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    ) {
        val cardShape = when (secondaryCardPosition) {
            CardPositionInGroup.FIRST -> MaterialTheme.shapes.extraLarge.copy(
                bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd,
                bottomStart = MaterialTheme.shapes.extraSmall.bottomStart
            )
            CardPositionInGroup.MIDDLE -> MaterialTheme.shapes.extraSmall
            CardPositionInGroup.LAST -> MaterialTheme.shapes.extraLarge.copy(
                topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                topStart = MaterialTheme.shapes.extraSmall.topStart
            )
            CardPositionInGroup.ONLY_ONE -> MaterialTheme.shapes.extraLarge
            null -> CardDefaults.elevatedShape
        }
        val cardColors = if (secondaryCardPosition == null)
            CardDefaults.elevatedCardColors()
        else
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        val elevation = if (secondaryCardPosition == null)
            CardDefaults.elevatedCardElevation()
        else
            CardDefaults.cardElevation()
        ElevatedCard(
            shape = cardShape,
            colors = cardColors,
            elevation = elevation,
            onClick = {
                navigator.navigate(
                    AddProgramDestination(
                        planId = plan.planId
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(
                dimensionResource(R.dimen.card_inner_padding)
            )) {
                Text(
                    text = getPlanDisplayName(plan.name),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                programs.forEach {
                    Text(getProgramDisplayName(it.name))
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)) {
                if (primaryActionIcon != null) {
                    Button(onPrimaryAction,
                        shapes = ButtonDefaults.shapes(),
                        enabled = canBeSwiped
                    ) {
                        Icon(primaryActionIcon, primaryActionDescription)
                        if (primaryActionDescription != null) {
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(primaryActionDescription)
                        }
                    }
                } else {
                    // placeholder so that more vert box is always on the right
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                }

                if (trailingActions.isNotEmpty()) {
                    var showDropdown by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showDropdown = true }
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.morevert_icon_options)
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false }
                        ) {
                            trailingActions.forEach {
                                it({
                                    showDropdown = false
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class CardPositionInGroup {
    FIRST,  // smooth corners top, sharp bottom
    MIDDLE, // sharp top, sharp bottom
    LAST, // sharp top, smooth corners bottom
    ONLY_ONE  // smooth top, smooth bottom
}

@Composable
fun ColumnScope.GeneratePlanButton(navigator: DestinationsNavigator){
    FilledTonalButton(
        onClick = {
            navigator.navigate(CustomizePlanGenerationDestination())
        },
        modifier = Modifier.align(Alignment.CenterHorizontally))
    {
        Icon(Icons.Filled.AutoAwesome, stringResource(R.string.generate_a_new_plan))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.generate_a_new_plan))
    }
}
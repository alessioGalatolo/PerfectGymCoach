package agdesigns.elevatefitness.ui.screens.workout_recap

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.navigation.FullscreenDialogTransition
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.shared.maybeKgToLb
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import agdesigns.elevatefitness.navigation.WorkoutOnlyGraph
import agdesigns.elevatefitness.ui.common.CurrentColumnKey
import agdesigns.elevatefitness.ui.common.ExerciseRecordsList
import agdesigns.elevatefitness.ui.common.HorizontalPagerIndicator
import agdesigns.elevatefitness.ui.common.columnProviderWithHighlight
import agdesigns.elevatefitness.ui.common.highlightSeriesKey
import agdesigns.elevatefitness.ui.common.lazyGroupedCard
import agdesigns.elevatefitness.ui.common.lineProviderWithHighlight
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withLink
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.rememberHorizontalLegend
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.common.LegendItem
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import agdesigns.elevatefitness.data.HealthConnectRepository
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.graphics.Color
import androidx.health.connect.client.PermissionController
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.HistoryDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.ExerciseStatsDestination
import java.time.format.DateTimeFormatter


@Destination<WorkoutOnlyGraph>(style = FullscreenDialogTransition::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun WorkoutRecap(
    navigator: DestinationsNavigator,
    workoutId: Long,
    viewModel: RecapViewModel = hiltViewModel()
) {
    val recapState by viewModel.state.collectAsState()
    viewModel.onEvent(RecapEvent.SetWorkoutId(workoutId))
    val volumeDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    val calorieDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    val highlightsCardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    InfoDialog(dialogueIsOpen = volumeDialogIsOpen.value,
        toggleDialogue = { volumeDialogIsOpen.value = !volumeDialogIsOpen.value })
    {
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = LocalContentColor.current)) {
                append(stringResource(R.string.volume_info))
            }
            withLink(
                LinkAnnotation.Url(
                    url = "https://doi.org/10.1007/s40279-017-0793-0",
                    styles = TextLinkStyles(
                        style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                    )
                )
            ) {
                append(stringResource(R.string.learn_more))
            }
        }

        Text(
            text = annotatedText
        )
    }
    InfoDialog(dialogueIsOpen = calorieDialogIsOpen.value,
        toggleDialogue = { calorieDialogIsOpen.value = !calorieDialogIsOpen.value })
    {
        Text(stringResource(R.string.calories_info))
    }
    if (
        recapState.workoutId != 0L &&
        recapState.workoutRecord != null
    ){
        val records = recapState.olderRecords
        val legend = listOf (
            listOf(
                "${stringResource(R.string.volume)} (${if (recapState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg)})",
                stringResource(R.string.this_workout)
            ),
            listOf(
                "${stringResource(R.string.calories)} (kcal)",
                stringResource(R.string.this_workout)
            ),
            listOf(
                "${stringResource(R.string.workout_time)} (s)",
                "${stringResource(R.string.workout_active_time)} (s)",
                stringResource(R.string.this_workout),
            )
        )
        Scaffold(topBar = {
            TopAppBar (
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Text(stringResource(R.string.workout_recap))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navigator.navigateUp()
                            navigator.navigateUp()
                            navigator.navigate(
                                HistoryDestination()
                            )
                        },
                        shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(Icons.Default.Close, stringResource(R.string.close_icon))
                }
            })
        }) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ){
                item {
                    Text(
                        stringResource(R.string.workout_recap_praise),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        color = MaterialTheme.colorScheme.secondary,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
                item{
                    val pagerState = rememberPagerState(pageCount = { 3 })
                    if (records.size > 1){
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.padding(top = 8.dp)
                        ) { page ->
                            ElevatedCard(Modifier.padding(horizontal = dimensionResource(R.dimen.card_outside_padding))) {
                                val formatter = DateTimeFormatter.ofPattern("d MMM")
                                val legendItemLabelComponent = rememberTextComponent(
                                    vicoTheme.textColor
                                )
                                val colors = vicoTheme.lineCartesianLayerColors.drop(1)
                                val primaryColor = vicoTheme.lineCartesianLayerColors[0]
                                val cartesianLayer = when (page) {
                                    // calories chart is column, others are lines
                                    1 -> rememberColumnCartesianLayer(
                                        columnProvider = columnProviderWithHighlight(
                                            baseShape = MaterialTheme.shapes.extraSmall,
                                            baseColor = MaterialTheme.colorScheme.secondary,
                                            highlightKey = CurrentColumnKey
                                        )
                                    )
                                    else -> rememberLineCartesianLayer(
                                        lineProviderWithHighlight()
                                    )
                                }
                                CartesianChartHost(
                                    chart = rememberCartesianChart(
                                        cartesianLayer,
                                        startAxis = VerticalAxis.rememberStart(),
                                        bottomAxis = HorizontalAxis.rememberBottom(
                                            valueFormatter = CartesianValueFormatter { _, value, _ ->
                                                recapState.index2date[value.toInt()]?.format(formatter)
                                                    ?: value.toString() // fall back to value, empty string crashes stuff
                                            }
                                        ),
                                        legend =
                                            rememberHorizontalLegend(
                                                items = { extraStore ->
                                                    legend[page].forEachIndexed { index, label ->
                                                        val highlights = extraStore.getOrNull(
                                                            highlightSeriesKey
                                                        ) ?: emptyList()
                                                        val color = if (highlights.contains(index)) {
                                                            primaryColor
                                                        } else {
                                                            colors[index % colors.size]
                                                        }
                                                        val shape = if (
                                                            // calories chart has opposite shapes
                                                            (page == 1 && !highlights.contains(index))
                                                            || (page != 1 && highlights.contains(index))
                                                        ) {
                                                            // FIXME: should use material extraSmall but doesn't work
                                                            CorneredShape.rounded(2f)
                                                        } else
                                                            CorneredShape.Pill
                                                        add(
                                                            LegendItem(
                                                                shapeComponent(
                                                                    fill = fill(color),
                                                                    shape = shape
                                                                ),
                                                                legendItemLabelComponent,
                                                                label,
                                                            )
                                                        )
                                                    }
                                                },
                                                padding = insets(top = 16.dp),
                                            ),
                                    ),
                                    modelProducer = when (page) {
                                        0 -> recapState.volumeChartProducer
                                        1 -> recapState.caloriesChartProducer
                                        2 -> recapState.timeChartProducer
                                        else -> recapState.volumeChartProducer // should not happen
                                    },
                                    modifier = Modifier.padding(8.dp),
                                    scrollState = rememberVicoScrollState(
                                        scrollEnabled = false, // TODO: should not enable scroll inside a horizontal pager
                                        initialScroll = Scroll.Absolute.End // FIXME: scroll to current workout instead
                                    ),
                                )
                            }
                        }
                        Column(Modifier.fillMaxWidth()) {
                            HorizontalPagerIndicator(
                                pagerState = pagerState,
                                pageCount = pagerState.pageCount,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.no_analytics),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 128.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                if (recapState.workoutRecord != null) {
                    item {
                        Text(
                            stringResource(R.string.s_header1_highlights),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(vertical = 8.dp)
                        )
                    }
                    lazyGroupedCard(
                        colors = highlightsCardColors,
                    ) {
                        subCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Card(
                                        shape = MaterialTheme.shapes.extraExtraLarge,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        ),
                                    ) {
                                        Icon(
                                            Icons.Outlined.LocalFireDepartment,
                                            stringResource(R.string.calories_burned),
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(50.dp).padding(8.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(
                                            R.string.calorie_consumption_i_kcal,
                                            recapState.workoutRecord!!.calories.toInt()
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                IconButton(
                                    onClick = { calorieDialogIsOpen.value = true },
                                    modifier = Modifier.weight(0.1f)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.HelpOutline,
                                        stringResource(R.string.help_icon_info)
                                    )
                                }
                            }
                        }
                        subCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Card(
                                        shape = MaterialTheme.shapes.extraExtraLarge,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        ),
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.weight_icon),
                                            stringResource(R.string.volume_lifted),
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(50.dp).padding(8.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.total_volume) +
                                                ": %.2f ".format(
                                                    maybeKgToLb(
                                                        recapState.workoutRecord!!.volume.toFloat(),
                                                        recapState.imperialSystem
                                                    )
                                                ) + if (recapState.imperialSystem) stringResource(
                                            R.string.lb
                                        ) else stringResource(R.string.kg)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                IconButton(
                                    onClick = { volumeDialogIsOpen.value = true },
                                    modifier = Modifier.weight(0.1f)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.HelpOutline,
                                        stringResource(R.string.help_icon_info)
                                    )
                                }
                            }
                        }
                        subCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    shape = MaterialTheme.shapes.extraExtraLarge,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                ) {
                                    Icon(
                                        Icons.Outlined.Schedule,
                                        stringResource(R.string.workout_time),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(50.dp).padding(8.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.total_time) +
                                            DateUtils.formatElapsedTime(
                                                recapState.workoutRecord!!.durationSeconds
                                            )
                                )
                            }
                        }
                        subCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    shape = MaterialTheme.shapes.extraExtraLarge,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                ) {
                                    Icon(
                                        Icons.Outlined.PendingActions,
                                        stringResource(R.string.workout_active_time),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(50.dp).padding(8.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.workout_active_time) + ": " +
                                            DateUtils.formatElapsedTime(
                                                recapState.workoutRecord!!.activeTimeSeconds
                                            )
                                )
                            }
                        }
                    }
                }
                // Health Connect export card
                if (recapState.isHealthConnectAvailable && recapState.workoutRecord != null) {
                    item {
                        val healthConnectPermissionsLauncher = rememberLauncherForActivityResult(
                            PermissionController.createRequestPermissionResultContract()
                        ) {
                            viewModel.onEvent(RecapEvent.RefreshHealthConnectPermissions)
                        }
                        Card(
                            modifier = Modifier
                                .padding(horizontal = dimensionResource(R.dimen.card_outside_padding))
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_health_connect_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp).padding(end = 8.dp),
                                    tint = Color.Unspecified
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.health_connect_title),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        when (recapState.healthConnectExportStatus) {
                                            HealthConnectExportStatus.EXPORTED ->
                                                stringResource(R.string.health_connect_exported)
                                            HealthConnectExportStatus.ERROR ->
                                                stringResource(R.string.health_connect_export_error)
                                            else -> stringResource(R.string.health_connect_export_prompt)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                when (recapState.healthConnectExportStatus) {
                                    HealthConnectExportStatus.EXPORTING -> {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                    HealthConnectExportStatus.EXPORTED -> {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    HealthConnectExportStatus.ERROR -> {
                                        TextButton(onClick = {
                                            viewModel.onEvent(RecapEvent.ExportToHealthConnect)
                                        }) {
                                            Text(stringResource(R.string.health_connect_retry))
                                        }
                                    }
                                    HealthConnectExportStatus.NOT_EXPORTED -> {
                                        if (recapState.hasHealthConnectPermissions) {
                                            TextButton(onClick = {
                                                viewModel.onEvent(RecapEvent.ExportToHealthConnect)
                                            }) {
                                                Text(stringResource(R.string.health_connect_export))
                                            }
                                        } else {
                                            TextButton(onClick = {
                                                healthConnectPermissionsLauncher.launch(
                                                    HealthConnectRepository.REQUIRED_PERMISSIONS
                                                )
                                            }) {
                                                Text(stringResource(R.string.health_connect_connect_and_export))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (recapState.exerciseRecords.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.workout_history),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    ExerciseRecordsList(
                        recapState.imperialSystem,
                        exerciseRecordsWithImage = recapState.exerciseRecords,
                        onRecordClick = { recordId ->
                            val exerciseId =
                                recapState.exerciseRecords.find { it.recordId == recordId }?.extExerciseId
                            if (exerciseId != null) {
                                navigator.navigate(
                                    ExerciseStatsDestination(exerciseId = exerciseId)
                                )
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
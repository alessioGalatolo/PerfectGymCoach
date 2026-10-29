package agdesigns.elevatefitness.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Sex
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.ui.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.navigation.ProfileDestination
import agdesigns.elevatefitness.ui.common.GroupedCard
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.utils.getLangPreferenceDropdownEntries
import agdesigns.elevatefitness.utils.plus
import android.os.Build
import agdesigns.elevatefitness.data.HealthConnectRepository
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.shared.SetType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.background
import agdesigns.elevatefitness.shared.maybeKgToLb
import agdesigns.elevatefitness.shared.maybeLbToKg
import agdesigns.elevatefitness.ui.screens.workout.ModificationSuggestion
import agdesigns.elevatefitness.ui.screens.workout.components.SuggestModificationCard
import agdesigns.elevatefitness.ui.screens.workout.components.WorkoutRepsWeightRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.ui.platform.LocalResources
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import agdesigns.elevatefitness.ui.common.GroupedCardScope
import agdesigns.elevatefitness.shared.SmartRestParams

@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
fun Profile(
    navigator: DestinationsNavigator,
    refreshContentRequest: SharedFlow<Any>,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var editName by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    LaunchedEffect(refreshContentRequest) {
        refreshContentRequest.collect {
            if (it == ProfileDestination) {
                // this refresh is for us
                lazyListState.animateScrollToItem(0)
            }
        }
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var bmiDialogueShown by remember { mutableStateOf(false) }

    InfoDialog(dialogueIsOpen = bmiDialogueShown,
        toggleDialogue = { bmiDialogueShown = !bmiDialogueShown }
    ) {
        Text(stringResource(R.string.bmi_info))
    }
    val createDocForDbBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mimeType = "application/x-sqlite3")
    ) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.ExportDatabase(it)) }
    }

    val openDocForDbRestore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.ImportDatabase(it)) }
    }
    val createDocForPreferencesBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mimeType = "application/json")
    ) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.ExportPreferences(uri)) }
    }
    val openDocForPreferencesRestore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.ImportPreferences(it)) }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = state.backupOutcomeResId?.let { stringResource(it) }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
        viewModel.onEvent(ProfileEvent.ResetOutcomeMessage)
    }
    val backfillMessage = state.healthConnectBackfillOutcome?.let {
        if (it.failed == 0)
            stringResource(R.string.health_connect_backfill_success)
        else
            pluralStringResource(R.plurals.health_connect_backfill_partial, it.exported, it.exported, it.failed)
    }
    LaunchedEffect(backfillMessage) {
        backfillMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(ProfileEvent.ResetHealthConnectBackfillOutcome)
        }
    }
    // TODO: replace with an expansion of the integration subcard
    state.healthConnectBackfillOffer?.let { count ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ProfileEvent.DismissHealthConnectBackfill) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_health_connect_logo),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Unspecified
                )
            },
            title = { Text(stringResource(R.string.health_connect_backfill_title)) },
            text = {
                Text(pluralStringResource(R.plurals.health_connect_backfill_text, count, count))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(ProfileEvent.ConfirmHealthConnectBackfill) }) {
                    Text(stringResource(R.string.health_connect_export))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ProfileEvent.DismissHealthConnectBackfill) }) {
                    Text(stringResource(R.string.health_connect_backfill_not_now))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            contentPadding = innerPadding.plus(PaddingValues(vertical = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp),
        ) {
            // Header Section
            item {
                val nameTextFieldState = rememberTextFieldState()
                ProfileHeader(
                    name = state.name,
                    editName = editName,
                    nameTextFieldState = nameTextFieldState,
                    onEditToggle = { newName ->
                        editName = !editName
                        if (!editName) {
                            viewModel.onEvent(ProfileEvent.UpdateName(newName))
                        }
                    },
                    onNameSubmit = { newName ->
                        keyboardController?.hide()
                        editName = false
                        viewModel.onEvent(ProfileEvent.UpdateName(newName))
                    }
                )
            }

            // Personal Information Section
            item {
                ProfileSection(title = stringResource(R.string.personal_information_title)) {
                    PersonalInfoContent(
                        userSex = state.sex,
                        userBirthday = state.userBirthday,
                        updateBirthday = {
                            viewModel.onEvent(ProfileEvent.UpdateBirthday(it))
                        },
                        onEditSex = {
                            viewModel.onEvent(ProfileEvent.UpdateSex(it))
                        }
                    )
                }
            }

            // Physical Measurements Section
            item {
                ProfileSection(title = stringResource(R.string.physical_measurements_title)) {
                    PhysicalMeasurementsContent(
                        profileState = state,
                        viewModel = viewModel,
                        keyboardController = keyboardController,
                        focusManager = focusManager,
                        onBmiInfoClick = { bmiDialogueShown = true }
                    )
                }
            }

            // Preferences Section
            item {
                Text(
                    stringResource(R.string.preferences_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
                )
                PreferencesContent(
                    profileState = state,
                    viewModel = viewModel
                )
            }

            // Smart Features Section
            item {
                Text(
                    stringResource(R.string.smart_features_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        bottom = dimensionResource(R.dimen.header_to_content_padding)
                    )
                )
                SmartFeaturesContent(
                    profileState = state,
                    viewModel = viewModel
                )
            }

            // Wearable Section
            item {
                Text(
                    stringResource(R.string.wearable_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        bottom = dimensionResource(R.dimen.header_to_content_padding)
                    )
                )
                WearableContent(
                    profileState = state,
                    viewModel = viewModel
                )
            }

            // Health Connect Section
            if (state.isHealthConnectAvailable) {
                item {
                    val healthConnectPermissionsLauncher = rememberLauncherForActivityResult(
                        PermissionController.createRequestPermissionResultContract()
                    ) {
                        viewModel.onEvent(ProfileEvent.HealthConnectPermissionsResult)
                    }
                    Text(
                        stringResource(R.string.integrations_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
                    )
                    GroupedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        subCard {
                            Column {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_health_connect_logo),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .padding(end = 8.dp),
                                        tint = Color.Unspecified
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.health_connect_title),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            if (state.hasHealthConnectPermissions)
                                                stringResource(R.string.health_connect_connected)
                                            else if (state.hasSomeHealthConnectPermissions)
                                                stringResource(R.string.health_connect_problems)
                                            else
                                                stringResource(R.string.health_connect_info),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (state.hasHealthConnectPermissions) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        TextButton(onClick = {
                                            healthConnectPermissionsLauncher.launch(
                                                HealthConnectRepository.REQUIRED_PERMISSIONS
                                            )
                                        }) {
                                            Text(stringResource(R.string.health_connect_connect))
                                        }
                                    }
                                }
                                state.healthConnectBackfillProgress?.let { progress ->
                                    Text(
                                        stringResource(
                                            R.string.health_connect_backfill_progress,
                                            progress.done,
                                            progress.total
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                    )
                                    LinearWavyProgressIndicator(
                                        progress = { progress.done.toFloat() / progress.total },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.backup_and_restore),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
                )
                GroupedCard(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    subCard {
                        Text(
                            stringResource(R.string.backup_restore_info),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    subCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.plans_and_workouts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.isBackupLoading) {
                                CircularWavyProgressIndicator()
                            } else {
                                Row {
                                    TextButton(onClick = {
                                        createDocForDbBackup.launch("backup.db")
                                    }) {
                                        Text(stringResource(R.string.backup_export))
                                    }
                                    TextButton(onClick = {
                                        openDocForDbRestore.launch(
                                            arrayOf(
                                                "application/x-sqlite3",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    }) {
                                        Text(stringResource(R.string.backup_import))
                                    }
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.preferences),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.isPreferencesBackupLoading) {
                                CircularWavyProgressIndicator()
                            } else {
                                Row {
                                    TextButton(onClick = {
                                        createDocForPreferencesBackup.launch("settings.preferences_pb")
                                    }) {
                                        Text(stringResource(R.string.backup_export))
                                    }
                                    TextButton(onClick = {
                                        openDocForPreferencesRestore.launch(
                                            arrayOf(
                                                "application/json",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    }) {
                                        Text(stringResource(R.string.backup_import))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Equipment Increments Section
            item {
                Text(
                    stringResource(R.string.equipment_increments_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
                )
                GroupedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    subCard {
                        IncrementRow(
                            label = stringResource(R.string.barbell_increment),
                            value = state.incrementBarbell,
                            unit = if (state.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementBarbell(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }
                    subCard {
                        IncrementRow(
                            label = stringResource(R.string.bodyweight_increment),
                            value = state.incrementBodyweight,
                            unit = if (state.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementBodyweight(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }
                    subCard {
                        IncrementRow(
                            label = stringResource(R.string.cable_increment),
                            value = state.incrementCable,
                            unit = if (state.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementCable(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }
                    subCard {
                        IncrementRow(
                            label = stringResource(R.string.dumbbell_increment),
                            value = state.incrementDumbbell,
                            unit = if (state.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementDumbbell(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }
                    subCard {
                        IncrementRow(
                            label = stringResource(R.string.machine_increment),
                            value = state.incrementMachine,
                            unit = if (state.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementMachine(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }
                }
            }
            item {
                val uriHandler = LocalUriHandler.current
                Text(
                    stringResource(R.string.feedback_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
                )
                GroupedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    subCard(onClick = {
                        uriHandler.openUri("https://github.com/alessioGalatolo/PerfectGymCoach/issues")
                    }) {
                        ExternalLink(
                            title = stringResource(R.string.bug_report_title),
                            description = stringResource(R.string.bug_report_info),
                            leadingIcon = Icons.Default.BugReport
                        )
                    }
                    subCard(onClick = {
                        uriHandler.openUri("https://github.com/alessioGalatolo/PerfectGymCoach/discussions")
                    }) {
                        ExternalLink(
                            title = stringResource(R.string.feature_requests_title),
                            description = stringResource(R.string.feature_request_info),
                            leadingIcon = Icons.Default.Feedback
                        )
                    }
                    subCard(onClick = {
                        uriHandler.openUri("https://github.com/alessioGalatolo/PerfectGymCoach")
                    }) {
                        ExternalLink(
                            title = stringResource(R.string.source_code_title),
                            description = stringResource(R.string.source_code_info),
                            leadingIcon = Icons.Default.Code
                        )
                    }
                }
            }
            if (state.developerMode) {
                item {
                    Text(
                        stringResource(R.string.developer_options_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            bottom = dimensionResource(R.dimen.header_to_content_padding)
                        )
                    )
                    DeveloperOptionsContent(
                        profileState = state,
                        snackbarHost = snackbarHostState,
                        viewModel = viewModel
                    )
                }
            }
            item {
                val resources = LocalResources.current
                val scope = rememberCoroutineScope()
                // like Android's build number, tapping repeatedly unlocks developer mode
                var developerTaps by rememberSaveable { mutableIntStateOf(0) }
                fun showDeveloperMessage(message: String) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
                ProfileSection(title = stringResource(R.string.acknowledgements_title)) {
                    Text(
                        stringResource(R.string.acknowledgements),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable(
                                interactionSource = null,
                                indication = null
                            ) {
                                if (state.developerMode) {
                                    showDeveloperMessage(resources.getString(R.string.developer_mode_already_enabled))
                                    return@clickable
                                }
                                developerTaps++
                                val remaining = DEVELOPER_MODE_TAPS - developerTaps
                                if (remaining <= 0) {
                                    developerTaps = 0
                                    viewModel.onEvent(ProfileEvent.ToggleDeveloperMode(true))
                                    showDeveloperMessage(resources.getString(R.string.developer_mode_enabled))
                                } else if (developerTaps >= DEVELOPER_MODE_TAPS_BEFORE_HINT) {
                                    showDeveloperMessage(
                                        resources.getQuantityString(R.plurals.developer_mode_steps_away, remaining, remaining)
                                    )
                                }
                            }
                    )
                }
            }
        }
    }
}

private const val DEVELOPER_MODE_TAPS = 7
// taps after which the user is told how many are left
private const val DEVELOPER_MODE_TAPS_BEFORE_HINT = 3

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileHeader(
    name: String,
    editName: Boolean,
    nameTextFieldState: TextFieldState,
    onEditToggle: (String) -> Unit,
    onNameSubmit: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(editName) {
        if (editName) {
            nameTextFieldState.setTextAndSelectAll(name)
            awaitFrame()
            awaitFrame()
            awaitFrame()
            awaitFrame()
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!editName) {
                    Text(
                        text = if (name.isNotBlank())
                            stringResource(R.string.salute_user, name)
                        else
                            stringResource(R.string.what_is_your_name),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onEditToggle(nameTextFieldState.text.toString()) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit_icon_user_name))
                    }
                } else {
                    OutlinedTextField(
                        state = nameTextFieldState,
                        isError = nameTextFieldState.text.isBlank(),
                        label = { Text(stringResource(R.string.name)) },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        onKeyboardAction = KeyboardActionHandler {
                            onNameSubmit(nameTextFieldState.text.toString())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            focusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            cursorColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                    )
                    IconButton(
                        onClick = { onEditToggle(nameTextFieldState.text.toString()) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Done, stringResource(R.string.done_icon))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonalInfoContent(
    userSex: Sex,
    userBirthday: ZonedDateTime,
    updateBirthday: (ZonedDateTime) -> Unit,
    onEditSex: (Sex) -> Unit,
) {
    var editYear by remember { mutableStateOf(false) }
    if (editYear) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDate = LocalDate.of(userBirthday.year, userBirthday.month, userBirthday.dayOfMonth)
        )
        val confirmEnabled = remember {
            derivedStateOf { datePickerState.selectedDateMillis != null }
        }
        DatePickerDialog(
            onDismissRequest = {
                editYear = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editYear = false
                        updateBirthday(Instant.ofEpochMilli(datePickerState.selectedDateMillis!!).atZone(
                            ZoneId.of("UTC")
                        ))
                    },
                    enabled = confirmEnabled.value,
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { editYear = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        ) {
            // The verticalScroll will allow scrolling to show the entire month in case there is not
            // enough horizontal space (for example, when in landscape mode).
            // Note that it's still currently recommended to use a DisplayMode.Input at the state in
            // those cases.
            DatePicker(
                state = datePickerState,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Age Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val age = ChronoUnit.YEARS.between(userBirthday, ZonedDateTime.now())

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.age),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pluralStringResource(R.plurals.i_years, age.toInt(), age.toInt()),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = { editYear = true }) {
                if (editYear) {
                    Icon(Icons.Default.Close, stringResource(R.string.cancel_icon))
                } else {
                    Icon(Icons.Default.Edit, stringResource(R.string.edit_icon_age))
                }
            }
        }

        // Biological Sex Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.sex),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Row(
                Modifier
                    .padding(horizontal = 8.dp)
                    .weight(5f),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                Sex.entries.forEachIndexed { index, sex ->
                    val modifier = if (sex == userSex)
                        Modifier.weight(1f + ButtonGroupDefaults.ExpandedRatio) // expanded
                    else Modifier.weight(1f)

                    ToggleButton(
                        checked = sex == userSex,
                        onCheckedChange = { onEditSex(sex) },
                        modifier = modifier,
                        shapes =
                            when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                Sex.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                    ) {
                        Text(stringResource(sex.displayRes))
                    }
                }
            }
        }
    }
}

@Composable
fun PhysicalMeasurementsContent(
    profileState: ProfileState,
    viewModel: ProfileViewModel,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    onBmiInfoClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Weight
        MeasurementRow(
            label = stringResource(R.string.weight),
            value = maybeKgToLb(profileState.weight, profileState.imperialSystem),
            unit = if (profileState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
            onValueChange = { newWeight ->
                viewModel.onEvent(ProfileEvent.UpdateWeight(
                    maybeLbToKg(newWeight, profileState.imperialSystem)
                ))
            },
            keyboardController = keyboardController,
            focusManager = focusManager
        )

        // Height
        if (profileState.imperialSystem) {
            var isEditing by remember { mutableStateOf(false) }
            var feetText by remember { mutableStateOf((profileState.height / 2.54f / 12).toInt().toString()) }
            var inchesText by remember { mutableStateOf(((profileState.height / 2.54f) % 12).toInt().toString()) }
            val feetIsValid = feetText.toFloatOrNull() != null && feetText.toFloat() > 0
            val inchesIsValid = inchesText.toFloatOrNull() != null && inchesText.toFloat() in 0f..11f

            LaunchedEffect(profileState.height) {
                if (!isEditing) {
                    feetText = (profileState.height / 2.54f / 12).toInt().toString()
                    inchesText = ((profileState.height / 2.54f) % 12).toInt().toString()
                }
            }

            val submitValue = {
                if (feetIsValid && inchesIsValid) {
                    viewModel.onEvent(
                        ProfileEvent.UpdateHeight(
                            (feetText.toFloat() * 12 + inchesText.toFloat()) * 2.54f
                        )
                    )
                    isEditing = false
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isEditing) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.height),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.feet_inches_value, feetText, inchesText),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit_icon_measurement_i, stringResource(R.string.height)))
                    }
                } else {
                    OutlinedTextField(
                        value = feetText,
                        onValueChange = { feetText = it },
                        label = { Text(stringResource(R.string.height)) },
                        suffix = { Text(stringResource(R.string.unit_ft)) },
                        isError = !feetIsValid,
                        supportingText = {
                            if (!feetIsValid) {
                                Text(stringResource(R.string.please_enter_a_valid_number))
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitValue() }),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = inchesText,
                        onValueChange = { inchesText = it },
                        label = { Text(stringResource(R.string.height)) },
                        suffix = { Text(stringResource(R.string.unit_in)) },
                        isError = !inchesIsValid,
                        supportingText = {
                            if (!inchesIsValid) {
                                Text(stringResource(R.string.please_enter_a_valid_number))
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitValue() }),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (inchesIsValid && feetIsValid) {
                                submitValue()
                            } else {
                                feetText = (profileState.height / 2.54f / 12).toInt().toString()
                                inchesText = ((profileState.height / 2.54f) % 12).toInt().toString()
                                isEditing = false
                            }
                        }
                    ) {
                        if (inchesIsValid && feetIsValid) {
                            Icon(Icons.Default.Done, stringResource(R.string.done_icon))
                        } else {
                            Icon(Icons.Default.Close, stringResource(R.string.cancel_icon))
                        }
                    }
                }
            }
        } else {
            MeasurementRow(
                label = stringResource(R.string.height),
                value = profileState.height,
                unit = "cm",
                onValueChange = { newHeight ->
                    viewModel.onEvent(
                        ProfileEvent.UpdateHeight(
                            newHeight
                        )
                    )
                },
                keyboardController = keyboardController,
                focusManager = focusManager
            )
        }

        // BMI
        val bmi = if (profileState.height != 0f)
            profileState.weight / (profileState.height/100).pow(2)
        else
            0f
        val bmiCategory = when {
            bmi < 18.5f -> stringResource(R.string.underweight)
            bmi > 30f -> stringResource(R.string.obese)
            bmi > 25f -> stringResource(R.string.overweight)
            else -> stringResource(R.string.normal)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.body_mass_index),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(bmi * 10).roundToInt() / 10.0} ($bmiCategory)",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = onBmiInfoClick) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.info_icon_bmi))
            }
        }
    }
}

@Composable
fun MeasurementRow(
    label: String,
    value: Float,
    unit: String,
    onValueChange: (Float) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(value.toString()) }
    val isValid by remember { derivedStateOf {
        textValue.toFloatOrNull() != null && textValue.toFloat() > 0
    }}

    LaunchedEffect(value) {
        if (!isEditing) {
            textValue = value.toString()
        }
    }

    val submitValue = {
        if (isValid) {
            onValueChange(textValue.toFloat())
            isEditing = false
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isEditing) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${value.toInt()} $unit",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, stringResource(R.string.edit_icon_measurement_i, label))
            }
        } else {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(label) },
                suffix = { Text(unit) },
                isError = !isValid,
                supportingText = {
                    if (!isValid) {
                        Text(stringResource(R.string.please_enter_a_valid_number))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitValue() }),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (isValid) {
                        submitValue()
                    } else {
                        textValue = value.toString()
                        isEditing = false
                    }
                }
            ) {
                if (isValid) {
                    Icon(Icons.Default.Done, stringResource(R.string.done_icon))
                } else {
                    Icon(Icons.Default.Close, stringResource(R.string.cancel_icon))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferencesContent(
    profileState: ProfileState,
    viewModel: ProfileViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // get locales from .xml config and add system default
    val locales = context.getLangPreferenceDropdownEntries().plus(
        Pair(
            "und",
            stringResource(R.string.system_default)
    ))
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GroupedCard(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
        ) {
            subCard(
                onClick = {
                    viewModel.onEvent(ProfileEvent.SwitchImperialSystem(
                        !profileState.imperialSystem
                    ))
                }
            ) {
                // Imperial System Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.use_imperial_system),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = profileState.imperialSystem,
                        onCheckedChange = { viewModel.onEvent(ProfileEvent.SwitchImperialSystem(it)) }
                    )
                }
            }
            subCard {
                // Dark Theme Dropdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.theme),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .weight(5.8f),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        // FIXME: "Same as system" overflows
                        Theme.entries.forEachIndexed { index, theme ->
                            val modifier = if (theme == profileState.theme)
                                Modifier.weight(1f + ButtonGroupDefaults.ExpandedRatio) // expanded
                            else Modifier.weight(1f)

                            var textAlign = when (index) {
                                0 -> TextAlign.End
                                Theme.entries.lastIndex -> TextAlign.Start
                                else -> TextAlign.Center
                            }
                            if (theme == profileState.theme) {
                                textAlign = TextAlign.Center
                            }
                            ToggleButton(
                                checked = theme == profileState.theme,
                                onCheckedChange = {
                                    viewModel.onEvent(ProfileEvent.UpdateTheme(theme))
                                },
                                modifier = modifier,
                                shapes =
                                    when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        Theme.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                            ) {
                                Text(
                                    stringResource(theme.displayRes),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = textAlign,
                                )
                            }
                        }
                    }
                }
            }
            subCard(
                onClick = { expanded = !expanded }
            ) {
                // select language
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // LocaleManager is only available for tiramisu+
                        // compat version does not work very well with compose
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = locales[profileState.language]
                                    ?: stringResource(R.string.system_default),
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expanded
                                    )
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    true
                                )
                            )
                            DropdownMenuPopup(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.exposedDropdownSize()
                            ) {
                                DropdownMenuGroup(
                                    shapes = MenuDefaults.groupShape(0, 1),
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    locales.forEach { locale ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    locale.value,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            },
                                            onClick = {
                                                expanded = false
                                                if (locale.key != "und") {
                                                    viewModel.onEvent(
                                                        ProfileEvent.ChangeLanguage(
                                                            locale.key
                                                        )
                                                    )
                                                } else {
                                                    viewModel.onEvent(
                                                        ProfileEvent.ChangeLanguage(
                                                            null
                                                        )
                                                    )
                                                }
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                            trailingIcon = if (
                                                    locale.key == profileState.language || (locale.key == "und" && profileState.language == null)
                                                ) {
                                                {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        null
                                                    )
                                                }
                                            } else null,
                                            selected = locale.key == profileState.language || (locale.key == "und" && profileState.language == null),
                                            shapes = MenuDefaults.itemShapes(),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            stringResource(R.string.inapp_language_selector_not_supported),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            subCard(
                onClick = {
                    viewModel.onEvent(ProfileEvent.ToggleLockHorizontalScroll(
                        !profileState.lockHorizontalScroll
                    ))
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.lock_workout_scroll),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = profileState.lockHorizontalScroll,
                        onCheckedChange = {
                            viewModel.onEvent(
                                ProfileEvent.ToggleLockHorizontalScroll(
                                    it
                                )
                            )
                        }
                    )
                }
            }
            subCard(
                onClick = {
                    viewModel.onEvent(ProfileEvent.ToggleCrashReporting(
                        !profileState.crashReporting
                    ))
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.send_crash_reports),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.send_crash_reports_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = profileState.crashReporting,
                        onCheckedChange = {
                            viewModel.onEvent(ProfileEvent.ToggleCrashReporting(it))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun IncrementRow(
    label: String,
    value: Float,
    unit: String,
    onValueChange: (Float) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    isValueValid: (Float) -> Boolean = { it > 0f },
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(value.toString()) }
    val isValid by remember { derivedStateOf {
        textValue.toFloatOrNull()?.let(isValueValid) == true
    }}

    LaunchedEffect(value) {
        if (!isEditing) {
            textValue = value.toString()
        }
    }

    val submitValue = {
        if (isValid) {
            onValueChange(textValue.toFloat())
            isEditing = false
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isEditing) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$value $unit",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, stringResource(R.string.edit_icon_increment_i, label))
            }
        } else {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(label) },
                suffix = { Text(unit) },
                isError = !isValid,
                supportingText = {
                    if (!isValid) {
                        Text(stringResource(R.string.please_enter_a_valid_number))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitValue() }),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (isValid) {
                        submitValue()
                    } else {
                        textValue = value.toString()
                        isEditing = false
                    }
                }
            ) {
                if (isValid) {
                    Icon(Icons.Default.Done, stringResource(R.string.done_icon))
                } else {
                    Icon(Icons.Default.Close, stringResource(R.string.cancel_icon))
                }
            }
        }
    }
}

@Composable
fun SmartFeaturesContent(
    profileState: ProfileState,
    viewModel: ProfileViewModel,
) {
    GroupedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        subCard(
            onClick = {
                viewModel.onEvent(ProfileEvent.ToggleSuggestRepsWeight(
                    !profileState.suggestRepsWeight
                ))
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.suggest_reps_weight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = profileState.suggestRepsWeight,
                    onCheckedChange = {
                        viewModel.onEvent(ProfileEvent.ToggleSuggestRepsWeight(
                            !profileState.suggestRepsWeight
                        ))
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            WorkoutRepsWeightRow(
                "8",
                "25",
                0,
                0,
                totalWarmupSets = 0,
                projectedReps = if (profileState.suggestRepsWeight) "10" else null,
                projectedWeight = if (profileState.suggestRepsWeight) "30" else null,
                toBeDone = true,
                isDurationBased = false,
                imperialSystem = profileState.imperialSystem,
                setType = SetType.NORMAL,
                updateRowValues = { _, _ -> },
                deleteSet = {  },
                updateBottomBar = { _, _ -> },
                updateSetType = { _ -> },
            )
        }
        subCard(onClick = {
            viewModel.onEvent(ProfileEvent.ToggleSuggestWorkoutModifications(
                !profileState.suggestWorkoutModifications
            ))
        }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.suggest_workout_modifications),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = profileState.suggestWorkoutModifications,
                    onCheckedChange = {
                        viewModel.onEvent(ProfileEvent.ToggleSuggestWorkoutModifications(it))
                    }
                )
            }
            AnimatedVisibility(
                profileState.suggestWorkoutModifications,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Spacer(Modifier.height(16.dp))
                SuggestModificationCard(
                    isLoading = false,
                    hasDoneSomeSets = false,
                    modificationSuggestion = ModificationSuggestion(
                        type = WorkoutRecord.ModificationType.EXERCISE_SKIPPED,
                        originalModification = WorkoutRecord.WorkoutModification(
                            sourceProgramExerciseId = 0L,
                            sourceExerciseId = 0L,
                            sourceWorkoutExerciseId = 0L,
                            targetWorkoutExerciseId = 0L,
                            targetExerciseId = 0L,
                            modificationType = WorkoutRecord.ModificationType.EXERCISE_SKIPPED
                        )
                    ),
                    onAcceptSuggestion = { },
                    disableActions = true
                )
            }
        }
    }
}

@Composable
fun WearableContent(
    profileState: ProfileState,
    viewModel: ProfileViewModel,
) {
    var helpDialogShown by remember { mutableStateOf(false) }

    InfoDialog(
        dialogueIsOpen = helpDialogShown,
        toggleDialogue = { helpDialogShown = !helpDialogShown }
    ) {
        Text(stringResource(R.string.wearable_help_info))
    }

    GroupedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        if (!profileState.hasConnectedWatch) {
            subCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.no_wearable_connected),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            stringResource(R.string.no_wearable_connected_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { helpDialogShown = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            stringResource(R.string.info_icon_wearable)
                        )
                    }
                }
            }
        } else {
            subCard(
                onClick = {
                    viewModel.onEvent(ProfileEvent.ToggleAutoOpenWear(!profileState.autoOpenWear))
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.auto_open_wearos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = profileState.autoOpenWear,
                        onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleAutoOpenWear(it)) }
                    )
                }
            }
            subCard(
                onClick = {
                    viewModel.onEvent(ProfileEvent.ToggleWearKeepScreenOn(!profileState.wearKeepScreenOn))
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.wear_keep_screen_on),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.wear_keep_screen_on_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = profileState.wearKeepScreenOn,
                        onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleWearKeepScreenOn(it)) }
                    )
                }
            }
            subCard(onClick = {
                viewModel.onEvent(ProfileEvent.ToggleInRestHints(!profileState.inRestHints))
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.in_rest_hints_wear),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = profileState.inRestHints,
                        onCheckedChange = {
                            viewModel.onEvent(ProfileEvent.ToggleInRestHints(it))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperOptionsContent(
    profileState: ProfileState,
    snackbarHost: SnackbarHostState,
    viewModel: ProfileViewModel,
) {
    val noCompatibleWatchMessage = stringResource(R.string.requires_compatible_wearable)
    val scope = rememberCoroutineScope()
    val createDocForSensorRecordings = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mimeType = "application/zip")
    ) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.ExportSensorRecordings(it)) }
    }
    var showDeleteSensorRecordingsDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteSensorRecordingsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSensorRecordingsDialog = false },
            title = { Text(stringResource(R.string.sensor_recordings_delete_title)) },
            text = {
                Text(pluralStringResource(
                    R.plurals.sensor_recordings_delete_text,
                    profileState.sensorRecordingsCount,
                    profileState.sensorRecordingsCount
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteSensorRecordingsDialog = false
                    viewModel.onEvent(ProfileEvent.DeleteSensorRecordings)
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSensorRecordingsDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    GroupedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        subCard(onClick = {
            viewModel.onEvent(ProfileEvent.ToggleDeveloperMode(false))
        }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.developer_mode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.developer_mode_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = true,
                    onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleDeveloperMode(it)) }
                )
            }
        }
        subCard(onClick = {
            viewModel.onEvent(ProfileEvent.ToggleSmartRest(!profileState.smartRest))
        }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.smart_rest_wear),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = profileState.smartRest,
                    onCheckedChange = {
                        viewModel.onEvent(ProfileEvent.ToggleSmartRest(it))
                    }
                )
            }
        }
        if (profileState.smartRest) {
            smartRestParamsCards(
                params = profileState.smartRestParams,
                onParamsChange = { viewModel.onEvent(ProfileEvent.UpdateSmartRestParams(it)) }
            )
        }
        subCard(onClick = {
            if (profileState.wearSupportsTempoRom == true) {
                viewModel.onEvent(ProfileEvent.ToggleTempoRomTracking(
                    !profileState.tempoRomTracking
                ))
            } else {
                scope.launch {
                    snackbarHost.showSnackbar(noCompatibleWatchMessage)
                }
            }
        }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val tempoEnabled = profileState.wearSupportsTempoRom == true
                Text(
                    stringResource(R.string.tempo_rom_tracking_wear),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tempoEnabled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = profileState.tempoRomTracking,
                    onCheckedChange = {
                        viewModel.onEvent(ProfileEvent.ToggleTempoRomTracking(it))
                    },
                    enabled = tempoEnabled
                )
            }
        }
        subCard(onClick = {
            viewModel.onEvent(ProfileEvent.ToggleRecordSensorData(!profileState.recordSensorData))
        }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.record_sensor_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.record_sensor_data_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = profileState.recordSensorData,
                    onCheckedChange = {
                        viewModel.onEvent(ProfileEvent.ToggleRecordSensorData(it))
                    }
                )
            }
        }
        subCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    pluralStringResource(
                        R.plurals.sensor_recordings_count,
                        profileState.sensorRecordingsCount,
                        profileState.sensorRecordingsCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (profileState.isSensorRecordingsExportLoading) {
                    CircularWavyProgressIndicator()
                } else {
                    val hasRecordings = profileState.sensorRecordingsCount > 0
                    TextButton(
                        onClick = {
                            val date = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                            createDocForSensorRecordings.launch("sensor_recordings_$date.zip")
                        },
                        enabled = hasRecordings
                    ) {
                        Text(stringResource(R.string.backup_export))
                    }
                    TextButton(
                        onClick = { showDeleteSensorRecordingsDialog = true },
                        enabled = hasRecordings
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

// subCard contents are composable but the GroupedCard builder is not
private fun GroupedCardScope.smartRestParamsCards(
    params: SmartRestParams,
    onParamsChange: (SmartRestParams) -> Unit,
) {
    subCard {
        IncrementRow(
            label = stringResource(R.string.smart_rest_recovery_fraction),
            value = params.recoveryFraction * 100f,
            unit = stringResource(R.string.unit_percent),
            onValueChange = { onParamsChange(params.copy(recoveryFraction = it / 100f)) },
            keyboardController = LocalSoftwareKeyboardController.current,
            focusManager = LocalFocusManager.current,
            isValueValid = { it > 0f && it <= 100f },
        )
    }
    subCard {
        IncrementRow(
            label = stringResource(R.string.smart_rest_min_target_above_baseline),
            value = params.minTargetAboveBaseline.toFloat(),
            unit = stringResource(R.string.unit_bpm),
            onValueChange = { onParamsChange(params.copy(minTargetAboveBaseline = it.roundToInt())) },
            keyboardController = LocalSoftwareKeyboardController.current,
            focusManager = LocalFocusManager.current,
            isValueValid = { it >= 0f },
        )
    }
    subCard {
        IncrementRow(
            label = stringResource(R.string.smart_rest_baseline_percentile),
            value = params.baselinePercentile * 100f,
            unit = stringResource(R.string.unit_percent),
            onValueChange = { onParamsChange(params.copy(baselinePercentile = it / 100f)) },
            keyboardController = LocalSoftwareKeyboardController.current,
            focusManager = LocalFocusManager.current,
            isValueValid = { it >= 0f && it < 100f },
        )
    }
    subCard {
        IncrementRow(
            label = stringResource(R.string.smart_rest_peak_lookback),
            value = params.peakLookbackSecs.toFloat(),
            unit = stringResource(R.string.unit_seconds_short),
            onValueChange = { onParamsChange(params.copy(peakLookbackSecs = it.roundToLong())) },
            keyboardController = LocalSoftwareKeyboardController.current,
            focusManager = LocalFocusManager.current,
            isValueValid = { it >= 0f },
        )
    }
    subCard {
        IncrementRow(
            label = stringResource(R.string.smart_rest_smoothing_window),
            value = params.smoothingWindowSecs.toFloat(),
            unit = stringResource(R.string.unit_seconds_short),
            onValueChange = { onParamsChange(params.copy(smoothingWindowSecs = it.roundToLong())) },
            keyboardController = LocalSoftwareKeyboardController.current,
            focusManager = LocalFocusManager.current,
            isValueValid = { it >= 1f },
        )
    }
    if (params != SmartRestParams()) {
        subCard(onClick = { onParamsChange(SmartRestParams()) }) {
            Text(
                stringResource(R.string.smart_rest_reset_params),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun ExternalLink(title: String, description: String, leadingIcon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                title,
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.width(16.dp))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            stringResource(R.string.open_in_icon_browser),
        )
    }
}
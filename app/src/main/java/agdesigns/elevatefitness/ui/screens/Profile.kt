package agdesigns.elevatefitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.Sex
import agdesigns.elevatefitness.data.Theme
import agdesigns.elevatefitness.ui.BottomNavigationGraph
import agdesigns.elevatefitness.ui.FadeTransition
import agdesigns.elevatefitness.ui.components.InfoDialog
import agdesigns.elevatefitness.ui.maybeKgToLb
import agdesigns.elevatefitness.ui.maybeLbToKg
import agdesigns.elevatefitness.viewmodels.ProfileEvent
import agdesigns.elevatefitness.viewmodels.ProfileViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.pow
import kotlin.math.round

@Destination<BottomNavigationGraph>(style = FadeTransition::class)
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
fun Profile(
    destinationsNavigator: DestinationsNavigator,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.state.collectAsState()
    var editName by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    LaunchedEffect(profileState.name){
        name = profileState.name
    }
    var userYear by remember { mutableStateOf("0") }
    val validUserYear by remember { derivedStateOf {
        userYear.toIntOrNull() != null && userYear.toInt() in 1900..ZonedDateTime.now().year
    }}
    LaunchedEffect(profileState.userYear){
        userYear = profileState.userYear.toString()
    }
    var editYear by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var bmiDialogueShown by remember { mutableStateOf(false) }
    InfoDialog(dialogueIsOpen = bmiDialogueShown,
        toggleDialogue = { bmiDialogueShown = !bmiDialogueShown }
    ) {
        Text(stringResource(R.string.bmi_info))
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
        .padding(horizontal = 16.dp)
        .fillMaxSize()
    ) {
        item {
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(2f)){
                    Text("Hi, ${profileState.name}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(2f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        editName = !editName
                        if (!editName)
                            viewModel.onEvent(ProfileEvent.UpdateName(name))
                    }, modifier = Modifier.weight(0.2f)
                    ) {
                        if (editName)
                            Icon(Icons.Default.Done, "Done")
                        else
                            Icon(Icons.Default.Edit, "Edit name")
                    }
                }
                Spacer(Modifier.width(16.dp))
//                Icon(Icons.Default.AccountCircle, "Profile",
//                    Modifier
//                        .size(60.dp)
//                        .weight(0.5f))
            }
            Spacer(Modifier.height(8.dp))
            if (editName){
                OutlinedTextField(value = name,
                    onValueChange = { name = it },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            editName = false
                            viewModel.onEvent(ProfileEvent.UpdateName(name))
                        }
                    ), modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val age by remember {
                    derivedStateOf {
                        ZonedDateTime.now().year - profileState.userYear
                    }
                }
                Text("Age: $age", Modifier.weight(1f))

                IconButton(onClick = {
                    editYear = !editYear
                    if (!editYear) {
                        if (validUserYear)
                            viewModel.onEvent(ProfileEvent.UpdateAgeYear(userYear.toInt()))
                        else
                            // reset userYear
                            userYear = profileState.userYear.toString()
                    }
                }) {
                    if (editYear)
                        if (validUserYear)
                            Icon(Icons.Default.Done, "Done")
                        else
                            Icon(Icons.Default.Close, "Cancel")
                    else
                        Icon(Icons.Default.Edit, "Edit age")
                }
            }
        }

        if(editYear){
            item {
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val focusRequester = remember { FocusRequester() }
                    Text("Born in: ", Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = userYear,
                            onValueChange = { userYear = it },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            isError = !validUserYear,
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (validUserYear) {
                                        keyboardController?.hide()
                                        viewModel.onEvent(ProfileEvent.UpdateAgeYear(userYear.toInt()))
                                        editYear = false
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            supportingText = {
                                if (!validUserYear)
                                    Text("Please enter a valid number")
                            },
                            modifier = Modifier
                                .widthIn(1.dp, Dp.Infinity)
                                .focusRequester(focusRequester)
                        )
                    }
                }
            }
        }
        item {
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                var showUpdateWeightButton by remember { mutableStateOf(false) }
                var weightValue by remember { mutableStateOf("") }
                val validWeight by remember { derivedStateOf { weightValue.toFloatOrNull() != null && weightValue.toFloat() > 0 }}
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(profileState.weight, profileState.imperialSystem){
                    weightValue = maybeKgToLb(
                        profileState.weight,
                        profileState.imperialSystem
                    ).toString()
                }
                val updateWeight: (String) -> Unit = { newWeight ->
                    keyboardController?.hide()
                    viewModel.onEvent(ProfileEvent.UpdateWeight(
                        maybeLbToKg(newWeight.toFloat(), profileState.imperialSystem)
                    ))
                    showUpdateWeightButton = false
                    focusManager.clearFocus()
                }
                Text("Weight: ", Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = weightValue,
                        onValueChange = { weightValue = it },
                        trailingIcon = {
                            if (profileState.imperialSystem)
                                Text("lb")
                            else
                                Text("kg")
                        },
                        isError = !validWeight,
                        supportingText = {
                            if (!validWeight)
                                Text("Please enter a valid number")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (validWeight)
                                    updateWeight(weightValue)
                            }
                        ),
                        modifier = Modifier
                            .widthIn(1.dp, Dp.Infinity)
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.hasFocus || it.isFocused) {
                                    showUpdateWeightButton = true
                                } // TODO: maybe save weight on focus lose
                            }
                    )
                    if (showUpdateWeightButton) {
                        IconButton(onClick = {
                            if (validWeight)
                                updateWeight(weightValue)
                            else
                                weightValue = maybeKgToLb(
                                    profileState.weight,
                                    profileState.imperialSystem
                                ).toString()
                        }) {
                            if (validWeight)
                                Icon(Icons.Default.Done, "Done editing")
                            else
                                Icon(Icons.Default.Close, "Cancel")
                        }
                    }
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                var showUpdateHeightButton by remember { mutableStateOf(false) }
                var heightValue by remember { mutableStateOf("") }
                val validHeight by remember { derivedStateOf { heightValue.toFloatOrNull() != null && heightValue.toFloat() > 0 } }
                LaunchedEffect(profileState.height, profileState.imperialSystem) {
                    heightValue = if (profileState.imperialSystem)
                        "${profileState.height / 2.54f}"
                    else
                        profileState.height.toString()
                }
                val focusRequester = remember { FocusRequester() }
                val updateHeight: (String) -> Unit = { newHeight ->
                    keyboardController?.hide()
                    viewModel.onEvent(
                        ProfileEvent.UpdateHeight(
                            if (profileState.imperialSystem)
                                newHeight.toFloat() * 2.54f
                            else
                                newHeight.toFloat()
                        )
                    )
                    showUpdateHeightButton = false
                    focusManager.clearFocus()
                }
                Text("Height: ", Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = heightValue,
                        onValueChange = { heightValue = it },
                        trailingIcon = {
                            if (profileState.imperialSystem)
                                Text("in")
                            else
                                Text("cm")
                        },
                        isError = !validHeight,
                        supportingText = {
                            if (!validHeight)
                                Text("Please enter a valid number")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (validHeight)
                                    updateHeight(heightValue)
                            }
                        ), modifier = Modifier
                            .widthIn(1.dp, Dp.Infinity)
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.hasFocus || it.isFocused) {
                                    showUpdateHeightButton = true
                                } // TODO: maybe save weight on focus lose
                            }
                    )
                    if (showUpdateHeightButton) {
                        IconButton(onClick = {
                            if (validHeight)
                                updateHeight(heightValue)
                            else
                                heightValue = profileState.height.toString()
                        }) {
                            if (validHeight)
                                Icon(Icons.Default.Done, "Done editing")
                            else
                                Icon(Icons.Default.Close, "Cancel")
                        }
                    }
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Biological sex: ")
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .widthIn(1.dp, Dp.Infinity)
                        .weight(1f)
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = profileState.sex.sexName,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        Sex.entries.forEach { selectionOption -> // fixme
                            DropdownMenuItem(
                                text = { Text(selectionOption.sexName) },
                                onClick = {
                                    viewModel.onEvent(ProfileEvent.UpdateSex(selectionOption))
                                    expanded = false
                                    focusManager.clearFocus()
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        }
        item {
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val bmi by remember { derivedStateOf {
                    profileState.weight / (profileState.height/100).pow(2)
                }}
                val body = if (bmi < 18.5f) "underweight" else
                    if (bmi > 30f) "obese" else
                        if (bmi > 25f) "overweight" else "normal"
                Text("Body Mass Index: ${round(bmi*10f)/10} ($body)", Modifier.weight(1f))

                IconButton(onClick = { bmiDialogueShown = true }) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, "Help/info")
                }
            }
        }
        item {
            HorizontalDivider()
        }
//        Text("Preferences", style = MaterialTheme.typography.labelMedium)
//        Spacer(Modifier.height(16.dp))
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use imperial system: ", Modifier.weight(1f))

                Switch(  // FIXME: should use segmented buttons when available
                    modifier = Modifier.semantics {
                        contentDescription = "Switch to imperial system"
                    },
                    checked = profileState.imperialSystem,
                    onCheckedChange = { viewModel.onEvent(ProfileEvent.SwitchImperialSystem(it)) }
                )
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dark theme: ", Modifier.weight(1f))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .widthIn(1.dp, Dp.Infinity)
                        .weight(1f)
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = profileState.theme.themeName,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        Theme.entries.forEach { selectionOption -> // fixme
                            DropdownMenuItem(
                                text = { Text(selectionOption.themeName) },
                                onClick = {
                                    viewModel.onEvent(ProfileEvent.UpdateTheme(selectionOption))
                                    expanded = false
                                    focusManager.clearFocus()
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        }
        item {
            ChangeGenericWeight(
                text = "Default barbell increment",
                initialWeightValue = profileState.incrementBarbell,
                imperialSystem = profileState.imperialSystem,
                keyboardController = keyboardController,
                focusManager = focusManager,
                doNotConvert = true // the increment do not change with system
            ) {
                viewModel.onEvent(
                    ProfileEvent.UpdateIncrementBarbell(
                        it.toFloat()
                    )
                )
            }
        }
        item {
            ChangeGenericWeight(
                text = "Default bodyweight increment",
                initialWeightValue = profileState.incrementBodyweight,
                imperialSystem = profileState.imperialSystem,
                keyboardController = keyboardController,
                focusManager = focusManager,
                doNotConvert = true
            ) {
                viewModel.onEvent(
                    ProfileEvent.UpdateIncrementBodyweight(
                        it.toFloat()
                    )
                )
            }
        }
        item {
            ChangeGenericWeight(
                text = "Default cable increment",
                initialWeightValue = profileState.incrementCable,
                imperialSystem = profileState.imperialSystem,
                keyboardController = keyboardController,
                focusManager = focusManager,
                doNotConvert = true
            ) {
                viewModel.onEvent(
                    ProfileEvent.UpdateIncrementCable(
                        it.toFloat()
                    )
                )
            }
        }
        item {
            ChangeGenericWeight(
                text = "Default dumbbell increment",
                initialWeightValue = profileState.incrementDumbbell,
                imperialSystem = profileState.imperialSystem,
                keyboardController = keyboardController,
                focusManager = focusManager,
                doNotConvert = true
            ) {
                viewModel.onEvent(
                    ProfileEvent.UpdateIncrementDumbbell(
                        it.toFloat()
                    )
                )
            }
        }
        item {
            ChangeGenericWeight(
                text = "Default machine increment",
                initialWeightValue = profileState.incrementMachine,
                imperialSystem = profileState.imperialSystem,
                keyboardController = keyboardController,
                focusManager = focusManager,
                doNotConvert = true
            ) {
                viewModel.onEvent(
                    ProfileEvent.UpdateIncrementMachine(
                        it.toFloat()
                    )
                )
            }
        }
    }
}


@Composable
fun LazyItemScope.ChangeGenericWeight(
    text: String,
    initialWeightValue: Float,
    imperialSystem: Boolean,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    doNotConvert: Boolean = false, // only show lb/kg without converting
    updateWeight: (String) -> Unit
) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        var showUpdateWeightButton by remember { mutableStateOf(false) }
        var weightValue by remember { mutableStateOf("") }
        LaunchedEffect(initialWeightValue, imperialSystem){
            weightValue = if (doNotConvert)
                initialWeightValue.toString()
            else
                maybeKgToLb(
                    initialWeightValue,
                    imperialSystem
                ).toString()
        }
        val validWeight by remember { derivedStateOf { weightValue.toFloatOrNull() != null && weightValue.toFloat() > 0 }}
        val focusRequester = remember { FocusRequester() }

        val updateWeightAndClose: (String) -> Unit = { newWeight ->
            keyboardController?.hide()
            updateWeight(newWeight)
            showUpdateWeightButton = false
            focusManager.clearFocus()
        }
        Text("$text: ", Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = weightValue,
                onValueChange = { weightValue = it },
                trailingIcon = {
                    if (imperialSystem)
                        Text("lb")
                    else
                        Text("kg")
                },
                isError = !validWeight,
                supportingText = {
                    if (!validWeight)
                        Text("Please enter a valid number")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (validWeight)
                            updateWeightAndClose(weightValue)
                    }
                ),
                modifier = Modifier
                    .widthIn(1.dp, Dp.Infinity)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (it.hasFocus || it.isFocused) {
                            showUpdateWeightButton = true
                        } // TODO: maybe save weight on focus lose
                    }
            )
            if (showUpdateWeightButton) {
                IconButton(onClick = {
                    if (validWeight)
                        updateWeightAndClose(weightValue)
                    else
                        weightValue = maybeKgToLb(
                            initialWeightValue,
                            imperialSystem
                        ).toString()
                }) {
                    if (validWeight)
                        Icon(Icons.Default.Done, "Done editing")
                    else
                        Icon(Icons.Default.Close, "Cancel")
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
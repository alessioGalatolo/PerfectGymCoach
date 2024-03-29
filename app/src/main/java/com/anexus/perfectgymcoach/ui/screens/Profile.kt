package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.Sex
import com.anexus.perfectgymcoach.data.Theme
import com.anexus.perfectgymcoach.ui.BottomNavigationNavGraph
import com.anexus.perfectgymcoach.ui.components.InfoDialog
import com.anexus.perfectgymcoach.ui.maybeKgToLb
import com.anexus.perfectgymcoach.ui.maybeLbToKg
import com.anexus.perfectgymcoach.viewmodels.ProfileEvent
import com.anexus.perfectgymcoach.viewmodels.ProfileViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.util.*
import kotlin.math.pow
import kotlin.math.round

@BottomNavigationNavGraph
@Destination
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
fun Profile(
    destinationsNavigator: DestinationsNavigator,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var editName by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    LaunchedEffect(viewModel.state.value.name){
        name = viewModel.state.value.name
    }
    var userYear by remember { mutableStateOf("0") }
    LaunchedEffect(viewModel.state.value.userYear){
        userYear = viewModel.state.value.userYear.toString()
    }
    var editYear by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var genderDialogueShown by remember { mutableStateOf(false) }
    InfoDialog(dialogueIsOpen = genderDialogueShown,
        toggleDialogue = { genderDialogueShown = !genderDialogueShown }
    ) {
        Text(stringResource(R.string.gender_info))
    }
    var bmiDialogueShown by remember { mutableStateOf(false) }
    InfoDialog(dialogueIsOpen = bmiDialogueShown,
        toggleDialogue = { bmiDialogueShown = !bmiDialogueShown }
    ) {
        Text(stringResource(R.string.bmi_info))
    }
    Column(modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
        .padding(top = 16.dp)
        .fillMaxSize()) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(2f)){
                Text("Hi, ${viewModel.state.value.name}",
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
                    Icon(if (editName) Icons.Default.Done else Icons.Default.Edit, null)
                }
            }
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Default.AccountCircle, null,
                Modifier
                    .size(60.dp)
                    .weight(0.5f))
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
        Spacer(Modifier.height(16.dp))

        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val age by remember { derivedStateOf { Calendar.getInstance().get(Calendar.YEAR) - viewModel.state.value.userYear }}
            Text("Age: $age")

            IconButton(onClick = { editYear = !editYear
                if(!editYear){
                    viewModel.onEvent(ProfileEvent.UpdateAgeYear(userYear.toInt()))
                }
            }) {
                Icon(if(editYear) Icons.Default.Done else Icons.Default.Edit, null)
            }
        }
        Spacer(Modifier.height(16.dp))
        if(editYear){
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val focusRequester = remember { FocusRequester() }
                Text("Born in: ")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = userYear,
                        onValueChange = { userYear = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                viewModel.onEvent(ProfileEvent.UpdateAgeYear(userYear.toInt()))
                                editYear = false
                                focusManager.clearFocus()
                            }
                        ), modifier = Modifier
                            .widthIn(1.dp, Dp.Infinity)
                            .focusRequester(focusRequester)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            var showUpdateWeightButton by remember { mutableStateOf(false) }
            var weightValue by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(viewModel.state.value.weight, viewModel.state.value.imperialSystem){
                weightValue = maybeKgToLb(
                    viewModel.state.value.weight,
                    viewModel.state.value.imperialSystem
                ).toString()
            }
            val updateWeight: (String) -> Unit = { newWeight ->
                keyboardController?.hide()
                viewModel.onEvent(ProfileEvent.UpdateWeight(
                    maybeLbToKg(newWeight.toFloat(), viewModel.state.value.imperialSystem)
                ))
                showUpdateWeightButton = false
                focusManager.clearFocus()
            }
            Text("Weight: ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = weightValue,
                    onValueChange = { weightValue = it },
                    trailingIcon = {
                        if (viewModel.state.value.imperialSystem)
                            Text("lb")
                        else
                            Text("kg")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            updateWeight(weightValue)
                        }
                    ), modifier = Modifier
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
                        updateWeight(weightValue)
                    }) {
                        Icon(Icons.Default.Done, null)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            var showUpdateHeightButton by remember { mutableStateOf(false) }
            var heightValue by remember { mutableStateOf("") }

            LaunchedEffect(viewModel.state.value.height, viewModel.state.value.imperialSystem){
                heightValue = if (viewModel.state.value.imperialSystem)
                    "${viewModel.state.value.height / 2.54f}"
                else
                    viewModel.state.value.height.toString()
            }
            val focusRequester = remember { FocusRequester() }
            val updateHeight: (String) -> Unit = { newHeight ->
                keyboardController?.hide()
                viewModel.onEvent(ProfileEvent.UpdateHeight(
                    if (viewModel.state.value.imperialSystem)
                        newHeight.toFloat() * 2.54f
                    else
                        newHeight.toFloat()
                ))
                showUpdateHeightButton = false
                focusManager.clearFocus()
            }
            Text("Height: ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = heightValue,
                    onValueChange = { heightValue = it },
                    trailingIcon = {
                        if (viewModel.state.value.imperialSystem)
                            Text("in")
                        else
                            Text("cm")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
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
                        updateHeight(heightValue)
                    }) {
                        Icon(Icons.Default.Done, null)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Biological sex: ")
            Row (verticalAlignment = Alignment.CenterVertically) {
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
                        value = viewModel.state.value.sex.sexName,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        Sex.values().forEach { selectionOption -> // fixme
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
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = { genderDialogueShown = true },
                    modifier = Modifier.weight(0.2f)) {
                    Icon(Icons.Default.HelpOutline, null)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val bmi by remember { derivedStateOf {
                viewModel.state.value.weight / (viewModel.state.value.height/100).pow(2)
            }}
            val body = if (bmi < 18.5f) "underweight" else
                if (bmi > 30f) "obese" else
                    if (bmi > 25f) "overweight" else "normal"
            Text("Body Mass Index: ${round(bmi*10f)/10} ($body)")

            IconButton(onClick = { bmiDialogueShown = true }) {
                Icon(Icons.Default.HelpOutline, null)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use imperial system: ")

            Switch(  // FIXME: should use segmented buttons when available
                modifier = Modifier.semantics { contentDescription = "Switch to imperial system" },
                checked = viewModel.state.value.imperialSystem,
                onCheckedChange = { viewModel.onEvent(ProfileEvent.SwitchImperialSystem(it)) }
            )
        }
        Spacer(Modifier.height(16.dp))
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dark theme: ")
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
                    value = viewModel.state.value.theme.themeName,
                    onValueChange = {},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    Theme.values().forEach { selectionOption -> // fixme
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
}
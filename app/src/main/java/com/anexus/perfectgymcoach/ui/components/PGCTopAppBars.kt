@file:OptIn(ExperimentalMaterial3Api::class)

package com.anexus.perfectgymcoach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController

@Composable
fun PGCSmallTopBar(scrollBehavior: TopAppBarScrollBehavior,
                   navController: NavHostController,
                   title: @Composable () -> Unit) {
    val backgroundColors = TopAppBarDefaults.smallTopAppBarColors()
    val backgroundColor = backgroundColors.containerColor(
        colorTransitionFraction = scrollBehavior.state.collapsedFraction
    ).value
    val foregroundColors = TopAppBarDefaults.smallTopAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent
    )
    Box (modifier = Modifier.background(backgroundColor)) {
        SmallTopAppBar(
            title = title,
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Go back"
                    )
                }
            },
            colors = foregroundColors,
            scrollBehavior = scrollBehavior,
            modifier = Modifier.statusBarsPadding()
        )
    }
}
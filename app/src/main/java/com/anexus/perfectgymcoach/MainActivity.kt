package com.anexus.perfectgymcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import com.anexus.perfectgymcoach.ui.theme.PerfectGymCoachTheme
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.anexus.perfectgymcoach.data.Repository
import com.anexus.perfectgymcoach.data.Theme
import com.anexus.perfectgymcoach.ui.*
import com.anexus.perfectgymcoach.ui.screens.*
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.popBackStack
import com.ramcosta.composedestinations.navigation.popUpTo
import com.ramcosta.composedestinations.utils.isRouteOnBackStack
import com.ramcosta.composedestinations.utils.startDestination
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: Repository

    @OptIn(ExperimentalAnimationApi::class,
        ExperimentalMaterialNavigationApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ComposeView(this).consumeWindowInsets = true

        setContent {
            val userPreference = repository.getTheme().collectAsState(initial = Theme.SYSTEM)
            val systemTheme = isSystemInDarkTheme()
            val darkTheme by remember { derivedStateOf {
                when (userPreference.value) {
                    Theme.SYSTEM -> systemTheme
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                }
            }}
            // navigation controller for everything (main screen)
            val engine = rememberAnimatedNavHostEngine()
            val navController = engine.rememberNavController()

            PerfectGymCoachTheme (darkTheme = darkTheme) {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    engine = engine,
                    navController = navController
                )
            }
        }
    }
}

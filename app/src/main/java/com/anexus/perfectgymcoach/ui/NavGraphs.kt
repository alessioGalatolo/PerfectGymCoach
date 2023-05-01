package com.anexus.perfectgymcoach.ui

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.RootNavGraph

@RootNavGraph
@NavGraph
annotation class BottomNavigationNavGraph(
    val start: Boolean = false
)

@BottomNavigationNavGraph
@RootNavGraph
@NavGraph
annotation class ChangePlanNavGraph(
    val start: Boolean = false
)

@BottomNavigationNavGraph
@RootNavGraph
@NavGraph
annotation class WorkoutNavGraph(
    val start: Boolean = false
)

@BottomNavigationNavGraph
@RootNavGraph
@NavGraph
annotation class GeneratePlanNavGraph(
    val start: Boolean = false
)
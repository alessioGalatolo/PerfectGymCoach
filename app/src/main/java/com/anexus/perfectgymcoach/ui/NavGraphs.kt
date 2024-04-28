package com.anexus.perfectgymcoach.ui

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.NavHostGraph
import com.ramcosta.composedestinations.annotation.RootGraph

@NavHostGraph
//@NavGraph<RootGraph>
annotation class BottomNavigationGraph(
    val start: Boolean = false
)

@NavGraph<BottomNavigationGraph>
@NavGraph<RootGraph>
annotation class ChangePlanGraph(
    val start: Boolean = false
)

@NavGraph<BottomNavigationGraph>
@NavGraph<RootGraph>
annotation class WorkoutOnlyGraph(  // Note: Needs the "only" otherwise name goes in conflict with the destination
    val start: Boolean = false
)

@NavGraph<BottomNavigationGraph>
@NavGraph<RootGraph>
annotation class GeneratePlanGraph(
    val start: Boolean = false
)
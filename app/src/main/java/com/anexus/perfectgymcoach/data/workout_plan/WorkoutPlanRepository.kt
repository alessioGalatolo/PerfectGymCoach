package com.anexus.perfectgymcoach.data.workout_plan

import javax.inject.Inject

class WorkoutPlanRepository @Inject constructor(private val workoutPlanDatabase: WorkoutPlanDatabase) {

    fun getPlans() = workoutPlanDatabase.workoutPlanDao.getPlans()

    suspend fun addPlan(plan: WorkoutPlan) = workoutPlanDatabase.workoutPlanDao.insert(plan)
}
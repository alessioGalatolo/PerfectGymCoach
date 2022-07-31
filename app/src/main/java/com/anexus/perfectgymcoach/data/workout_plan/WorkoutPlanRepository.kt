package com.anexus.perfectgymcoach.data.workout_plan

import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutPlanRepository @Inject constructor(private val db: WorkoutPlanDatabase) {

    fun getPlans() = db.workoutPlanDao.getPlans()

    fun getPrograms(planId: Int) = db.workoutProgramDao.getPrograms(planId)

    fun getWorkoutExercises(programId: Int) = db.workoutExerciseDao.getExercises(programId)

    fun getExercises(muscle: Exercise.Muscle): Flow<List<Exercise>> {
        return if (muscle == Exercise.Muscle.EVERYTHING) {
            db.exerciseDao.getAllExercises()
        } else {
            db.exerciseDao.getExercises(muscle)
        }
    }

    suspend fun addPlan(plan: WorkoutPlan) = db.workoutPlanDao.insert(plan)

    suspend fun addProgram(program: WorkoutProgram) = db.workoutProgramDao.insert(program)

    suspend fun addWorkoutExercise(exercise: WorkoutExercise) = db.workoutExerciseDao.insert(exercise)

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: WorkoutPlanRepository? = null

        fun getInstance(workoutPlanDatabase: WorkoutPlanDatabase) =
            instance ?: synchronized(this) {
                instance ?: WorkoutPlanRepository(workoutPlanDatabase).also { instance = it }
            }
    }
}
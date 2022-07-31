package com.anexus.perfectgymcoach.data.workout_program

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "program",
    foreignKeys = [ForeignKey(
        entity = WorkoutPlan::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = CASCADE
    )]
)
@Parcelize
data class WorkoutProgram(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val planId: Int,
    val name: String
) : Parcelable
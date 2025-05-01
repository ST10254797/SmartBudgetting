package com.st10254797.smartbudgetting

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal")
data class Goal(
    @PrimaryKey val userId: String,
    val minGoal: Float,
    val maxGoal: Float
)
//GeeksforGeeks, 2021.How to Perform CRUD Operations in Room Database in Android? [online] Available at: https://www.geeksforgeeks.org/how-to-perform-crud-operations-in-room-database-in-android/ (Accessed 28 April 2025)
package com.st10254797.smartbudgetting

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    val date: String,
    val category: Long, // Reference to the category in Room
    val imageUrl: String?, // URL of the uploaded image
    val userId: String // this will store the Firebase UID
)
//GeeksforGeeks, 2021.How to Perform CRUD Operations in Room Database in Android? [online] Available at: https://www.geeksforgeeks.org/how-to-perform-crud-operations-in-room-database-in-android/ (Accessed 28 April 2025)
package com.st10254797.smartbudgetting

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Category::class, Expense::class, Goal::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao  // Add GoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_budgetting_db"
                )
                    .fallbackToDestructiveMigration() // Automatically reset the database on schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
//GeeksforGeeks, 2021.How to Perform CRUD Operations in Room Database in Android? [online] Available at: https://www.geeksforgeeks.org/how-to-perform-crud-operations-in-room-database-in-android/ (Accessed 28 April 2025)
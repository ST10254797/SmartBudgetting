package com.st10254797.smartbudgetting

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: Goal)

    @Query("SELECT * FROM goal WHERE userId = :userId LIMIT 1")
    suspend fun getGoal(userId: String): Goal?

    @Query("SELECT * FROM Goal WHERE userId = :userId LIMIT 1")
    suspend fun getGoalForUser(userId: String): Goal?
}
//GeeksforGeeks, 2021.How to Perform CRUD Operations in Room Database in Android? [online] Available at: https://www.geeksforgeeks.org/how-to-perform-crud-operations-in-room-database-in-android/ (Accessed 28 April 2025)
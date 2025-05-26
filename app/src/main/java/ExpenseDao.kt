package com.st10254797.smartbudgetting

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE category = :categoryId")
    suspend fun getExpensesByCategory(categoryId: Long): List<Expense>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getExpensesByUser(userId: String): List<Expense>

    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getCategoriesByUser(userId: String): List<Category>

    @Query("SELECT * FROM expenses WHERE category = :categoryId AND userId = :userId")
    suspend fun getExpensesByCategory(categoryId: Long, userId: String): List<Expense>

    @Query("""
    SELECT c.name AS category, SUM(e.amount) AS totalAmount
    FROM expenses e
    INNER JOIN categories c ON e.category = c.id
    WHERE e.userId = :userId
    GROUP BY c.name
""")
    suspend fun getCategoryTotalsForUser(userId: String): List<CategoryTotal>

    @Query("""
    SELECT c.name AS category, SUM(e.amount) AS totalAmount
    FROM expenses e
    INNER JOIN categories c ON e.category = c.id
    WHERE e.userId = :userId AND e.date BETWEEN :startDate AND :endDate
    GROUP BY c.name
""")
    suspend fun getCategoryTotalsForUserAndDateRange(userId: String, startDate: String, endDate: String): List<CategoryTotal>


}
//GeeksforGeeks, 2021.How to Perform CRUD Operations in Room Database in Android? [online] Available at: https://www.geeksforgeeks.org/how-to-perform-crud-operations-in-room-database-in-android/ (Accessed 28 April 2025)
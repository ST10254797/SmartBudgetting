package com.st10254797.smartbudgetting

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: Category)

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Int): Category

    // Add delete function
    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategory(name: String)

    @Query("SELECT * FROM categories WHERE name = :name AND userId = :userId LIMIT 1")
    suspend fun getCategoryByNameAndUser(name: String, userId: String): Category?

    @Query("DELETE FROM categories WHERE name = :name AND userId = :userId")
    suspend fun deleteCategoryByNameAndUser(name: String, userId: String)

    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getCategoriesByUser(userId: String): List<Category>
}
//GeeksforGeeks, 2021.How to Perform CRUD Operations in Room Database in Android? [online] Available at: https://www.geeksforgeeks.org/how-to-perform-crud-operations-in-room-database-in-android/ (Accessed 28 April 2025)



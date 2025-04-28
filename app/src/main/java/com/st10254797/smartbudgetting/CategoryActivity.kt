package com.st10254797.smartbudgetting

import kotlinx.coroutines.async
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.st10254797.smartbudgetting.databinding.ActivityCategoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding
    private lateinit var appDatabase: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseDao: ExpenseDao  // Declare expenseDao
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)


        appDatabase = AppDatabase.getDatabase(applicationContext)
        categoryDao = appDatabase.categoryDao()
        expenseDao = appDatabase.expenseDao()  // Initialize expenseDao


        setupWindowInsets()
        setupClickListeners()
        updateCategoryList()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        binding.buttonAddCategory.setOnClickListener { handleAddCategory() }
        binding.buttonDeleteCategory.setOnClickListener { handleDeleteCategory() }
        binding.buttonBackToHome.setOnClickListener { navigateToMainActivity() }
        binding.buttonGoToExpense.setOnClickListener { navigateToExpenseActivity() }
    }

    private fun handleAddCategory() {
        val categoryName = binding.editTextCategoryName.text.toString()

        if (categoryName.isEmpty()) {
            showToast("Please enter a category name")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            val existingCategory = categoryDao.getCategoryByNameAndUser(categoryName, userId)

            if (existingCategory == null) {
                categoryDao.insert(Category(name = categoryName, userId = userId))
                withContext(Dispatchers.Main) {
                    showToast("$categoryName added")
                    binding.editTextCategoryName.text?.clear()
                    updateCategoryList()
                }
            } else {
                showToast("$categoryName already exists")
            }
        }
    }

    private fun handleDeleteCategory() {
        val categoryName = binding.editTextCategoryName.text.toString()

        if (categoryName.isEmpty()) {
            showToast("Please enter a category name to delete")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val userId = auth.currentUser?.uid ?: return@launch
            val existingCategory = categoryDao.getCategoryByNameAndUser(categoryName, userId)

            if (existingCategory != null) {
                categoryDao.deleteCategoryByNameAndUser(categoryName, userId)
                withContext(Dispatchers.Main) {
                    showToast("$categoryName deleted")
                    binding.editTextCategoryName.text?.clear()
                    updateCategoryList()
                }
            } else {
                showToast("$categoryName does not exist")
            }
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToExpenseActivity() {
        Intent(this, ExpenseActivity::class.java).apply {
            binding.editTextCategoryName.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
                putExtra("category_name", it)  // Passing the category name to ExpenseActivity
            }
            startActivityForResult(this, 1)  // Start ExpenseActivity for result
        }
    }
    @SuppressLint("SetTextI18n")
    private fun updateCategoryList() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val categories = categoryDao.getCategoriesByUser(userId)

            withContext(Dispatchers.Main) {
                binding.textViewCategories.text = "Loading..."
                val displayText = StringBuilder()

                if (categories.isEmpty()) {
                    binding.textViewCategories.text = "No categories found."
                    return@withContext
                }

                coroutineScope {
                    categories.forEachIndexed { index, category ->
                        launch {
                            val expenses = fetchExpensesForCategory(userId, category.id)

                            withContext(Dispatchers.Main) {
                                displayText.append("Category: ${category.name}\n")

                                if (expenses.isEmpty()) {
                                    displayText.append("  No expenses\n")
                                } else {
                                    expenses.forEach { expense ->
                                        displayText.append("  • R${"%.2f".format(expense.amount)} - ${expense.description}\n")
                                    }
                                }
                                displayText.append("\n")

                                if (index == categories.lastIndex) {
                                    binding.textViewCategories.text = displayText.toString()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // Add this suspend function
    private suspend fun fetchExpensesForCategory(userId: String, categoryId: Long): List<Expense> {
        return withContext(Dispatchers.IO) {
            expenseDao.getExpensesByCategory(categoryId, userId)
        }
    }

//    private fun updateExpensesUI(expenses: List<Expense>) {
//        // This part will check if the expenses list is empty, and display the appropriate message
//        val displayText = StringBuilder()
//
//        if (expenses.isEmpty()) {
//            displayText.append("No expenses\n")
//        } else {
//            // Proper for-loop to iterate over the expenses list
//            for (expense in expenses) {
//                displayText.append("  • ${expense.amount} - ${expense.description}\n")
//            }
//        }
//
//        binding.textViewCategories.text = displayText.toString()
//    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {
            val expenseAdded = data?.getBooleanExtra("expense_added", false) == true
            if (expenseAdded) {
                // Delay slightly to allow Firestore to save the expense properly
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(500)  // Wait 0.5 second
                    updateCategoryList()
                }
            }
        }
    }
}

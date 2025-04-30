package com.st10254797.smartbudgetting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ExpenseActivity : AppCompatActivity() {

    private lateinit var appDatabase: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var goalDao: GoalDao
    private lateinit var budgetWarningTextView: TextView

    private lateinit var categorySpinner: Spinner
    private lateinit var amountEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var dateEditText: EditText
    private lateinit var saveExpenseBtn: Button
    private lateinit var uploadImageBtn: Button
    private lateinit var returnBtn: Button
    private lateinit var expensesListView: ListView // ListView to display expenses

    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var filterDateButton: Button

    private var imageUrl: String? = null
    private var selectedCategoryId: Long = -1

    // ✅ Modern image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Persist permission so the app can read it later
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            imageUrl = it.toString()
            Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense)

        appDatabase = AppDatabase.getDatabase(this)
        expenseDao = appDatabase.expenseDao()
        categoryDao = appDatabase.categoryDao()

        amountEditText = findViewById(R.id.amountEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        dateEditText = findViewById(R.id.dateEditText)
        saveExpenseBtn = findViewById(R.id.saveExpenseBtn)
        uploadImageBtn = findViewById(R.id.uploadImageBtn)
        categorySpinner = findViewById(R.id.categorySpinner)
        returnBtn = findViewById(R.id.returnBtn)
        expensesListView = findViewById(R.id.expensesListView)

        startDateEditText = findViewById(R.id.startDateEditText)
        endDateEditText = findViewById(R.id.endDateEditText)
        filterDateButton = findViewById(R.id.filterDateButton)

        goalDao = appDatabase.goalDao()
        budgetWarningTextView = findViewById(R.id.budgetWarningTextView)

        val clearFilterButton: Button = findViewById(R.id.clearFilterButton)

        setupCategorySpinner()

        uploadImageBtn.setOnClickListener {
            pickImage()
        }

        saveExpenseBtn.setOnClickListener {
            saveExpense()
        }

        filterDateButton.setOnClickListener {
            filterExpensesByDate()
        }
        clearFilterButton.setOnClickListener {
            clearExpenseFilter()
        }

        returnBtn.setOnClickListener {
            finish()
        }
    }

    private fun setupCategorySpinner() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val categories = categoryDao.getCategoriesByUser(userId)
                val categoryNames = categories.map { it.name }

                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(
                        this@ExpenseActivity,
                        android.R.layout.simple_spinner_item,
                        categoryNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    categorySpinner.adapter = adapter

                    categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>, view: View?, position: Int, id: Long
                        ) {
                            selectedCategoryId = categories[position].id.toLong()
                            loadExpensesForCategory(selectedCategoryId)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            selectedCategoryId = -1
                        }
                    }
                }
            }
        }
    }

    private fun loadExpensesForCategory(categoryId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val expenses = expenseDao.getExpensesByUser(userId) // Get all user expenses
            val total = expenses.sumOf { it.amount }
            val goal = goalDao.getGoalForUser(userId)

            withContext(Dispatchers.Main) {
                val adapter = ExpenseAdapter(
                    this@ExpenseActivity,
                    expenses.filter { it.category == categoryId }.toMutableList(),
                    onDeleteClick = { expense -> deleteExpense(expense) }
                )
                expensesListView.adapter = adapter

                // Show budget warning
                if (goal != null) {
                    when {
                        total > goal.maxGoal -> {
                            budgetWarningTextView.visibility = View.VISIBLE
                            budgetWarningTextView.text = "⚠️ You've exceeded your max budget of ${goal.maxGoal}!"
                            budgetWarningTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                        }
                        total < goal.minGoal -> {
                            budgetWarningTextView.visibility = View.VISIBLE
                            budgetWarningTextView.text = "💡 You're below your minimum budget goal of ${goal.minGoal}."
                            budgetWarningTextView.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                        }
                        else -> {
                            budgetWarningTextView.visibility = View.VISIBLE
                            budgetWarningTextView.text = "✅ You're within your budget."
                            budgetWarningTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                        }
                    }
                } else {
                    budgetWarningTextView.visibility = View.GONE
                }
            }
        }
    }

    // ✅ Updated to use modern Activity Result API
    private fun pickImage() {
        imagePickerLauncher.launch(arrayOf("image/*"))
    }

    private fun saveExpense() {
        val amount = amountEditText.text.toString().toDoubleOrNull()
        val description = descriptionEditText.text.toString()
        val dateString = dateEditText.text.toString()

        if (amount == null || description.isEmpty() || dateString.isEmpty() || selectedCategoryId.toInt() == -1) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val newExpense = Expense(
                id = 0,
                amount = amount,
                description = description,
                date = dateString,
                category = selectedCategoryId,
                imageUrl = imageUrl,
                userId = userId
            )

            lifecycleScope.launch(Dispatchers.IO) {
                expenseDao.insert(newExpense)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExpenseActivity, "Expense added successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    // Method to delete an expense
    private fun deleteExpense(expense: Expense) {
        lifecycleScope.launch(Dispatchers.IO) {
            expenseDao.delete(expense) // Delete the expense from the database
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ExpenseActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                loadExpensesForCategory(selectedCategoryId) // Reload expenses to refresh the list
            }
        }
    }

    private fun filterExpensesByDate() {
        val startDate = startDateEditText.text.toString()
        val endDate = endDateEditText.text.toString()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please enter both start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDateParsed = dateFormat.parse(startDate)
            val endDateParsed = dateFormat.parse(endDate)

            if (startDateParsed == null || endDateParsed == null) {
                Toast.makeText(this, "Invalid date format. Please use yyyy-MM-dd.", Toast.LENGTH_SHORT).show()
                return
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val expenses = expenseDao.getExpensesByUser(userId)

                val filteredExpenses = expenses.filter {
                    val expenseDate = dateFormat.parse(it.date)
                    expenseDate != null && !expenseDate.before(startDateParsed) && !expenseDate.after(endDateParsed)
                }

                // Group expenses by category and calculate totals
                val categoryTotals = calculateCategoryTotals(filteredExpenses)

                withContext(Dispatchers.Main) {
                    val adapter = ExpenseAdapter(
                        this@ExpenseActivity,
                        filteredExpenses.toMutableList(),
                        onDeleteClick = { expense -> deleteExpense(expense) }
                    )
                    expensesListView.adapter = adapter

                    // Display the category totals
                    displayCategoryTotals(categoryTotals)
                }
            }

        } catch (e: ParseException) {
            Toast.makeText(this, "Error parsing dates", Toast.LENGTH_SHORT).show()
        }
    }
    private fun clearExpenseFilter() {
        // Clear the date fields
        startDateEditText.text.clear()
        endDateEditText.text.clear()

        // Clear the category totals view
        val categoryTotalsLayout: LinearLayout = findViewById(R.id.categoryTotalsLayout)
        categoryTotalsLayout.removeAllViews()

        // Reload all expenses without any filter
        loadExpensesForCategory(selectedCategoryId)
    }
    private suspend fun calculateCategoryTotals(expenses: List<Expense>): Map<String, Double> {
        val categoryTotals = mutableMapOf<String, Double>()
        val categories = categoryDao.getAllCategories() // Get all categories

        for (expense in expenses) {
            // Find the category name by ID
            val categoryName = categories.find { it.id == expense.category }?.name ?: "Unknown"
            categoryTotals[categoryName] = categoryTotals.getOrDefault(categoryName, 0.0) + expense.amount
        }

        return categoryTotals
    }
    private fun displayCategoryTotals(categoryTotals: Map<String, Double>) {
        val categoryTotalsLayout: LinearLayout = findViewById(R.id.categoryTotalsLayout)

        // Clear any existing totals
        categoryTotalsLayout.removeAllViews()

        // Display the totals for each category
        for ((category, totalAmount) in categoryTotals) {
            val textView = TextView(this)
            textView.text = "$category: R%.2f".format(totalAmount)
            categoryTotalsLayout.addView(textView)
        }
    }

}

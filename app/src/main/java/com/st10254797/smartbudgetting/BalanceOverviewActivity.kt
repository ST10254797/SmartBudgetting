package com.st10254797.smartbudgetting

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BalanceOverviewActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var progressBar: ProgressBar
    private lateinit var summaryTextView: TextView

    private lateinit var appDatabase: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var goalDao: GoalDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_overview)

        pieChart = findViewById(R.id.pieChart)
        progressBar = findViewById(R.id.budgetProgressBar)
        summaryTextView = findViewById(R.id.summaryTextView)

        appDatabase = AppDatabase.getDatabase(this)
        expenseDao = appDatabase.expenseDao()
        categoryDao = appDatabase.categoryDao()
        goalDao = appDatabase.goalDao()

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val allExpenses = expenseDao.getExpensesByUser(userId)
            val categories = categoryDao.getAllCategories()
            val goal = goalDao.getGoalForUser(userId)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val thisMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }.time

            val thisMonthExpenses = allExpenses.filter {
                val expenseDate = sdf.parse(it.date)
                expenseDate != null && !expenseDate.before(thisMonth)
            }

            val totalSpent = thisMonthExpenses.sumOf { it.amount }
            val categorySums = thisMonthExpenses.groupBy { expense ->
                categories.find { it.id == expense.category }?.name ?: "Unknown"
            }.mapValues { (_, v) -> v.sumOf { it.amount } }

            withContext(Dispatchers.Main) {
                updatePieChart(categorySums)
                updateProgressBar(totalSpent, goal)
                updateSummary(totalSpent, goal)
            }
        }
    }

    private fun updatePieChart(dataMap: Map<String, Double>) {
        val entries = dataMap.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "Expenses by Category").apply {
            setColors(*intArrayOf(
                android.graphics.Color.BLUE,
                android.graphics.Color.RED,
                android.graphics.Color.GREEN,
                android.graphics.Color.MAGENTA,
                android.graphics.Color.CYAN
            ))
            valueTextSize = 14f
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            centerText = "Spending Split"
            animateY(1000)
            invalidate()
        }
    }

    private fun updateProgressBar(totalSpent: Double, goal: Goal?) {
        if (goal != null && goal.maxGoal > 0) {
            val progress = ((totalSpent / goal.maxGoal) * 100).coerceAtMost(100.0)
            progressBar.progress = progress.toInt()
        } else {
            progressBar.progress = 0
        }
    }

    private fun updateSummary(totalSpent: Double, goal: Goal?) {
        val daysLeft = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) -
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        val summary = buildString {
            append("💸 Spent This Month: R%.2f\n".format(totalSpent))
            if (goal != null) {
                append("📉 Min Goal: R${goal.minGoal}\n")
                append("📈 Max Goal: R${goal.maxGoal}\n")
            }
            append("📅 Days Left: $daysLeft")
        }

        summaryTextView.text = summary
    }
}

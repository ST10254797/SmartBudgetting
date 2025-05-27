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
import com.github.mikephil.charting.formatter.PercentFormatter
import android.text.Html
import android.text.method.LinkMovementMethod
import com.github.mikephil.charting.components.Legend


class BalanceOverviewActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var progressBar: ProgressBar
    private lateinit var summaryTextView: TextView

    private lateinit var appDatabase: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var goalDao: GoalDao
    private lateinit var budgetStatusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_overview)

        pieChart = findViewById(R.id.pieChart)
        progressBar = findViewById(R.id.budgetProgressBar)
        summaryTextView = findViewById(R.id.summaryTextView)
        budgetStatusTextView = findViewById(R.id.budgetStatusTextView)

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

            val categoryMap = categories.associateBy { it.id } // Map categoryId -> Category

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
                categoryMap[expense.category]?.name ?: "Unknown"
            }.mapValues { (_, v) -> v.sumOf { it.amount } }

            withContext(Dispatchers.Main) {
                updatePieChart(categorySums)
                updateProgressBar(totalSpent, goal)
                updateSummary(totalSpent, goal)
                updateBudgetStatus(totalSpent, goal)
            }
        }
    }

    private fun updatePieChart(dataMap: Map<String, Double>) {
        // Show max 10 slices (9 categories + Others)
        val maxCategories = 10

        val sortedEntries = dataMap.entries.sortedByDescending { it.value }

        val entries = mutableListOf<PieEntry>()

        if (sortedEntries.size <= maxCategories) {
            sortedEntries.forEach { (category, amount) ->
                entries.add(PieEntry(amount.toFloat(), category))
            }
        } else {
            val topCategories = sortedEntries.take(maxCategories - 1) // top 9
            val others = sortedEntries.drop(maxCategories - 1) // rest

            topCategories.forEach { (category, amount) ->
                entries.add(PieEntry(amount.toFloat(), category))
            }

            val othersSum = others.sumOf { it.value }
            entries.add(PieEntry(othersSum.toFloat(), "Others"))
        }

        val dataSet = PieDataSet(entries, "").apply {
            setColors(
                android.graphics.Color.rgb(244, 67, 54),
                android.graphics.Color.rgb(33, 150, 243),
                android.graphics.Color.rgb(76, 175, 80),
                android.graphics.Color.rgb(255, 193, 7),
                android.graphics.Color.rgb(156, 39, 176),
                android.graphics.Color.rgb(0, 150, 136),
                android.graphics.Color.rgb(255, 87, 34),
                android.graphics.Color.rgb(121, 85, 72),
                android.graphics.Color.rgb(63, 81, 181),
                android.graphics.Color.rgb(189, 189, 189) // Others color (gray)
            )
            valueTextSize = 14f
            valueTextColor = android.graphics.Color.WHITE
            sliceSpace = 3f
            selectionShift = 8f
            setDrawValues(true)
            valueFormatter = PercentFormatter(pieChart)
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false

            isDrawHoleEnabled = true
            holeRadius = 45f
            transparentCircleRadius = 50f
            setHoleColor(android.graphics.Color.WHITE)

            setCenterText("Spending Split")
            setCenterTextSize(18f)
            setCenterTextColor(android.graphics.Color.DKGRAY)

            setUsePercentValues(true)
            setDrawEntryLabels(true)
            setEntryLabelColor(android.graphics.Color.BLACK)
            legend.textColor = android.graphics.Color.BLACK
            setEntryLabelTextSize(12f)
            pieChart.setExtraOffsets(40f, 20f, 40f, 80f) // increase left and bottom offset

            legend.apply {
                isEnabled = true
                textSize = 14f
                form = Legend.LegendForm.CIRCLE

                setDrawInside(false)  // keep legend outside pie chart but inside view
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                isWordWrapEnabled = true

                yOffset = 6f  // move legend slightly up
                xEntrySpace = 10f // horizontal spacing between legend entries
                xOffset = -60f
            }

            animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
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
    private fun updateBudgetStatus(totalSpent: Double, goal: Goal?) {
        if (goal == null || goal.maxGoal <= 0) {
            budgetStatusTextView.text = "No budget goal set"
            budgetStatusTextView.setTextColor(android.graphics.Color.GRAY)
            return
        }

        when {
            totalSpent > goal.maxGoal -> {
                budgetStatusTextView.text = "⚠️ You are OVER budget!"
                budgetStatusTextView.setTextColor(android.graphics.Color.RED)
            }
            totalSpent < goal.minGoal -> {
                budgetStatusTextView.text = "✅ You are UNDER budget!"
                budgetStatusTextView.setTextColor(android.graphics.Color.GREEN)
            }
            else -> {
                budgetStatusTextView.text = "⚠️ You are WITHIN budget range."
                budgetStatusTextView.setTextColor(android.graphics.Color.parseColor("#FFA500")) // Orange
            }
        }
    }

    private fun updateSummary(totalSpent: Double, goal: Goal?) {
        val daysLeft = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) -
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        val htmlSummary = buildString {
            append("💸 <b>Spent This Month:</b> R%.2f<br/><br/>".format(totalSpent))
            if (goal != null) {
                append("📉 <b>Min Goal:</b> R${goal.minGoal}<br/>")
                append("📈 <b>Max Goal:</b> R${goal.maxGoal}<br/><br/>")
            }
            append("📅 <b>Days Left:</b> $daysLeft")
        }

        summaryTextView.text = Html.fromHtml(htmlSummary, Html.FROM_HTML_MODE_LEGACY)
        summaryTextView.movementMethod = LinkMovementMethod.getInstance() // optional if you have links
    }

}

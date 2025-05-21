package com.st10254797.smartbudgetting

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.widget.Button

class CategoryGraphActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var expenseDao: ExpenseDao
    private lateinit var goalDao: GoalDao
    private lateinit var btnRefreshGraph: Button  // Add this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_graph)

        barChart = findViewById(R.id.barChart)
        btnRefreshGraph = findViewById(R.id.btnRefreshGraph)  // Initialize button

        val backButton: Button = findViewById(R.id.backButton)  // Initialize new back button
        backButton.setOnClickListener {
            finish()
        }

        expenseDao = AppDatabase.getDatabase(this).expenseDao()
        goalDao = AppDatabase.getDatabase(this).goalDao()

        displayCategoryGraph()

        btnRefreshGraph.setOnClickListener {
            displayCategoryGraph()  // Refresh the graph data on button click
        }
    }

    private fun displayCategoryGraph() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            val categoryTotals = expenseDao.getCategoryTotalsForUser(userId)
            val goal = goalDao.getGoalForUser(userId)

            val entries = ArrayList<BarEntry>()
            val labels = ArrayList<String>()

            var index = 0f
            for (item in categoryTotals) {
                val category = item.category
                val amount = item.totalAmount

                val jitter = (Math.random() * 0.2 - 0.1).toFloat()
                entries.add(BarEntry(index + jitter, amount.toFloat()))
                labels.add(category)
                index++
            }

            val barDataSet = BarDataSet(entries, "Spent")
            barDataSet.color = Color.BLUE

            val data = BarData(barDataSet)
            barChart.data = data

            val xAxis = barChart.xAxis
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)

            barChart.axisRight.isEnabled = false
            barChart.description.isEnabled = false
            barChart.animateY(1000)

            goal?.let {
                val minGoalLine = LimitLine(it.minGoal.toFloat(), "Min Goal")
                minGoalLine.lineColor = Color.GREEN
                minGoalLine.lineWidth = 2f

                val maxGoalLine = LimitLine(it.maxGoal.toFloat(), "Max Goal")
                maxGoalLine.lineColor = Color.RED
                maxGoalLine.lineWidth = 2f

                val yAxis = barChart.axisLeft
                yAxis.removeAllLimitLines()
                yAxis.addLimitLine(minGoalLine)
                yAxis.addLimitLine(maxGoalLine)
            }

            barChart.invalidate()
        }
    }
}
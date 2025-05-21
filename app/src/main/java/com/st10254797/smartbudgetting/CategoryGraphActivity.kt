package com.st10254797.smartbudgetting

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class CategoryGraphActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var expenseDao: ExpenseDao
    private lateinit var goalDao: GoalDao
    private lateinit var btnRefreshGraph: Button
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_graph)

        // Initialize views
        barChart = findViewById(R.id.barChart)
        btnRefreshGraph = findViewById(R.id.btnRefreshGraph)
        backButton = findViewById(R.id.backButton)

        // Set up DAOs
        expenseDao = AppDatabase.getDatabase(this).expenseDao()
        goalDao = AppDatabase.getDatabase(this).goalDao()

        // Set click listeners
        backButton.setOnClickListener { finish() }
        btnRefreshGraph.setOnClickListener { displayCategoryGraph() }

        // Initial display
        displayCategoryGraph()
    }

    private fun displayCategoryGraph() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            val categoryTotals = expenseDao.getCategoryTotalsForUser(userId)
            val goal = goalDao.getGoalForUser(userId)

            if (categoryTotals.isEmpty()) {
                barChart.clear()
                barChart.setNoDataText("No expense data to display.")
                return@launch
            }

            val entries = ArrayList<BarEntry>()
            val labels = ArrayList<String>()
            var index = 0f

            for (item in categoryTotals) {
                entries.add(BarEntry(index, item.totalAmount.toFloat()))
                labels.add(item.category)
                index++
            }

            val barDataSet = BarDataSet(entries, "Spent")
            barDataSet.color = ContextCompat.getColor(this@CategoryGraphActivity, R.color.teal_700)
            barDataSet.valueTextSize = 12f

            val data = BarData(barDataSet)
            data.barWidth = 0.9f
            barChart.data = data
            barChart.setFitBars(true)

            val xAxis = barChart.xAxis
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.setDrawGridLines(false)
            xAxis.labelRotationAngle = -60f
            xAxis.setLabelCount(labels.size, false)
            xAxis.textSize = 10f
            xAxis.axisMinimum = -0.5f
            xAxis.axisMaximum = entries.size - 0.5f
            xAxis.spaceMin = 0.5f // ✅ added spacing to the left
            xAxis.spaceMax = 0.5f // ✅ added spacing to the right


            val yAxis = barChart.axisLeft
            yAxis.axisMinimum = 0f
            yAxis.textSize = 12f
            barChart.axisRight.isEnabled = false

            yAxis.removeAllLimitLines()
            goal?.let {
                val minLine = LimitLine(it.minGoal.toFloat(), "Min Goal")
                minLine.lineColor = Color.GREEN
                minLine.lineWidth = 2f

                val maxLine = LimitLine(it.maxGoal.toFloat(), "Max Goal")
                maxLine.lineColor = Color.RED
                maxLine.lineWidth = 2f

                yAxis.addLimitLine(minLine)
                yAxis.addLimitLine(maxLine)
            }

            // Adjust width for horizontal scroll (as you had)
            val barWidthInPixels = 110
            val totalWidth = (labels.size * barWidthInPixels).coerceAtLeast(barChart.width)
            barChart.layoutParams.width = totalWidth
            barChart.requestLayout()

            // Move the legend down
            val legend = barChart.legend
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.textSize = 12f

            barChart.description.isEnabled = false
            barChart.animateY(1000)
            barChart.invalidate()
        }
    }


}

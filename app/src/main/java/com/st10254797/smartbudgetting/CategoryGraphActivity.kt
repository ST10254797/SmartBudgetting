package com.st10254797.smartbudgetting

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
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
import java.text.SimpleDateFormat
import java.util.*

class CategoryGraphActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var expenseDao: ExpenseDao
    private lateinit var goalDao: GoalDao

    private lateinit var btnRefreshGraph: androidx.appcompat.widget.AppCompatButton
    private lateinit var btnClearFilter: androidx.appcompat.widget.AppCompatButton
    private lateinit var backButton: androidx.appcompat.widget.AppCompatButton

    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText

    private var startDateString: String? = null
    private var endDateString: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_graph)

        barChart = findViewById(R.id.barChart)
        btnRefreshGraph = findViewById(R.id.btnRefreshGraph)
        btnClearFilter = findViewById(R.id.btnClearFilter)
        backButton = findViewById(R.id.backButton)
        startDateEditText = findViewById(R.id.startDateEditText)
        endDateEditText = findViewById(R.id.endDateEditText)

        expenseDao = AppDatabase.getDatabase(this).expenseDao()
        goalDao = AppDatabase.getDatabase(this).goalDao()

        startDateEditText.setOnClickListener { pickDate(true) }
        endDateEditText.setOnClickListener { pickDate(false) }

        backButton.setOnClickListener { finish() }
        btnRefreshGraph.setOnClickListener { loadGraphData() }
        btnClearFilter.setOnClickListener {
            startDateString = null
            endDateString = null
            startDateEditText.text.clear()
            endDateEditText.text.clear()
            Toast.makeText(this, "Filter cleared. Showing all data.", Toast.LENGTH_SHORT).show()
            loadGraphData()
        }

        loadGraphData()
    }

    private fun pickDate(isStart: Boolean) {
        val calendar = Calendar.getInstance()

        if (isStart && startDateString != null) {
            dateFormat.parse(startDateString!!)?.let { calendar.time = it }
        } else if (!isStart && endDateString != null) {
            dateFormat.parse(endDateString!!)?.let { calendar.time = it }
        }

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = dateFormat.format(calendar.time)

                if (isStart) {
                    if (endDateString != null && calendar.time.after(dateFormat.parse(endDateString!!))) {
                        Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }
                    startDateString = selectedDate
                    startDateEditText.setText(selectedDate)
                } else {
                    if (startDateString != null && calendar.time.before(dateFormat.parse(startDateString!!))) {
                        Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }
                    endDateString = selectedDate
                    endDateEditText.setText(selectedDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun loadGraphData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            val categoryTotals = when {
                startDateString != null && endDateString != null -> {
                    val start = dateFormat.parse(startDateString!!)
                    val end = dateFormat.parse(endDateString!!)
                    if (start != null && end != null && !start.after(end)) {
                        Toast.makeText(this@CategoryGraphActivity, "Showing data from $startDateString to $endDateString", Toast.LENGTH_SHORT).show()
                        expenseDao.getCategoryTotalsForUserAndDateRange(userId, startDateString!!, endDateString!!)
                    } else {
                        Toast.makeText(this@CategoryGraphActivity, "Invalid date range. Showing all data.", Toast.LENGTH_SHORT).show()
                        expenseDao.getCategoryTotalsForUser(userId)
                    }
                }
                else -> {
                    Toast.makeText(this@CategoryGraphActivity, "Showing all data (no date filter).", Toast.LENGTH_SHORT).show()
                    expenseDao.getCategoryTotalsForUser(userId)
                }
            }

            if (categoryTotals.isEmpty()) {
                barChart.clear()
                barChart.setNoDataText("No expense data to display.")
                return@launch
            }

            val entries = ArrayList<BarEntry>()
            val labels = ArrayList<String>()

            // Populate the entries and labels
            for ((i, item) in categoryTotals.withIndex()) {
                entries.add(BarEntry(i.toFloat(), item.totalAmount.toFloat()))
                labels.add(item.category)
            }

            // Clear old chart
            barChart.clear()

            val barDataSet = BarDataSet(entries, "Spent")
            barDataSet.color = ContextCompat.getColor(this@CategoryGraphActivity, R.color.teal_700)
            barDataSet.valueTextSize = 12f

            val data = BarData(barDataSet)
            data.barWidth = 0.9f

            barChart.data = data
            barChart.setFitBars(true)

            // X-axis configuration
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
            xAxis.axisMaximum = labels.size - 0.5f
            xAxis.spaceMin = 0.5f
            xAxis.spaceMax = 0.5f
            xAxis.setDrawLabels(true)  // <--- critical fix

            // Y-axis configuration
            val yAxis = barChart.axisLeft
            yAxis.removeAllLimitLines()
            yAxis.axisMinimum = 0f
            yAxis.textSize = 12f
            barChart.axisRight.isEnabled = false

            // Optional Part - Add Min and Max Goal Lines
            val goal = goalDao.getGoalForUser(userId)
            goal?.let {
                val minLine = LimitLine(it.minGoal.toFloat(), "Min Goal").apply {
                    lineColor = Color.GREEN
                    lineWidth = 2f
                    textColor = Color.GREEN
                    textSize = 10f
                }
                val maxLine = LimitLine(it.maxGoal.toFloat(), "Max Goal").apply {
                    lineColor = Color.RED
                    lineWidth = 2f
                    textColor = Color.RED
                    textSize = 10f
                }
                yAxis.addLimitLine(minLine)
                yAxis.addLimitLine(maxLine)
            }

            // Auto scroll if too many bars
            val barWidthInPixels = 110
            val screenWidth = resources.displayMetrics.widthPixels
            val totalWidth = (labels.size * barWidthInPixels).coerceAtLeast(screenWidth)
            barChart.layoutParams.width = totalWidth
            barChart.requestLayout()

            barChart.setDragEnabled(true)
            barChart.setVisibleXRangeMaximum(6f)
            barChart.moveViewToX(entries.size.toFloat())

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

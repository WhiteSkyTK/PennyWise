package com.example.pennywise

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.drawerlayout.widget.DrawerLayout
import com.example.pennywise.utils.BottomNavManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate

class ReportActivity : BaseActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        HeaderManager(this, drawerLayout) { updatedCalendar ->
            // Optional callback when month changes
        }.setupHeader("Report")

        BottomNavManager.setupBottomNav(this, R.id.nav_report)

        // Chart setup (unchanged)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)

        setupPieChart()
        setupBarChart()
        Log.d("DEBUG", "onCreate reached in ReportActivity")

    }

    private fun setupPieChart() {
        val entries = ArrayList<PieEntry>()
        val income = 3000f
        val expense = 1800f

        entries.add(PieEntry(income, "Income"))
        entries.add(PieEntry(expense, "Expenses"))

        val pieDataSet = PieDataSet(entries, "Income vs Expenses")
        pieDataSet.colors = listOf(ColorTemplate.COLORFUL_COLORS[0], ColorTemplate.COLORFUL_COLORS[1])
        pieDataSet.valueTextSize = 14f
        pieDataSet.valueTextColor = Color.WHITE

        val pieData = PieData(pieDataSet)

        pieChart.data = pieData
        pieChart.setUsePercentValues(true)
        pieChart.centerText = "Monthly Overview"
        pieChart.setCenterTextSize(16f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.description = Description().apply { text = "" }
        pieChart.invalidate() // refresh
    }

    private fun setupBarChart() {
        val entries = ArrayList<BarEntry>()

        entries.add(BarEntry(1f, 1000f)) // January
        entries.add(BarEntry(2f, 1200f)) // February
        entries.add(BarEntry(3f, 800f))  // March
        entries.add(BarEntry(4f, 1500f)) // April

        val dataSet = BarDataSet(entries, "Expenses Over Time")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val data = BarData(dataSet)
        data.barWidth = 0.9f

        barChart.data = data
        barChart.setFitBars(true)
        barChart.animateY(1500)
        barChart.invalidate()

    }

}

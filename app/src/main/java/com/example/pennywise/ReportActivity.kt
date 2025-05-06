package com.example.pennywise

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.pennywise.data.AppDatabase
import com.example.pennywise.utils.BottomNavManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Locale

class ReportActivity : BaseActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.aboutActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userEmail = intent.getStringExtra("email") ?: "user@example.com"
        val initials = userEmail.take(2).uppercase(Locale.getDefault())
        val profileInitials = findViewById<TextView>(R.id.profileInitials)
        profileInitials.text = initials

        profileInitials.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sign_out -> {
                        startActivity(Intent(this, Activity_Login_Resgister::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val transactionDao = AppDatabase.getDatabase(this).transactionDao()

        HeaderManager(this, drawerLayout, transactionDao, lifecycleScope) { updatedCalendar ->
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

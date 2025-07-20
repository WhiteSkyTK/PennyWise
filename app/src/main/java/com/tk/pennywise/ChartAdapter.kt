package com.tk.pennywise

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue // For getting theme attributes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt // For type safety
import androidx.compose.foundation.layout.size
import androidx.core.content.ContextCompat // For context-based color retrieval
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.* // Import all data types
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Random // For generating more random distinct colors if needed

class ChartAdapter(private val context: Context, private val chartDataList: List<ChartData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //region Enhanced Color Palette
    private val extendedColors: List<Int> by lazy {
        // Start with ColorTemplate's diverse palettes
        val colors = mutableListOf<Int>()
        colors.addAll(ColorTemplate.LIBERTY_COLORS.toList())
        colors.addAll(ColorTemplate.PASTEL_COLORS.toList())
        colors.addAll(ColorTemplate.VORDIPLOM_COLORS.toList())
        colors.addAll(ColorTemplate.JOYFUL_COLORS.toList())
        colors.addAll(ColorTemplate.COLORFUL_COLORS.toList())


        // Add more distinct programmatic colors if needed for up to 100
        // This is a simple way; for truly distinct colors for many entries,
        // you might need a more sophisticated generation algorithm or a predefined list.
        val random = Random(0) // Seed for consistency if desired
        val existingColors = colors.toHashSet()
        while (colors.size < 100) {
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            val newColor = Color.rgb(r, g, b)
            if (!existingColors.contains(newColor)) {
                colors.add(newColor)
                existingColors.add(newColor)
            }
        }
        // Shuffle to mix predefined and generated colors for better initial distribution
        // colors.shuffle(Random(1)) // Seed for consistent shuffle
        colors.distinct() // Ensure no duplicates from combining templates
    }

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            0 -> PieChartViewHolder(inflater.inflate(R.layout.item_chart_pie, parent, false))
            1 -> BarChartViewHolder(inflater.inflate(R.layout.item_chart_bar, parent, false))
            2 -> LineChartViewHolder(inflater.inflate(R.layout.item_chart_line, parent, false))
            3 -> RadarChartViewHolder(inflater.inflate(R.layout.item_chart_radar, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = 4

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PieChartViewHolder -> holder.bind(chartDataList, extendedColors, getBackgroundColor(context))
            is BarChartViewHolder -> holder.bind(chartDataList, extendedColors)
            is LineChartViewHolder -> holder.bind(chartDataList) // Line chart colors are usually specific
            is RadarChartViewHolder -> holder.bind(chartDataList)  // Radar chart colors specific
        }
    }

    // Helper to get the current background color from the theme
    @ColorInt
    private fun getBackgroundColor(context: Context): Int {
        val typedValue = TypedValue()
        // Assuming your theme defines 'android.R.attr.colorBackground' or a custom attribute
        // R.attr.colorBackground might be more specific if you've defined it in your app theme
        val themeAttribute = android.R.attr.colorBackground
        context.theme.resolveAttribute(themeAttribute, typedValue, true)
        return typedValue.data
    }



    private fun isDarkMode(): Boolean { // This can be removed if using theme attributes for text color
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    // It's better to get text color from theme attributes as well for consistency
    @ColorInt
    private fun getThemeTextColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        return typedValue.data
    }

    class PieChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val pieChart: PieChart = view.findViewById(R.id.pieChart)

        fun bind(dataList: List<ChartData>, colorPalette: List<Int>, @ColorInt holeBackgroundColor: Int) {
            if (dataList.isEmpty()) {
                pieChart.clear()
                pieChart.setNoDataText("No data available")
                pieChart.invalidate() // Make sure to invalidate
                return
            }

            val entries = dataList.map { PieEntry(it.value.toFloat(), shortenLabel(it.category)) }
            val dataSet = PieDataSet(entries, "").apply { // Label for dataset often not needed if legend is off or using entry labels
                // Use a slice of the extended color palette
                colors = colorPalette.take(entries.size)
                valueTextColor = if (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
                valueTextSize = 10f // Smaller for more entries
                sliceSpace = 2f // Add space between slices
                valueLinePart1OffsetPercentage = 80f
                valueLinePart1Length = 0.4f
                valueLinePart2Length = 0.4f
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            }

            pieChart.apply {
                data = PieData(dataSet)
                setUsePercentValues(false) // Or true, depending on your preference
                description.isEnabled = false // Usually cleaner without it
                legend.isEnabled = true // Or false, depending on if you want a legend

                // Center hole configuration
                isDrawHoleEnabled = true
                // Option 1: Transparent center
                setHoleColor(Color.TRANSPARENT)
                // Option 2: Match background color (passed as parameter)
                // setHoleColor(holeBackgroundColor)

                transparentCircleRadius = 58f // Adjust as needed
                holeRadius = 55f // Make it slightly smaller than transparentCircleRadius for a "donut" effect

                // Entry labels (labels on slices)
                setDrawEntryLabels(true) // Set true to show labels on slices
                setEntryLabelColor(dataSet.valueTextColor)
                setEntryLabelTextSize(9f) // Adjust for readability

                rotationAngle = 0f
                isRotationEnabled = true
                isHighlightPerTapEnabled = true

                animateY(1000)
                invalidate()
            }
        }
    }

    class BarChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val barChart: BarChart = view.findViewById(R.id.barChart)

        fun bind(dataList: List<ChartData>, colorPalette: List<Int>) {
            if (dataList.isEmpty()) {
                barChart.clear()
                barChart.setNoDataText("No data available")
                barChart.invalidate()
                return
            }

            val entries = dataList.mapIndexed { index, item ->
                BarEntry(index.toFloat(), item.value.toFloat())
            }
            val dataSet = BarDataSet(entries, "Spending by Category").apply {
                // Use a slice of the extended color palette for bars
                colors = colorPalette.take(entries.size)
                valueTextSize = 10f
                valueTextColor = if (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
            }

            barChart.apply {
                data = BarData(dataSet)
                setFitBars(true) // make the bars X values evenly spaced

                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(dataList.map { shortenLabel(it.category) })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    isGranularityEnabled = true
                    labelRotationAngle = -45f // Negative for better readability if labels are long
                    setLabelCount(dataList.size, false) // Set to false to allow adjustments
                    textColor = dataSet.valueTextColor
                    setDrawGridLines(false)
                    // Adjust axis min/max slightly for padding if needed, but fitBars helps
                }
                axisLeft.apply {
                    axisMinimum = 0f
                    // Add some padding to the top, e.g., 20% more than max value
                    val maxValue = dataList.maxOfOrNull { it.value.toFloat() } ?: 0f
                    axisMaximum = maxValue * 1.2f
                    textColor = dataSet.valueTextColor
                    // setLabelCount(6, false) // Adjust number of labels on Y-axis
                    setDrawGridLines(true) // Grid lines can be helpful for bar charts
                }
                axisRight.isEnabled = false
                legend.textColor = dataSet.valueTextColor
                description.isEnabled = false // Usually cleaner

                animateY(1000)
                invalidate()
            }
        }
    }

    // LineChartViewHolder and RadarChartViewHolder remain mostly the same
    // unless you want to apply the extended color palette to them as well.
    // For simplicity, I'll leave them as they were.
    class LineChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val lineChart: LineChart = view.findViewById(R.id.lineChart)
        fun bind(dataList: List<ChartData>) {
            if (dataList.isEmpty()) {
                lineChart.clear(); lineChart.setNoDataText("No data available"); lineChart.invalidate(); return
            }
            val entries = dataList.mapIndexed { index, item -> Entry(index.toFloat(), item.value.toFloat()) }
            val dataSet = LineDataSet(entries, "Trend by Category").apply {
                color = ContextCompat.getColor(itemView.context, R.color.main_purple) // Example: use theme color
                setCircleColor(ContextCompat.getColor(itemView.context, R.color.main_green))
                lineWidth = 2.5f; circleRadius = 5f; setDrawCircleHole(false)
                valueTextSize = 10f;
                valueTextColor = if (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
                setDrawFilled(true)
                fillDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.chart_fill_gradient) // Example gradient
            }
            lineChart.apply {
                data = LineData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(dataList.map { shortenLabel(it.category) })
                    position = XAxis.XAxisPosition.BOTTOM; granularity = 1f; textColor = dataSet.valueTextColor
                    setDrawGridLines(false); labelRotationAngle = -45f; setLabelCount(dataList.size, false)
                }
                axisLeft.apply {
                    axisMinimum = 0f; val maxValue = dataList.maxOfOrNull { it.value.toFloat() } ?: 0f
                    axisMaximum = maxValue * 1.2f; textColor = dataSet.valueTextColor
                }
                axisRight.isEnabled = false; legend.textColor = dataSet.valueTextColor; description.isEnabled = false
                animateX(1000); invalidate()
            }
        }
    }

    class RadarChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val radarChart: RadarChart = view.findViewById(R.id.radarChart)
        fun bind(dataList: List<ChartData>) {
            if (dataList.isEmpty()) {
                radarChart.clear(); radarChart.setNoDataText("No data available"); radarChart.invalidate(); return
            }
            val entries = dataList.map { RadarEntry(it.value.toFloat()) }
            val labels = dataList.map { shortenLabel(it.category) }
            val dataSet = RadarDataSet(entries, "Radar Overview").apply {
                color = ContextCompat.getColor(itemView.context, R.color.main_green) // Example
                fillColor = ContextCompat.getColor(itemView.context, R.color.main_purple)
                fillAlpha = 100 // Semi-transparent fill
                setDrawFilled(true); valueTextSize = 10f
                valueTextColor = if (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
                lineWidth = 2f
            }
            radarChart.apply {
                data = RadarData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(labels); textColor = dataSet.valueTextColor
                    textSize = 9f ; setLabelCount(labels.size, true)
                }
                yAxis.apply {
                    axisMinimum = 0f; val maxValue = dataList.maxOfOrNull { it.value.toFloat() } ?: 0f
                    axisMaximum = maxValue * 1.2f; textColor = dataSet.valueTextColor
                    setLabelCount(5, false) // Adjust label count on Y-axis
                }
                legend.textColor = dataSet.valueTextColor; description.isEnabled = false
                webLineWidth = 1f; webColor = Color.LTGRAY; webLineWidthInner = 1f; webColorInner = Color.LTGRAY
                animateXY(1000, 1000); invalidate()
            }
        }
    }
}

// Ensure shortenLabel is defined, preferably outside or as a top-level function if used by multiple classes
private fun shortenLabel(label: String, maxLen: Int = 10): String {
    return if (label.length > maxLen) label.substring(0, maxLen) + "…" else label
}
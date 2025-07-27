package com.tk.pennywise

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.size
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Random

class ChartAdapter(
    private val context: Context,
    private val chartDataList: List<ChartData>,
    private val animatedPositions: MutableSet<Int> // Pass the set from Activity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var currentViewPagerPosition = 0 // To know which chart is currently visible

    // Method to be called from Activity when page changes
    fun requestAnimateChart(position: Int) {
        currentViewPagerPosition = position
        notifyItemChanged(position, PAYLOAD_ANIMATE) // Use payload to trigger animation check
    }

    companion object {
        const val PAYLOAD_ANIMATE = "PAYLOAD_ANIMATE"
    }

    //region Enhanced Color Palette
    private val extendedColors: List<Int> by lazy {
        val colors = mutableListOf<Int>()
        colors.addAll(ColorTemplate.LIBERTY_COLORS.toList())
        colors.addAll(ColorTemplate.PASTEL_COLORS.toList())
        colors.addAll(ColorTemplate.VORDIPLOM_COLORS.toList())
        colors.addAll(ColorTemplate.JOYFUL_COLORS.toList())
        colors.addAll(ColorTemplate.COLORFUL_COLORS.toList())
        // Change here: Use a non-seeded Random for different colors on each generation
        val random = Random() // Or Random(0) if you prefer consistency
        val existingColors = colors.toHashSet()
        val minLuminance = 0.3 // Adjust this threshold (0=black, 1=white)
        val maxLuminance = 0.8 // Adjust this threshold

        while (colors.size < 100) { // Or a higher number if you prefer more variety
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            val newColor = Color.rgb(r, g, b)

            // Calculate perceived luminance (simplified formula)
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255

            if (!existingColors.contains(newColor) && luminance > minLuminance && luminance < maxLuminance) {
                colors.add(newColor)
                existingColors.add(newColor)
            } else if (!existingColors.contains(newColor) && colors.size < 20) {
                // Fallback to add some colors if the luminance filter is too strict initially
                colors.add(newColor)
                existingColors.add(newColor)
            }
        }
        // Shuffle to make the order less predictable if you combine templates and random
        colors.shuffle()
        colors.distinct() // Still good to have as a final safety
    }

    override fun getItemViewType(position: Int): Int = position % 4

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        // Ensure viewType maps correctly if you have more than 4 items
        // or a fixed set of 4 charts. Assuming fixed 4 charts for now.
        return when (viewType) {
            0 -> PieChartViewHolder(inflater.inflate(R.layout.item_chart_pie, parent, false), ::getThemeTextColor)
            1 -> BarChartViewHolder(inflater.inflate(R.layout.item_chart_bar, parent, false), ::getThemeTextColor)
            2 -> LineChartViewHolder(inflater.inflate(R.layout.item_chart_line, parent, false), ::getThemeTextColor)
            3 -> RadarChartViewHolder(inflater.inflate(R.layout.item_chart_radar, parent, false), ::getThemeTextColor)
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun getItemCount(): Int = chartDataList.takeIf { it.isNotEmpty() }?.let { 4 } ?: 0 // Show 4 chart types if data, else 0

    // Overload onBindViewHolder to handle payloads
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_ANIMATE)) {
            // This is a specific request to check for animation
            val shouldAnimate = !animatedPositions.contains(position)
            if (shouldAnimate) {
                when (holder) {
                    is PieChartViewHolder -> holder.animateChart()
                    is BarChartViewHolder -> holder.animateChart()
                    is LineChartViewHolder -> holder.animateChart()
                    is RadarChartViewHolder -> holder.animateChart()
                }
                animatedPositions.add(position)
            }
        } else {
            // Full bind
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Determine if this chart should animate ONCE upon first binding and becoming visible
        val shouldAnimate = !animatedPositions.contains(position) && position == currentViewPagerPosition

        when (holder) {
            is PieChartViewHolder -> {
                holder.bind(chartDataList, extendedColors, getBackgroundColor(context))
                if (shouldAnimate) {
                    holder.animateChart()
                    animatedPositions.add(position)
                }
            }
            is BarChartViewHolder -> {
                holder.bind(chartDataList, extendedColors)
                if (shouldAnimate) {
                    holder.animateChart()
                    animatedPositions.add(position)
                }
            }
            is LineChartViewHolder -> {
                holder.bind(chartDataList)
                if (shouldAnimate) {
                    holder.animateChart()
                    animatedPositions.add(position)
                }
            }
            is RadarChartViewHolder -> {
                holder.bind(chartDataList)
                if (shouldAnimate) {
                    holder.animateChart()
                    animatedPositions.add(position)
                }
            }
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

    // It's better to get text color from theme attributes as well for consistency
    @ColorInt
    private fun getThemeTextColor(context: Context): Int {
        val typedValue = TypedValue()
        val resolved = context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        Log.d("ChartAdapter", "getThemeTextColor called. Attribute resolved: $resolved. Color: #${Integer.toHexString(typedValue.data)}. Type: ${typedValue.type}")
        if (!resolved) {
            Log.e("ChartAdapter", "Failed to resolve android.R.attr.textColorPrimary! Defaulting to Color.RED for debugging.")
            return Color.RED // Fallback for debugging
        }
        // Add this check to see if the resolved color is transparent by mistake
        if (Color.alpha(typedValue.data) == 0) {
            Log.w("ChartAdapter", "Resolved textColorPrimary is TRANSPARENT! Defaulting to Color.BLUE for debugging.")
            return Color.BLUE
        }
        return typedValue.data
    }

    abstract class BaseChartViewHolder(view: View, protected val getTextColorFromAdapter: (Context) -> Int) : RecyclerView.ViewHolder(view) {
        abstract fun animateChart()
    }

    class PieChartViewHolder(
        view: View,
        private val passedInGetTextColor: (Context) -> Int
    ) : BaseChartViewHolder(view, passedInGetTextColor) {
        private val pieChart: PieChart = view.findViewById(R.id.pieChart)
        private val themedTextColor = passedInGetTextColor(itemView.context)

        fun bind(dataList: List<ChartData>, colorPalette: List<Int>, @ColorInt holeBackgroundColor: Int) {
            if (dataList.isEmpty()) {
                pieChart.clear()
                pieChart.setNoDataText("No data available")
                pieChart.setNoDataTextColor(themedTextColor)
                pieChart.invalidate()
                return
            }

            val entries = dataList.map { PieEntry(it.value.toFloat(), shortenLabel(it.category)) }
            val dataSet = PieDataSet(entries, "").apply {
                colors = colorPalette.take(entries.size)
                setDrawValues(true)
                valueTextColor = themedTextColor // Use themed text color
                valueTextSize = 10f
                sliceSpace = 2f
                valueLinePart1OffsetPercentage = 80f
                valueLinePart1Length = 0.4f
                valueLinePart2Length = 0.4f
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            }

            pieChart.apply {
                data = PieData(dataSet)
                setUsePercentValues(false)
                description.isEnabled = false
                legend.apply { // Apply themed text color to legend
                    textColor = themedTextColor
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    setDrawInside(false)
                    isWordWrapEnabled = true
                }
                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                transparentCircleRadius = 58f
                holeRadius = 55f
                setDrawEntryLabels(true)
                setEntryLabelColor(themedTextColor) // Use themed text color for entry labels
                setEntryLabelTextSize(9f)
                rotationAngle = 0f
                isRotationEnabled = true
                isHighlightPerTapEnabled = true
                // Do not animate here, will be called by onBindViewHolder or requestAnimateChart
                invalidate()
            }
        }

        override fun animateChart() {
            pieChart.animateY(1000)
        }
    }

    class BarChartViewHolder(
        view: View,
        getTextColor: (Context) -> Int
    ) : BaseChartViewHolder(view, getTextColor) {
        private val barChart: BarChart = view.findViewById(R.id.barChart)
        private val themedTextColor = getTextColor(itemView.context)

        fun bind(dataList: List<ChartData>, colorPalette: List<Int>) {
            if (dataList.isEmpty()) {
                barChart.clear()
                barChart.setNoDataText("No data available")
                barChart.setNoDataTextColor(themedTextColor)
                barChart.invalidate()
                return
            }

            val entries = dataList.mapIndexed { index, item ->
                BarEntry(index.toFloat(), item.value.toFloat())
            }
            val dataSet = BarDataSet(entries, "Spending by Category").apply {
                colors = colorPalette.take(entries.size)
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = themedTextColor // Use themed text color
            }


            barChart.apply {
                data = BarData(dataSet)
                setFitBars(true)
                description.isEnabled = false
                axisRight.isEnabled = false

                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(dataList.map { shortenLabel(it.category) })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    isGranularityEnabled = true
                    setDrawLabels(true)
                    labelRotationAngle = -45f
                    setLabelCount(dataList.size, true)
                    textColor = themedTextColor // Use themed text color
                    setDrawGridLines(false)

                }
                axisLeft.apply {
                    axisMinimum = 0f
                    val maxValue = dataList.maxOfOrNull { it.value.toFloat() } ?: 0f
                    axisMaximum = maxValue * 1.2f
                    setDrawLabels(true)
                    textColor = themedTextColor // Use themed text color
                    setDrawGridLines(true)
                }
                legend.apply{ // <<< Access legend directly if needed
                    isEnabled = true // <<< ENSURE TRUE
                    textColor = themedTextColor
                    textSize = 10f // <<< Optional: Explicitly set legend text size
                }
                axisRight.isEnabled = false
                legend.textColor = themedTextColor // Use themed text color
                description.isEnabled = false
                // Do not animate here
                invalidate()
            }
        }
        override fun animateChart() {
            barChart.animateY(1000)
        }
    }

    class LineChartViewHolder(
        view: View,
        getTextColor: (Context) -> Int
    ) : BaseChartViewHolder(view, getTextColor) {
        private val lineChart: LineChart = view.findViewById(R.id.lineChart)
        private val themedTextColor = getTextColor(itemView.context)

        fun bind(dataList: List<ChartData>) {
            if (dataList.isEmpty()) {
                lineChart.clear()
                lineChart.setNoDataText("No data available")
                lineChart.setNoDataTextColor(themedTextColor)
                lineChart.invalidate()
                return
            }
            val entries = dataList.mapIndexed { index, item -> Entry(index.toFloat(), item.value.toFloat()) }
            val dataSet = LineDataSet(entries, "Trend by Category").apply {
                color = ContextCompat.getColor(itemView.context, R.color.main_purple)
                setCircleColor(ContextCompat.getColor(itemView.context, R.color.main_green))
                lineWidth = 2.5f; circleRadius = 5f; setDrawCircleHole(false)
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = themedTextColor // Use themed text color
                setDrawFilled(true)
                fillDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.chart_fill_gradient)
            }
            lineChart.apply {
                data = LineData(dataSet)
                description.isEnabled = false
                axisRight.isEnabled = false
                xAxis.apply {
                    setDrawLabels(true) // <<< ENSURE TRUE
                    textColor = themedTextColor
                    valueFormatter = IndexAxisValueFormatter(dataList.map { shortenLabel(it.category) })
                    position = XAxis.XAxisPosition.BOTTOM; granularity = 1f; textColor = themedTextColor // Use themed text color
                    setDrawGridLines(false); labelRotationAngle = -45f; setLabelCount(dataList.size, true)
                }
                axisLeft.apply {
                    setDrawLabels(true) // <<< ENSURE TRUE
                    textColor = themedTextColor
                    axisMinimum = 0f; val maxValue = dataList.maxOfOrNull { it.value.toFloat() } ?: 0f
                    axisMaximum = maxValue * 1.2f; textColor = themedTextColor // Use themed text color
                }
                legend.apply{ // <<< Access legend directly if needed
                    isEnabled = true // <<< ENSURE TRUE
                    textColor = themedTextColor
                    textSize = 10f // <<< Optional: Explicitly set legend text size
                }
                axisRight.isEnabled = false; legend.textColor = themedTextColor; description.isEnabled = false // Use themed text color
                // Do not animate here
                invalidate()
            }
        }
        override fun animateChart() {
            lineChart.animateX(1000)
        }
    }

    class RadarChartViewHolder(
        view: View,
        getTextColor: (Context) -> Int
    ) : BaseChartViewHolder(view, getTextColor) {
        private val radarChart: RadarChart = view.findViewById(R.id.radarChart)
        private val themedTextColor = getTextColor(itemView.context)

        fun bind(dataList: List<ChartData>) {
            if (dataList.isEmpty()) {
                radarChart.clear()
                radarChart.setNoDataText("No data available")
                radarChart.setNoDataTextColor(themedTextColor)
                radarChart.invalidate(); return
            }
            val entries = dataList.map { RadarEntry(it.value.toFloat()) }
            val labels = dataList.map { shortenLabel(it.category) }
            val dataSet = RadarDataSet(entries, "Radar Overview").apply {
                color = ContextCompat.getColor(itemView.context, R.color.main_green)
                fillColor = ContextCompat.getColor(itemView.context, R.color.main_purple)
                fillAlpha = 100
                setDrawValues(true)
                setDrawFilled(true); valueTextSize = 10f
                valueTextColor = themedTextColor // Use themed text color
                lineWidth = 2f
            }
            radarChart.apply {
                data = RadarData(dataSet)
                description.isEnabled = false
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(labels); textColor = themedTextColor // Use themed text color
                    textSize = 9f ; setLabelCount(labels.size, true)
                    textColor = themedTextColor
                    setDrawLabels(true)
                }
                yAxis.apply {
                    axisMinimum = 0f; val maxValue = dataList.maxOfOrNull { it.value.toFloat() } ?: 0f
                    axisMaximum = maxValue * 1.2f; textColor = themedTextColor // Use themed text color
                    setLabelCount(5, true)
                    textColor = themedTextColor
                    setDrawLabels(true)
                }
                legend.apply{ // <<< Access legend directly
                    isEnabled = true // <<< ENSURE TRUE
                    textColor = themedTextColor
                }
                setDrawWeb(true)
                legend.textColor = themedTextColor; description.isEnabled = false // Use themed text color
                webLineWidth = 1f
                webColor = themedTextColor
                webLineWidthInner = 1f
                webColorInner = themedTextColor
                // Make the web lines semi-transparent to look better
                webAlpha = 100 // Example alpha value (0-255)
                // Do not animate here
                invalidate()
            }
        }
        override fun animateChart() {
            radarChart.animateXY(1000, 1000)
        }
    }
}

// Ensure shortenLabel is defined, preferably outside or as a top-level function if used by multiple classes
private fun shortenLabel(label: String, maxLen: Int = 10): String {
    return if (label.length > maxLen) label.substring(0, maxLen) + "…" else label
}
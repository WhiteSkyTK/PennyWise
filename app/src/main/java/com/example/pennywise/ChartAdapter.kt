package com.example.pennywise

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class ChartAdapter(private val context: Context, private val chartDataList: List<ChartData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            is PieChartViewHolder -> holder.bind(chartDataList)
            is BarChartViewHolder -> holder.bind(chartDataList)
            is LineChartViewHolder -> holder.bind(chartDataList)
            is RadarChartViewHolder -> holder.bind(chartDataList)
        }
    }

    private fun isDarkMode(): Boolean {
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun getTextColor(): Int {
        return if (isDarkMode()) Color.WHITE else Color.BLACK
    }

    class PieChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val pieChart: PieChart = view.findViewById(R.id.pieChart)

        fun bind(dataList: List<ChartData>) {
            if (dataList.isEmpty()) {
                pieChart.clear()
                pieChart.setNoDataText("No data available")
                return
            }

            val entries = dataList.map { PieEntry(it.value.toFloat(), shortenLabel(it.category)) }
            val dataSet = PieDataSet(entries, "Category Totals").apply {
                colors = ColorTemplate.COLORFUL_COLORS.toList()
                valueTextColor = if (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
                valueTextSize = 12f
            }

            pieChart.apply {
                data = PieData(dataSet)
                setUsePercentValues(false)
                setEntryLabelColor(dataSet.valueTextColor)
                setEntryLabelTextSize(10f)
                description.isEnabled = true
                legend.textColor = dataSet.valueTextColor
                val minValue = dataList.minOf { it.value.toFloat() }
                val maxValue = dataList.maxOf { it.value.toFloat() }
                description.text = "Min: $minValue | Max: $maxValue"
                val isDarkMode = itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                description.textColor = if (isDarkMode) Color.WHITE else Color.BLACK
                animateY(1000)
                invalidate()
            }
        }
    }

    class BarChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val barChart: BarChart = view.findViewById(R.id.barChart)

        fun bind(dataList: List<ChartData>) {
            if (dataList.isEmpty()) {
                barChart.clear()
                barChart.setNoDataText("No data available")
                return
            }

            val entries = dataList.mapIndexed { index, item ->
                BarEntry(index.toFloat(), item.value.toFloat())
            }
            val dataSet = BarDataSet(entries, "Spending by Category").apply {
                colors = ColorTemplate.COLORFUL_COLORS.toList()
                valueTextSize = 12f
                valueTextColor = if (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
            }

            barChart.apply {
                data = BarData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(dataList.map { shortenLabel(it.category) })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textColor = dataSet.valueTextColor
                    setDrawGridLines(false)
                    labelRotationAngle = 45f // rotate labels 45 degrees to reduce overlap
                    setLabelCount(dataList.size, true) // enforce exact label count
                }
                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = dataList.maxOf { it.value.toFloat() } * 1.2f
                    textColor = dataSet.valueTextColor
                }
                axisRight.isEnabled = false
                legend.textColor = dataSet.valueTextColor
                description.isEnabled = true
                description.text = "Min: ${dataList.minOf { it.value }} | Max: ${dataList.maxOf { it.value }}"
                val isDarkMode = itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                description.textColor = if (isDarkMode) Color.WHITE else Color.BLACK
                animateY(1000)
                invalidate()
            }
        }
    }

    class LineChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val lineChart: LineChart = view.findViewById(R.id.lineChart)

        fun bind(dataList: List<ChartData>) {
            if (dataList.isEmpty()) {
                lineChart.clear()
                lineChart.setNoDataText("No data available")
                return
            }

            val entries = dataList.mapIndexed { index, item ->
                Entry(index.toFloat(), item.value.toFloat())
            }
            val dataSet = LineDataSet(entries, "Trend by Category").apply {
                color = Color.CYAN
                setCircleColor(Color.MAGENTA)
                lineWidth = 2f
                circleRadius = 5f
                valueTextSize = 10f
                valueTextColor = if (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
            }

            lineChart.apply {
                data = LineData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(dataList.map { shortenLabel(it.category) })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textColor = dataSet.valueTextColor
                    setDrawGridLines(false)
                    labelRotationAngle = 45f
                    setLabelCount(dataList.size, true)
                }
                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = dataList.maxOf { it.value.toFloat() } * 1.2f
                    textColor = dataSet.valueTextColor
                }
                axisRight.isEnabled = false
                legend.textColor = dataSet.valueTextColor
                description.isEnabled = true
                description.text = "Min: ${dataList.minOf { it.value }} | Max: ${dataList.maxOf { it.value }}"
                val isDarkMode = itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                description.textColor = if (isDarkMode) Color.WHITE else Color.BLACK
                animateX(1000)
                invalidate()
            }
        }
    }

    class RadarChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val radarChart: RadarChart = view.findViewById(R.id.radarChart)

        fun bind(dataList: List<ChartData>) {
            if (dataList.isEmpty()) {
                radarChart.clear()
                radarChart.setNoDataText("No data available")
                return
            }

            val entries = dataList.map { RadarEntry(it.value.toFloat()) }
            val labels = dataList.map { shortenLabel(it.category) }

            val dataSet = RadarDataSet(entries, "Radar Overview").apply {
                color = Color.MAGENTA
                fillColor = Color.MAGENTA
                setDrawFilled(true)
                valueTextSize = 10f
                valueTextColor = if (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
            }

            radarChart.apply {
                data = RadarData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(labels)
                    textColor = dataSet.valueTextColor
                    textSize = 10f // adjust this to smaller if needed, e.g. 8f
                    setLabelCount(labels.size, true) // enforce exact label count shown
                }
                yAxis.apply {
                    axisMinimum = 0f
                    axisMaximum = dataList.maxOf { it.value.toFloat() } * 1.2f
                    textColor = dataSet.valueTextColor
                }
                legend.textColor = dataSet.valueTextColor
                description.isEnabled = true
                description.text = "Min: ${dataList.minOf { it.value }} | Max: ${dataList.maxOf { it.value }}"
                val isDarkMode = itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                description.textColor = if (isDarkMode) Color.WHITE else Color.BLACK
                animateXY(1000, 1000)
                invalidate()
            }
        }
    }
}
private fun shortenLabel(label: String, maxLen: Int = 10): String {
    return if (label.length > maxLen) label.substring(0, maxLen) + "…" else label
}


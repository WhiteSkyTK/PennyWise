package com.example.pennywise

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.ChartData
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class ChartAdapter(private val context: Context, private val chartDataList: List<ChartData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            0 -> PieChartViewHolder(inflater.inflate(R.layout.item_chart_pie, parent, false))
            1 -> BarChartViewHolder(inflater.inflate(R.layout.item_chart_bar, parent, false), context)
            2 -> LineChartViewHolder(inflater.inflate(R.layout.item_chart_line, parent, false), context)
            3 -> RadarChartViewHolder(inflater.inflate(R.layout.item_chart_radar, parent, false), context)
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

    class PieChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val pieChart: PieChart = view.findViewById(R.id.pieChart)

        fun bind(dataList: List<ChartData>) {
            val entries = dataList.map { PieEntry(it.value.toFloat(), it.category) }
            val dataSet = PieDataSet(entries, "Categories").apply {
                colors = if (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    ColorTemplate.MATERIAL_COLORS.toList()
                } else {
                    ColorTemplate.PASTEL_COLORS.toList()
                }
            }

            pieChart.apply {
                data = PieData(dataSet)
                setUsePercentValues(true)
                description.isEnabled = false
                legend.isEnabled = true
                // Custom logic to show min and max values in a label or legend (since PieChart doesn't have axes)
                val minValue = dataList.minOf { it.value.toFloat() }
                val maxValue = dataList.maxOf { it.value.toFloat() }
                description.text = "Min: $minValue | Max: $maxValue" // Showing min and max in description
                invalidate()
            }
        }
    }

    class BarChartViewHolder(view: View, private val context: Context) : RecyclerView.ViewHolder(view) {
        private val barChart: BarChart = view.findViewById(R.id.barChart)

        fun bind(dataList: List<ChartData>) {
            val entries = dataList.mapIndexed { index, item ->
                BarEntry(index.toFloat(), item.value.toFloat())
            }

            val dataSet = BarDataSet(entries, "Categories").apply {
                colors = if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    ColorTemplate.MATERIAL_COLORS.toList()
                } else {
                    ColorTemplate.PASTEL_COLORS.toList()
                }
            }

            barChart.apply {
                data = BarData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(dataList.map { it.category })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                }

                axisLeft.apply {
                    // Display min and max on the left axis
                    axisMinimum = dataList.minOf { it.value.toFloat() }
                    axisMaximum = dataList.maxOf { it.value.toFloat() }
                }
                axisRight.isEnabled = false
                description.isEnabled = false
                invalidate()
            }
        }
    }

    class LineChartViewHolder(view: View, private val context: Context) : RecyclerView.ViewHolder(view) {
        private val lineChart: LineChart = view.findViewById(R.id.lineChart)

        fun bind(dataList: List<ChartData>) {
            val entries = dataList.mapIndexed { index, item ->
                Entry(index.toFloat(), item.value.toFloat())
            }

            val dataSet = LineDataSet(entries, "Categories").apply {
                color = ColorTemplate.MATERIAL_COLORS[0]
                setCircleColor(ColorTemplate.MATERIAL_COLORS[0])
                lineWidth = 2f
                circleRadius = 4f
            }

            lineChart.apply {
                data = LineData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(dataList.map { it.category })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                }

                axisLeft.apply {
                    // Display min and max on the left axis
                    axisMinimum = dataList.minOf { it.value.toFloat() }
                    axisMaximum = dataList.maxOf { it.value.toFloat() }
                }
                axisRight.isEnabled = false
                description.isEnabled = false
                invalidate()
            }
        }
    }

    class RadarChartViewHolder(view: View, private val context: Context) : RecyclerView.ViewHolder(view) {
        private val radarChart: RadarChart = view.findViewById(R.id.radarChart)

        fun bind(dataList: List<ChartData>) {
            val entries = dataList.map { RadarEntry(it.value.toFloat()) }
            val labels = dataList.map { it.category }

            val dataSet = RadarDataSet(entries, "Categories").apply {
                color = ColorTemplate.MATERIAL_COLORS[1]
                fillColor = ColorTemplate.MATERIAL_COLORS[1]
                setDrawFilled(true)
            }

            radarChart.apply {
                data = RadarData(dataSet)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                description.isEnabled = false

                // Set min and max for the radial axis (yAxis)
                yAxis.apply {
                    axisMinimum = dataList.minOf { it.value.toFloat() }
                    axisMaximum = dataList.maxOf { it.value.toFloat() }
                }

                invalidate()
            }
        }
    }
}
package com.example.pennywise

import android.app.Activity
import android.widget.ImageView
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import java.text.SimpleDateFormat
import java.util.*

class HeaderManager(
    private val activity: Activity,
    private val drawerLayout: DrawerLayout,
    private val onMonthChanged: ((String) -> Unit)? = null
) {
    private val calendar = Calendar.getInstance()
    private val calendarText: TextView = activity.findViewById(R.id.calendarText)
    private val prevButton: ImageView = activity.findViewById(R.id.calendarPrev)
    private val nextButton: ImageView = activity.findViewById(R.id.calendarNext)
    private val menuIcon: ImageView = activity.findViewById(R.id.ic_menu)
    private val titleText: TextView = activity.findViewById(R.id.topTitle)

    init {
        updateCalendarText()

        prevButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendarText()
            onMonthChanged?.invoke(getFormattedDate())
        }

        nextButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendarText()
            onMonthChanged?.invoke(getFormattedDate())
        }

        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }
    }

    fun setupHeader(title: String) {
        titleText.text = title
        updateCalendarText()
    }

    private fun updateCalendarText() {
        calendarText.text = getFormattedDate()
    }

    private fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("yyyy MMM", Locale.getDefault())
        return dateFormat.format(calendar.time).uppercase(Locale.getDefault())
    }
}

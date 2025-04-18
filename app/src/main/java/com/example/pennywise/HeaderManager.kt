package com.example.pennywise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.PopupMenu
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
    private val profileInitials: TextView? = activity.findViewById(R.id.profileInitials)

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

        setupInitialsFromPrefs()
    }

    fun setupHeader(title: String) {
        titleText.text = title
        updateCalendarText()
    }

    private fun setupInitialsFromPrefs() {
        val sharedPref = activity.getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        val initials = email.take(2).uppercase(Locale.getDefault())

        profileInitials?.text = initials

        profileInitials?.setOnClickListener {
            val popup = PopupMenu(activity, it)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sign_out -> {
                        with(sharedPref.edit()) {
                            remove("loggedInUserEmail")
                            apply()
                        }
                        activity.startActivity(Intent(activity, Activity_Login_Resgister::class.java))
                        activity.finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun updateCalendarText() {
        calendarText.text = getFormattedDate()
    }

    private fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("yyyy MMM", Locale.getDefault())
        return dateFormat.format(calendar.time).uppercase(Locale.getDefault())
    }
}

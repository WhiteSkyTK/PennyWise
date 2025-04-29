package com.example.pennywise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class HeaderManager(
    private val activity: Activity,
    private val drawerLayout: DrawerLayout,
    private val transactionDao: TransactionDao,
    private val lifecycleScope: LifecycleCoroutineScope,
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
        loadAndDisplayBalance()

        prevButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendarText()
            loadAndDisplayBalance()
            onMonthChanged?.invoke(getFormattedDate())
        }

        nextButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendarText()
            loadAndDisplayBalance()
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
        loadAndDisplayBalance()
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

    fun setupDrawerNavigation(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawers()
            when (item.itemId) {
                R.id.nav_about -> {
                    activity.startActivity(Intent(activity, AboutActivity::class.java))
                    true
                }
                R.id.nav_currency -> {
                    activity.startActivity(Intent(activity, CurrencySettingsActivity::class.java))
                    true
                }
                R.id.nav_gamification -> {
                    activity.startActivity(Intent(activity, GamificationActivity::class.java))
                    true
                }
                R.id.nav_feedback -> {
                    activity.startActivity(Intent(activity, FeedbackActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }


    private fun loadAndDisplayBalance() {
        val sharedPref = activity.getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val email =
            sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        val selectedMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val selectedYear = calendar.get(Calendar.YEAR).toString()

        lifecycleScope.launch {
            val transactions =
                transactionDao.getTransactionsByUserAndMonth(email, selectedMonth, selectedYear)
            val totalIncome =
                transactions.filter { it.type.lowercase() == "income" }.sumOf { it.amount }
            val totalExpense =
                transactions.filter { it.type.lowercase() == "expense" }.sumOf { it.amount }
            val totalBalance = totalIncome - totalExpense

            val incomeText = activity.findViewById<TextView?>(R.id.incomeAmount)
            val expenseText = activity.findViewById<TextView?>(R.id.expenseAmount)
            val balanceText = activity.findViewById<TextView?>(R.id.balanceAmount)

            incomeText?.text = "R%.2f".format(abs(totalIncome))
            expenseText?.text = "R%.2f".format(abs(totalExpense))
            balanceText?.text =
                if (totalBalance < 0) "-R%.2f".format(abs(totalBalance)) else "R%.2f".format(
                    totalBalance
                )
        }
    }
}

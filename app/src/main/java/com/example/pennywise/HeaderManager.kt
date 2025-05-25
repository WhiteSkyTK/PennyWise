package com.example.pennywise

import android.animation.ValueAnimator
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class HeaderManager(
    private val activity: Activity,
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView, // <-- add this
    private val onMonthChanged: ((String) -> Unit)? = null
) {
    private val calendar = Calendar.getInstance()
    private val calendarText: TextView = activity.findViewById(R.id.calendarText)
    private val prevButton: ImageView = activity.findViewById(R.id.calendarPrev)
    private val nextButton: ImageView = activity.findViewById(R.id.calendarNext)
    private val menuIcon: ImageView = activity.findViewById(R.id.ic_menu)
    private val titleText: TextView = activity.findViewById(R.id.topTitle)
    private val profileInitials: TextView? = activity.findViewById(R.id.profileInitials)
    private val firestore = FirebaseFirestore.getInstance()

    //load (Call)
    init {
        updateCalendarText()
        loadAndDisplayBalance()

        calendarText.setOnClickListener {
            openDatePicker()
        }

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

        // Apply extra padding only in ReportActivity
        if (activity is ReportActivity) {
            val headerContainer = activity.findViewById<View>(R.id.headerContainer)
            headerContainer?.setPadding(
                headerContainer.paddingLeft,
                headerContainer.paddingTop + 32, // increase top padding
                headerContainer.paddingRight,
                headerContainer.paddingBottom
            )
        }

        setupInitialsFromPrefs()
    }

    //setup header
    fun setupHeader(title: String) {
        titleText.text = title
        updateCalendarText()
        loadAndDisplayBalance()
    }

    //fetch email
    private fun setupInitialsFromPrefs() {
        val sharedPref = activity.getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val email =
            sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        val initials = email.take(2).uppercase(Locale.getDefault())

        profileInitials?.text = initials

        // Access the nav header view and set the email
        val headerView = navigationView.getHeaderView(0)
        val emailTextView = headerView.findViewById<TextView>(R.id.navHeaderEmail)
        emailTextView.text = email

        profileInitials?.setOnClickListener {
            val popup = PopupMenu(activity, it)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sign_out -> {
                        with(sharedPref.edit()) {
                            remove("loggedInUserEmail")
                            remove("loggedInUserId")
                            apply()
                        }
                        activity.startActivity(Intent(activity, ActivityLoginResgister::class.java))
                        activity.finish()
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    //update calender function
    private fun updateCalendarText() {
        calendarText.text = getFormattedDate()
    }

    //date formatted
    private fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("yyyy MMM", Locale.getDefault())
        return dateFormat.format(calendar.time).uppercase(Locale.getDefault())
    }

    //open date logic
    private fun openDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(activity, { _, selectedYear, selectedMonth, _ ->
            calendar.set(Calendar.YEAR, selectedYear)
            calendar.set(Calendar.MONTH, selectedMonth)
            updateCalendarText()
            loadAndDisplayBalance()
            onMonthChanged?.invoke(getFormattedDate())
        }, year, month, day)

        datePicker.show()
    }

    //functions for the nav
    fun setupDrawerNavigation(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawers()
            when (item.itemId) {
                R.id.nav_about -> {
                    activity.startActivity(Intent(activity, AboutActivity::class.java))
                    activity.overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                    )
                    true
                }

                R.id.nav_gamification -> {
                    activity.startActivity(Intent(activity, GamificationActivity::class.java))
                    activity.overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                    )
                    true
                }

                R.id.nav_feedback -> {
                    activity.startActivity(Intent(activity, FeedbackActivity::class.java))
                    activity.overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                    )
                    true
                }

                R.id.nav_theme -> {
                    ThemeUtils.toggleTheme(activity)
                    activity.window.setWindowAnimations(android.R.style.Animation_Dialog) // Optional for smoother recreate
                    activity.overridePendingTransition(
                        android.R.anim.fade_out,
                        android.R.anim.fade_in
                    )
                    activity.recreate()
                    true
                }

                R.id.nav_profile -> {
                    val sharedPref =
                        activity.getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
                    val email = sharedPref.getString("loggedInUserEmail", "user@example.com")
                        ?: "user@example.com"

                    val intent = Intent(activity, ProfileActivity::class.java)
                    intent.putExtra("user_email", email)

                    activity.startActivity(intent)
                    activity.overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                    )
                    true
                }

                else -> false
            }
        }
    }

    private fun animateCount(
        textView: TextView?,
        targetValue: Double,
        isNegative: Boolean = false
    ) {
        textView ?: return

        val startValue = 0.0
        val animator = ValueAnimator.ofFloat(startValue.toFloat(), targetValue.toFloat())
        animator.duration = 1000 // 1 second
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            val formatted = "R%.2f".format(abs(animatedValue.toDouble()))
            textView.text = if (isNegative) "-$formatted" else formatted
        }
        animator.start()
    }

    //load balance
    private fun loadAndDisplayBalance() {
        val sharedPref = activity.getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("loggedInUserId", "") ?: ""

        if (userId.isEmpty()) return  // no user logged in

        val selectedMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val selectedYear = calendar.get(Calendar.YEAR).toString()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Get all transactions from the subcollection
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .get()
                    .await()

                val allTransactions = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }

                // Client-side filter for selected month and year
                val filteredTransactions = allTransactions.filter { transaction ->
                    val date = java.util.Date(transaction.date)
                    val cal = Calendar.getInstance().apply { time = date }
                    val month = cal.get(Calendar.MONTH) + 1
                    val year = cal.get(Calendar.YEAR)
                    month == selectedMonth.toInt() && year == selectedYear.toInt()
                }

                val totalIncome = filteredTransactions.filter { it.type.equals("income", true) }.sumOf { it.amount }
                val totalExpense = filteredTransactions.filter { it.type.equals("expense", true) }.sumOf { it.amount }
                val totalBalance = totalIncome - totalExpense

                val incomeText = activity.findViewById<TextView>(R.id.incomeAmount)
                val expenseText = activity.findViewById<TextView>(R.id.expenseAmount)
                val balanceText = activity.findViewById<TextView>(R.id.balanceAmount)

                animateCount(incomeText, abs(totalIncome))
                animateCount(expenseText, abs(totalExpense))
                animateCount(balanceText, abs(totalBalance), totalBalance < 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
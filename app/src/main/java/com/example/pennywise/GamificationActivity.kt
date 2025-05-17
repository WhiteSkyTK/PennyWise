package com.example.pennywise

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pennywise.data.AppDatabase
import kotlinx.coroutines.launch

class GamificationActivity : AppCompatActivity() {

    private lateinit var badgeRecycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var levelText: TextView
    private lateinit var xpText: TextView
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("GamificationActivity", "onCreate called")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gamification)

        supportActionBar?.hide()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gamificationLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val userEmail = getLoggedInUserEmail()
        Log.d("GamificationActivity", "Logged in user email: $userEmail")
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        badgeRecycler = findViewById(R.id.badgeRecycler)
        progressBar = findViewById(R.id.levelProgressBar)
        levelText = findViewById(R.id.levelText)
        xpText = findViewById(R.id.xpText)

        loadUserGamificationData() // fetch actual login streak + badges
    }

    private fun setupBadgeRecyclerAsync(email: String, loginStreak: LoginStreak?) {
        lifecycleScope.launch {
            val badgeList = getBadgesForUser(email)
            badgeRecycler.layoutManager = GridLayoutManager(this@GamificationActivity, 2)
            badgeRecycler.adapter = BadgeAdapter(badgeList, loginStreak)
            Log.d("Gamification", "Badges retrieved: ${badgeList.size}")

        }
    }

    private fun getLoggedInUserEmail(): String {
        val prefs = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
        return prefs.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
    }

    private fun loadUserGamificationData() {
        userEmail = getLoggedInUserEmail()
        Log.d("Gamification", "Loaded email: $userEmail")

        if (userEmail != null) {
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val streak = db.loginStreakDao().getStreak(userEmail)
                Log.d("Gamification", "Login streak fetched: $streak")

                var totalXP = getXP()

                streak?.let {
                    val baseXP = 10
                    val multiplier = 1 + (streak.streak / 5) // e.g., 1x at <5, 2x at 10+, etc.

                    Log.d("Gamification", "LoginStreak Days: ${streak.totalLoginDaysThisYear}, Multiplier: $multiplier")

                    totalXP += it.totalLoginDaysThisYear * baseXP * multiplier


                    if (it.streak >= 5) {
                        totalXP += (it.streak - 4) * 2
                        Log.d("Gamification", "Bonus XP from streak: ${(streak.streak - 4) * 2}")
                    }else {
                        Log.d("Gamification", "No login streak found for user")
                    }
                }

                val (level, progress) = calculateLevelFromXP(totalXP)
                Log.d("Gamification", "Calculated level: $level, Progress: $progress")
                levelText.text = "Level $level"
                progressBar.progress = progress
                xpText.text = "$totalXP XP"

                checkAndAwardAllLoginBadges(userEmail, streak) {
                    lifecycleScope.launch {
                        checkAndAwardBudgetKeeper(userEmail)
                        checkAndAwardSavedMore(userEmail) // Now suspendable
                        setupBadgeRecyclerAsync(userEmail, streak)
                    }
                }

                Log.d("Gamification", "XP: $totalXP | Level: $level | Progress: $progress%")
                Log.d("Gamification", "Calling setupBadgeRecyclerAsync with $userEmail")
            }
        }else {
            Log.w("Gamification", "User email is null!")
        }
    }

    private fun calculateLevelFromXP(totalXP: Int): Pair<Int, Int> {
        val xpPerLevel = 100
        val level = totalXP / xpPerLevel + 1
        val progress = totalXP % xpPerLevel
        return Pair(level, progress)
    }

    private suspend fun generateBudgetGoalFromCategoryLimits(month: String, db: AppDatabase) {
        val categoryLimits = db.categoryLimitDao().getCategoryLimits(month)
        if (categoryLimits.isEmpty()) {
            Log.w("BudgetSync", "No category limits found for month $month, skipping BudgetGoal generation")
            return
        }

        // Sum minAmounts and maxAmounts separately across all categories for the month
        val totalMinAmount = categoryLimits.sumOf { it.minAmount }
        val totalMaxAmount = categoryLimits.sumOf { it.maxAmount }

        val budgetGoal = BudgetGoal(
            month = month,
            minAmount = totalMinAmount,
            maxAmount = totalMaxAmount
        )
        db.budgetGoalDao().insertBudgetGoal(budgetGoal)
        Log.d("BudgetSync", "Inserted BudgetGoal for $month: min=$totalMinAmount, max=$totalMaxAmount")
    }

    private suspend fun checkAndAwardBudgetKeeper(email: String) {
        val db = AppDatabase.getDatabase(applicationContext)

        try {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            val monthStr = String.format("%04d-%02d", year, month)
            Log.d("BudgetBadge", "Checking budget for month: $monthStr")

            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.set(java.util.Calendar.DAY_OF_MONTH, daysInMonth)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            calendar.set(java.util.Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.timeInMillis

            var budgetGoal = db.budgetGoalDao().getBudgetGoal(monthStr)

            if (budgetGoal == null) {
                Log.w("BudgetBadge", "No budget goal found for $monthStr, attempting to generate...")
                generateBudgetGoalFromCategoryLimits(monthStr, db)
                budgetGoal = db.budgetGoalDao().getBudgetGoal(monthStr)
            }

            if (budgetGoal != null) {
                val now = System.currentTimeMillis()
                if (now > endOfMonth) {
                    val totalSpent = db.transactionDao().getSpendingInRange(email, startOfMonth, endOfMonth)
                    Log.d("BudgetBadge", "Total spent: $totalSpent vs Budget: ${budgetGoal.maxAmount}")
                    if (totalSpent <= budgetGoal.maxAmount) {
                        awardBadgeIfNew("Budget Keeper", email)
                    }
                } else {
                    Log.d("BudgetBadge", "Month not over yet, skipping badge evaluation.")
                }
            } else {
                Log.w("BudgetBadge", "No budget goal could be generated. Skipping badge award.")
            }
        } catch (e: Exception) {
            Log.e("BudgetBadge", "Error during budget badge check", e)
        }
    }

    private suspend fun checkAndAwardSavedMore(email: String) {
        val db = AppDatabase.getDatabase(applicationContext)

        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        val startOfDay = calendar.timeInMillis

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)

        val endOfDay = calendar.timeInMillis

        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val monthStr = String.format("%04d-%02d", year, month)

        val budgetGoal = db.budgetGoalDao().getBudgetGoal(monthStr) ?: return
        val minDaily = budgetGoal.minAmount / daysInMonth
        val spentToday = db.transactionDao().getSpendingInRange(email, startOfDay, endOfDay)

        if (spentToday < minDaily) {
            awardBadgeIfNew("Saved More", email)
        }
        Log.d("SavedMoreBadge", "Spent today: $spentToday, Min daily: $minDaily")
    }

    private fun showAchievementPopup(title: String) {
        val description = getDescriptionForBadge(title)
        val iconResId = getIconForBadge(title)

        val dialog = AchievementDialogFragment.newInstance(title, description, iconResId)
        dialog.show(supportFragmentManager, "achievementDialog")
    }

    private suspend fun awardBadgeIfNew(title: String, userEmail: String) {
        val db = AppDatabase.getDatabase(applicationContext)
        val alreadyEarned = db.earnedBadgeDao().isBadgeEarned(userEmail, title) > 0
        Log.d("BadgeCheck", "Is badge '$title' already earned by $userEmail: $alreadyEarned")

        if (!alreadyEarned) {
            Log.d("BadgeAward", "Awarding new badge: $title to $userEmail")
            val badge = EarnedBadge(
                userEmail = userEmail,
                badgeTitle = title,
                earnedTimestamp = System.currentTimeMillis()
            )
            db.earnedBadgeDao().insertBadge(badge)
            showAchievementPopup(title)

            // Add XP
            val currentXP = getXP()
            val xpEarned = getXPForBadge(title)
            val newXP = currentXP + xpEarned
            setXP(newXP)

            // Optional: update XP UI
            val (level, progress) = calculateLevelFromXP(newXP)
            runOnUiThread {
                levelText.text = "Level $level"
                progressBar.progress = progress
                xpText.text = "$newXP XP"
            }
        } else {
            Log.d("BadgeAward", "Badge '$title' already earned. Skipping.")
        }
    }


    private suspend fun getBadgesForUser(email: String): List<Badge> {
        val db = AppDatabase.getDatabase(applicationContext)
        val earnedBadges = db.earnedBadgeDao().getEarnedBadges(email)
        val earnedTitles = earnedBadges.map { it.badgeTitle }

        val badgeList = mutableListOf<Badge>()

        val allBadgeTitles = listOf(
            "First Login",
            "Daily Visitor",
            "Login Streak",
            "Budget Keeper",
            "Saved More",
            "No Spend Day"
        )

        for ((index, title) in allBadgeTitles.withIndex()) {
            val isEarned = earnedTitles.contains(title)
            val overlay = when (title) {
                "Daily Visitor" -> {
                    val badge = earnedBadges.find { it.badgeTitle == title }
                    val count = badge?.metadata ?: 1
                    "x$count"
                }
                else -> null
            }
            badgeList.add(
                Badge(
                    id = index,
                    title = title,
                    description = getDescriptionForBadge(title),
                    iconResId = getIconForBadge(title),
                    isEarned = isEarned,
                    overlayText = overlay
                )
            )
        }
        Log.d("Badge", "Earned badges: ${earnedTitles.joinToString()}")
        Log.d("Badge", "Total badges prepared for adapter: ${badgeList.size}")
        return badgeList
    }

    private fun getDescriptionForBadge(title: String): String {
        return when (title) {
            "First Login" -> "Logged in for the first time"
            "Daily Visitor" -> "Logged in multiple days"
            "Login Streak" -> "Maintained login streak"
            "Budget Keeper" -> "Stayed within daily budget"
            "Saved More" -> "Saved more than your budget goal"
            "No Spend Day" -> "Spent nothing today"
            else -> ""
        }
    }

    private fun getIconForBadge(title: String): Int {
        return when (title) {
            "First Login" -> R.drawable.badge1
            "Daily Visitor" -> R.drawable.badge2
            "Login Streak" -> R.drawable.badge3
            "Budget Keeper" -> R.drawable.badge4
            "Saved More" -> R.drawable.badge1
            "No Spend Day" -> R.drawable.badge2
            else -> R.drawable.badge1
        }
    }

    private fun checkAndAwardAllLoginBadges(email: String, streak: LoginStreak?, onComplete: () -> Unit) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val alreadyEarned = db.earnedBadgeDao().getEarnedBadges(email)
            Log.d("Gamification", "checkAndAwardAllLoginBadges started")
            if (alreadyEarned.isEmpty()) {
                awardBadgeIfNew("First Login", email)
            }

            if (streak != null) {
                // Daily Visitor (any login day)
                if (streak.totalLoginDaysThisYear >= 1) {
                    awardBadgeIfNew("Daily Visitor", email)
                }

                // Login Streak (if at least 3 days in a row)
                if (streak.streak >= 3) {
                    awardBadgeIfNew("Login Streak", email)
                }
            }

            // No Spend Day
            val calendar = java.util.Calendar.getInstance()
            val start = calendar.apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            calendar.set(java.util.Calendar.MILLISECOND, 999)

            val end = calendar.timeInMillis

            if ((streak?.totalLoginDaysThisYear ?: 0) > 1) {
                val spentToday = db.transactionDao().getSpendingInRange(email, start, end)
                if (spentToday <= 0.0) {
                    awardBadgeIfNew("No Spend Day", email)
                }
            }
            Log.d("Gamification", "checkAndAwardAllLoginBadges Complete")

            onComplete()
        }
    }
    private fun getXP(): Int {
        val prefs = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
        return prefs.getInt("userXP_$userEmail", 0)
    }

    private fun setXP(xp: Int) {
        val prefs = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
        prefs.edit().putInt("userXP_$userEmail", xp).apply()
    }

    private fun getXPForBadge(title: String): Int {
        return when (title) {
            "First Login" -> 30
            "Daily Visitor" -> 15
            "Login Streak" -> 40
            "Budget Keeper" -> 39
            "Saved More" -> 45
            "No Spend Day" -> 35
            else -> 10
        }
    }
}
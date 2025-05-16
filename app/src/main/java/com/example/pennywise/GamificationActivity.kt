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
            badgeRecycler.layoutManager = GridLayoutManager(this@GamificationActivity, 3)
            badgeRecycler.adapter = BadgeAdapter(badgeList, loginStreak)
        }
    }

    private fun getLoggedInUserEmail(): String {
        val prefs = getSharedPreferences("PennyWisePrefs", MODE_PRIVATE)
        return prefs.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
    }

    private fun loadUserGamificationData() {
        val email = getLoggedInUserEmail()
        Log.d("Gamification", "Loaded email: $email")

        if (email != null) {
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val streak = db.loginStreakDao().getStreak(email)

                var totalXP = 0

                streak?.let {
                    totalXP += it.totalLoginDaysThisYear * 10
                    if (it.streak >= 5) {
                        totalXP += (it.streak - 4) * 2
                    }
                }

                val (level, progress) = calculateLevelFromXP(totalXP)
                levelText.text = "Level $level"
                progressBar.progress = progress
                xpText.text = "$totalXP XP"

                setupBadgeRecyclerAsync(email, streak)
                checkAndAwardBudgetKeeper(email) {
                    setupBadgeRecyclerAsync(email, streak) // reload after badge awarded
                }
                Log.d("Gamification", "XP: $totalXP | Level: $level | Progress: $progress%")
                Log.d("Gamification", "Calling setupBadgeRecyclerAsync with $email")
            }
        }
    }

    private fun calculateLevelFromXP(totalXP: Int): Pair<Int, Int> {
        val xpPerLevel = 100
        val level = totalXP / xpPerLevel + 1
        val progress = totalXP % xpPerLevel
        return Pair(level, progress)
    }

    private fun checkAndAwardBudgetKeeper(email: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
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
            val month = calendar.get(java.util.Calendar.MONTH) + 1 // 0-based
            val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

            val monthStr = String.format("%04d-%02d", year, month)

            val budgetGoal = db.budgetGoalDao().getBudgetGoal(monthStr) ?: return@launch
            val dailyBudget = (budgetGoal.minAmount + budgetGoal.maxAmount) / 2 / daysInMonth

            val spentToday = db.transactionDao().getSpendingInRange(email, startOfDay, endOfDay)

            if (spentToday <= dailyBudget) {
                awardBadgeIfNew("Budget Keeper", email)
            }
            Log.d("BudgetBadge", "Spent today: $spentToday, Daily budget: $dailyBudget")
            onComplete()
        }
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
        Log.d("Badge", "Checking if $title is already earned by $userEmail: $alreadyEarned")
        if (!alreadyEarned) {
            Log.d("Badge", "Awarding new badge: $title to $userEmail")
            val badge = EarnedBadge(
                userEmail = userEmail,
                badgeTitle = title,
                earnedTimestamp = System.currentTimeMillis()
            )
            db.earnedBadgeDao().insertBadge(badge)
            showAchievementPopup(title)
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

        for (title in allBadgeTitles) {
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
                    title = title,
                    description = getDescriptionForBadge(title), // you'll define this
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
}
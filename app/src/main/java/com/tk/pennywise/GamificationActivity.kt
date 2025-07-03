package com.tk.pennywise


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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class GamificationActivity : AppCompatActivity() {
    private val TAG = "GamificationActivity"
    private lateinit var badgeRecycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var levelText: TextView
    private lateinit var xpText: TextView
    private lateinit var badgeAdapter: BadgeAdapter
    private val fs by lazy { Firebase.firestore }
    private val userId: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        setContentView(R.layout.activity_gamification)
        supportActionBar?.hide()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gamificationLayout)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
        badgeRecycler = findViewById(R.id.badgeRecycler)
        progressBar = findViewById(R.id.levelProgressBar)
        levelText = findViewById(R.id.levelText)
        xpText = findViewById(R.id.xpText)

        lifecycleScope.launch {
            val uid = userId ?: run { Log.w(TAG, "User not logged in"); return@launch }
            // 1) Track today's visit and return if first visit
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
            val newVisit = trackTodayVisit(uid, userEmail)
            // 2) Now load and update UI
            loadUserGamificationData(uid, newVisit)
        }
    }

    private suspend fun trackTodayVisit(userId: String, userEmail: String): Boolean {
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayKey = "%04d-%02d-%02d".format(
            todayCalendar.get(Calendar.YEAR),
            todayCalendar.get(Calendar.MONTH) + 1,
            todayCalendar.get(Calendar.DAY_OF_MONTH)
        )

        val visitRef = fs.collection("users").document(userId)
            .collection("visitLogs").document(todayKey)

        val visitDoc = visitRef.get().await()
        if (visitDoc.exists()) {
            Log.d(TAG, "Visit already logged for $todayKey")
            return false
        }

        visitRef.set(mapOf("visited" to true, "timestamp" to System.currentTimeMillis())).await()
        Log.d(TAG, "Logged daily visit for $todayKey")

        val streakRef = fs.collection("users").document(userId)
            .collection("loginStreaks").document("current")
        val streakDocSnapshot = streakRef.get().await()
        if (!streakDocSnapshot.exists()) {
            Log.d(TAG, "Login streak document does not exist!")
        } else {
            Log.d(TAG, "Login streak document exists: ${streakDocSnapshot.data}")
        }
        val currentStreak = streakDocSnapshot.toObject(LoginStreak::class.java)
        Log.d(TAG, "currentStreak object: $currentStreak")

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val updatedStreak = if (currentStreak != null) {
            val lastLogin = Calendar.getInstance().apply {
                timeInMillis = currentStreak.lastLoginDate
            }

            val isYesterday = lastLogin.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    lastLogin.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

            LoginStreak(
                userEmail = userEmail,
                streak = if (isYesterday) currentStreak.streak + 1 else 1,
                totalLoginDaysThisYear = currentStreak.totalLoginDaysThisYear + 1,
                lastLoginDate = today.timeInMillis
            )
        } else {
            LoginStreak(
                userEmail = userEmail,
                streak = 1,
                totalLoginDaysThisYear = 1,
                lastLoginDate = today.timeInMillis
            )
        }

        streakRef.set(updatedStreak).await()
        Log.d(TAG, "Updated login streak: $updatedStreak")

        return true
    }

    private fun loadUserGamificationData(userId: String, newVisit: Boolean) = lifecycleScope.launch {
        try {
            val streak = fs.collection("users").document(userId)
                .collection("loginStreaks").document("current")
                .get().await().toObject(LoginStreak::class.java)

            Log.d(TAG, "Fetched streak: $streak")

            var xp = fs.collection("users").document(userId)
                .collection("xp").document("current")
                .get().await().getLong("value")?.toInt() ?: 0

            if (newVisit && streak != null) {
                Log.d(TAG, "New visit detected, awarding XP based on streak: $streak")
                val bonus = streak.totalLoginDaysThisYear * 10 * (1 + streak.streak / 5)
                val streakBonus = if (streak.streak >= 5) (streak.streak - 4) * 2 else 0
                Log.d(TAG, "XP bonus: $bonus + streak bonus: $streakBonus")

                xp += bonus + streakBonus
            }

            // Award Daily Visitor if new visit
            if (newVisit) {
                Log.d(TAG, "First visit today – awarding Daily Visitor badge & XP")
                awardBadgeIfNew(userId, "Daily Visitor")
                xp += getXPForBadge("Daily Visitor")
            }

            // Persist and display
            Log.d(TAG, "Setting XP to: $xp")
            setXP(userId, xp)
            updateUI(xp)

            // One-time badges
            checkAndAwardAllLoginBadges(userId, streak)
            checkBudgetAndSavingsBadges(userId)

            // Fetch all earned badges with metadata
            val earnedDocs = fs.collection("users").document(userId)
                .collection("earnedBadges").get().await()

            val earnedBadges = earnedDocs.associateBy({ it.id }, { it })

            val allBadges = listOf(
                "First Login", "Daily Visitor", "Login Streak",
                "Budget Keeper", "Saved More", "No Spend Day"
            )

            val freshBadges = allBadges.mapIndexed { i, title ->
                val earnedDoc = earnedBadges[title]
                val isEarned = earnedDoc != null
                val count = earnedDoc?.getLong("metadata")?.toInt()

                Badge(
                    id = i,
                    title = title,
                    description = getDescriptionForBadge(title),
                    iconResId = getIconForBadge(title),
                    isEarned = isEarned,
                    overlayText = if ((count ?: 0) >= 2) "x$count" else null
                )
            }

            // Setup RecyclerView with updated data
            badgeAdapter = BadgeAdapter(freshBadges, streak)
            badgeRecycler.layoutManager = GridLayoutManager(this@GamificationActivity, 2)
            badgeRecycler.adapter = badgeAdapter

        } catch (e: Exception) {
            Log.e(TAG, "Error loading gamification data", e)
        }
    }

    private fun setupBadgeRecycler(userId: String, streak: LoginStreak?) = lifecycleScope.launch {
        val earned = fs.collection("users").document(userId)
            .collection("earnedBadges").get().await().documents
            .mapNotNull { it.id }

        val badges = listOf(
            "First Login", "Daily Visitor", "Login Streak",
            "Budget Keeper", "Saved More", "No Spend Day"
        ).mapIndexed { i, title ->
            val count = if (title == "Daily Visitor")
                fs.collection("users").document(userId)
                    .collection("earnedBadges").document(title)
                    .get().await().getLong("metadata")?.toInt() ?: 1
            else null

            Badge(
                id = i,
                title = title,
                description = getDescriptionForBadge(title),
                iconResId = getIconForBadge(title),
                isEarned = earned.contains(title),
                overlayText = count?.let { "x$it" }
            )
        }

        badgeRecycler.layoutManager = GridLayoutManager(this@GamificationActivity, 2)
        badgeRecycler.adapter = BadgeAdapter(badges, streak)
    }

    private fun updateUI(xp: Int) {
        val (level, progress) = calculateLevelFromXP(xp)
        levelText.text = "Level $level"
        progressBar.progress = progress
        xpText.text = "$xp XP"
    }

    private fun calculateLevelFromXP(totalXP: Int) = Pair(totalXP / 100 + 1, totalXP % 100)
    private fun startOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun endOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    private suspend fun awardBadgeIfNew(userId: String, title: String) {
        val badgeRef = fs.collection("users").document(userId)
            .collection("earnedBadges").document(title)

        val doc = badgeRef.get().await()
        val alreadyEarned = doc.exists()

        if (!alreadyEarned) {
            Log.d(TAG, "Awarding new badge: $title")
            badgeRef.set(mapOf("earnedTimestamp" to System.currentTimeMillis(), "metadata" to 1)).await()
            showAchievementPopup(title)

            val currentXP = fs.collection("users").document(userId)
                .collection("xp").document("current").get().await().getLong("value")?.toInt() ?: 0
            val updatedXP = currentXP + getXPForBadge(title)
            setXP(userId, updatedXP)
            updateUI(updatedXP)
        } else {
            // Increment metadata count (e.g., Daily Visitor x5)
            if (title == "Daily Visitor") {
                val currentCount = doc.getLong("metadata")?.toInt() ?: 1
                badgeRef.update("metadata", currentCount + 1).await()
                Log.d(TAG, "Incremented metadata count for $title to ${currentCount + 1}")
            } else {
                Log.d(TAG, "Badge already earned: $title")
            }
        }
    }

    private fun checkAndAwardAllLoginBadges(userId: String, streak: LoginStreak?) = lifecycleScope.launch {
        val earnedBefore = fs.collection("users").document(userId)
            .collection("earnedBadges").get().await().documents.map { it.id }.toSet()

        val toAward = mutableSetOf<String>()

        // First Login
        if (!earnedBefore.contains("First Login")) {
            Log.d(TAG, "Checking 'First Login': Not earned yet → Will be awarded")
            toAward += "First Login"
        } else Log.d(TAG, "'First Login' already earned")

        // Daily Visitor
        val totalDays = streak?.totalLoginDaysThisYear ?: 0
        if (totalDays >= 1) {
            Log.d(TAG, "Checking 'Daily Visitor': $totalDays days visited → Eligible")
            toAward += "Daily Visitor"
        } else Log.d(TAG, "Skipping 'Daily Visitor': only $totalDays days visited")

        // Login Streak
        val streakCount = streak?.streak ?: 0
        if (streakCount >= 3) {
            Log.d(TAG, "Checking 'Login Streak': streak is $streakCount → Eligible")
            toAward += "Login Streak"
        } else Log.d(TAG, "Skipping 'Login Streak': streak is $streakCount")

        Log.d(TAG, "Fetched streak: $streak")

        // No Spend Day
        val spentToday = fs.collection("users").document(userId)
            .collection("transactions")
            .whereGreaterThanOrEqualTo("date", startOfDay())
            .whereLessThanOrEqualTo("date", endOfDay())
            .whereEqualTo("type", "expense")
            .get().await().documents.sumOf { it.getDouble("amount") ?: 0.0 }

        if (totalDays > 1 && spentToday <= 0.0) {
            Log.d(TAG, "Checking 'No Spend Day': spentToday=$spentToday → Eligible")
            toAward += "No Spend Day"
        } else Log.d(TAG, "Skipping 'No Spend Day': spentToday=$spentToday or totalDays=$totalDays")

        Log.d(TAG, "Badges to award: $toAward")

        var anyAwarded = false
        for (title in toAward) {
            val alreadyEarned = fs.collection("users").document(userId)
                .collection("earnedBadges").document(title).get().await().exists()

            if (!alreadyEarned) {
                Log.d(TAG, "Awarding badge: $title")
                awardBadgeIfNew(userId, title)
                anyAwarded = true
            } else {
                Log.d(TAG, "Already earned: $title")
            }
        }

        if (anyAwarded) setupBadgeRecycler(userId, streak)
    }

    private suspend fun checkBudgetAndSavingsBadges(userId: String) {
        try {
            val now = Calendar.getInstance()

            // Get current month's start and end in millis
            val firstDay = now.apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val lastDay = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // Fetch transactions (expenses and incomes)
            val transactionRef = fs.collection("users").document(userId).collection("transactions")

            val expenseDocs = transactionRef
                .whereGreaterThanOrEqualTo("date", firstDay)
                .whereLessThanOrEqualTo("date", lastDay)
                .whereEqualTo("type", "expense")
                .get().await()

            val incomeDocs = transactionRef
                .whereGreaterThanOrEqualTo("date", firstDay)
                .whereLessThanOrEqualTo("date", lastDay)
                .whereEqualTo("type", "income")
                .get().await()

            val totalExpenses = expenseDocs.sumOf { it.getDouble("amount") ?: 0.0 }
            val totalIncomes = incomeDocs.sumOf { it.getDouble("amount") ?: 0.0 }

            // Fetch active budgets for the current month
            val budgetDocs = fs.collection("users").document(userId)
                .collection("budgets")
                .whereLessThanOrEqualTo("startDate", lastDay)
                .whereGreaterThanOrEqualTo("endDate", firstDay)
                .get().await()

            val totalBudget = budgetDocs.sumOf { it.getDouble("amount") ?: 0.0 }

            // Award Budget Keeper badge
            if (budgetDocs.isEmpty && totalExpenses <= totalBudget) {
                awardBadgeIfNew(userId, "Budget Keeper")
            }

            // Award Saved More badge
            if (totalIncomes > totalExpenses) {
                awardBadgeIfNew(userId, "Saved More")
            }

        } catch (e: Exception) {
            Log.e("Gamification", "Error checking budget/savings badges: ${e.message}")
        }
    }

    private suspend fun setXP(userId: String, xp: Int) {
        fs.collection("users").document(userId)
            .collection("xp").document("current")
            .set(mapOf("value" to xp))
            .await()
    }

    private fun getXPForBadge(title: String) = when (title) {
        "First Login" -> 30
        "Daily Visitor" -> 15
        "Login Streak" -> 40
        "Budget Keeper" -> 39
        "Saved More" -> 45
        "No Spend Day" -> 35
        else -> 10
    }

    private fun showAchievementPopup(title: String) {
        val dialog = AchievementDialogFragment.newInstance(
            title,
            getDescriptionForBadge(title),
            getIconForBadge(title)
        )
        dialog.show(supportFragmentManager, "achievementDialog")
    }

    private fun getDescriptionForBadge(title: String) = when (title) {
        "First Login" -> "Logged in for the first time"
        "Daily Visitor" -> "Logged in multiple days"
        "Login Streak" -> "Maintained login streak"
        "Budget Keeper" -> "Stayed within daily budget"
        "Saved More" -> "Saved more than your budget goal"
        "No Spend Day" -> "Spent nothing today"
        else -> ""
    }

    private fun getIconForBadge(title: String) = when (title) {
        "First Login" -> R.drawable.badge1
        "Daily Visitor" -> R.drawable.badge2
        "Login Streak" -> R.drawable.badge3
        "Budget Keeper" -> R.drawable.badge4
        "Saved More" -> R.drawable.badge1
        "No Spend Day" -> R.drawable.badge2
        else -> R.drawable.badge1
    }
}
package com.example.pennywise

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isEmpty
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.io.path.exists

class GamificationActivity : AppCompatActivity() {
    private val TAG = "GamificationActivity"
    private lateinit var badgeRecycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var levelText: TextView
    private lateinit var xpText: TextView
    private lateinit var badgeAdapter: BadgeAdapter
    private val fs by lazy { Firebase.firestore }
    private val userId: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    // --- Variables to store the last displayed state for animation ---
    private var lastDisplayedXP: Int = -1 // Initialize to a value that indicates it hasn't been set
    private var lastDisplayedLevel: Int = -1
    private var lastDisplayedProgress: Int = -1
    private var isInitialAnimationDone: Boolean = false
    // ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }

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

        // Set a default visual state before any data is loaded
        progressBar.progress = 0
        levelText.text = "Level 1"
        xpText.text = "0 XP"


        lifecycleScope.launch {
            val uid = userId ?: run { Log.w(TAG, "User not logged in"); return@launch }
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
            val newVisit = trackTodayVisit(uid, userEmail)
            loadUserGamificationData(uid, newVisit)
        }
    }

    private suspend fun trackTodayVisit(userId: String, userEmail: String): Boolean {
        // ... (This function remains unchanged from your original code)
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

            // Fetch initial XP
            var currentTotalXP = fs.collection("users").document(userId)
                .collection("xp").document("current")
                .get().await().getLong("value")?.toInt() ?: 0
            Log.d(TAG, "Initial XP from DB: $currentTotalXP")

            var xpGainedThisSession = 0

            if (newVisit && streak != null) {
                Log.d(TAG, "New visit detected, awarding XP based on streak: $streak")
                val bonus = streak.totalLoginDaysThisYear * 10 * (1 + streak.streak / 5)
                val streakBonus = if (streak.streak >= 5) (streak.streak - 4) * 2 else 0
                Log.d(TAG, "XP bonus for streak: $bonus + streak bonus: $streakBonus")
                xpGainedThisSession += bonus + streakBonus
            }

            if (newVisit) {
                Log.d(TAG, "First visit today – awarding Daily Visitor badge & XP")
                // awardBadgeIfNew will internally call setXP -> updateUI
                // We let it handle its own XP update and animation.
                // The XP added here is for the badge itself.
                awardBadgeIfNew(userId, "Daily Visitor") // This adds badge XP

                // After awardBadgeIfNew, currentTotalXP in DB might have changed.
                // We re-fetch to ensure our `currentTotalXP` variable is accurate
                // before potentially adding the session's streak bonus.
                currentTotalXP = fs.collection("users").document(userId)
                    .collection("xp").document("current")
                    .get().await().getLong("value")?.toInt() ?: currentTotalXP
            }

            // Add the session-specific XP (like streak bonus) to the latest fetched currentTotalXP
            // and then persist this new total.
            if (xpGainedThisSession > 0) {
                currentTotalXP += xpGainedThisSession
                Log.d(TAG, "Persisting XP after session gains (e.g. streak bonus). New total: $currentTotalXP")
                setXP(userId, currentTotalXP) // This will call updateUI for this combined update
            } else if (!isInitialAnimationDone || lastDisplayedXP == -1) {
                // If no XP was gained this session, but it's the first load,
                // still call updateUI to trigger the initial animation from 0 to currentTotalXP.
                Log.d(TAG, "No specific XP gained this session, calling updateUI for initial load or refresh.")
                updateUI(currentTotalXP)
            }
            // If xpGainedThisSession is 0 AND it's not the initial load,
            // updateUI will have been called by badge awards if any, or we don't need to call it.


            // One-time badges (these also call awardBadgeIfNew -> setXP -> updateUI)
            checkAndAwardAllLoginBadges(userId, streak)
            checkBudgetAndSavingsBadges(userId)

            // Setup RecyclerView (data should be fresh after all potential updates)
            val finalStreak = fs.collection("users").document(userId) // Re-fetch streak for adapter
                .collection("loginStreaks").document("current")
                .get().await().toObject(LoginStreak::class.java)
            val earnedDocs = fs.collection("users").document(userId)
                .collection("earnedBadges").get().await()
            val earnedBadges = earnedDocs.associateBy({ it.id }, { it })
            val allBadges = listOf(
                "First Login", "Daily Visitor", "Login Streak",
                "Budget Keeper", "Saved More", "No Spend Day"
            )
            val freshBadges = allBadges.mapIndexed { i, title ->
                val earnedDoc = earnedBadges[title]
                Badge(
                    id = i, title = title, description = getDescriptionForBadge(title),
                    iconResId = getIconForBadge(title), isEarned = earnedDoc != null,
                    overlayText = if ((earnedDoc?.getLong("metadata")?.toInt() ?: 0) >= 2) "x${earnedDoc?.getLong("metadata")?.toInt()}" else null
                )
            }
            badgeAdapter = BadgeAdapter(freshBadges, finalStreak)
            badgeRecycler.layoutManager = GridLayoutManager(this@GamificationActivity, 2)
            badgeRecycler.adapter = badgeAdapter

        } catch (e: Exception) {
            Log.e(TAG, "Error loading gamification data", e)
            // Reset to a safe state on error
            lastDisplayedXP = 0; lastDisplayedLevel = 1; lastDisplayedProgress = 0; isInitialAnimationDone = true
            xpText.text = "0 XP"; levelText.text = "Level 1"; progressBar.progress = 0
        }
    }


    // MODIFIED updateUI: This is the core of the animation logic
    private fun updateUI(targetXP: Int) {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity is finishing, skipping UI update.")
            return
        }

        val (targetLevel, targetProgress) = calculateLevelFromXP(targetXP)

        val animateFromXP: Int
        val animateFromLevel: Int
        val animateFromProgress: Int

        if (!isInitialAnimationDone || lastDisplayedXP == -1) {
            // First time animation (page open) or if last state is unknown
            animateFromXP = 0
            val (lvl, prog) = calculateLevelFromXP(animateFromXP)
            animateFromLevel = lvl
            animateFromProgress = prog
            Log.d(TAG, "Initial animation: From 0 XP to TargetXP: $targetXP")
        } else {
            // Subsequent animations: from the last displayed state
            animateFromXP = lastDisplayedXP
            animateFromLevel = lastDisplayedLevel
            animateFromProgress = lastDisplayedProgress
            Log.d(TAG, "Subsequent animation: From LastXP: $lastDisplayedXP to TargetXP: $targetXP")
        }

        // Only animate if there's an actual change or if it's the initial forced animation
        if (targetXP != animateFromXP || (!isInitialAnimationDone && targetXP != -1) ) {
            animateXPText(animateFromXP, targetXP)
            animateLevelAndProgress(animateFromLevel, animateFromProgress, targetLevel, targetProgress)
        } else {
            // If no change and not initial, just set the text (should be rare if logic is right)
            xpText.text = "$targetXP XP"
            levelText.text = "Level $targetLevel"
            progressBar.progress = targetProgress
            Log.d(TAG, "No change in XP detected for animation ($animateFromXP -> $targetXP). Setting UI directly.")
        }

        // Update the last displayed state
        lastDisplayedXP = targetXP
        lastDisplayedLevel = targetLevel
        lastDisplayedProgress = targetProgress
        if (!isInitialAnimationDone) {
            isInitialAnimationDone = true
        }
    }


    private fun animateXPText(fromXP: Int, toXP: Int) {
        val animator = ValueAnimator.ofInt(fromXP, toXP)
        animator.duration = 1000L // Adjust duration as needed
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            xpText.text = "${animation.animatedValue as Int} XP"
        }
        animator.start()
    }

    private fun animateLevelAndProgress(fromLevel: Int, fromProgress: Int, toLevel: Int, toProgress: Int) {
        val animationDuration = 1000L // Base duration

        if (toLevel > fromLevel) { // Level Up
            val progressToFull = ObjectAnimator.ofInt(progressBar, "progress", fromProgress, 100)
            progressToFull.duration = (animationDuration * ((100f - fromProgress) / 100f)).toLong().coerceAtLeast(150L)
            progressToFull.interpolator = AccelerateDecelerateInterpolator()

            progressToFull.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Animate Level Text
                    val levelAnimator = ValueAnimator.ofInt(fromLevel, toLevel)
                    levelAnimator.duration = (animationDuration / 2).coerceAtLeast(200L) // Shorter for level count
                    levelAnimator.interpolator = AccelerateDecelerateInterpolator()
                    levelAnimator.addUpdateListener { lvlAnim ->
                        levelText.text = "Level ${lvlAnim.animatedValue as Int}"
                    }
                    levelAnimator.start()

                    // Reset progress bar (visually quick) and then animate to new progress
                    progressBar.progress = 0
                    val progressToNew = ObjectAnimator.ofInt(progressBar, "progress", 0, toProgress)
                    progressToNew.duration = (animationDuration * (toProgress / 100f)).toLong().coerceAtLeast(150L)
                    progressToNew.interpolator = AccelerateDecelerateInterpolator()
                    progressToNew.start()
                }
            })
            progressToFull.start()
        } else if (toLevel == fromLevel && toProgress != fromProgress) { // Progress change, same level
            val progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", fromProgress, toProgress)
            progressAnimator.duration = animationDuration
            progressAnimator.interpolator = AccelerateDecelerateInterpolator()
            progressAnimator.start()
            levelText.text = "Level $toLevel" // Update level text directly if it hasn't changed
        } else { // No change in level, or level might have decreased (not typical for XP system) or initial setup
            levelText.text = "Level $toLevel"
            progressBar.progress = toProgress
        }
    }

    private fun calculateLevelFromXP(totalXP: Int) = Pair(totalXP / 100 + 1, totalXP % 100)

    // MODIFIED setXP: Ensures updateUI is called after DB write
    private suspend fun setXP(userId: String, xp: Int) {
        Log.d(TAG, "setXP called with UserID: $userId, XP: $xp. LastDisplayedXP was: $lastDisplayedXP")
        fs.collection("users").document(userId)
            .collection("xp").document("current")
            .set(mapOf("value" to xp))
            .await()
        // Crucially, call updateUI here to reflect the change and trigger animation
        updateUI(xp)
    }

    private suspend fun awardBadgeIfNew(userId: String, title: String) {
        val badgeRef = fs.collection("users").document(userId)
            .collection("earnedBadges").document(title)
        val doc = badgeRef.get().await()
        val alreadyEarned = doc.exists()

        if (!alreadyEarned) {
            Log.d(TAG, "Awarding new badge: $title")
            badgeRef.set(mapOf("earnedTimestamp" to System.currentTimeMillis(), "metadata" to 1)).await()
            if (!isFinishing && !isDestroyed) showAchievementPopup(title)

            // Fetch current XP from DB before adding badge XP
            val currentXPBeforeBadge = fs.collection("users").document(userId)
                .collection("xp").document("current").get().await().getLong("value")?.toInt() ?: 0
            val xpFromBadge = getXPForBadge(title)
            val newTotalXP = currentXPBeforeBadge + xpFromBadge
            Log.d(TAG, "Badge '$title': currentXPBeforeBadge=$currentXPBeforeBadge, xpFromBadge=$xpFromBadge, newTotalXP=$newTotalXP")
            setXP(userId, newTotalXP) // This will save to DB and then call updateUI
        } else {
            if (title == "Daily Visitor") { // Only increment metadata for Daily Visitor
                val currentCount = doc.getLong("metadata")?.toInt() ?: 1
                badgeRef.update("metadata", currentCount + 1).await()
                Log.d(TAG, "Incremented metadata count for $title to ${currentCount + 1}")
            } else {
                Log.d(TAG, "Badge '$title' already earned or not 'Daily Visitor', no XP awarded/metadata incremented here.")
            }
        }
    }


    // --- Other functions (setupBadgeRecycler, startOfDay, endOfDay, checkAndAward..., getXPForBadge, etc.)
    // --- can largely remain as they were in your original code, as the primary trigger for UI
    // --- animation is now centralized in setXP -> updateUI.
    // --- Ensure checkAndAwardAllLoginBadges & checkBudgetAndSavingsBadges correctly call awardBadgeIfNew.

    private fun setupBadgeRecycler(userId: String, streak: LoginStreak?) = lifecycleScope.launch {
        // ... (Your original setupBadgeRecycler implementation)
        val earned = fs.collection("users").document(userId)
            .collection("earnedBadges").get().await().documents
            .mapNotNull { it.id }

        val badges = listOf(
            "First Login", "Daily Visitor", "Login Streak",
            "Budget Keeper", "Saved More", "No Spend Day"
        ).mapIndexed { i, title ->
            val countDoc = fs.collection("users").document(userId)
                .collection("earnedBadges").document(title).get().await()
            val count = if (title == "Daily Visitor" || title == "Login Streak") // Example: if login streak also shows a count
                countDoc.getLong("metadata")?.toInt()
            else null


            Badge(
                id = i,
                title = title,
                description = getDescriptionForBadge(title),
                iconResId = getIconForBadge(title),
                isEarned = earned.contains(title),
                overlayText = count?.let { if (it >= 2) "x$it" else null } // Only show if count >=2
            )
        }

        if (!::badgeAdapter.isInitialized) {
            badgeAdapter = BadgeAdapter(badges, streak)
            badgeRecycler.layoutManager = GridLayoutManager(this@GamificationActivity, 2)
            badgeRecycler.adapter = badgeAdapter
        } else {
            badgeAdapter.updateData(badges, streak)
        }
    }

    private fun startOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun endOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    private fun checkAndAwardAllLoginBadges(userId: String, streak: LoginStreak?) = lifecycleScope.launch {
        // ... (Your original checkAndAwardAllLoginBadges, ensure it calls awardBadgeIfNew for awarding)
        val earnedBefore = fs.collection("users").document(userId)
            .collection("earnedBadges").get().await().documents.map { it.id }.toSet()

        val toAward = mutableSetOf<String>()

        if (!earnedBefore.contains("First Login")) toAward += "First Login"

        val totalDays = streak?.totalLoginDaysThisYear ?: 0
        // Award Daily Visitor only if it's a new day and not already earned (awardBadgeIfNew handles this)
        // The metadata increment for Daily Visitor is handled in awardBadgeIfNew
        if (totalDays >= 1) { // Potentially award "Daily Visitor" if conditions in awardBadgeIfNew are met
            awardBadgeIfNew(userId, "Daily Visitor") // Let awardBadgeIfNew decide if it's truly new or just metadata update
        }


        val streakCount = streak?.streak ?: 0
        if (streakCount >= 3 && !earnedBefore.contains("Login Streak")) toAward += "Login Streak"


        val spentToday = fs.collection("users").document(userId)
            .collection("transactions")
            .whereGreaterThanOrEqualTo("date", startOfDay())
            .whereLessThanOrEqualTo("date", endOfDay())
            .whereEqualTo("type", "expense")
            .get().await().documents.sumOf { it.getDouble("amount") ?: 0.0 }

        if (totalDays >= 1 && spentToday <= 0.0 && !earnedBefore.contains("No Spend Day")) toAward += "No Spend Day"


        Log.d(TAG, "Badges to award by checkAndAwardAllLoginBadges: $toAward")
        var anyAwardedInThisCheck = false
        for (badgeTitle in toAward) {
            awardBadgeIfNew(userId, badgeTitle) // awardBadgeIfNew handles the alreadyEarned check for XP
            anyAwardedInThisCheck = true
        }

        // if (anyAwardedInThisCheck) { // Refresh recycler if any new badge might have been added
        //     val updatedStreak = fs.collection("users").document(userId) // Re-fetch streak
        //         .collection("loginStreaks").document("current")
        //         .get().await().toObject(LoginStreak::class.java)
        //     setupBadgeRecycler(userId, updatedStreak)
        // }
        // Let loadUserGamificationData handle final recycler setup
    }

    private suspend fun checkBudgetAndSavingsBadges(userId: String) {
        // ... (Your original checkBudgetAndSavingsBadges, ensure it calls awardBadgeIfNew for awarding)
        try {
            val now = Calendar.getInstance()
            val firstDay = Calendar.getInstance().apply { time = now.time; set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val lastDay = Calendar.getInstance().apply { time = now.time; set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis

            val transactionRef = fs.collection("users").document(userId).collection("transactions")
            val expenseDocs = transactionRef.whereGreaterThanOrEqualTo("date", firstDay).whereLessThanOrEqualTo("date", lastDay).whereEqualTo("type", "expense").get().await()
            val incomeDocs = transactionRef.whereGreaterThanOrEqualTo("date", firstDay).whereLessThanOrEqualTo("date", lastDay).whereEqualTo("type", "income").get().await()
            val totalExpenses = expenseDocs.sumOf { it.getDouble("amount") ?: 0.0 }
            val totalIncomes = incomeDocs.sumOf { it.getDouble("amount") ?: 0.0 }

            val budgetDocs = fs.collection("users").document(userId).collection("budgets").whereLessThanOrEqualTo("startDate", lastDay).whereGreaterThanOrEqualTo("endDate", firstDay).get().await()
            val totalBudgetAmount = budgetDocs.sumOf { it.getDouble("amount") ?: 0.0 }


            val earnedBadgesSnap = fs.collection("users").document(userId).collection("earnedBadges").get().await()
            val earnedBadgeTitles = earnedBadgesSnap.documents.map { it.id }.toSet()

            if (!budgetDocs.isEmpty && totalExpenses <= totalBudgetAmount && !earnedBadgeTitles.contains("Budget Keeper")) {
                awardBadgeIfNew(userId, "Budget Keeper")
            }
            if (totalIncomes > totalExpenses && !earnedBadgeTitles.contains("Saved More")) {
                awardBadgeIfNew(userId, "Saved More")
            }
        } catch (e: Exception) {
            Log.e("GamificationActivity", "Error checking budget/savings badges: ${e.message}")
        }
    }


    private fun getXPForBadge(title: String) = when (title) {
        "First Login" -> 30; "Daily Visitor" -> 15; "Login Streak" -> 40
        "Budget Keeper" -> 39; "Saved More" -> 45; "No Spend Day" -> 35
        else -> 10
    }

    private fun showAchievementPopup(title: String) {
        val dialog = AchievementDialogFragment.newInstance(title, getDescriptionForBadge(title), getIconForBadge(title))
        dialog.show(supportFragmentManager, "achievementDialog")
    }

    private fun getDescriptionForBadge(title: String) = when (title) {
        "First Login" -> "Logged in for the first time"; "Daily Visitor" -> "Logged in on different days" // Clarified
        "Login Streak" -> "Maintained a login streak"; "Budget Keeper" -> "Stayed within your budget for a period"
        "Saved More" -> "Saved more than you spent in a period"; "No Spend Day" -> "Completed a day without spending"
        else -> "Well done!"
    }

    private fun getIconForBadge(title: String) = when (title) {
        "First Login" -> R.drawable.badge1; "Daily Visitor" -> R.drawable.badge2
        "Login Streak" -> R.drawable.badge3; "Budget Keeper" -> R.drawable.badge4
        "Saved More" -> R.drawable.badge1; "No Spend Day" -> R.drawable.badge2
        else -> R.drawable.badge1
    }
}
package com.example.pennywise

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout // Added for bulkActionLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog // Added for delete confirmation
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton // Added for deleteSelectedButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import android.view.ViewAnimationUtils
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.tasks.await // Added for cleaner async operations
import kotlin.math.hypot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {
    //decleartion
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var calendarText: TextView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var loggedInUserId: String

    private var currentCalendar = Calendar.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var isAnimatingThemeChange = false
    private var currentGroupedTransactions: MutableList<TransactionItem> = mutableListOf()

    // --- Views for Bulk Actions ---
    private lateinit var bulkActionLayout: LinearLayout
    private lateinit var cancelSelectionButton: ImageView
    private lateinit var selectedCountText: TextView
    private lateinit var deleteSelectedButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        Log.d("MainActivity", "onCreate called")
        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }
        db.firestoreSettings = settings

        // Handle incoming reveal intent
        handleRevealIntent()
        setContentView(R.layout.activity_main)

        // --- Initialize Bulk Action Views ---
        bulkActionLayout = findViewById(R.id.bulkActionLayout)
        cancelSelectionButton = findViewById(R.id.cancelSelectionButton)
        selectedCountText = findViewById(R.id.selectedCountText)
        deleteSelectedButton = findViewById(R.id.deleteSelectedButton)

        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        // Access header layout inside NavigationView
        val headerView = navigationView.getHeaderView(0)
        val navHeaderEmail = headerView.findViewById<TextView>(R.id.navHeaderEmail)
        val navHeaderTitle = headerView.findViewById<TextView>(R.id.navHeaderTitle)
        val profileImage = headerView.findViewById<ImageView>(R.id.profileImage)

        // Hide the default action bar for full-screen experience
        supportActionBar?.hide()

        //Set today
        val todayDateTextView = findViewById<TextView>(R.id.todayDate)
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        todayDateTextView.text = currentDate
        Log.d("MainActivity", "Today's date: $currentDate")

        val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        loggedInUserId = sharedPref.getString("loggedInUserId", "") ?: ""
        Log.d("MainActivity", "Loaded loggedInUserId: $loggedInUserId")

        //set layout settings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            // No Firebase user—send them back to login:
            startActivity(Intent(this, ActivityLoginResgister::class.java))
            finish()
            return
        }
        // Ensure loggedInUserId is from Firebase Auth as the primary source
        if (loggedInUserId.isEmpty() || loggedInUserId != user.uid) {
            loggedInUserId = user.uid
            // Optionally update SharedPreferences if it was out of sync
            sharedPref.edit().putString("loggedInUserId", loggedInUserId).apply()
            Log.d("MainActivity", "Updated loggedInUserId from FirebaseAuth: $loggedInUserId")
        }

        //adaptors
        BottomNavManager.setupBottomNav(this, R.id.nav_transaction)
        drawerLayout = findViewById(R.id.drawerLayout)
        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)

        transactionAdapter = TransactionAdapter(
            context = this,
            loggedInUserId = loggedInUserId,
            items = mutableListOf(), // Start with an empty list
            onSelectionModeChange = { isInSelectionMode ->
                toggleBulkActionUI(isInSelectionMode)
            },
            onItemSelectionChanged = { count ->
                updateSelectedCount(count)
            }
        )
        transactionRecyclerView.adapter = transactionAdapter
        Log.d("MainActivity", "TransactionAdapter initialized with callbacks.")


        val userEmail = sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        // Set the values dynamically
        navHeaderEmail.text = userEmail
        navHeaderTitle.text = "Welcome back!"
        Log.d("MainActivity", "User email set in nav drawer: $userEmail")

        //set users initials
        val initials = userEmail.take(2).uppercase(Locale.getDefault())
        val profileInitials = findViewById<TextView>(R.id.profileInitials)
        profileInitials.text = initials
        Log.d("MainActivity", "Initials set: $initials")

        findViewById<ImageView>(R.id.ic_menu).setOnClickListener {
            if (transactionAdapter.isSelectionModeActive) { // If in selection mode, menu should cancel it
                transactionAdapter.clearSelectionsAndExitMode()
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            Log.d("MainActivity", "Menu icon clicked")
        }

        //setup navigations

        navigationView.setNavigationItemSelectedListener { item ->
            // If in selection mode, any navigation should cancel it
            if (transactionAdapter.isSelectionModeActive) {
                transactionAdapter.clearSelectionsAndExitMode()
            }
            drawerLayout.closeDrawers()
            when (item.itemId) {
                R.id.nav_about -> {
                    showAppVersion();
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.nav_gamification -> {
                    gameAchieve();
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.nav_feedback -> {
                    openSupport();
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.nav_profile -> {
                    openProfile();
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.nav_theme -> {
                    val menuIcon = findViewById<View>(R.id.ic_menu)
                    val x = menuIcon.left + menuIcon.width
                    val y = menuIcon.top + menuIcon.height / 2
                    TransitionUtil.animateThemeChangeWithReveal(this, x, y)
                    true
                }
                else -> false
            }
        }

        //profile menu functionality
        profileInitials.setOnClickListener {
            if (transactionAdapter.isSelectionModeActive) { // If in selection mode, profile click should cancel it
                transactionAdapter.clearSelectionsAndExitMode()
                return@setOnClickListener // Don't show popup menu
            }
            Log.d("MainActivity", "Profile initials clicked")
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sign_out -> {
                        Log.d("MainActivity", "Signing out")
                        getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
                            .edit()
                            .remove("loggedInUserEmail")
                            .remove("loggedInUserId")
                            .apply()
                        FirebaseAuth.getInstance().signOut() // Sign out from Firebase
                        val intent = Intent(this, ActivityLoginResgister::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        setupCalendarText()
        //transactionRecyclerView.layoutManager = LinearLayoutManager(this)
        // --- Setup Listeners for Bulk Action Buttons ---
        cancelSelectionButton.setOnClickListener {
            transactionAdapter.clearSelectionsAndExitMode()
        }

        deleteSelectedButton.setOnClickListener {
            val selectedIds = transactionAdapter.getSelectedTransactionIds()
            if (selectedIds.isNotEmpty()) {
                showDeleteConfirmationDialog(selectedIds)
            }
        }
    }

    //reload logic
    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume: Reloading transactions")
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            // Should be handled by onCreate, but as a safeguard
            startActivity(Intent(this, ActivityLoginResgister::class.java))
            finish()
            return
        }
        // Ensure loggedInUserId is up-to-date, especially if app was backgrounded for a long time
        if (loggedInUserId != firebaseUser.uid) {
            loggedInUserId = firebaseUser.uid
            // Re-initialize adapter if loggedInUserId changed fundamentally,
            // or ensure adapter's internal userId is updated if it uses it directly.
            // For now, loadTransactions will use the updated loggedInUserId.
            Log.w("MainActivity", "loggedInUserId mismatch in onResume, updated to: $loggedInUserId")
        }

        loadTransactions() // This will now use the initialized adapter
    }

    // --- Back Press Handling for Selection Mode ---
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (transactionAdapter.isSelectionModeActive) {
            transactionAdapter.clearSelectionsAndExitMode()
        } else {
            super.onBackPressed()
        }
    }

    private fun handleRevealIntent() {
        val revealX = intent.getIntExtra("reveal_x", -1)
        val revealY = intent.getIntExtra("reveal_y", -1)

        if (revealX != -1 && revealY != -1) {
            val decor = window.decorView
            decor.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int,
                                            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                    v.removeOnLayoutChangeListener(this)

                    val finalRadius = hypot(decor.width.toDouble(), decor.height.toDouble()).toFloat()
                    val anim = ViewAnimationUtils.createCircularReveal(decor, revealX, revealY, 0f, finalRadius)
                    anim.duration = 400
                    decor.visibility = View.VISIBLE
                    anim.start()
                }
            })
            window.decorView.visibility = View.INVISIBLE
        }
    }

    //setup the calender
    private fun setupCalendarText() {
        calendarText = findViewById(R.id.calendarText)
        val calendarPrev = findViewById<ImageView>(R.id.calendarPrev)
        val calendarNext = findViewById<ImageView>(R.id.calendarNext)

        updateCalendarText()

        calendarPrev.setOnClickListener {
            if (transactionAdapter.isSelectionModeActive) transactionAdapter.clearSelectionsAndExitMode()
            Log.d("MainActivity", "Calendar previous clicked")
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendarText()
            loadTransactions()
        }

        calendarNext.setOnClickListener {
            if (transactionAdapter.isSelectionModeActive) transactionAdapter.clearSelectionsAndExitMode()
            Log.d("MainActivity", "Calendar next clicked")
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendarText()
            loadTransactions()
        }

        calendarText.setOnClickListener {
            if (transactionAdapter.isSelectionModeActive) transactionAdapter.clearSelectionsAndExitMode()
            Log.d("MainActivity", "Calendar text clicked")
            openDatePicker()
        }
    }

    //update calender
    private fun updateCalendarText() {
        val dateFormat = SimpleDateFormat("yyyy MMM", Locale.getDefault())
        val text = dateFormat.format(currentCalendar.time)
        calendarText.text = text
        Log.d("MainActivity", "Updated calendarText: $text")
    }

    //date logic
    private fun openDatePicker() {
        Log.d("MainActivity", "Opening Date Picker")
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)
        val day = currentCalendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, _ ->
            currentCalendar.set(Calendar.YEAR, selectedYear)
            currentCalendar.set(Calendar.MONTH, selectedMonth)
            updateCalendarText()
            loadTransactions()
        }, year, month, day)

        datePicker.show()
    }

    //loading transaction
    private fun loadTransactions() {
        //val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)

        if (loggedInUserId.isEmpty()) {
            Log.e("MainActivity", "loggedInUserId is empty in loadTransactions. Cannot load.")
            // Potentially redirect to login or show an error
            // For now, let's prevent a crash by not proceeding.
            if (FirebaseAuth.getInstance().currentUser == null) {
                startActivity(Intent(this, ActivityLoginResgister::class.java))
                finish()
            }
            return
        }

        lifecycleScope.launch {
            val selectedMonth = String.format("%02d", currentCalendar.get(Calendar.MONTH) + 1)
            val selectedYear = currentCalendar.get(Calendar.YEAR).toString()

            Log.d("MainActivity", "Loading transactions from subcollection for $loggedInUserId | $selectedMonth-$selectedYear")

            try {
                val snap = firestore.collection("users")
                    .document(loggedInUserId)
                    .collection("transactions")
                    // If you store monthYear like "YYYY-MM" on transactions, you can query directly:
                    // .whereEqualTo("monthYear", "$selectedYear-$selectedMonth")
                    .get()
                    .await() // Using await for cleaner async code

                Log.d("DEBUG_ALL", "Total docs in users/$loggedInUserId/transactions (before client filter): ${snap.size()}")

                val allTransactions = snap.documents.mapNotNull { doc ->
                    val transaction = doc.toObject(Transaction::class.java)
                    transaction?.id = doc.id
                    transaction
                }

                Log.d("DEBUG_QUERY", "Mapped to Transaction objects: ${allTransactions.size}")

                val filteredTransactions = allTransactions.filter {
                    val date = Date(it.date)
                    val cal = Calendar.getInstance().apply { time = date }
                    val month = cal.get(Calendar.MONTH) + 1
                    val year = cal.get(Calendar.YEAR)
                    month == selectedMonth.toInt() && year == selectedYear.toInt()
                }.sortedByDescending { it.date }

                Log.d("TransactionLoad", "Filtered transactions for $selectedMonth-$selectedYear: ${filteredTransactions.size}")

                // Clear previous and add new. Grouping by month might not be needed if already filtered by month.
                currentGroupedTransactions.clear()
                currentGroupedTransactions.addAll(filteredTransactions.map { TransactionItem.Entry(it) })


                val totalIncome = filteredTransactions.filter { it.type.equals("income", true) }.sumOf { it.amount }
                val totalExpense = filteredTransactions.filter { it.type.equals("expense", true) }.sumOf { it.amount }
                val totalBalance = totalIncome - totalExpense

                animateCount(findViewById(R.id.incomeAmount), 0.0, abs(totalIncome))
                animateCount(findViewById(R.id.expenseAmount), 0.0, abs(totalExpense))
                animateCount(findViewById(R.id.balanceAmount), 0.0, totalBalance)

                // Adapter is already initialized, just update its data
                transactionAdapter.updateData(currentGroupedTransactions.toList()) // Pass a copy
                Log.d("TransactionLoad", "TransactionAdapter data updated with ${currentGroupedTransactions.size} items.")

                // RecyclerView should be available as a member or from findViewById
                val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)
                transactionRecyclerView.post {
                    transactionRecyclerView.scrollToPosition(0)
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to load transactions", e)
                // Optionally show a user-friendly error message
            }
        }
    }

    // --- Callbacks for Adapter and UI Update ---
    private fun toggleBulkActionUI(isInSelectionMode: Boolean) {
        bulkActionLayout.visibility = if (isInSelectionMode) View.VISIBLE else View.GONE
        if (!isInSelectionMode) {
            updateSelectedCount(0) // Reset count text when exiting selection mode
        }
    }

    private fun updateSelectedCount(count: Int) {
        if (count > 0) {
            selectedCountText.text = "$count selected"
            deleteSelectedButton.isEnabled = true
        } else {
            selectedCountText.text = "" // Or "0 selected"
            deleteSelectedButton.isEnabled = false
        }
    }

    // --- Delete Logic ---
    private fun showDeleteConfirmationDialog(transactionIds: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transactions")
            .setMessage("Are you sure you want to delete ${transactionIds.size} selected transaction(s)? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteTransactions(transactionIds)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteTransactions(transactionIds: List<String>) {
        if (loggedInUserId.isEmpty()) {
            Log.e("MainActivity", "Cannot delete transactions, loggedInUserId is empty.")
            // Show error to user
            return
        }
        if (transactionIds.isEmpty()) {
            Log.d("MainActivity", "No transactions selected for deletion.")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) { // Perform Firestore operations on IO dispatcher
            try {
                val batch = firestore.batch()
                transactionIds.forEach { id ->
                    val docRef = firestore.collection("users").document(loggedInUserId)
                        .collection("transactions").document(id)
                    batch.delete(docRef)
                }
                batch.commit().await() // Wait for batch commit to complete

                withContext(Dispatchers.Main) {
                    Log.d("MainActivity", "${transactionIds.size} transactions successfully deleted.")
                    // UI updates must be on the Main thread
                    transactionAdapter.clearSelectionsAndExitMode() // This will also update UI via callbacks
                    loadTransactions() // Refresh the list from Firestore
                    // Optionally show a success message (e.g., Toast)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Error deleting transactions", e)
                    // Optionally show an error message to the user
                    // It's good practice to allow the user to try again or inform them of the failure.
                    transactionAdapter.clearSelectionsAndExitMode() // Still exit selection mode
                }
            }
        }
    }

    private fun showAppVersion() {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun gameAchieve() {
        val intent = Intent(this, GamificationActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openSupport() {
        val intent = Intent(this, FeedbackActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openProfile() {
        Log.d("MainActivity", "Navigating to ProfileActivity")
        val sharedPref = getSharedPreferences("PennyWisePrefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("loggedInUserEmail", "user@example.com") ?: "user@example.com"
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("user_email", email)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    // Theme reveal animation
    private fun animateThemeChangeWithReveal(x: Int, y: Int) {
        if (isAnimatingThemeChange) return
        isAnimatingThemeChange = true
        val decor = window.decorView
        val finalRadius = hypot(decor.width.toDouble(), decor.height.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(decor, x, y, finalRadius, 0f).apply {
            duration = 400
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                ThemeUtils.toggleTheme(this@MainActivity)
                recreate()
                overridePendingTransition(0, 0)
                isAnimatingThemeChange = false
            }
        })
        anim.start()
    }

    private fun animateCount(view: TextView, from: Double, to: Double) {
        val duration = 1000L
        val animator = ValueAnimator.ofFloat(from.toFloat(), to.toFloat())
        animator.duration = duration
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            view.text = "R%.2f".format(value)
        }
        animator.start()
    }
}
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
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import kotlin.math.hypot

class MainActivity : BaseActivity() {
    //decleartion
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var calendarText: TextView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var loggedInUserId: String

    private var currentCalendar = Calendar.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var isAnimatingThemeChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        Log.d("MainActivity", "onCreate called")
        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true // <-- This is the key part!
        }
        db.firestoreSettings = settings

        // Handle incoming reveal intent
        handleRevealIntent()

        setContentView(R.layout.activity_main)

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
        loggedInUserId = user.uid
        Log.d("MainActivity", "Using FirebaseAuth UID: $loggedInUserId")

        //adaptors
        BottomNavManager.setupBottomNav(this, R.id.nav_transaction)
        drawerLayout = findViewById(R.id.drawerLayout)
        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)

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
            Log.d("MainActivity", "Menu icon clicked")
            drawerLayout.openDrawer(GravityCompat.START)
        }

        //setup navigations
        navigationView.setNavigationItemSelectedListener { item ->
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
                    // choose your reveal origin; e.g. center of the menu icon
                    val menuIcon = findViewById<View>(R.id.ic_menu)
                    val x = menuIcon.left + menuIcon.width  // right edge
                    val y = menuIcon.top + menuIcon.height / 2
                    TransitionUtil.animateThemeChangeWithReveal(this, x, y)
                    true
                }
                else -> false
            }
        }

        //profile menu functionality
        profileInitials.setOnClickListener {
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
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    //reload logic
    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume: Reloading transactions")
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        Log.d("DEBUG_AUTH", "firebaseUser = $firebaseUser, uid=${firebaseUser?.uid}")
        loadTransactions()
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
            Log.d("MainActivity", "Calendar previous clicked")
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendarText()
            loadTransactions()
        }

        calendarNext.setOnClickListener {
            Log.d("MainActivity", "Calendar next clicked")
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendarText()
            loadTransactions()
        }

        calendarText.setOnClickListener {
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
        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionList)

        lifecycleScope.launch {
            val selectedMonth = String.format("%02d", currentCalendar.get(Calendar.MONTH) + 1)
            val selectedYear = currentCalendar.get(Calendar.YEAR).toString()

            Log.d("MainActivity", "Loading transactions from subcollection for $loggedInUserId | $selectedMonth-$selectedYear")

            // Read from user's transactions sub-collection
            firestore.collection("users")
                .document(loggedInUserId)
                .collection("transactions")
                .get()
                .addOnSuccessListener { snap ->
                    Log.d("DEBUG_ALL", "Total docs in users/$loggedInUserId/transactions: ${snap.size()}")
                    snap.documents.forEach { Log.d("DEBUG_ALL", "${it.id} → ${it.data}") }

                    val allTransactions = snap.documents.mapNotNull { doc ->
                        val transaction = doc.toObject(Transaction::class.java)
                        transaction?.id = doc.id  // Set the Firestore document ID
                        transaction
                    }

                    Log.d("DEBUG_QUERY", "Mapped to Transaction objects: ${allTransactions.size}")

                    allTransactions.forEach {
                        val date = Date(it.date)
                        val cal = Calendar.getInstance().apply { time = date }
                        Log.d("FilterDebug", "Transaction: ${it.id} on ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)} → Month: ${cal.get(Calendar.MONTH) + 1}, Year: ${cal.get(Calendar.YEAR)}")
                    }

                    // Client-side filter by selected month and year
                    val filteredTransactions = allTransactions.filter {
                        val date = Date(it.date)
                        val cal = Calendar.getInstance().apply { time = date }
                        val month = cal.get(Calendar.MONTH) + 1
                        val year = cal.get(Calendar.YEAR)
                        month == selectedMonth.toInt() && year == selectedYear.toInt()
                    }.sortedByDescending { it.date } // <- Sort by date DESCENDING

                    Log.d("TransactionLoad", "Filtered transactions: ${filteredTransactions.size}")

                    val groupedItems = mutableListOf<TransactionItem>()
                    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

                    filteredTransactions.groupBy {
                        val date = Date(it.date)
                        monthFormatter.format(date)
                    }.forEach { (_, group) ->
                        groupedItems.addAll(group.map { TransactionItem.Entry(it) })
                    }

                    val totalIncome  = filteredTransactions.filter { it.type.equals("income", true) }.sumOf { it.amount }
                    val totalExpense = filteredTransactions.filter { it.type.equals("expense", true) }.sumOf { it.amount }
                    val totalBalance = totalIncome - totalExpense

                    animateCount(findViewById(R.id.incomeAmount),  0.0, abs(totalIncome))
                    animateCount(findViewById(R.id.expenseAmount), 0.0, abs(totalExpense))
                    animateCount(findViewById(R.id.balanceAmount),  0.0, totalBalance)

                    if (!::transactionAdapter.isInitialized) {
                        transactionAdapter = TransactionAdapter(groupedItems, loggedInUserId)
                        transactionRecyclerView.adapter = transactionAdapter
                        Log.d("TransactionLoad", "TransactionAdapter initialized")
                    } else {
                        transactionAdapter.updateData(groupedItems)
                        Log.d("TransactionLoad", "TransactionAdapter data updated")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Failed to load transactions from subcollection", e)
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
            duration = 300
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
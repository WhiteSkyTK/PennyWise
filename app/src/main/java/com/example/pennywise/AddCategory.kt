package com.example.pennywise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlin.math.hypot

class AddCategory : BaseActivity() {
    private var lastVisibleDocument: DocumentSnapshot? = null
    private val pageSize = 15 // Items per page
    private var isLastPage = false
    private var isLoading = false
    private var allCategories: List<Category> = emptyList()
    private var loadedCategories: MutableList<Category> = mutableListOf()

    companion object {
        const val REQUEST_CODE_ADD_CATEGORY = 1
        const val REQUEST_CODE_EDIT_CATEGORY = 2
        var shouldRefreshOnResume: Boolean = false
    }

    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var userEmail: String
    private var selectedMonth: String = getCurrentMonth()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

        val db = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }
        db.firestoreSettings = settings

        // Reveal animation if triggered with reveal_x & reveal_y
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
                    decor.visibility = View.VISIBLE
                    anim.duration = 350
                    anim.start()
                }
            })
            window.decorView.visibility = View.INVISIBLE
        }

        setContentView(R.layout.activity_category)
        ThemeUtils.applyTheme(this)

        supportActionBar?.hide()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        val headerManager = HeaderManager(this, drawerLayout, navigationView)
        headerManager.setupDrawerNavigation(navigationView) {
            val view = findViewById<View>(R.id.nav_theme) ?: window.decorView
            val x = (view.x + view.width / 2).toInt()
            val y = (view.y + view.height / 2).toInt()
            TransitionUtil.animateThemeChangeWithReveal(this, x, y)
        }
        headerManager.setupHeader("Report")

        userEmail = auth.currentUser?.email ?: "user@example.com"
        val initials = userEmail.take(2).uppercase(Locale.getDefault())
        val profileInitials = findViewById<TextView>(R.id.profileInitials)
        profileInitials.text = initials

        profileInitials.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sign_out -> {
                        auth.signOut()
                        startActivity(Intent(this, ActivityLoginResgister::class.java))
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        findViewById<TextView>(R.id.addCategoryText).setOnClickListener {
            startActivity(Intent(this, Activityaddcategory::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        findViewById<TextView>(R.id.deleteAllCategoryText).setOnClickListener {
            if (isDeleteUnlocked()) {
                performDeleteAll()
            } else {
                showWatchAdDialog()
            }
        }

        HeaderManager(this, drawerLayout, navigationView) { updatedMonthString ->
            val parts = updatedMonthString.split(" ")
            if (parts.size == 2) {
                selectedMonth = "${parts[0]}-${convertMonthNameToNumber(parts[1])}"
                loadCategories()
            }
        }.setupHeader("Category")

        BottomNavManager.setupBottomNav(this, R.id.nav_category)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoryLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this)

        categoryAdapter = CategoryAdapter(
            emptyList(),
            emptyMap(),
            onEdit = { category -> editCategory(category) },
            onDelete = { category -> deleteCategory(category) }
        )

        categoryRecyclerView.adapter = categoryAdapter
        categoryRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        loadCategories(loadMore = true)
                    }
                }
            }
        })

        loadCategories()
        MobileAds.initialize(this) {}
        loadRewardedAd()
    }

    private fun editCategory(category: Category) {
        val intent = Intent(this, Activityaddcategory::class.java)
        intent.putExtra("category_id", category.id ?: "")
        startActivityForResult(intent, REQUEST_CODE_EDIT_CATEGORY)
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                firestore.collection("users")
                    .document(uid)
                    .collection("categories")
                    .document(category.id)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("DELETE", "Category deleted successfully.")
                        loadCategories()
                    }
                    .addOnFailureListener { e ->
                        Log.e("DELETE", "Failed to delete category", e)
                    }
            } else {
                Log.e("DELETE", "User UID is null. Not signed in?")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD_CATEGORY || requestCode == REQUEST_CODE_EDIT_CATEGORY) {
            if (resultCode == RESULT_OK) {
                loadCategories()
            }
        }
    }

    private fun getCurrentMonth(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    private fun convertMonthNameToNumber(monthName: String): String {
        val month = java.text.SimpleDateFormat("MMM", Locale.ENGLISH).parse(monthName)
        val cal = Calendar.getInstance()
        cal.time = month!!
        return String.format("%02d", cal.get(Calendar.MONTH) + 1)
    }

    private fun loadCategories(loadMore: Boolean = false) {
        if (isLoading || isLastPage) return
        isLoading = true

        lifecycleScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch

                if (!loadMore) {
                    // Fetch once when not loading more
                    val (categories, transactions) = withContext(Dispatchers.IO) {
                        val categoryDocs = firestore.collection("users")
                            .document(uid)
                            .collection("categories")
                            .orderBy("name")
                            .get()
                            .await()

                        val categories = categoryDocs.documents.mapNotNull {
                            it.toObject(Category::class.java)?.apply { id = it.id }
                        }

                        val txDocs = firestore.collection("users")
                            .document(uid)
                            .collection("transactions")
                            .whereEqualTo("monthYear", selectedMonth)
                            .get()
                            .await()

                        Pair(categories, txDocs)
                    }

                    allCategories = categories
                    loadedCategories.clear()

                    // Compute usage totals
                    val usageResults = mutableMapOf<String, Double>()
                    transactions.documents.forEach { doc ->
                        val catId = doc.getString("categoryId")
                        val amount = doc.getDouble("amount") ?: 0.0
                        if (!catId.isNullOrBlank()) {
                            usageResults[catId] = usageResults.getOrDefault(catId, 0.0) + amount
                        }
                    }

                    // Save totals once
                    withContext(Dispatchers.IO) {
                        val batch = firestore.batch()
                        val totalsCollection = firestore.collection("users")
                            .document(uid)
                            .collection("monthlyCategoryTotals")

                        allCategories.forEach { category ->
                            if (category.id.isNullOrEmpty()) return@forEach
                            val total = usageResults[category.id] ?: 0.0
                            val categoryTotal = CategoryTotal(category = category.name, total = total)
                            val docId = "${selectedMonth}_${category.id}"
                            val docRef = totalsCollection.document(docId)
                            batch.set(docRef, categoryTotal)
                        }

                        batch.commit().await()
                    }

                    categoryAdapter.updateTotals(usageResults)
                }

                // Pagination logic
                val nextPage = allCategories.drop(loadedCategories.size).take(pageSize)
                loadedCategories.addAll(nextPage)
                categoryAdapter.updateData(loadedCategories)

                if (!loadMore) {
                    categoryRecyclerView.post {
                        categoryRecyclerView.scrollToPosition(0)
                    }
                }

                isLastPage = loadedCategories.size >= allCategories.size
            } catch (e: Exception) {
                Log.e("CAT_DEBUG", "Error loading categories", e)
            } finally {
                isLoading = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefreshOnResume) {
            allCategories = emptyList()
            loadedCategories.clear()
            isLastPage = false
            loadCategories()
            shouldRefreshOnResume = false
        }
    }

    private fun performDeleteAll() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            lifecycleScope.launch {
                try {
                    val snapshot = firestore.collection("users")
                        .document(uid)
                        .collection("categories")
                        .get()
                        .await()

                    val batch = firestore.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()

                    loadedCategories.clear()
                    allCategories = emptyList()
                    categoryAdapter.updateData(emptyList())
                    categoryAdapter.updateTotals(emptyMap())

                    showToast("All categories deleted!")
                } catch (e: Exception) {
                    Log.e("DELETE_ALL", "Failed to delete all categories", e)
                    showToast("Failed to delete categories.")
                }
            }
        } else {
            showToast("User not signed in.")
        }
    }

    private fun showWatchAdDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unlock Feature")
            .setMessage("Watch a short ad to unlock 'Delete All Categories' for 24 hours.")
            .setPositiveButton("Watch Ad") { _, _ ->
                rewardedAd?.show(this) {
                    markDeleteUnlockedForOneDay()
                    showToast("Feature unlocked for 24 hours!")
                    performDeleteAll()
                    loadRewardedAd()
                } ?: showToast("Ad not ready. Please try again later.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this,
            "ca-app-pub-5040172786842435~9648134546",
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("AdMob", "Failed to load ad: $adError")
                    rewardedAd = null
                }
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isDeleteUnlocked(): Boolean {
        val prefs = getSharedPreferences("PremiumPrefs", MODE_PRIVATE)
        val expiry = prefs.getLong("delete_unlock_expiry", 0L)
        return System.currentTimeMillis() < expiry
    }

    private fun markDeleteUnlockedForOneDay() {
        val expiry = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24 hours
        getSharedPreferences("PremiumPrefs", MODE_PRIVATE).edit()
            .putLong("delete_unlock_expiry", expiry)
            .apply()
    }

}
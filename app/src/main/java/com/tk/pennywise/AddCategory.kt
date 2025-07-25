package com.tk.pennywise


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.isEmpty
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.hypot
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton

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
    private lateinit var lottieLoadingView: LottieAnimationView // <-- Declare Lottie view
    private lateinit var addCategoryButton: MaterialButton
    private lateinit var deleteAllCategoryButton: MaterialButton

    private lateinit var userEmail: String
    private var selectedMonth: String = getCurrentMonth()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeUtils.applyTheme(this)

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

        // Initialize LottieAnimationView and other views
        lottieLoadingView = findViewById(R.id.lottieLoadingViewCategories)
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        addCategoryButton = findViewById(R.id.addCategoryButton)
        deleteAllCategoryButton = findViewById(R.id.deleteAllCategoriesButton)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        val headerManager = HeaderManager(this, drawerLayout, navigationView) { updatedMonthString ->
            // This is the onMonthChanged callback
            val parts = updatedMonthString.split(" ")
            if (parts.size == 2) {
                selectedMonth = "${parts[0]}-${convertMonthNameToNumber(parts[1])}"
                Log.d("AddCategory", "Month changed in headerManager. New selectedMonth: $selectedMonth. Reloading categories.")
                Log.i("AddCategory_MonthChange", "Month changed via HeaderManager. New selectedMonth: $selectedMonth. Reloading categories.")
                loadCategories() // Reload categories based on the new month
            }
        }

        // Setup drawer navigation using the single instance
        headerManager.setupDrawerNavigation(navigationView) {
            val view = findViewById<View>(R.id.nav_theme) ?: window.decorView
            val x = (view.x + view.width / 2).toInt()
            val y = (view.y + view.height / 2).toInt()
            TransitionUtil.animateThemeChangeWithReveal(this, x, y)
        }
        headerManager.setupHeader("Categories")

        userEmail = auth.currentUser?.email ?: "user@example.com"

        addCategoryButton.setOnClickListener {
            startActivity(Intent(this, Activityaddcategory::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        deleteAllCategoryButton.setOnClickListener {
            if (isDeleteUnlocked()) {
                showDeleteConfirmationDialog()
            } else {
                showWatchAdDialog()
            }
        }

        BottomNavManager.setupBottomNav(this, R.id.nav_category) { fabView ->
            Log.d("AddCategory_FAB", "FAB clicked, preparing to launch AddEntry for result.")
            val intent = Intent(this@AddCategory, Activityaddentry::class.java)
            intent.putExtra("default_month_year", selectedMonth)
            addEntryLauncher.launch(intent) // Now 'addEntryLauncher' is defined
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoryLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

    private fun showLoadingAnimation(show: Boolean) {
        if (show) {
            categoryRecyclerView.visibility = View.GONE
            // Optionally disable action buttons during full load
            addCategoryButton.isEnabled = false
            deleteAllCategoryButton.isEnabled = false
            lottieLoadingView.visibility = View.VISIBLE
            lottieLoadingView.playAnimation()
        } else {
            categoryRecyclerView.visibility = View.VISIBLE
            addCategoryButton.isEnabled = true
            deleteAllCategoryButton.isEnabled = true
            lottieLoadingView.visibility = View.GONE
            lottieLoadingView.cancelAnimation()
        }
    }

    private fun editCategory(category: Category) {
        val intent = Intent(this, Activityaddcategory::class.java)
        intent.putExtra("category_id", category.id ?: "")
        // Using startActivityForResult is deprecated, prefer ActivityResultLauncher
        // For simplicity, if you still use it, ensure it triggers onResume logic or a direct reload.
        // The current onActivityResult and shouldRefreshOnResume handles this.
        editCategoryLauncher.launch(intent) // Use a launcher for edit
    }
    private val editCategoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("AddCategory", "Returned from Edit Category. Reloading categories.")
            allCategories = emptyList()
            loadedCategories.clear()
            isLastPage = false
            loadCategories() // Full reload
        }
    }

    private fun deleteCategory(category: Category) {
        // Consider showing a small loading indicator or disabling the item during delete
        lifecycleScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    firestore.collection("users")
                        .document(uid)
                        .collection("categories")
                        .document(category.id)
                        .delete()
                        .await() // Use await for coroutine-friendly operation
                    Log.d("DELETE", "Category deleted successfully.")
                    // More efficient way to remove from lists and update adapter
                    allCategories = allCategories.filterNot { it.id == category.id }
                    loadedCategories.removeAll { it.id == category.id }
                    if (loadedCategories.isEmpty() && !allCategories.isEmpty() && !isLastPage) {
                        // If current page became empty due to deletion and there's more data, load next
                        loadCategories(loadMore = true)
                    } else {
                        categoryAdapter.updateData(loadedCategories) // Update adapter with modified list
                        // Also, re-check if it's the last page if an item was deleted
                        isLastPage = loadedCategories.size >= allCategories.size
                    }
                    // No need for full loadCategories() here, just update totals for the deleted one if necessary
                    // Or, if totals are complex, a full loadCategories() might be simpler as you have it.
                    // For now, let's keep your existing full reload on delete for simplicity of totals:
                    // allCategories = emptyList()
                    // loadedCategories.clear()
                    // isLastPage = false
                    // lastVisibleDocument = null
                    // loadCategories() // This will also re-calculate totals
                    // Simpler: Just refresh after delete
                    allCategories = emptyList(); loadedCategories.clear(); isLastPage = false; loadCategories()


                } catch (e: Exception) {
                    Log.e("DELETE", "Failed to delete category", e)
                    Toast.makeText(this@AddCategory, "Failed to delete category.", Toast.LENGTH_SHORT).show()
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
        if (!loadMore) {
            showLoadingAnimation(true)
        }
        if (isLoading || (isLastPage && loadMore)) { // Allow full refresh even if isLastPage is true but loadMore is false
            Log.d("AddCategory_LoadSkip", "Skipping loadCategories. isLoading: $isLoading, isLastPage: $isLastPage, loadMore: $loadMore")
            return
        }
        isLoading = true
        // <<< LOG 2: loadCategories called, indicating if it's a full refresh or loadMore >>>
        Log.i("AddCategory_LoadStart", "loadCategories called. loadMore: $loadMore, current selectedMonth: $selectedMonth")

        lifecycleScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: run {
                    Log.e("AddCategory_Load", "User UID is null. Cannot load categories.")
                    isLoading = false
                    return@launch
                }
                if (!loadMore) {
                    Log.d("AddCategory_LoadBranch", "Executing FULL REFRESH branch (!loadMore)")
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

                        // <<< LOG 3: Which month are transactions being fetched for? >>>
                        Log.d("AddCategory_Firestore", "Fetching transactions for month: $selectedMonth")
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

                    // <<< LOG 4: What are the calculated usage results? >>>
                    Log.i("AddCategory_UsageCalc", "Calculated usageResults for $selectedMonth: $usageResults")

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
                    isLastPage = false
                    lastVisibleDocument = null
                }

                // Pagination logic (applies to both initial load and loadMore)
                val nextPageStartIndex = loadedCategories.size
                val nextPage = allCategories.drop(nextPageStartIndex).take(pageSize)

                if (nextPage.isNotEmpty()) {
                    loadedCategories.addAll(nextPage)
                }
                isLastPage = loadedCategories.size >= allCategories.size
                categoryAdapter.updateData(loadedCategories) // Update adapter with current page or full list

                if (!loadMore) { // Scroll to top only on full refresh
                    categoryRecyclerView.post { categoryRecyclerView.scrollToPosition(0) }
                }

            } catch (e: Exception) {
                Log.e("AddCategory_LoadError", "Error loading categories", e)
                Toast.makeText(this@AddCategory, "Error loading categories.", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false // Reset loading state
                // Only hide full screen animation if it was an initial load (not loadMore)
                if (!loadMore) {
                    showLoadingAnimation(false)
                }
                Log.d("AddCategory_LoadFinish", "loadCategories finished. isLoading: $isLoading")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefreshOnResume) {
            // Reset for a full reload, which will trigger the loading animation
            allCategories = emptyList()
            loadedCategories.clear()
            isLastPage = false
            lastVisibleDocument = null // Ensure pagination starts fresh
            loadCategories() // This will call showLoadingAnimation(true) if not loadMore
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

    private fun showDeleteConfirmationDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Delete All Categories")
        builder.setMessage("Are you sure you want to delete all categories? This action cannot be undone.")

        builder.setPositiveButton("Yes") { _, _ ->
            performDeleteAll()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showWatchAdDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unlock Feature")
            .setMessage("Watch a short ad to unlock 'Delete All Categories'.")
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
        val adUnitId = BuildConfig.REWARDED_AD_UNIT_ID // <--- CORRECTED
        Log.d("AdMob_AppOpen", "Loading App Open Ad with Unit ID: $adUnitId")
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this,
            adUnitId,
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

    private val addEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // A transaction was added/edited.
            // For AddCategory screen, this means totals might have changed.
            Log.d("AddCategory", "Returned from AddEntry via FAB. Reloading categories to update totals.")
            // Reset and reload to ensure totals displayed with categories are up-to-date.
            // If loadCategories() is heavy, consider a more targeted refresh if possible,
            // but loadCategories() already handles fetching transactions for totals.
            allCategories = emptyList() // Resetting to ensure a full refresh of display
            loadedCategories.clear()
            isLastPage = false
            loadCategories()
        } else {
            Log.d("AddCategory", "Returned from AddEntry via FAB with result code: ${result.resultCode}")
        }
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
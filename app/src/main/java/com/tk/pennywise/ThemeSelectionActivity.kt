package com.tk.pennywise

import androidx.appcompat.app.AppCompatActivity

// Data class for theme options in RecyclerView
data class HeaderColorThemeOption(
    val name: String, // User-facing name like "Default", "Purple Wave"
    val themeKey: String, // Key used in ThemeUtils (e.g., ThemeUtils.COLOR_THEME_PURPLE)
    val lightGradientResId: Int, // e.g., R.drawable.gradient_purple
    val darkGradientResId: Int,  // e.g., R.drawable.gradient_purple_dark
    val isPremium: Boolean = false
)

class ThemeSelectionActivity : AppCompatActivity() {
/*
    private lateinit var binding: ActivityThemeSelectionBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var headerThemeAdapter: HeaderThemeAdapter
    private val headerThemes = mutableListOf<HeaderColorThemeOption>()

    // Placeholder for subscription status
    private var isSubscribed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate and setContentView
        ThemeUtils.applySelectedTheme(this) // Make sure ThemeUtils is accessible and correct
        super.onCreate(savedInstanceState)
        binding = ActivityThemeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

        setupToolbar()
        setupOverallModeSelection()
        loadHeaderThemeOptions() // Populate theme options
        setupHeaderThemeRecyclerView()
        setupPremiumSection()

        // Handle reveal animation if started with one
        val revealX = intent.getIntExtra("reveal_x", -1)
        val revealY = intent.getIntExtra("reveal_y", -1)
        if (revealX != -1 && revealY != -1) {
            val decor = window.decorView
            decor.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View, left: Int, top: Int, right: Int, bottom: Int,
                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                ) {
                    v.removeOnLayoutChangeListener(this)
                    val finalRadius = kotlin.math.hypot(decor.width.toDouble(), decor.height.toDouble()).toFloat()
                    val anim = ViewAnimationUtils.createCircularReveal(decor, revealX, revealY, 0f, finalRadius)
                    decor.visibility = View.VISIBLE // Use View.VISIBLE
                    anim.duration = 350
                    anim.start()
                }
            })
            window.decorView.visibility = View.INVISIBLE // Use View.INVISIBLE
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finishWithReveal()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishWithReveal()
    }

    private fun finishWithReveal() {
        val decor = window.decorView
        val cx = decor.width / 2
        val cy = decor.height / 2
        val initialRadius = kotlin.math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(decor, cx, cy, initialRadius, 0f)
        anim.duration = 300
        // Use androidx.core.animation.AnimatorListenerAdapter
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: androidx.core.animation.Animator) {
                decor.visibility = View.INVISIBLE // Use View.INVISIBLE
                finish()
                overridePendingTransition(0, 0)
            }
        })
        anim.start()
    }


    private fun setupOverallModeSelection() {
        // Ensure ThemeUtils constants are correctly defined and accessible
        val currentMode = prefs.getString(ThemeUtils.KEY_OVERALL_MODE, ThemeUtils.MODE_SYSTEM)
        when (currentMode) {
            ThemeUtils.MODE_LIGHT -> binding.radioModeLight.isChecked = true
            ThemeUtils.MODE_DARK -> binding.radioModeDark.isChecked = true
            else -> binding.radioModeSystem.isChecked = true
        }

        binding.overallModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = when (checkedId) {
                R.id.radioModeLight -> ThemeUtils.MODE_LIGHT
                R.id.radioModeDark -> ThemeUtils.MODE_DARK
                else -> ThemeUtils.MODE_SYSTEM
            }
            ThemeUtils.setOverallMode(this, selectedMode)
        }
    }

    private fun loadHeaderThemeOptions() {
        headerThemes.clear()
        // Ensure ThemeUtils constants and R.drawable resources exist
        headerThemes.add(HeaderColorThemeOption("Default", ThemeUtils.COLOR_THEME_DEFAULT, R.drawable.gradient_light, R.drawable.gradient_dark))
        headerThemes.add(HeaderColorThemeOption("Purple Wave", ThemeUtils.COLOR_THEME_PURPLE, R.drawable.gradient_purple, R.drawable.gradient_purple_dark))
        headerThemes.add(HeaderColorThemeOption("Sunset Red", ThemeUtils.COLOR_THEME_RED, R.drawable.gradient_red, R.drawable.gradient_red_dark))
        headerThemes.add(HeaderColorThemeOption("Ocean Blue", "Blue", R.drawable.gradient_light, R.drawable.gradient_dark, isPremium = true))
        headerThemes.add(HeaderColorThemeOption("Forest Green", "Green", R.drawable.gradient_light, R.drawable.gradient_dark, isPremium = true))
    }


    private fun setupHeaderThemeRecyclerView() {
        headerThemeAdapter = HeaderThemeAdapter(this, headerThemes) { themeOption ->
            onHeaderThemeSelected(themeOption)
        }
        binding.headerThemeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ThemeSelectionActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = headerThemeAdapter
        }
    }

    private fun onHeaderThemeSelected(themeOption: HeaderColorThemeOption) {
        if (themeOption.isPremium && !isSubscribed) {
            handleUnlockAttempt(themeOption)
            return
        }
        ThemeUtils.setColorTheme(this, themeOption.themeKey)
    }

    private fun setupPremiumSection() {
        if (isSubscribed) {
            binding.premiumSection.visibility = View.GONE // Use View.GONE
        } else {
            binding.premiumSection.visibility = View.VISIBLE // Use View.VISIBLE
            binding.unlockThemesButton.setOnClickListener {
                Toast.makeText(this, "Simulating watching an ad...", Toast.LENGTH_SHORT).show()
                pretendAdWatchedSuccessfully()
            }
        }
    }

    private fun handleUnlockAttempt(themeOption: HeaderColorThemeOption) {
        binding.unlockThemesButton.performClick() // This is correct for a View system Button
    }

    private fun pretendAdWatchedSuccessfully() {
        isSubscribed = true
        Toast.makeText(this, "Themes Unlocked!", Toast.LENGTH_LONG).show()
        setupPremiumSection()
        headerThemeAdapter.notifyUserUnlocked()
    }

    class HeaderThemeAdapter(
        private val context: Context,
        private val themes: List<HeaderColorThemeOption>,
        private val onThemeClick: (HeaderColorThemeOption) -> Unit
    ) : RecyclerView.Adapter<HeaderThemeAdapter.ThemeViewHolder>() {

        private var userHasUnlocked = false

        fun notifyUserUnlocked() {
            userHasUnlocked = true
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_theme_color_preview, parent, false)
            return ThemeViewHolder(view)
        }

        override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
            val themeOption = themes[position]
            holder.bind(themeOption, userHasUnlocked, context)
            holder.itemView.setOnClickListener { onThemeClick(themeOption) }
        }

        override fun getItemCount(): Int = themes.size

        class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val previewView: View = itemView.findViewById(R.id.themeColorPreview)
            private val nameTextView: TextView = itemView.findViewById(R.id.themeColorName)

            fun bind(theme: HeaderColorThemeOption, isUnlocked: Boolean, context: Context) {
                nameTextView.text = theme.name

                val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                val gradientResId = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                    theme.darkGradientResId
                } else {
                    theme.lightGradientResId
                }
                previewView.background = ContextCompat.getDrawable(context, gradientResId)

                if (theme.isPremium && !isUnlocked) {
                    itemView.alpha = 0.5f
                } else {
                    itemView.alpha = 1.0f
                }
            }
        }
    }
    */
}

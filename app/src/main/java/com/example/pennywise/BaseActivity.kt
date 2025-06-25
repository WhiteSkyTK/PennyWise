package com.example.pennywise

import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

open class BaseActivity : AppCompatActivity() {

    fun setSystemBars(colorResId: Int?, useLightIcons: Boolean = false, transparent: Boolean = false) {
        val window = window
        val decorView = window.decorView

        if (transparent) {
            // Make the status bar transparent
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT

            // Required flags to draw behind bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            } else {
                @Suppress("DEPRECATION")
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
        } else {
            // Use solid color
            colorResId?.let {
                window.statusBarColor = ContextCompat.getColor(this, it)
                window.navigationBarColor = ContextCompat.getColor(this, it)
            }
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }

        // Set icon contrast (false = white icons)
        val controller = WindowCompat.getInsetsController(window, decorView)
        controller?.isAppearanceLightStatusBars = !useLightIcons
        controller?.isAppearanceLightNavigationBars = !useLightIcons
    }
}

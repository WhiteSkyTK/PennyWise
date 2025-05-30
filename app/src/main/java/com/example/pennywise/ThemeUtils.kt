package com.example.pennywise

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "app_theme"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (prefs.getString(KEY_THEME, THEME_LIGHT)) {
            THEME_DARK -> {
                // Apply dark mode theme
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                // Set status bar to black in dark mode
                setStatusBarColor(context, R.color.black)
            }
            THEME_LIGHT -> {
                // Apply light mode theme
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                // Set status bar to green in light mode
                setStatusBarColor(context, R.color.white)
            }
        }
    }

    fun toggleTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_THEME, THEME_LIGHT)
        val newTheme = if (current == THEME_LIGHT) THEME_DARK else THEME_LIGHT
        prefs.edit().putString(KEY_THEME, newTheme).apply()

        if (context is Activity) {
            context.recreate()
        } else {
            applyTheme(context)
        }
    }


    // Function to set the status bar color dynamically based on theme
    private fun setStatusBarColor(context: Context, colorResId: Int) {
        if (context is Activity) {
            val color = context.getColor(colorResId)
            val window = context.window
            window.statusBarColor = color
            window.navigationBarColor = color
        } else {
            // Log a warning instead of crashing
            Log.w("ThemeUtils", "Context is not an Activity. Skipping status bar color change.")
        }
    }
}

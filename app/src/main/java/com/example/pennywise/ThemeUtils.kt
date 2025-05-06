package com.example.pennywise

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "app_theme"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (prefs.getString(KEY_THEME, THEME_LIGHT)) {
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun toggleTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_THEME, THEME_LIGHT)
        val newTheme = if (current == THEME_LIGHT) THEME_DARK else THEME_LIGHT
        prefs.edit().putString(KEY_THEME, newTheme).apply()
        applyTheme(context)
    }
}

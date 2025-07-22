package com.tk.pennywise

import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

open class BaseActivity : AppCompatActivity() {

    fun setSystemBars() {
        enableEdgeToEdge()
    }
}

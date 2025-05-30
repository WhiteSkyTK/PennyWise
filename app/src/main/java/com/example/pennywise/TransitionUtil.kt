package com.example.pennywise

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewAnimationUtils
import android.animation.AnimatorListenerAdapter
import kotlin.math.hypot

object TransitionUtil {

    fun startCircularRevealTransition(activity: Activity, intent: Intent, centerView: View) {
        val cx = centerView.x.toInt() + centerView.width / 2
        val cy = centerView.y.toInt() + centerView.height / 2

        val decor = activity.window.decorView
        val finalRadius = hypot(decor.width.toDouble(), decor.height.toDouble()).toFloat()

        // Add flags to clear activity stack if needed
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra("from_nav", true) // Optional extra if you want to track it

        intent.putExtra("reveal_x", cx)
        intent.putExtra("reveal_y", cy)

        val anim = ViewAnimationUtils.createCircularReveal(decor, cx, cy, 0f, finalRadius)
        anim.duration = 350
        anim.start()

        // Post delay to allow animation before starting activity
        decor.postDelayed({
            activity.startActivity(intent)
            activity.overridePendingTransition(0, 0)
        }, anim.duration)
    }

    /**
     * Plays a circular‐reveal from (x,y), toggles the saved theme flag,
     * then recreates the activity under the new theme.
     */
    fun animateThemeChangeWithReveal(activity: Activity, x: Int, y: Int) {
        val decor = activity.window.decorView as View
        val overlay = View(activity).apply {
            setBackgroundColor(android.graphics.Color.WHITE) // Or your theme's bg
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        (decor as android.view.ViewGroup).addView(overlay)

        val finalRadius = hypot(decor.width.toDouble(), decor.height.toDouble()).toFloat()
        val expandAnim = ViewAnimationUtils.createCircularReveal(overlay, x, y, 0f, finalRadius).apply {
            duration = 350
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ThemeUtils.toggleTheme(activity)

                    val intent = activity.intent
                    intent.putExtra("reveal_x", x)
                    intent.putExtra("reveal_y", y)

                    activity.finish()
                    activity.overridePendingTransition(0, 0)
                    activity.startActivity(intent)
                }
            })
        }

        expandAnim.start()
    }

}

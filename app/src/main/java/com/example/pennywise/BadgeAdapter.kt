package com.example.pennywise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BadgeAdapter(
    private var badgeList: List<Badge>,
    private var loginStreak: LoginStreak? = null
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    val badges: List<Badge>
        get() = badgeList

    fun updateData(newBadgeList: List<Badge>, newLoginStreak: LoginStreak?) {
        badgeList = newBadgeList
        loginStreak = newLoginStreak
        notifyDataSetChanged()
    }

    class BadgeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val badgeIcon: ImageView = view.findViewById(R.id.badgeIcon)
        val badgeTitle: TextView = view.findViewById(R.id.badgeTitle)
        val badgeDescription: TextView = view.findViewById(R.id.badgeDescription)
        val badgeCount: TextView = view.findViewById(R.id.badgeCount)
        val lockOverlay: ImageView = view.findViewById(R.id.lockOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badgeList[position]

        holder.badgeIcon.setImageResource(badge.iconResId)
        holder.badgeTitle.text = badge.title
        holder.badgeDescription.text = if (badge.isEarned) badge.description else "Locked - Complete a challenge to earn this badge"

        // Fade icon and show lock overlay
        holder.badgeIcon.alpha = if (badge.isEarned) 1f else 0.4f
        holder.lockOverlay.visibility = if (badge.isEarned) View.GONE else View.VISIBLE

        // Dim text for locked badges
        val dimAlpha = if (badge.isEarned) 1f else 0.6f
        holder.badgeTitle.alpha = dimAlpha
        holder.badgeDescription.alpha = dimAlpha

        // Overlay count for streak-based or repeatable badges
        val overlay = when (badge.title) {
            "Daily Visitor" -> {
                val count = badge.overlayText?.removePrefix("x")?.toIntOrNull() ?: 1
                if (count > 1) "x$count" else ""
            }
            "Login Streak" -> {
                val streakCount = loginStreak?.streak ?: 0
                if (streakCount > 1) "x$streakCount" else ""
            }
            else -> {
                val count = badge.overlayText?.removePrefix("x")?.toIntOrNull() ?: 0
                if (count > 1) "x$count" else ""
            }
        }

        if (overlay.isNotEmpty() && badge.isEarned) {
            holder.badgeCount.visibility = View.VISIBLE
            holder.badgeCount.text = overlay
        } else {
            holder.badgeCount.visibility = View.GONE
        }

        // Background based on earned
        val bgRes = if (badge.isEarned) R.drawable.pill_background else R.drawable.pill_background_locked
        holder.itemView.setBackgroundResource(bgRes)
    }

    override fun getItemCount(): Int = badgeList.size


}
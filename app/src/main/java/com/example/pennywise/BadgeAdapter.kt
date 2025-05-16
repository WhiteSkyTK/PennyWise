package com.example.pennywise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BadgeAdapter(
    private val badgeList: List<Badge>,
    private val loginStreak: LoginStreak? = null // null-safe if not available
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    class BadgeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val badgeIcon: ImageView = view.findViewById(R.id.badgeIcon)
        val badgeTitle: TextView = view.findViewById(R.id.badgeTitle)
        val badgeDescription: TextView = view.findViewById(R.id.badgeDescription)
        val badgeCount: TextView = view.findViewById(R.id.badgeCount)
        val lockOverlay: ImageView = view.findViewById(R.id.lockOverlay) // 👈 ADD THIS LINE
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
        holder.badgeDescription.text = if (badge.isEarned) {
            badge.description
        } else {
            "Locked - Complete a challenge to earn this badge"
        }

        // Fade the icon if not earned
        holder.badgeIcon.alpha = if (badge.isEarned) 1f else 0.4f
        holder.lockOverlay.visibility = if (badge.isEarned) View.GONE else View.VISIBLE

        // Optional: dim text for locked badges
        val dimmedAlpha = if (badge.isEarned) 1f else 0.6f
        holder.badgeTitle.alpha = dimmedAlpha
        holder.badgeDescription.alpha = dimmedAlpha

        // Show overlay text (like login streak count) if applicable
        if (badge.overlayText != null && badge.isEarned) {
            holder.badgeCount.visibility = View.VISIBLE
            holder.badgeCount.text = badge.overlayText
        } else {
            holder.badgeCount.visibility = View.GONE
        }

        // Optional: change background based on locked state
        val backgroundRes = if (badge.isEarned) {
            R.drawable.pill_background
        } else {
            R.drawable.pill_background_locked // create this drawable if needed
        }
        holder.itemView.setBackgroundResource(backgroundRes)

        holder.lockOverlay.visibility = if (badge.isEarned) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = badgeList.size
}
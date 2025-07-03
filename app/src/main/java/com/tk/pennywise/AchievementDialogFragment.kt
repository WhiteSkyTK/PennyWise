package com.tk.pennywise

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.airbnb.lottie.LottieAnimationView

class AchievementDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(title: String, description: String, iconResId: Int): AchievementDialogFragment {
            val fragment = AchievementDialogFragment()
            val args = Bundle().apply {
                putString("title", title)
                putString("description", description)
                putInt("iconResId", iconResId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AchievementDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_achievement_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val icon = view.findViewById<ImageView>(R.id.badgeIcon)
        val titleText = view.findViewById<TextView>(R.id.badgeTitle)
        val descText = view.findViewById<TextView>(R.id.badgeDescription)
        val closeBtn = view.findViewById<ImageView>(R.id.closeButton)
        val confettiView = view.findViewById<LottieAnimationView>(R.id.confettiAnimation)

        val title = arguments?.getString("title") ?: "New Achievement!"
        val desc = arguments?.getString("description") ?: ""
        val iconResId = arguments?.getInt("iconResId") ?: R.drawable.badge1

        icon.setImageResource(iconResId)
        titleText.text = title
        descText.text = desc

        // Animate badge
        icon.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.pop_bounce))

        // Start confetti animation
        confettiView.playAnimation()

        // Close button
        closeBtn.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.CENTER)
        }
    }
}
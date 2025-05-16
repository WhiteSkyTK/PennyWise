package com.example.pennywise

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class AchievementDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(title: String, description: String, iconResId: Int): AchievementDialogFragment {
            val fragment = AchievementDialogFragment()
            val args = Bundle()
            args.putString("title", title)
            args.putString("description", description)
            args.putInt("iconResId", iconResId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.AchievementDialogStyle)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.fragment_achievement_dialog)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.CENTER)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val icon = view.findViewById<ImageView>(R.id.badgeIcon)
        val titleText = view.findViewById<TextView>(R.id.badgeTitle)
        val descText = view.findViewById<TextView>(R.id.badgeDescription)
        val closeBtn = view.findViewById<ImageView>(R.id.closeButton)

        val args = arguments
        val title = args?.getString("title") ?: "New Achievement!"
        val desc = args?.getString("description") ?: ""
        val iconResId = args?.getInt("iconResId") ?: R.drawable.badge1

        icon.setImageResource(iconResId)
        titleText.text = title
        descText.text = desc

        val bounce = AnimationUtils.loadAnimation(context, R.anim.pop_bounce)
        icon.startAnimation(bounce)

        closeBtn.setOnClickListener {
            dismiss()
        }
    }
}

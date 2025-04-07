package com.example.gamapulse

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    /* ----------------------------- Fragment Lifecycle Methods ----------------------------- */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        setupMoodEmojis(view)
        setupTaskLogButton(view)
        setupProfileButton(view)
        return view
    }
    /* ----------------------------- End Fragment Lifecycle Methods ----------------------------- */

    /* ----------------------------- Emoji Setup Methods ----------------------------- */
    private fun setupMoodEmojis(view: View) {
        // Find each emoji container by position in the LinearLayout
        val moodContainer = view.findViewById<LinearLayout>(R.id.mood_container)

        // Getting all emoji containers (assuming they are direct children of the mood_container)
        val marahContainer = moodContainer.getChildAt(0) as LinearLayout
        val sedihContainer = moodContainer.getChildAt(1) as LinearLayout
        val bahagiaContainer = moodContainer.getChildAt(2) as LinearLayout
        val biasaContainer = moodContainer.getChildAt(3) as LinearLayout

        // Set IDs programmatically if not set in XML
        marahContainer.id = View.generateViewId()
        sedihContainer.id = View.generateViewId()
        bahagiaContainer.id = View.generateViewId()
        biasaContainer.id = View.generateViewId()

        // Find emoji ImageViews (assuming first child is ImageView)
        val marahEmoji = marahContainer.getChildAt(0) as ImageView
        val sedihEmoji = sedihContainer.getChildAt(0) as ImageView
        val bahagiaEmoji = bahagiaContainer.getChildAt(0) as ImageView
        val biasaEmoji = biasaContainer.getChildAt(0) as ImageView

        // Set IDs for the emojis
        marahEmoji.id = View.generateViewId()
        sedihEmoji.id = View.generateViewId()
        bahagiaEmoji.id = View.generateViewId()
        biasaEmoji.id = View.generateViewId()

        // Set click listeners with animations
        setupEmojiClickListener(marahEmoji, "Marah")
        setupEmojiClickListener(sedihEmoji, "Sedih")
        setupEmojiClickListener(bahagiaEmoji, "Bahagia")
        setupEmojiClickListener(biasaEmoji, "Biasa")
    }

    private fun setupEmojiClickListener(emojiView: ImageView, moodType: String) {
        emojiView.setOnClickListener {
            animateEmoji(emojiView)
            showMoodRatingPopup(moodType)
        }
    }
    /* ----------------------------- End Emoji Setup Methods ----------------------------- */

    /* ----------------------------- Animation Methods ----------------------------- */
    private fun animateEmoji(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.3f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 500
        animatorSet.interpolator = OvershootInterpolator()
        animatorSet.start()
    }

    private fun animateButtonAndExecute(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            view.postDelayed({
                action()
            }, 150)
        }.start()
    }
    /* ----------------------------- End Animation Methods ----------------------------- */

    /* ----------------------------- UI Helper Methods ----------------------------- */
    private fun getRippleDrawable(color: Int): RippleDrawable {
        return RippleDrawable(
            ColorStateList.valueOf(requireContext().getColor(R.color.ripple_color)),
            null,
            ColorDrawable(color)
        )
    }
    /* ----------------------------- End UI Helper Methods ----------------------------- */

    /* ----------------------------- Popup Dialog Methods ----------------------------- */
    private fun showMoodRatingPopup(moodType: String) {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.popup_intensitas_mood, null)
        val titleTextView = popupView.findViewById<TextView>(R.id.popup_title)
        val numberPickerValue = popupView.findViewById<TextView>(R.id.number_value)
        val increaseButton = popupView.findViewById<View>(R.id.increase_button)
        val decreaseButton = popupView.findViewById<View>(R.id.decrease_button)
        val cancelButton = popupView.findViewById<Button>(R.id.cancel_button)
        val okButton = popupView.findViewById<Button>(R.id.ok_button)

        titleTextView.text = "Seberapa $moodType kamu?"

        var currentValue = 1
        numberPickerValue.text = currentValue.toString()

        cancelButton.foreground = getRippleDrawable(requireContext().getColor(R.color.teal))
        okButton.foreground = getRippleDrawable(requireContext().getColor(R.color.teal))
        increaseButton.setOnClickListener {
            if (currentValue < 5) {
                currentValue++
                numberPickerValue.text = currentValue.toString()
            }
        }
        decreaseButton.setOnClickListener {
            if (currentValue > 1) {
                currentValue--
                numberPickerValue.text = currentValue.toString()
            }
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setView(popupView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        cancelButton.setOnClickListener {
            animateButtonAndExecute(cancelButton) {
                dialog.dismiss()
            }
        }
        okButton.setOnClickListener {
            animateButtonAndExecute(okButton) {
                saveMoodRating(moodType, currentValue)
                dialog.dismiss()
                val intent = Intent(requireContext(), Notes::class.java)
                startActivity(intent)
            }
        }
        dialog.show()
    }
    /* ----------------------------- End Popup Dialog Methods ----------------------------- */

    /* ----------------------------- Data Methods ----------------------------- */
    private fun saveMoodRating(moodType: String, rating: Int) {
        // Implement your logic to save the mood rating
        // This could be storing to SharedPreferences, a database, or sending to an API
        // For now, we'll just print to the console
        println("Mood: $moodType, Rating: $rating")
    }
    /* ----------------------------- End Data Methods ----------------------------- */

    /* ----------------------------- Navigation ----------------------------- */
    private fun setupTaskLogButton(view: View) {
        view.findViewById<View>(R.id.btnTaksLog)?.setOnClickListener {
            // Navigate to TaskLogActivity with animation
            animateButtonAndExecute(it) {
                val intent = Intent(requireContext(), TaskLogActivity::class.java)
                startActivity(intent)
            }
        }
        view.findViewById<View>(R.id.btnViewCalendar)?.setOnClickListener {
            // Navigate to TaskLogActivity with animation
            animateButtonAndExecute(it) {
                val intent = Intent(requireContext(), ViewCalendarActivity::class.java)
                startActivity(intent)
            }
        }
    }
    /* ----------------------------- End Navigation ----------------------------- */

    /* ----------------------------- Profil Navigation ----------------------------- */
    private fun setupProfileButton(view: View) {
        val profileButton = view.findViewById<ImageView>(R.id.btn_profil)
        profileButton.setOnClickListener {
            animateButtonAndExecute(it) {
                val intent = Intent(requireContext(), ProfilActivity::class.java)
                startActivity(intent)
            }
        }
    }
    /* ----------------------------- Profil Navigation ----------------------------- */



}
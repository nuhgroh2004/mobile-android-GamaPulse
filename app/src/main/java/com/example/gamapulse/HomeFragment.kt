// HomeFragment.kt
package com.example.gamapulse

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Set up click listeners for all mood emojis
        setupMoodEmojis(view)

        return view
    }

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
            // Play animation when emoji is clicked
            animateEmoji(emojiView)

            // Show rating popup for this mood
            showMoodRatingPopup(moodType)
        }
    }

    private fun animateEmoji(view: View) {
        // Create scale animations
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.3f, 1f)

        // Create animator set
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 500
        animatorSet.interpolator = OvershootInterpolator()

        // Start animation
        animatorSet.start()
    }

    private fun showMoodRatingPopup(moodType: String) {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.popup_intensitas_mood, null)

        // Find views in the popup
        val titleTextView = popupView.findViewById<TextView>(R.id.popup_title)
        val numberPickerValue = popupView.findViewById<TextView>(R.id.number_value)
        val increaseButton = popupView.findViewById<View>(R.id.increase_button)
        val decreaseButton = popupView.findViewById<View>(R.id.decrease_button)
        val cancelButton = popupView.findViewById<Button>(R.id.cancel_button)
        val okButton = popupView.findViewById<Button>(R.id.ok_button)

        // Set popup title based on mood type
        titleTextView.text = "Seberapa $moodType kamu?"

        // Initialize counter
        var currentValue = 1
        numberPickerValue.text = currentValue.toString()

        // Set click listeners for increase/decrease buttons
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

        // Create dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(popupView)
            .create()

        // Set transparent background to allow for rounded corners in layout
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set button click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            // Save the mood and rating here
            saveMoodRating(moodType, currentValue)
            dialog.dismiss()

            // Navigate to Notes activity
            val intent = Intent(requireContext(), Notes::class.java)
            startActivity(intent)
        }

        dialog.show()
    }

    private fun saveMoodRating(moodType: String, rating: Int) {
        // Implement your logic to save the mood rating
        // This could be storing to SharedPreferences, a database, or sending to an API
        // For now, we'll just print to the console
        println("Mood: $moodType, Rating: $rating")
    }
}
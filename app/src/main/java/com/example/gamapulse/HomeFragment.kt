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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import cn.pedant.SweetAlert.BuildConfig

class HomeFragment : Fragment() {

    private var moodSelectionContainer: LinearLayout? = null
    private var moodParentContainer: LinearLayout? = null
    private var devRefreshButton: Button? = null

    /* ----------------------------- Fragment Lifecycle Methods ----------------------------- */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Get references to views
        moodParentContainer = view.findViewById<LinearLayout>(R.id.mood_container)?.parent as? LinearLayout
        moodSelectionContainer = view.findViewById(R.id.mood_container)
        devRefreshButton = view.findViewById(R.id.dev_refresh_button)

        view.findViewById<View>(R.id.current_mood_card).visibility = View.GONE

        setupMoodEmojis(view)
        setupTaskLogButton(view)
        setupProfileButton(view)
        setupDevRefreshButton()
        checkMoodSelectionStatus()
        updateMoodDisplay(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            checkMoodSelectionStatus()
            updateMoodDisplay(it)
        }
    }
    /* ----------------------------- End Fragment Lifecycle Methods ----------------------------- */

    /* ----------------------------- Developer Mode Methods ----------------------------- */
    private fun setupDevRefreshButton() {
        // Make developer button visible in debug builds
        if (BuildConfig.DEBUG) {
            devRefreshButton?.visibility = View.VISIBLE
        }

        devRefreshButton?.setOnClickListener {
            animateButtonAndExecute(it) {
                // Clear the last mood selection date
                val sharedPref = requireActivity().getSharedPreferences("MoodPrefs", AppCompatActivity.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    remove("LAST_MOOD_DATE")
                    remove("LAST_MOOD_TYPE")
                    remove("LAST_MOOD_INTENSITY")
                    remove("LAST_MOOD_NOTE")
                    apply()
                }

                checkMoodSelectionStatus()
                updateMoodDisplay(requireView())
                showMessageToast("Mood selection reset for testing")
            }
        }
    }

    private fun showMessageToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
    /* ----------------------------- End Developer Mode Methods ----------------------------- */

    /* ----------------------------- Mood Selection Status ----------------------------- */
    private fun checkMoodSelectionStatus() {
        val sharedPref = requireActivity().getSharedPreferences("MoodPrefs", AppCompatActivity.MODE_PRIVATE)
        val lastMoodDate = sharedPref.getString("LAST_MOOD_DATE", "")
        val currentDate = getCurrentDate()

        // Show mood selection if it's a new day or no selection has been made yet
        if (lastMoodDate.isNullOrEmpty() || lastMoodDate != currentDate) {
            moodParentContainer?.visibility = View.VISIBLE
        } else {
            moodParentContainer?.visibility = View.GONE
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }
    /* ----------------------------- End Mood Selection Status ----------------------------- */

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

    /* ----------------------------- Mood Display Methods ----------------------------- */
    private fun updateMoodDisplay(view: View) {
        // Get the saved mood data
        val sharedPref = requireActivity().getSharedPreferences("MoodPrefs", AppCompatActivity.MODE_PRIVATE)
        val moodType = sharedPref.getString("LAST_MOOD_TYPE", null)
        val moodIntensity = sharedPref.getInt("LAST_MOOD_INTENSITY", 0)

        // Find the container where we'll display the current mood
        val currentMoodContainer = view.findViewById<LinearLayout>(R.id.current_mood_container)
        val currentMoodCard = view.findViewById<View>(R.id.current_mood_card)

        // If there's no saved mood data, hide the container
        if (moodType == null || moodIntensity == 0) {
            currentMoodCard.visibility = View.GONE
            return
        }

        // Set visibility to VISIBLE
        currentMoodCard.visibility = View.VISIBLE

        // Set the mood emoji
        val moodEmoji = view.findViewById<ImageView>(R.id.current_mood_emoji)
        val emojiResource = when (moodType) {
            "Marah" -> R.drawable.icon_mood_marah
            "Sedih" -> R.drawable.icon_mood_sedih
            "Bahagia" -> R.drawable.icon_mood_bahagia
            else -> R.drawable.icon_mood_biasa
        }
        moodEmoji.setImageResource(emojiResource)

        // Set the mood text
        val moodText = view.findViewById<TextView>(R.id.current_mood_text)
        moodText.text = "Saya merasa $moodType dengan intensitas $moodIntensity pada hari ini."

        // Optional: Get and display the note text if you want to include it
        val moodNote = sharedPref.getString("LAST_MOOD_NOTE", "")
        if (!moodNote.isNullOrEmpty()) {
            val noteTextView = view.findViewById<TextView>(R.id.current_mood_note)
            noteTextView?.text = moodNote
            noteTextView?.visibility = View.VISIBLE
        }
    }
    /* ----------------------------- End Mood Display Methods ----------------------------- */

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
                // Pass the mood data to Notes activity
                intent.putExtra("MOOD_TYPE", moodType)
                intent.putExtra("MOOD_INTENSITY", currentValue)
                startActivity(intent)
            }
        }
        dialog.show()
    }
    /* ----------------------------- End Popup Dialog Methods ----------------------------- */

    /* ----------------------------- Data Methods ----------------------------- */
    private fun saveMoodRating(moodType: String, rating: Int) {
        // Store the mood with the current date
        val sharedPref = requireActivity().getSharedPreferences("MoodPrefs", AppCompatActivity.MODE_PRIVATE)
        val currentDate = getCurrentDate()

        with(sharedPref.edit()) {
            putString("TEMP_MOOD_TYPE", moodType)
            putInt("TEMP_MOOD_INTENSITY", rating)
            putString("LAST_MOOD_DATE", currentDate)
            apply()
        }

        // Hide the mood selection after user makes a choice
        checkMoodSelectionStatus()

        println("Mood: $moodType, Rating: $rating, Date: $currentDate")
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
            // Navigate to ViewCalendarActivity with animation
            animateButtonAndExecute(it) {
                val intent = Intent(requireContext(), ViewCalendarActivity::class.java)
                startActivity(intent)
            }
        }
    }
    /* ----------------------------- End Navigation ----------------------------- */

    /* ----------------------------- Profile Navigation ----------------------------- */
    private fun setupProfileButton(view: View) {
        val profileButton = view.findViewById<ImageView>(R.id.btn_profil)
        profileButton.setOnClickListener {
            animateButtonAndExecute(it) {
                val intent = Intent(requireContext(), ProfilActivity::class.java)
                startActivity(intent)
            }
        }
    }
    /* ----------------------------- End Profile Navigation ----------------------------- */
}
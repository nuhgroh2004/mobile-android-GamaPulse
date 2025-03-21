package com.example.gamapulse

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EditMoodNotesActivity : AppCompatActivity() {

    private lateinit var moodTitleTextView: TextView
    private lateinit var selectedMoodImageView: ImageView
    private lateinit var moodNotesEditText: EditText
    private lateinit var moodSelectionLayout: LinearLayout
    private lateinit var editButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    // Mood images views
    private lateinit var angryMoodImageView: ImageView
    private lateinit var sadMoodImageView: ImageView
    private lateinit var happyMoodImageView: ImageView
    private lateinit var calmMoodImageView: ImageView

    // Current mood intensity
    private var currentMoodIntensity = 1

    // Mood emojis and their respective tints
    private val moodEmojis = mapOf(
        "Marah" to "#FF6B6B",
        "Sedih" to "#FFE66D",
        "Bahagia" to "#FF9F65",
        "Biasa" to "#D4D4D4"
    )

    // Current selected mood
    private var currentMood = "Marah"
    private var selectedMoodView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_mood_notes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI elements
        initializeViews()

        // Get data from intent if exists
        val mood = intent.getStringExtra("mood") ?: "Marah"
        val notes = intent.getStringExtra("notes") ?: ""

        // Set initial data
        currentMood = mood
        updateMoodDisplay()
        moodNotesEditText.setText(notes)

        // Set initial view state - display mode
        setViewMode(isEditMode = false)

        // Set up click listeners
        setupClickListeners()

        // Set initial selected mood view
        when (currentMood) {
            "Marah" -> selectMoodView(angryMoodImageView)
            "Sedih" -> selectMoodView(sadMoodImageView)
            "Bahagia" -> selectMoodView(happyMoodImageView)
            "Biasa" -> selectMoodView(calmMoodImageView)
        }
    }

    private fun initializeViews() {
        moodTitleTextView = findViewById(R.id.moodTitleTextView)
        selectedMoodImageView = findViewById(R.id.selectedMoodImageView)
        moodNotesEditText = findViewById(R.id.moodNotesEditText)
        moodSelectionLayout = findViewById(R.id.moodSelectionLayout)
        editButton = findViewById(R.id.editButton)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Initialize mood views
        angryMoodImageView = findViewById(R.id.angryMoodImageView)
        sadMoodImageView = findViewById(R.id.sadMoodImageView)
        happyMoodImageView = findViewById(R.id.happyMoodImageView)
        calmMoodImageView = findViewById(R.id.calmMoodImageView)
    }

    private fun setupClickListeners() {
        // Edit button - switch to edit mode
        editButton.setOnClickListener {
            setViewMode(isEditMode = true)
        }

        // Save button - save changes and switch to display mode
        saveButton.setOnClickListener {
            // Here you would save the data to your database or preferences
            // For now, we'll just switch back to display mode
            setViewMode(isEditMode = false)
        }

        // Cancel button - discard changes and go back
        cancelButton.setOnClickListener {
            finish()
        }

        // Setup mood selection click listeners
        setupMoodSelectionListeners()
    }

    private fun setupMoodSelectionListeners() {
        val moodViews = listOf(
            angryMoodImageView,
            sadMoodImageView,
            happyMoodImageView,
            calmMoodImageView
        )

        moodViews.forEach { moodView ->
            moodView.setOnClickListener {
                // Get the mood type from the tag
                currentMood = it.tag as String

                // Animate the view
                animateEmoji(it)

                // Select this mood view
                selectMoodView(moodView)

                // Show intensity rating popup
                showMoodRatingPopup(currentMood)
            }
        }
    }

    private fun selectMoodView(moodView: ImageView) {
        // Reset all backgrounds
        angryMoodImageView.setBackgroundResource(android.R.color.transparent)
        sadMoodImageView.setBackgroundResource(android.R.color.transparent)
        happyMoodImageView.setBackgroundResource(android.R.color.transparent)
        calmMoodImageView.setBackgroundResource(android.R.color.transparent)

        // Set the selected view's background
        moodView.setBackgroundResource(R.drawable.calendar_cell_today)
        selectedMoodView = moodView
    }

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

    private fun getRippleDrawable(color: Int): ColorStateList {
        return ColorStateList.valueOf(color)
    }

    private fun showMoodRatingPopup(moodType: String) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_intensitas_mood, null)
        val titleTextView = popupView.findViewById<TextView>(R.id.popup_title)
        val numberPickerValue = popupView.findViewById<TextView>(R.id.number_value)
        val increaseButton = popupView.findViewById<View>(R.id.increase_button)
        val decreaseButton = popupView.findViewById<View>(R.id.decrease_button)
        val cancelButton = popupView.findViewById<Button>(R.id.cancel_button)
        val okButton = popupView.findViewById<Button>(R.id.ok_button)

        titleTextView.text = "Seberapa $moodType kamu?"

        var currentValue = currentMoodIntensity
        numberPickerValue.text = currentValue.toString()

        cancelButton.backgroundTintList = getRippleDrawable(getColor(R.color.teal))
        okButton.backgroundTintList = getRippleDrawable(getColor(R.color.teal))

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

        val dialog = AlertDialog.Builder(this)
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
                currentMoodIntensity = currentValue
                updateMoodDisplay()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateMoodDisplay() {
        // Update the mood title
        moodTitleTextView.text = "SAYA MERASA ${currentMood.uppercase()} (${currentMoodIntensity})"

        // Get the appropriate drawable ID based on the mood
        val drawableId = when (currentMood) {
            "Marah" -> R.drawable.icon_mood_marah
            "Sedih" -> R.drawable.icon_mood_sedih
            "Bahagia" -> R.drawable.icon_mood_bahagia
            "Biasa" -> R.drawable.icon_mood_biasa
            else -> R.drawable.icon_mood_biasa
        }

        // Update the mood image
        selectedMoodImageView.setImageResource(drawableId)
    }

    private fun setViewMode(isEditMode: Boolean) {
        if (isEditMode) {
            // Edit mode
            moodSelectionLayout.visibility = View.VISIBLE
            selectedMoodImageView.visibility = View.GONE // Hide the selected mood image
            moodNotesEditText.isEnabled = true
            editButton.visibility = View.GONE
            saveButton.visibility = View.VISIBLE
        } else {
            // Display mode
            moodSelectionLayout.visibility = View.GONE
            selectedMoodImageView.visibility = View.VISIBLE // Show the selected mood image
            moodNotesEditText.isEnabled = false
            editButton.visibility = View.VISIBLE
            saveButton.visibility = View.GONE
        }
    }
}
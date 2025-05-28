package com.example.gamapulse

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.gamapulse.model.MoodNotesRequest
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.launch

class EditMoodNotesActivity : AppCompatActivity() {
    private lateinit var moodTitleTextView: TextView
    private lateinit var selectedMoodImageView: ImageView
    private lateinit var moodNotesEditText: EditText
    private lateinit var moodSelectionLayout: LinearLayout
    private lateinit var editButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var angryMoodImageView: ImageView
    private lateinit var sadMoodImageView: ImageView
    private lateinit var happyMoodImageView: ImageView
    private lateinit var calmMoodImageView: ImageView
    private var currentMoodIntensity = 1
    private var currentMood = "Marah"
    private var selectedMoodView: ImageView? = null

    // Parameters from intent
    private var day = 0
    private var month = 0
    private var year = 0

    /* ----------------------------- Lifecycle Methods ----------------------------- */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_mood_notes)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get date parameters from intent
        day = intent.getIntExtra("day", 1)
        month = intent.getIntExtra("month", 1)
        year = intent.getIntExtra("year", 2025)

        initializeViews()
        setupClickListeners()

        // Fetch mood data for the specified date
        fetchMoodData(day, month, year)
    }
    /* ----------------------------- End Lifecycle Methods ----------------------------- */

    /* ----------------------------- API Methods ----------------------------- */
    private fun fetchMoodData(day: Int, month: Int, year: Int) {
        val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "Memuat data..."
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
                val token = sharedPreferences.getString("token", null)

                if (token != null) {
                    val authToken = "Bearer $token"
                    val response = ApiClient.apiService.getMoodNotes(authToken, day, month, year)

                    if (response.isSuccessful && response.body() != null) {
                        val moodResponse = response.body()!!
                        val mood = moodResponse.mood

                        if (mood != null) {
                            currentMood = when (mood.mood_level) {
                                1 -> "Marah"
                                2 -> "Sedih"
                                3 -> "Biasa"
                                4 -> "Bahagia"
                                else -> "Biasa"
                            }
                            currentMoodIntensity = mood.mood_intensity
                            moodNotesEditText.setText(mood.mood_note ?: "") // Set the notes here
                            updateMoodDisplay()
                            selectMoodView(
                                when (currentMood) {
                                    "Marah" -> angryMoodImageView
                                    "Sedih" -> sadMoodImageView
                                    "Bahagia" -> happyMoodImageView
                                    else -> calmMoodImageView
                                }
                            )
                        } else {
                            showPlaceholder()
                        }
                    } else {
                        showPlaceholder()
                    }
                } else {
                    Toast.makeText(this@EditMoodNotesActivity, "Token tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("EditMoodNotes", "Error fetching mood data", e)
                showPlaceholder()
            } finally {
                loadingDialog.dismissWithAnimation()
            }
        }
    }

    private fun showPlaceholder() {
        currentMood = "Biasa"
        currentMoodIntensity = 1
        moodNotesEditText.setText("")
        updateMoodDisplay()
        selectMoodView(calmMoodImageView)
    }
    /* ----------------------------- End API Methods ----------------------------- */

    /* ----------------------------- View Initialization ----------------------------- */
    private fun initializeViews() {
        moodTitleTextView = findViewById(R.id.moodTitleTextView)
        selectedMoodImageView = findViewById(R.id.selectedMoodImageView)
        moodNotesEditText = findViewById(R.id.moodNotesEditText)
        moodSelectionLayout = findViewById(R.id.moodSelectionLayout)
        editButton = findViewById(R.id.editButton)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
        angryMoodImageView = findViewById(R.id.angryMoodImageView)
        sadMoodImageView = findViewById(R.id.sadMoodImageView)
        happyMoodImageView = findViewById(R.id.happyMoodImageView)
        calmMoodImageView = findViewById(R.id.calmMoodImageView)
    }
    /* ----------------------------- End View Initialization ----------------------------- */

    /* ----------------------------- Event Listeners ----------------------------- */
    private fun setupClickListeners() {
        editButton.setOnClickListener {
            setViewMode(isEditMode = true)
        }
        saveButton.setOnClickListener {
            showSaveConfirmation()
        }
        cancelButton.setOnClickListener {
            finish()
        }
        setupMoodSelectionListeners()
    }
    /* ----------------------------- End Event Listeners ----------------------------- */

    /* ----------------------------- UI Helpers ----------------------------- */
    private fun showSaveConfirmation() {
        val dialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Konfirmasi")
            .setContentText("Apakah Anda yakin ingin menyimpan catatan mood ini?")
            .setCancelText("Batal")
            .setConfirmText("Simpan")
            .showCancelButton(true)
            .setConfirmClickListener { sDialog ->
                saveMoodData()
                sDialog.dismissWithAnimation()
                val successDialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Berhasil!")
                    .setContentText("Catatan mood berhasil disimpan")
                    .setConfirmClickListener { it ->
                        it.dismissWithAnimation()
                        setViewMode(isEditMode = false)
                    }
                successDialog.show()
                successDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM)?.apply {
                    background = resources.getDrawable(R.drawable.allert_button_ok, theme)
                    setTextColor(Color.WHITE)
                    setPadding(24, 12, 24, 12)
                    minWidth = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
                    ).toInt()
                    backgroundTintList = null
                }
            }
            .setCancelClickListener { sDialog ->
                sDialog.dismissWithAnimation()
            }
        dialog.show()
        val cancelButton = dialog.getButton(SweetAlertDialog.BUTTON_CANCEL)
        val confirmButton = dialog.getButton(SweetAlertDialog.BUTTON_CONFIRM)
        cancelButton.apply {
            background = resources.getDrawable(R.drawable.allert_button_cancel, theme)
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
            ).toInt()
            backgroundTintList = null
        }
        confirmButton.apply {
            background = resources.getDrawable(R.drawable.allert_button_confirm, theme)
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
            ).toInt()
            backgroundTintList = null
        }
    }
    /* ----------------------------- End UI Helpers ----------------------------- */

    /* ----------------------------- Mood Selection ----------------------------- */
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
        val dialog = AlertDialog.Builder(this).setView(popupView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        cancelButton.setOnClickListener {
            cancelButton.background = ColorDrawable(Color.RED)
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
    /* ----------------------------- End Mood Selection ----------------------------- */

    /* ----------------------------- Data Persistence ----------------------------- */
    private fun saveMoodData() {
        val notes = moodNotesEditText.text.toString()

        // TODO: Implement API call to save mood data
        println("Menyimpan mood: $currentMood dengan intensitas: $currentMoodIntensity dan catatan: $notes")
    }
    /* ----------------------------- End Data Persistence ----------------------------- */

    /* ----------------------------- Selection Helpers ----------------------------- */
    private fun setupMoodSelectionListeners() {
        val moodViews = listOf(
            angryMoodImageView,
            sadMoodImageView,
            happyMoodImageView,
            calmMoodImageView
        )
        moodViews.forEach { moodView ->
            moodView.setOnClickListener {
                currentMood = it.tag as String
                animateEmoji(it)
                selectMoodView(moodView)
                showMoodRatingPopup(currentMood)
            }
        }
    }

    private fun selectMoodView(moodView: ImageView) {
        angryMoodImageView.setBackgroundResource(android.R.color.transparent)
        sadMoodImageView.setBackgroundResource(android.R.color.transparent)
        happyMoodImageView.setBackgroundResource(android.R.color.transparent)
        calmMoodImageView.setBackgroundResource(android.R.color.transparent)

        moodView.setBackgroundResource(R.drawable.calendar_cell_today)
        selectedMoodView = moodView
    }
    /* ----------------------------- End Selection Helpers ----------------------------- */

    /* ----------------------------- Animation Helpers ----------------------------- */
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
    /* ----------------------------- End Animation Helpers ----------------------------- */

    /* ----------------------------- UI State Management ----------------------------- */
    private fun updateMoodDisplay() {
        moodTitleTextView.text = "SAYA MERASA ${currentMood.uppercase()} (${currentMoodIntensity})"
        val drawableId = when (currentMood) {
            "Marah" -> R.drawable.icon_mood_marah
            "Sedih" -> R.drawable.icon_mood_sedih
            "Bahagia" -> R.drawable.icon_mood_bahagia
            "Biasa" -> R.drawable.icon_mood_biasa
            else -> R.drawable.icon_mood_biasa
        }
        selectedMoodImageView.setImageResource(drawableId)
    }

    private fun setViewMode(isEditMode: Boolean) {
        if (isEditMode) {
            moodSelectionLayout.visibility = View.VISIBLE
            selectedMoodImageView.visibility = View.GONE
            moodNotesEditText.isEnabled = true
            editButton.visibility = View.GONE
            saveButton.visibility = View.VISIBLE
        } else {
            moodSelectionLayout.visibility = View.GONE
            selectedMoodImageView.visibility = View.VISIBLE
            moodNotesEditText.isEnabled = false
            editButton.visibility = View.VISIBLE
            saveButton.visibility = View.GONE
        }
    }
    /* ----------------------------- End UI State Management ----------------------------- */
}
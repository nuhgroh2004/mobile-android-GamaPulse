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
import com.example.gamapulse.model.UpdateMoodRequest
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

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
    private var day = 0
    private var month = 0
    private var year = 0
    private var moodId: Int = 0

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
        day = intent.getIntExtra("day", 1)
        month = intent.getIntExtra("month", 1)
        year = intent.getIntExtra("year", 2025)
        initializeViews()
        setupClickListeners()
        moodNotesEditText.isEnabled = false
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        dateTextView.text = String.format("%02d/%02d/%d", day, month, year)
        fetchMoodData(day, month, year)
    }
    /* ----------------------------- End Lifecycle Methods ----------------------------- */

    /* ----------------------------- API Methods ----------------------------- */
    private fun fetchMoodData(day: Int, month: Int, year: Int) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Memuat data...")
            .setView(LayoutInflater.from(this).inflate(R.layout.progress_dialog, null))
            .setCancelable(false)
            .create()
        progressDialog.show()
        val isDateToday = isToday(day, month, year)

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
                            moodId = mood.mood_id
                            currentMood = when (mood.mood_level) {
                                1 -> "Marah"
                                2 -> "Sedih"
                                3 -> "Biasa"
                                4 -> "Bahagia"
                                else -> ""
                            }
                            currentMoodIntensity = mood.mood_intensity
                            moodNotesEditText.setText(mood.mood_note ?: "")
                            val dateTextView = findViewById<TextView>(R.id.dateTextView)
                            dateTextView.text = String.format("%02d/%02d/%d", day, month, year)
                            if (mood.updated_at != null || mood.created_at != null) {
                                try {
                                    val timeString = mood.updated_at ?: mood.created_at
                                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    val date = inputFormat.parse(timeString)
                                } catch (e: Exception) {
                                    Log.e("EditMoodNotes", "Error parsing time", e)
                                }
                            }
                            moodTitleTextView.visibility = View.VISIBLE
                            updateMoodDisplay()
                            selectMoodView(
                                when (currentMood) {
                                    "Marah" -> angryMoodImageView
                                    "Sedih" -> sadMoodImageView
                                    "Bahagia" -> happyMoodImageView
                                    else -> calmMoodImageView
                                }
                            )
                            setViewMode(isEditMode = false)
                            editButton.isEnabled = isDateToday
                            if (!isDateToday) {
                                editButton.setOnClickListener {
                                    showPastDateDialog()
                                }
                            }
                        } else {
                            moodTitleTextView.visibility = View.GONE
                            selectedMoodImageView.setImageResource(R.drawable.placeholder_circle)
                        }
                    } else {
                        moodTitleTextView.visibility = View.GONE
                        selectedMoodImageView.setImageResource(R.drawable.placeholder_circle)
                    }
                } else {
                    Toast.makeText(this@EditMoodNotesActivity, "Token tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("EditMoodNotes", "Error fetching mood data", e)
                moodTitleTextView.visibility = View.GONE
                selectedMoodImageView.setImageResource(R.drawable.placeholder_circle)
            } finally {
                progressDialog.dismiss()
            }
        }
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
            saveMoodData()
        }
        cancelButton.setOnClickListener {
            setResult(RESULT_OK) // Set result untuk memastikan calendar di-refresh
            finish()
        }
        setupMoodSelectionListeners()
    }
    /* ----------------------------- End Event Listeners ----------------------------- */

    /* ----------------------------- UI Helpers ----------------------------- */
    private fun showSaveConfirmation() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin ingin menyimpan catatan mood ini?")
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Simpan") { dialog, _ ->
                dialog.dismiss()
                saveMoodData()
            }
            .create()
        dialog.show()
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
            dialog.dismiss()
        }
        okButton.setOnClickListener {
            currentMoodIntensity = currentValue
            updateMoodDisplay()
            dialog.dismiss()
        }
        dialog.show()
    }
    /* ----------------------------- End Mood Selection ----------------------------- */

    /* ----------------------------- Data Persistence ----------------------------- */
    private fun saveMoodData() {
        if (!isToday(day, month, year)) {
            AlertDialog.Builder(this)
                .setTitle("Tidak Dapat Mengedit")
                .setMessage("Anda tidak dapat mengedit mood selain hari ini.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val notes = moodNotesEditText.text.toString()
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Menyimpan data...")
            .setView(LayoutInflater.from(this).inflate(R.layout.progress_dialog, null))
            .setCancelable(false)
            .create()
        progressDialog.show()
        lifecycleScope.launch {
            try {
                val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("token", null)
                if (token != null) {
                    val authToken = "Bearer $token"
                    val moodLevel = when (currentMood) {
                        "Marah" -> 1
                        "Sedih" -> 2
                        "Biasa" -> 3
                        "Bahagia" -> 4
                        else -> 3
                    }
                    val request = UpdateMoodRequest(
                        mood_level = moodLevel,
                        mood_note = notes,
                        mood_intensity = currentMoodIntensity.toString() // Konversi ke string
                    )
                    Log.d("EditMoodNotes", "Request Payload: $request")
                    val response = ApiClient.apiService.updateMoodNote(authToken, moodId, request)
                    progressDialog.dismiss()
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditMoodNotesActivity, "Berhasil memperbarui mood", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        setViewMode(isEditMode = false)
                        fetchMoodData(day, month, year)
                    } else {
                        Log.e("EditMoodNotes", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(this@EditMoodNotesActivity, "Gagal memperbarui mood: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this@EditMoodNotesActivity, "Token tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e("EditMoodNotes", "Error updating mood data", e)
                Toast.makeText(this@EditMoodNotesActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
            moodNotesEditText.isEnabled = false // Nonaktifkan EditText
            editButton.visibility = View.VISIBLE
            saveButton.visibility = View.GONE
        }
    }
    /* ----------------------------- End UI State Management ----------------------------- */
}
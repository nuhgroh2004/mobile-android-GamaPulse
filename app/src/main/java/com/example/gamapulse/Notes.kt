package com.example.gamapulse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import cn.pedant.SweetAlert.SweetAlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gamapulse.databinding.ActivityNotesBinding
import com.example.gamapulse.model.StoreMoodRequest
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Notes : AppCompatActivity() {
    private lateinit var binding: ActivityNotesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnKembali.foreground = getRippleDrawable(getColor(R.color.teal))
        binding.btnSimpan.foreground = getRippleDrawable(getColor(R.color.white))
        binding.btnKembali.setOnClickListener {
            animateButtonAndExecute(it) {
                val sharedPref = getSharedPreferences("MoodPrefs", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putBoolean("TEMP_NAVIGATING_TO_NOTES", false)
                    putBoolean("SHOULD_SHOW_MOOD_SELECTION", true)
                    apply()
                }
                finish()
            }
        }

        binding.btnSimpan.setOnClickListener {
            animateButtonAndExecute(it) {
                val dialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Konfirmasi")
                    .setContentText("Apakah Anda yakin ingin menyimpan catatan ini?")
                    .setCancelText("Batal")
                    .setConfirmText("Simpan")
                    .showCancelButton(true)
                    .setConfirmClickListener { sDialog ->
                        val catatan = binding.editTextCatatan.text.toString()
                        val moodType = intent.getStringExtra("MOOD_TYPE") ?: "Biasa"
                        val moodIntensity = intent.getIntExtra("MOOD_INTENSITY", 1)

                        // Save locally
                        val sharedPref = getSharedPreferences("MoodPrefs", MODE_PRIVATE)
                        val currentDate = getCurrentDate()
                        with(sharedPref.edit()) {
                            putString("LAST_MOOD_TYPE", moodType)
                            putInt("LAST_MOOD_INTENSITY", moodIntensity)
                            putString("LAST_MOOD_NOTE", catatan)
                            putString("LAST_MOOD_DATE", currentDate)
                            apply()
                        }

                        // Save to API
                        saveMoodToApi(moodType, moodIntensity.toString(), catatan)

                        sDialog.dismissWithAnimation()
                        val successDialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Berhasil!")
                            .setContentText("Catatan berhasil disimpan")
                            .setConfirmClickListener { it ->
                                it.dismissWithAnimation()
                                val sharedPref = getSharedPreferences("MoodPrefs", MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putBoolean("TEMP_NAVIGATING_TO_NOTES", false)
                                    apply()
                                }
                                finish()
                            }
                        successDialog.show()
                        styleConfirmButton(successDialog)
                    }
                    .setCancelClickListener { sDialog ->
                        sDialog.dismissWithAnimation()
                    }
                dialog.show()
                styleAlertButtons(dialog)
            }
        }
    }

    override fun onBackPressed() {
        val sharedPref = getSharedPreferences("MoodPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("TEMP_NAVIGATING_TO_NOTES", false)
            apply()
        }
        super.onBackPressed()
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun animateButtonAndExecute(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            view.postDelayed({
                action()
            }, 150)
        }.start()
    }

    private fun setupButtonWithAnimation(button: View, destinationClass: Class<*>) {
        button.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                it.postDelayed({
                    val intent = Intent(this, destinationClass)
                    startActivity(intent)
                }, 150)
            }.start()
        }
    }

    private fun getRippleDrawable(color: Int): RippleDrawable {
        return RippleDrawable(
            ColorStateList.valueOf(getColor(R.color.ripple_color)),
            null,
            ColorDrawable(color)
        )
    }

    private fun saveMoodToApi(emotion: String, intensity: String, notes: String) {
        val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }
        val mappedEmotion = when (emotion) {
            "Biasa" -> "Biasa saja"
            "Bahagia" -> "Senang"
            else -> emotion
        }

        val validIntensity = if (intensity.isEmpty() || intensity == "0") "1" else intensity
        val authToken = "Bearer $token"
        val request = StoreMoodRequest(mappedEmotion, validIntensity, notes)
        Log.d("MoodAPI", "Sending mood request: Original emotion: $emotion, Mapped to: $mappedEmotion, intensity: $validIntensity, notes: $notes")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.storeMood(authToken, request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d("MoodAPI", "Mood saved successfully")
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("MoodAPI", "Failed to save mood: $errorBody")
                        Toast.makeText(
                            this@Notes,
                            "Failed to save mood: $errorBody",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MoodAPI", "Exception while saving mood", e)
                    Toast.makeText(
                        this@Notes,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun styleConfirmButton(dialog: SweetAlertDialog) {
        dialog.getButton(SweetAlertDialog.BUTTON_CONFIRM)?.apply {
            background = getDrawable(R.drawable.allert_button_ok)
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
            ).toInt()
            backgroundTintList = null
        }
    }

    private fun styleAlertButtons(dialog: SweetAlertDialog) {
        val cancelButton = dialog.getButton(SweetAlertDialog.BUTTON_CANCEL)
        val confirmButton = dialog.getButton(SweetAlertDialog.BUTTON_CONFIRM)

        cancelButton?.apply {
            background = getDrawable(R.drawable.allert_button_cancel)
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
            ).toInt()
            backgroundTintList = null
        }

        confirmButton?.apply {
            background = getDrawable(R.drawable.allert_button_confirm)
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
            ).toInt()
            backgroundTintList = null
        }
    }
}
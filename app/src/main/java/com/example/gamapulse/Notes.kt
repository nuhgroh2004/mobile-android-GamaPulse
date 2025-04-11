package com.example.gamapulse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import android.graphics.Color
import android.util.TypedValue
import cn.pedant.SweetAlert.SweetAlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gamapulse.databinding.ActivityNotesBinding
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
                        val sharedPref = getSharedPreferences("MoodPrefs", MODE_PRIVATE)
                        val currentDate = getCurrentDate()
                        with(sharedPref.edit()) {
                            putString("LAST_MOOD_TYPE", moodType)
                            putInt("LAST_MOOD_INTENSITY", moodIntensity)
                            putString("LAST_MOOD_NOTE", catatan)
                            putString("LAST_MOOD_DATE", currentDate)
                            apply()
                        }
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
        }
    }
    @Override
    override fun onBackPressed() {
        // When user manually presses back button, clear temporary navigation flag
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

    // Function for button animation and action execution
    private fun animateButtonAndExecute(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            view.postDelayed({
                action()
            }, 150)
        }.start()
    }

    // Function for button animation
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
}
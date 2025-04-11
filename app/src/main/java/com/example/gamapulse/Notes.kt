package com.example.gamapulse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
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

        // Set foreground ripple effects
        binding.btnKembali.foreground = getRippleDrawable(getColor(R.color.teal))
        binding.btnSimpan.foreground = getRippleDrawable(getColor(R.color.white))

        // Integration of animation with regular click handling
        binding.btnKembali.setOnClickListener {
            animateButtonAndExecute(it) {
                // Just finish this activity to return to previous screen
                finish()
            }
        }

        binding.btnSimpan.setOnClickListener {
            animateButtonAndExecute(it) {
                // Save note data logic here
                val catatan = binding.editTextCatatan.text.toString()

                // Get the mood data from the intent extras
                val moodType = intent.getStringExtra("MOOD_TYPE") ?: "Biasa"
                val moodIntensity = intent.getIntExtra("MOOD_INTENSITY", 1)

                // Store the data in SharedPreferences
                val sharedPref = getSharedPreferences("MoodPrefs", MODE_PRIVATE)
                val currentDate = getCurrentDate()

                with(sharedPref.edit()) {
                    putString("LAST_MOOD_TYPE", moodType)
                    putInt("LAST_MOOD_INTENSITY", moodIntensity)
                    putString("LAST_MOOD_NOTE", catatan)
                    putString("LAST_MOOD_DATE", currentDate)  // Save the current date
                    apply()
                }

                // Return to previous screen
                finish()
            }
        }
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
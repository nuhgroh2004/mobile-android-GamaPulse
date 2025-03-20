// Notes.kt
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

                // Implement saving logic here
                // For example, store in a database

                // Return to previous screen
                finish()
            }
        }
    }

    // Fungsi untuk animasi tombol dan eksekusi aksi binding
    private fun animateButtonAndExecute(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            view.postDelayed({
                action()
            }, 150)
        }.start()
    }
    // Fungsi untuk animasi tombol dan eksekusi aksi binding

    // Fungsi untuk animasi tombol
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
    // Fungsi untuk animasi tombol
}
// Notes.kt
package com.example.gamapulse

import android.os.Bundle
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

        // Set up click listeners for buttons
        binding.buttonKembali.setOnClickListener {
            // Just finish this activity to return to previous screen
            finish()
        }

        binding.buttonSimpan.setOnClickListener {
            // Save note data logic here
            // You would typically save the data from the form fields
            val tanggal = binding.editTextTanggal.text.toString()
            val jam = binding.editTextJam.text.toString()
            val catatan = binding.editTextCatatan.text.toString()

            // Implement saving logic here
            // For example, store in a database

            // Return to previous screen
            finish()
        }
    }
}
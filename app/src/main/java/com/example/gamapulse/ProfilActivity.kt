package com.example.gamapulse

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gamapulse.databinding.ActivityProfilBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfilActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfilBinding
    private val calendar = Calendar.getInstance()
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadUserData()
        setupBackButton()
        setupEditButton()
        setupDatePicker()
    }

    private fun loadUserData() {
        binding.tvUsername.text = "john_doe"
        binding.etUsername.setText("john_doe")
        binding.etEmail.setText("john.doe@example.com")
        binding.etProgramStudy.setText("Computer Science")
        binding.etNIM.setText("123456789")
        binding.etDob.setText("15 Jan 1995")
        binding.etPhone.setText("8123456789")
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            it.animate().alpha(0.5f).setDuration(100).withEndAction {
                it.animate().alpha(1f).setDuration(100).start()
                finish()
            }.start() }
    }

    private fun setupEditButton() {
        binding.btnUpdateProfile.setOnClickListener {
            if (!isEditMode) {
                isEditMode = true
                binding.btnUpdateProfile.text = "Cancel"
                binding.btnUpdateProfile.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                enableEditMode(true)
            } else {
                isEditMode = false
                binding.btnUpdateProfile.text = "Update Profile"
                binding.btnUpdateProfile.setBackgroundColor(ContextCompat.getColor(this, R.color.blue))
                enableEditMode(false)
                loadUserData()
            }
        }

        binding.btnSave.setOnClickListener {
            saveUserData()
            isEditMode = false
            binding.btnUpdateProfile.text = "Update Profile"
            binding.btnUpdateProfile.setBackgroundColor(ContextCompat.getColor(this, R.color.blue))
            enableEditMode(false)
        }
    }

    private fun enableEditMode(enabled: Boolean) {
        binding.etUsername.isEnabled = false
        binding.etEmail.isEnabled = enabled
        binding.etProgramStudy.isEnabled = enabled
        binding.etNIM.isEnabled = false
        binding.etDob.isEnabled = enabled
        binding.etPhone.isEnabled = enabled

        val visibility = if (enabled) View.VISIBLE else View.GONE
        binding.passwordLayout.visibility = visibility
        binding.btnSave.visibility = visibility
        binding.usernameLayout.visibility = visibility
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        binding.etDob.setOnClickListener {
            if (isEditMode) {
                showDatePicker(dateSetListener)
            }
        }

        binding.dobLayout.setEndIconOnClickListener {
            if (isEditMode) {
                showDatePicker(dateSetListener)
            }
        }
    }

    private fun showDatePicker(dateSetListener: DatePickerDialog.OnDateSetListener) {
        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateInView() {
        val format = "dd MMM yyyy"
        val sdf = SimpleDateFormat(format, Locale.US)
        binding.etDob.setText(sdf.format(calendar.time))
    }

    private fun saveUserData() {
        val username = binding.etUsername.text.toString()
        val email = binding.etEmail.text.toString()
        val programStudy = binding.etProgramStudy.text.toString()
        val nim = binding.etNIM.text.toString()
        val dob = binding.etDob.text.toString()
        val phone = binding.etPhone.text.toString()
        val password = binding.etPassword.text.toString()

        binding.tvUsername.text = username
    }
}
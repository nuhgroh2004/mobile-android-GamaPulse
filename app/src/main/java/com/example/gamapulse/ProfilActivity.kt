package com.example.gamapulse

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.gamapulse.databinding.ActivityProfilBinding
import com.example.gamapulse.model.ProfileResponse
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfilActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfilBinding
    private val calendar = Calendar.getInstance()
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadProfileFromApi()
        setupBackButton()
        setupEditButton()
        setupDatePicker()
        setupLogoutButton()
    }

    private fun getAuthToken(): String? {
//        return "2|obe8R2DYU9pitzFyQy51ZzXIlU66P7dEWQrn00LL57578c10"
        val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("token", null)
    }

    private fun loadProfileFromApi() {
        val token = getAuthToken()
        if (token == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            navigateToLoginActivity()
            return
        }

        val authToken = "Bearer $token"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getProfile(authToken)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val profileData = response.body()
                        if (profileData != null) {
                            updateUIWithProfileData(profileData)
                        }
                    } else {
                        Toast.makeText(
                            this@ProfilActivity,
                            "Failed to load profile: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfilActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateUIWithProfileData(profileData: ProfileResponse) {
        val user = profileData.user
        val mahasiswa = profileData.mahasiswa

        binding.tvUsername.text = user.name
        binding.etUsername.setText(user.name)
        binding.etEmail.setText(user.email)
        binding.etProgramStudy.setText(mahasiswa.prodi)
        binding.etNIM.setText(mahasiswa.NIM)
        binding.etDob.setText(mahasiswa.tanggal_lahir)
        mahasiswa.nomor_hp?.let {
            binding.etPhone.setText(it.replace("+62", ""))
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            it.animate().alpha(0.5f).setDuration(100).withEndAction {
                it.animate().alpha(1f).setDuration(100).start()
                finish()
            }.start()
        }
    }

    private fun setupEditButton() {
        binding.btnUpdateProfile.setOnClickListener {
            if (!isEditMode) {
                isEditMode = true
                binding.btnUpdateProfile.text = "Batal"
                binding.btnUpdateProfile.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                enableEditMode(true)
                binding.btnLogOut.visibility = View.GONE // Hide logout button
            } else {
                isEditMode = false
                binding.btnUpdateProfile.text = "Ubah Profile"
                binding.btnUpdateProfile.setBackgroundColor(ContextCompat.getColor(this, R.color.blue))
                enableEditMode(false)
                loadProfileFromApi()
                binding.btnLogOut.visibility = View.VISIBLE // Show logout button
            }
        }

        binding.btnSave.setOnClickListener {
            showSaveConfirmation()
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogOut.setOnClickListener {
            val dialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Konfirmasi Logout")
                .setContentText("Apakah Anda yakin ingin keluar dari akun?")
                .setCancelText("Batal")
                .setConfirmText("Keluar")
                .showCancelButton(true)
                .setConfirmClickListener { sDialog ->
                    sDialog.dismissWithAnimation()

                    // Clear token when logging out
                    clearAuthToken()

                    val successDialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Berhasil!")
                        .setContentText("Anda telah berhasil logout.")
                        .setConfirmClickListener { it ->
                            it.dismissWithAnimation()
                            navigateToLoginActivity()
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

    private fun clearAuthToken() {
        val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("token").apply()
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showSaveConfirmation() {
        val dialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Konfirmasi")
            .setContentText("Apakah Anda yakin ingin menyimpan perubahan profil?")
            .setCancelText("Batal")
            .setConfirmText("Simpan")
            .showCancelButton(true)
            .setConfirmClickListener { sDialog ->
                saveUserData()
                sDialog.dismissWithAnimation()

                val successDialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Berhasil!")
                    .setContentText("Profil berhasil diperbarui")
                    .setConfirmClickListener { it ->
                        it.dismissWithAnimation()
                        isEditMode = false
                        binding.btnUpdateProfile.text = "Ubah Profile"
                        binding.btnUpdateProfile.setBackgroundColor(ContextCompat.getColor(this, R.color.blue))
                        enableEditMode(false)
                        binding.btnLogOut.visibility = View.VISIBLE // Show logout button
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

    private fun styleAlertButtons(dialog: SweetAlertDialog) {
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

    private fun styleConfirmButton(dialog: SweetAlertDialog) {
        dialog.getButton(SweetAlertDialog.BUTTON_CONFIRM)?.apply {
            background = resources.getDrawable(R.drawable.allert_button_ok, theme)
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
            ).toInt()
            backgroundTintList = null
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
        // In a real implementation, you would call an API to update the profile
    }
}
package com.example.gamapulse

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.gamapulse.databinding.ActivityProfilBinding
import com.example.gamapulse.model.ProfileResponse
import com.example.gamapulse.model.UpdateProfileRequest
import com.example.gamapulse.network.ApiClient
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

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

        val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
            .setTitleText("Loading Profile")
            .setContentText("Please wait...")
        loadingDialog.show()

        val authToken = "Bearer $token"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getProfile(authToken)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismissWithAnimation()
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
                    loadingDialog.dismissWithAnimation()
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

        // Format date from API (yyyy-MM-dd) to display format (dd MMM yyyy)
        val formattedDate = convertApiDateToDisplayFormat(mahasiswa.tanggal_lahir)
        binding.etDob.setText(formattedDate)

        mahasiswa.nomor_hp?.let {
            binding.etPhone.setText(it)
        }
    }

    private fun convertApiDateToDisplayFormat(apiDate: String): String {
        try {
            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
            val date = apiFormat.parse(apiDate) ?: return apiDate
            return displayFormat.format(date)
        } catch (e: Exception) {
            Log.e("DateConversion", "Error converting API date to display format", e)
            return apiDate
        }
    }

    private fun convertDisplayDateToApiFormat(displayDate: String): String {
        try {
            val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = displayFormat.parse(displayDate) ?: return ""
            return apiFormat.format(date)
        } catch (e: Exception) {
            Log.e("DateConversion", "Error converting display date to API format", e)
            return ""
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
                binding.btnLogOut.visibility = View.GONE
            } else {
                isEditMode = false
                binding.btnUpdateProfile.text = "Ubah Profile"
                binding.btnUpdateProfile.setBackgroundColor(ContextCompat.getColor(this, R.color.blue))
                enableEditMode(false)
                loadProfileFromApi()
                binding.btnLogOut.visibility = View.VISIBLE
            }
        }

        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                showSaveConfirmation()
            }
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
                    val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
                        .setTitleText("Proses Logout")
                        .setContentText("Mohon tunggu...")
                    loadingDialog.show()
                    logoutFromApi(loadingDialog)
                }
                .setCancelClickListener { sDialog ->
                    sDialog.dismissWithAnimation()
                }
            dialog.show()
            styleAlertButtons(dialog)
        }
    }

    private fun logoutFromApi(loadingDialog: SweetAlertDialog) {
        val token = getAuthToken()
        if (token == null) {
            performLocalLogout()
            loadingDialog.dismissWithAnimation()
            return
        }

        val authToken = "Bearer $token"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.logout(authToken)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismissWithAnimation()
                    if (response.isSuccessful) {
                        performLocalLogout()
                    } else {
                        Toast.makeText(
                            this@ProfilActivity,
                            "Logout failed on server but logged out locally",
                            Toast.LENGTH_SHORT
                        ).show()
                        performLocalLogout()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismissWithAnimation()
                    Log.e("LogoutAPI", "Error during logout", e)
                    Toast.makeText(
                        this@ProfilActivity,
                        "Error: ${e.message}, logged out locally",
                        Toast.LENGTH_SHORT
                    ).show()
                    performLocalLogout()
                }
            }
        }
    }

    private fun performLocalLogout() {
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

    private fun validateInputs(): Boolean {
        var isValid = true

        // Email validation
        val email = binding.etEmail.text.toString()
        if (email.isEmpty() || !Pattern.matches("^[a-zA-Z0-9._%+-]+@mail\\.ugm\\.ac\\.id$", email)) {
            binding.emailLayout.error = "Email harus menggunakan format @mail.ugm.ac.id"
            isValid = false
        } else {
            binding.emailLayout.error = null
        }

        // Program Study validation
        val prodi = binding.etProgramStudy.text.toString()
        if (prodi.isEmpty()) {
            binding.programStudyLayout.error = "Program studi tidak boleh kosong"
            isValid = false
        } else {
            binding.programStudyLayout.error = null
        }

        // DOB validation
        val dob = binding.etDob.text.toString()
        if (dob.isEmpty()) {
            binding.dobLayout.error = "Tanggal lahir tidak boleh kosong"
            isValid = false
        } else {
            binding.dobLayout.error = null
        }

        // NIM validation
        val nim = binding.etNIM.text.toString()
        if (nim.isEmpty() || !Pattern.matches("^\\d{2}/\\d{6}/[A-Za-z]{2}/\\d{5}$", nim)) {
            binding.nimLayout.error = "Format NIM tidak valid (xx/xxxxxx/XX/xxxxx)"
            isValid = false
        } else {
            binding.nimLayout.error = null
        }

        // Phone validation
        val phone = binding.etPhone.text.toString()
        if (phone.isNotEmpty() && !Pattern.matches("^\\d{11}$", phone)) {
            binding.phoneLayout.error = "Nomor telepon harus 11 digit angka"
            isValid = false
        } else {
            binding.phoneLayout.error = null
        }

        // Password validation
        val password = binding.etPassword.text.toString()
        if (password.isNotEmpty() && (password.length < 8 || !Pattern.matches("^(?=.*[A-Za-z])(?=.*\\d).+$", password))) {
            binding.passwordLayout.error = "Password minimal 8 karakter, mengandung huruf dan angka"
            isValid = false
        } else {
            binding.passwordLayout.error = null
        }

        return isValid
    }

    private fun showSaveConfirmation() {
        val dialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Konfirmasi")
            .setContentText("Apakah Anda yakin ingin menyimpan perubahan profil?")
            .setCancelText("Batal")
            .setConfirmText("Simpan")
            .showCancelButton(true)
            .setConfirmClickListener { sDialog ->
                sDialog.dismissWithAnimation()

                // Show loading dialog while saving
                val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
                    .setTitleText("Updating Profile")
                    .setContentText("Please wait...")
                loadingDialog.show()

                saveUserData(loadingDialog)
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

        cancelButton?.apply {
            background = resources.getDrawable(R.drawable.allert_button_cancel, theme)
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
            ).toInt()
            backgroundTintList = null
        }

        confirmButton?.apply {
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
        binding.etUsername.isEnabled = enabled
        binding.etEmail.isEnabled = enabled
        binding.etProgramStudy.isEnabled = enabled
        binding.etNIM.isEnabled = enabled
        binding.etDob.isEnabled = enabled
        binding.etPhone.isEnabled = enabled

        val visibility = if (enabled) View.VISIBLE else View.GONE
        binding.passwordLayout.visibility = visibility
        binding.btnSave.visibility = visibility

        // Better password field handling
        if (enabled) {
            binding.etPassword.setText("********")
            binding.passwordLayout.hint = "Password"
            binding.passwordLayout.helperText = "Ketuk untuk mengganti password"
            binding.passwordLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            binding.passwordLayout.setEndIconDrawable(R.drawable.ic_edit)

            // Make password field look like it contains existing data
            binding.etPassword.setTextColor(ContextCompat.getColor(this, R.color.black))

            binding.passwordLayout.setEndIconOnClickListener {
                binding.passwordLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                binding.etPassword.setText("")
                binding.etPassword.requestFocus()
                binding.passwordLayout.helperText = "Min. 8 karakter, kombinasi huruf dan angka"
            }
        }
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

    private fun saveUserData(dialog: SweetAlertDialog) {
        val name = binding.etUsername.text.toString()
        val email = binding.etEmail.text.toString()
        val prodi = binding.etProgramStudy.text.toString()
        val nim = binding.etNIM.text.toString()

        // Convert display date format (DD MMM YYYY) back to API format (YYYY-MM-DD)
        val displayDate = binding.etDob.text.toString()
        val tanggalLahir = convertDisplayDateToApiFormat(displayDate)

        val phoneNumber = binding.etPhone.text.toString()
        val password = binding.etPassword.text.toString()
        val passwordToSend = if (password.isNotEmpty()) password else null

        // Call API
        updateProfileToApi(name, email, prodi, tanggalLahir, phoneNumber, nim, passwordToSend, dialog)

        // Update the displayed username
        binding.tvUsername.text = name
    }

    private fun updateProfileToApi(
        name: String,
        email: String,
        prodi: String,
        tanggalLahir: String,
        phoneNumber: String,
        nim: String,
        password: String?,
        dialog: SweetAlertDialog
    ) {
        val token = getAuthToken()
        if (token == null) {
            dialog.dismissWithAnimation()
            Toast.makeText(this, "Authorization error. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val authToken = "Bearer $token"
        val request = UpdateProfileRequest(name, email, prodi, tanggalLahir, phoneNumber, nim, password)

        Log.d("ProfileUpdate", "Sending update: name=$name, email=$email, prodi=$prodi, tanggal=$tanggalLahir, phone=$phoneNumber, nim=$nim")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.updateProfile(authToken, request)
                withContext(Dispatchers.Main) {
                    dialog.dismissWithAnimation()
                    if (response.isSuccessful) {
                        Log.d("ProfileUpdate", "Profile updated successfully")
                        val successDialog = SweetAlertDialog(this@ProfilActivity, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Berhasil!")
                            .setContentText("Profil berhasil diperbarui")
                            .setConfirmClickListener { it ->
                                it.dismissWithAnimation()
                                isEditMode = false
                                binding.btnUpdateProfile.text = "Ubah Profile"
                                binding.btnUpdateProfile.setBackgroundColor(ContextCompat.getColor(this@ProfilActivity, R.color.blue))
                                enableEditMode(false)
                                binding.btnLogOut.visibility = View.VISIBLE
                            }
                        successDialog.show()
                        styleConfirmButton(successDialog)
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("ProfileUpdate", "Failed to update profile: $errorBody")

                        val errorDialog = SweetAlertDialog(this@ProfilActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText("Failed to update profile: $errorBody")
                            .setConfirmText("OK")
                        errorDialog.show()
                        styleConfirmButton(errorDialog)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dialog.dismissWithAnimation()
                    Log.e("ProfileUpdate", "Exception during profile update", e)

                    val errorDialog = SweetAlertDialog(this@ProfilActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("Error: ${e.message}")
                        .setConfirmText("OK")
                    errorDialog.show()
                    styleConfirmButton(errorDialog)
                }
            }
        }
    }
}
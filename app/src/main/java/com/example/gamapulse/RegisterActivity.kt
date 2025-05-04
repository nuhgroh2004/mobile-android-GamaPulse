package com.example.gamapulse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gamapulse.model.RegisterRequest
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class RegisterActivity : AppCompatActivity() {
    private lateinit var etUsername: EditText
    private lateinit var etEmailUgm: EditText
    private lateinit var etProdi: EditText
    private lateinit var etTanggalLahir: EditText
    private lateinit var etNomorTelepon: EditText
    private lateinit var etNIM: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnTogglePassword: ImageButton
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initializeViews()
        setupPasswordVisibility()
        btnRegister.foreground = getRippleDrawable(getColor(R.color.teal))
        btnRegister.setOnClickListener { validateAndRegister() }
        setupButtonWithAnimation(findViewById(R.id.tvSignIn), LoginActivity::class.java)
        setupButtonWithAnimation(findViewById(R.id.btnBack), SparseScreenActivity::class.java)
    }

    private fun initializeViews() {
        etUsername = findViewById(R.id.etUsername)
        etEmailUgm = findViewById(R.id.etEmailUgm)
        etProdi = findViewById(R.id.etProdi)
        etTanggalLahir = findViewById(R.id.etTanggalLahir)
        etNomorTelepon = findViewById(R.id.etNomorTelepon)
        etNIM = findViewById(R.id.etNIM)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
    }

    private fun setupPasswordVisibility() {
        btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        btnTogglePassword.background = null
        btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            btnTogglePassword.setImageResource(R.drawable.ic_visibility)
        } else {
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        }
        etPassword.setSelection(etPassword.text.length)
    }

    private fun validateAndRegister() {
        val name = etUsername.text.toString().trim()
        val email = etEmailUgm.text.toString().trim()
        val prodi = etProdi.text.toString().trim()
        val tanggalLahir = etTanggalLahir.text.toString().trim()
        val phoneNumber = etNomorTelepon.text.toString().trim()
        val nim = etNIM.text.toString().trim()
        val password = etPassword.text.toString().trim()
        if (name.isEmpty() || email.isEmpty() || prodi.isEmpty() ||
            tanggalLahir.isEmpty() || phoneNumber.isEmpty() ||
            nim.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }
        if (!email.endsWith("@mail.ugm.ac.id") && !email.endsWith("@ugm.ac.id")) {
            Toast.makeText(this, "Please use a valid UGM email", Toast.LENGTH_SHORT).show()
            return
        }
        val nimRegex = "^\\d{2}/\\d{6}/[A-Za-z]{2}/\\d{5}$".toRegex()
        if (!nimRegex.matches(nim)) {
            Toast.makeText(this, "NIM format should be XX/XXXXXX/AA/XXXXX", Toast.LENGTH_SHORT).show()
            return
        }
        val registerRequest = RegisterRequest(
            name = name,
            email = email,
            prodi = prodi,
            tanggal_lahir = tanggalLahir,
            phone_number = phoneNumber,
            nim = nim,
            password = password
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.register(registerRequest)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val registerResponse = response.body()
                        println("REGISTER SUCCESS: ${registerResponse?.message}")
                        if (registerResponse?.token != null) {
                            saveAuthToken(registerResponse.token)
                        }
                        Toast.makeText(this@RegisterActivity,
                            "Registration successful", Toast.LENGTH_SHORT).show()
                        navigateToMainActivity()
                    } else {
                        val errorMsg = try {
                            response.errorBody()?.string() ?: "Unknown error"
                        } catch (e: Exception) {
                            "Error: ${response.code()}"
                        }
                        Toast.makeText(this@RegisterActivity,
                            "Registration Failed: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveAuthToken(token: String) {
        val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("token", token)
            apply()
        }
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
}
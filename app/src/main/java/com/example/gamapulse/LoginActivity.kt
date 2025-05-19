package com.example.gamapulse

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gamapulse.model.LoginRequest
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    /* ----------------------------- Lifecycle Methods ----------------------------- */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        // Make status bar white with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        initializeViews()
        setupPasswordToggle()
        setupBackButton()
        setupLoginButton()
        setupSignUpNavigation()
    }
    /* ----------------------------- End Lifecycle Methods ----------------------------- */

    /* ----------------------------- Session Management ----------------------------- */
    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        return !token.isNullOrEmpty()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    /* ----------------------------- End Session Management ----------------------------- */

    /* ----------------------------- View Initialization ----------------------------- */
    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
    }
    /* ----------------------------- End View Initialization ----------------------------- */

    /* ----------------------------- Password Toggle ----------------------------- */
    private fun setupPasswordToggle() {
        val btnTogglePassword = findViewById<ImageButton>(R.id.btnTogglePassword)
        btnTogglePassword.setOnClickListener {
            if (etPassword.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }
            etPassword.setSelection(etPassword.text.length)
        }
    }
    /* ----------------------------- End Password Toggle ----------------------------- */

    /* ----------------------------- Navigation ----------------------------- */
    private fun setupBackButton() {
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            val intent = Intent(this, SparseScreenActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupSignUpNavigation() {
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToLoading() {
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)
        finish()
    }
    /* ----------------------------- End Navigation ----------------------------- */

    /* ----------------------------- Login Functionality ----------------------------- */
    private fun setupLoginButton() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                Toast.makeText(this, "Email dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun login(email: String, password: String) {
        val loadingDialog = ProgressDialog(this).apply {
            setMessage("Sedang masuk...")
            setCancelable(false)
            show()
        }
        btnLogin.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.login(LoginRequest(email, password))
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    btnLogin.isEnabled = true
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            saveToken(responseBody.token)
                            Toast.makeText(this@LoginActivity, "Berhasil masuk", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Respon kosong dari server", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Gagal masuk: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    btnLogin.isEnabled = true
                    Toast.makeText(this@LoginActivity, "Koneksi gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    /* ----------------------------- End Login Functionality ----------------------------- */

    /* ----------------------------- Data Storage ----------------------------- */
    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        editor.apply()
    }
    /* ----------------------------- End Data Storage ----------------------------- */
}
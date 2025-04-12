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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // parameter view password icon
        etPassword = findViewById(R.id.etPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        btnTogglePassword.background = null
        btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
        // parameter view password icon

        findViewById<Button>(R.id.btnLogin).foreground = getRippleDrawable(getColor(R.color.teal))
        // Modified to go to LoadingActivity first
        setupButtonWithAnimation(findViewById(R.id.btnLogin), LoadingActivity::class.java)
        setupButtonWithAnimation(findViewById(R.id.tvSignUp), RegisterActivity::class.java)
        setupButtonWithAnimation(findViewById(R.id.btnBack), SparseScreenActivity::class.java)
    }
    // Funsi view password
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
    // Funsi view password

    // Fungsi untuk mengatur animasi tombol
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
    // Fungsi untuk mengatur animasi tombol

}
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

class SparseScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sparse_screen)
        // Setup animasi
        findViewById<Button>(R.id.btnLogin).foreground = getRippleDrawable(getColor(R.color.teal))
        findViewById<Button>(R.id.btnNewUser).foreground = getRippleDrawable(android.R.color.white)
        // menngarhkan tombol
        setupButtonWithAnimation(findViewById(R.id.btnLogin), LoginActivity::class.java)
        setupButtonWithAnimation(findViewById(R.id.btnNewUser), RegisterActivity::class.java)
    }

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
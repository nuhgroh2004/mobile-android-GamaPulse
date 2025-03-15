package com.example.gamapulse

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class FirstWindowActivity : AppCompatActivity() {

    // Waktu tampilan splash screen (dalam milidetik)
    private val splashDuration: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengatur status bar agar transparan dan icon status bar berwarna gelap (jika background terang)
        setTransparentStatusBar()

        enableEdgeToEdge()
        setContentView(R.layout.activity_first_window)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Mendapatkan referensi ke elemen UI
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val appTitle = findViewById<TextView>(R.id.appTitle)
        val appTagline = findViewById<TextView>(R.id.appTagline)

        // Memulai animasi
        playEntranceAnimation(logoImage, appTitle, appTagline)

        // Menjalankan exit animation dan pindah ke activity berikutnya setelah waktu tertentu
        Handler(Looper.getMainLooper()).postDelayed({
            playExitAnimation(logoImage, appTitle, appTagline)
        }, splashDuration)
    }

    private fun setTransparentStatusBar() {
        // Membuat status bar transparan
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Mengaktifkan edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Mengatur ikon status bar berwarna gelap (untuk background terang)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        // Alternatif cara setting status bar color untuk Android M (API 23) ke atas
        // window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
    }

    private fun playEntranceAnimation(logoImage: ImageView, appTitle: TextView, appTagline: TextView) {
        // Animasi untuk logo
        val logoFadeIn = ObjectAnimator.ofFloat(logoImage, View.ALPHA, 0f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }

        val logoScale = ObjectAnimator.ofFloat(logoImage, View.SCALE_X, 0.8f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }

        val logoScaleY = ObjectAnimator.ofFloat(logoImage, View.SCALE_Y, 0.8f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }

        // Animasi untuk judul
        val titleFadeIn = ObjectAnimator.ofFloat(appTitle, View.ALPHA, 0f, 1f).apply {
            duration = 600
            startDelay = 400
            interpolator = AccelerateDecelerateInterpolator()
        }

        val titleSlideUp = ObjectAnimator.ofFloat(appTitle, View.TRANSLATION_Y, 50f, 0f).apply {
            duration = 600
            startDelay = 400
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Animasi untuk tagline
        val taglineFadeIn = ObjectAnimator.ofFloat(appTagline, View.ALPHA, 0f, 1f).apply {
            duration = 600
            startDelay = 600
            interpolator = AccelerateDecelerateInterpolator()
        }

        val taglineSlideUp = ObjectAnimator.ofFloat(appTagline, View.TRANSLATION_Y, 50f, 0f).apply {
            duration = 600
            startDelay = 600
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Menjalankan semua animasi secara bersamaan
        AnimatorSet().apply {
            playTogether(
                logoFadeIn, logoScale, logoScaleY,
                titleFadeIn, titleSlideUp,
                taglineFadeIn, taglineSlideUp
            )
            start()
        }
    }

    private fun playExitAnimation(logoImage: ImageView, appTitle: TextView, appTagline: TextView) {
        // Animasi untuk logo
        val logoFadeOut = ObjectAnimator.ofFloat(logoImage, View.ALPHA, 1f, 0f).apply {
            duration = 500
            startDelay = 100
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Animasi untuk judul
        val titleFadeOut = ObjectAnimator.ofFloat(appTitle, View.ALPHA, 1f, 0f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Animasi untuk tagline
        val taglineFadeOut = ObjectAnimator.ofFloat(appTagline, View.ALPHA, 1f, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Menjalankan semua animasi keluar secara bersamaan
        val animatorSet = AnimatorSet().apply {
            playTogether(logoFadeOut, titleFadeOut, taglineFadeOut)
            start()
        }

        // Pindah ke activity berikutnya setelah animasi keluar selesai
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Ganti MainActivity dengan activity tujuan Anda
                val intent = Intent(this@FirstWindowActivity, SparseScreenActivity::class.java)
                startActivity(intent)

                // Menambahkan transisi halus antar activity
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                // Mengakhiri splash screen activity
                finish()
            }
        })
    }
}
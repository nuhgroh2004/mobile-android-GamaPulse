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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class FirstWindowActivity : AppCompatActivity() {
    private val splashDuration: Long = 3000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransparentStatusBar()
        enableEdgeToEdge()
        setContentView(R.layout.activity_first_window)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val appTitle = findViewById<TextView>(R.id.appTitle)
        val appTagline = findViewById<TextView>(R.id.appTagline)
        playEntranceAnimation(logoImage, appTitle, appTagline)
        Handler(Looper.getMainLooper()).postDelayed({
            playExitAnimation(logoImage, appTitle, appTagline)
        }, splashDuration)
    }

    private fun setTransparentStatusBar() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
    }

    private fun playEntranceAnimation(logoImage: ImageView, appTitle: TextView, appTagline: TextView) {
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
        val logoFadeOut = ObjectAnimator.ofFloat(logoImage, View.ALPHA, 1f, 0f).apply {
            duration = 500
            startDelay = 100
            interpolator = AccelerateDecelerateInterpolator()
        }
        val titleFadeOut = ObjectAnimator.ofFloat(appTitle, View.ALPHA, 1f, 0f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
        }
        val taglineFadeOut = ObjectAnimator.ofFloat(appTagline, View.ALPHA, 1f, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
        val animatorSet = AnimatorSet().apply {
            playTogether(logoFadeOut, titleFadeOut, taglineFadeOut)
            start()
        }
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                val intent = Intent(this@FirstWindowActivity, SparseScreenActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        })
    }
}
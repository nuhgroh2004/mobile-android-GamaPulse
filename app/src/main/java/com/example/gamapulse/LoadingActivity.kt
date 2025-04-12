package com.example.gamapulse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat

class LoadingActivity : AppCompatActivity() {
    private lateinit var squareTopLeft: View
    private lateinit var squareTopRight: View
    private lateinit var squareBottomLeft: View
    private lateinit var squareBottomRight: View
    private lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set status bar color to match background
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        setContentView(R.layout.activity_loading)
        squareTopLeft = findViewById(R.id.square_top_left)
        squareTopRight = findViewById(R.id.square_top_right)
        squareBottomLeft = findViewById(R.id.square_bottom_left)
        squareBottomRight = findViewById(R.id.square_bottom_right)
        loadingText = findViewById(R.id.loading_text)
        startLoadingAnimation()

        // Ensure we transition to MainActivity after a fixed time
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMainActivity()
        }, 2000) // 5 seconds timeout
    }

    private fun startLoadingAnimation() {
        val animDuration = 1500L
        val topLeftScale = createScaleAnimator(squareTopLeft, 0.8f, 1.0f, animDuration)
        val topLeftAlpha = createAlphaAnimator(squareTopLeft, 0.5f, 1.0f, animDuration)
        val topRightScale = createScaleAnimator(squareTopRight, 0.8f, 1.0f, animDuration)
        val topRightAlpha = createAlphaAnimator(squareTopRight, 0.5f, 1.0f, animDuration)
        val bottomLeftScale = createScaleAnimator(squareBottomLeft, 0.8f, 1.0f, animDuration)
        val bottomLeftAlpha = createAlphaAnimator(squareBottomLeft, 0.5f, 1.0f, animDuration)
        val bottomRightScale = createScaleAnimator(squareBottomRight, 0.8f, 1.0f, animDuration)
        val bottomRightAlpha = createAlphaAnimator(squareBottomRight, 0.5f, 1.0f, animDuration)
        val textAnimator = createLoadingTextAnimator(loadingText)

        val set1 = AnimatorSet().apply {
            playTogether(topLeftScale, topLeftAlpha)
        }
        val set2 = AnimatorSet().apply {
            playTogether(topRightScale, topRightAlpha)
            startDelay = animDuration / 6
        }
        val set3 = AnimatorSet().apply {
            playTogether(bottomRightScale, bottomRightAlpha)
            startDelay = animDuration / 3
        }
        val set4 = AnimatorSet().apply {
            playTogether(bottomLeftScale, bottomLeftAlpha)
            startDelay = animDuration / 2
        }

        val mainSet = AnimatorSet().apply {
            playTogether(set1, set2, set3, set4, textAnimator)
        }

        mainSet.doOnEnd {
            navigateToMainActivity()
        }
        mainSet.start()
    }

    private fun navigateToMainActivity() {
        // Check if activity is not finishing to avoid crashes
        if (!isFinishing && !isDestroyed) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }

    private fun createScaleAnimator(view: View, start: Float, end: Float, duration: Long): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, start, end, start)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, start, end, start)
        return AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun createAlphaAnimator(view: View, start: Float, end: Float, duration: Long): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, View.ALPHA, start, end, start).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun createLoadingTextAnimator(textView: TextView): ValueAnimator {
        return ValueAnimator.ofFloat(0.7f, 1.0f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                textView.alpha = value
            }
        }
    }
}
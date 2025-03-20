package com.example.gamapulse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gamapulse.databinding.ActivityTaskLogBinding
import java.util.concurrent.TimeUnit

class TaskLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskLogBinding
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    private var isTimerRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var targetTimeInSeconds = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTaskLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop + systemBars.top,
                v.paddingRight,
                v.paddingBottom + systemBars.bottom
            )
            insets
        }
        setupTimePickers()
        setupButtonAnimations()
        setupListeners()
    }
    private fun setupButtonAnimations() {
        binding.btnCreateTarget.foreground = getRippleDrawable(getColor(R.color.teal))
        binding.btnAddTarget.foreground = getRippleDrawable(getColor(R.color.teal))
        binding.btnStartPause.foreground = getRippleDrawable(getColor(R.color.teal))
        binding.btnFinish.foreground = getRippleDrawable(getColor(R.color.blue))
        binding.btnBack.foreground = getRippleDrawable(getColor(R.color.teal))
    }
    private fun setupTimePickers() {
        binding.hoursPicker.apply {
            minValue = 0
            maxValue = 24
            wrapSelectorWheel = false
            setFormatter { value -> String.format("%02d", value) }
        }
        binding.minutesPicker.apply {
            minValue = 0
            maxValue = 59
            wrapSelectorWheel = true
            setFormatter { value -> String.format("%02d", value) }
        }
        binding.secondsPicker.apply {
            minValue = 0
            maxValue = 59
            wrapSelectorWheel = true
            setFormatter { value -> String.format("%02d", value) }
        }
    }
    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            animateButtonClick(it) {
                finish()
            }
        }
        binding.btnCreateTarget.setOnClickListener {
            animateButtonClick(it) {
                showTargetInputForm()
            }
        }
        binding.btnAddTarget.setOnClickListener {
            animateButtonClick(it) {
                addNewTarget()
            }
        }
        binding.btnStartPause.setOnClickListener {
            animateButtonClick(it) {
                toggleTimer()
            }
        }
        binding.btnFinish.setOnClickListener {
            animateButtonClick(it) {
                showFinishConfirmationDialog()
            }
        }
    }
    private fun animateButtonClick(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            view.postDelayed({
                action.invoke()
            }, 150)
        }.start()
    }
    private fun getRippleDrawable(color: Int): RippleDrawable {
        return RippleDrawable(
            ColorStateList.valueOf(getColor(R.color.ripple_color)),
            null,
            ColorDrawable(color)
        )
    }
    private fun showTargetInputForm() {
        binding.btnCreateTarget.visibility = View.GONE
        binding.timePickerContainer.visibility = View.VISIBLE
        binding.btnAddTarget.visibility = View.VISIBLE
    }
    private fun addNewTarget() {
        val hours = binding.hoursPicker.value
        val minutes = binding.minutesPicker.value
        val seconds = binding.secondsPicker.value
        if (hours == 24 && (minutes > 0 || seconds > 0)) {
            showAlert("Waktu tidak boleh lebih dari 24 jam")
            return
        }
        if (hours == 0 && minutes == 0 && seconds == 0) {
            showAlert("Target waktu tidak boleh 0")
            return
        }
        targetTimeInSeconds = (hours * 3600 + minutes * 60 + seconds).toLong()
        val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        binding.timePickerContainer.visibility = View.GONE
        binding.btnAddTarget.visibility = View.GONE
        displayCurrentTarget(formattedTime)
        binding.llTimerControls.visibility = View.VISIBLE
        resetTimer()
    }
    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.custom_alert_dialog, null)

        val tvMessage = dialogView.findViewById<TextView>(R.id.tvAlertMessage)
        val btnOk = dialogView.findViewById<Button>(R.id.btnAlertOk)
        val imgResult = dialogView.findViewById<ImageView>(R.id.imgResult)

        tvMessage.text = message
        imgResult.setImageResource(R.drawable.ic_warning)

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun displayCurrentTarget(formattedTime: String) {
        binding.tvCurrentTargetTime.text = "Target: $formattedTime"
        binding.cvCurrentTarget.visibility = View.VISIBLE
    }
    private fun resetTimer() {
        startTime = 0L
        elapsedTime = 0L
        isTimerRunning = false
        updateTimerDisplay(0)
        binding.btnStartPause.text = "Mulai"
        binding.btnStartPause.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.green, theme))
    }
    private fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
            binding.btnStartPause.text = "Lanjutkan"
            binding.btnStartPause.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.green, theme))
            binding.btnStartPause.foreground = getRippleDrawable(getColor(R.color.green))
        } else {
            startTimer()
            binding.btnStartPause.text = "Jeda"
            binding.btnStartPause.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.orange, theme))
        }
    }
    private fun startTimer() {
        if (startTime == 0L) {
            startTime = SystemClock.elapsedRealtime()
        } else {
            startTime = SystemClock.elapsedRealtime() - elapsedTime
        }
        isTimerRunning = true
        handler.post(object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    elapsedTime = SystemClock.elapsedRealtime() - startTime
                    updateTimerDisplay(elapsedTime)
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }
    private fun pauseTimer() {
        isTimerRunning = false
    }
    private fun updateTimerDisplay(elapsedMillis: Long) {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        val seconds = totalSeconds % 60
        binding.tvTimer.text = String.format("%02d : %02d : %02d", hours, minutes, seconds)
    }
    private fun showFinishConfirmationDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.custom_confirmation_dialog, null)

        val tvMessage = dialogView.findViewById<TextView>(R.id.tvConfirmMessage)
        val btnYes = dialogView.findViewById<Button>(R.id.btnConfirmYes)
        val btnNo = dialogView.findViewById<Button>(R.id.btnConfirmNo)

        tvMessage.text = "Apakah Anda yakin ingin menyelesaikan tugas ini?"

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnYes.setOnClickListener {
            dialog.dismiss()
            finishTask()
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun showResultDialog(resultMessage: String) {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.custom_result_dialog, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvResultTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvResultMessage)
        val btnOk = dialogView.findViewById<Button>(R.id.btnResultOk)
        val imgResult = dialogView.findViewById<ImageView>(R.id.imgResult)

        tvTitle.text = "Hasil"
        tvMessage.text = resultMessage

        // Set appropriate icon based on result
        if (resultMessage.contains("Selamat!")) {
            imgResult.setImageResource(R.drawable.ic_success)
        } else {
            imgResult.setImageResource(R.drawable.ic_warning)
        }

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnOk.setOnClickListener {
            dialog.dismiss()
            resetToInitialState()
        }

        dialog.show()
    }
    private fun finishTask() {
        if (isTimerRunning) {
            pauseTimer()
        }

        val elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
        val isTargetAchieved = elapsedSeconds >= targetTimeInSeconds
        val resultMessage = if (isTargetAchieved) {
            "Selamat! Target tercapai."
        } else {
            "Target belum tercapai. Anda masih kurang ${formatTimeDifference(targetTimeInSeconds - elapsedSeconds)}"
        }

        showResultDialog(resultMessage)
    }
    private fun formatTimeDifference(diffSeconds: Long): String {
        val hours = TimeUnit.SECONDS.toHours(diffSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(diffSeconds) % 60
        val seconds = diffSeconds % 60
        return if (hours > 0) {
            String.format("%d jam %d menit %d detik", hours, minutes, seconds)
        } else if (minutes > 0) {
            String.format("%d menit %d detik", minutes, seconds)
        } else {
            String.format("%d detik", seconds)
        }
    }
    private fun resetToInitialState() {
        binding.cvCurrentTarget.visibility = View.GONE
        binding.llTimerControls.visibility = View.GONE
        resetTimer()
        binding.btnCreateTarget.visibility = View.VISIBLE
        binding.hoursPicker.value = 0
        binding.minutesPicker.value = 0
        binding.secondsPicker.value = 0
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
package com.example.gamapulse

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import cn.pedant.SweetAlert.SweetAlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gamapulse.databinding.ActivityTaskLogBinding
import com.example.gamapulse.model.StoreProgressRequest
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TaskLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskLogBinding
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    private var isTimerRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var targetTimeInSeconds = 0L
    private var elapsedTimeInSeconds = 0L

    /* ----------------------------- onCreate ----------------------------- */
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
    /* ----------------------------- onCreate ----------------------------- */

    /* ----------------------------- setupButtonAnimations ----------------------------- */
    private fun setupButtonAnimations() {
        binding.btnCreateTarget.foreground = getRippleDrawable(getColor(R.color.teal))
        binding.btnAddTarget.foreground = getRippleDrawable(getColor(R.color.teal))
        binding.btnStartPause.foreground = getRippleDrawable(getColor(R.color.teal))
        binding.btnFinish.foreground = getRippleDrawable(getColor(R.color.blue))
        binding.btnBack.foreground = getRippleDrawable(getColor(R.color.teal))
    }
    /* ----------------------------- setupButtonAnimations ----------------------------- */

    /* ----------------------------- setupTimePickers ----------------------------- */
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
    /* ----------------------------- setupTimePickers ----------------------------- */

    /* ----------------------------- setupListeners ----------------------------- */
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
    /* ----------------------------- setupListeners ----------------------------- */

    /* ----------------------------- animateButtonClick ----------------------------- */
    private fun animateButtonClick(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            view.postDelayed({
                action.invoke()
            }, 150)
        }.start()
    }
    /* ----------------------------- animateButtonClick ----------------------------- */

    /* ----------------------------- getRippleDrawable ----------------------------- */
    private fun getRippleDrawable(color: Int): RippleDrawable {
        return RippleDrawable(
            ColorStateList.valueOf(getColor(R.color.ripple_color)),
            null,
            ColorDrawable(color)
        )
    }
    /* ----------------------------- getRippleDrawable ----------------------------- */

    /* ----------------------------- showTargetInputForm ----------------------------- */
    private fun showTargetInputForm() {
        binding.btnCreateTarget.visibility = View.GONE
        binding.timePickerContainer.visibility = View.VISIBLE
        binding.btnAddTarget.visibility = View.VISIBLE
    }
    /* ----------------------------- showTargetInputForm ----------------------------- */

    /* ----------------------------- addNewTarget ----------------------------- */
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
    /* ----------------------------- addNewTarget ----------------------------- */

    /* ----------------------------- showAlert ----------------------------- */
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
    /* ----------------------------- showAlert ----------------------------- */

    /* ----------------------------- displayCurrentTarget ----------------------------- */
    private fun displayCurrentTarget(formattedTime: String) {
        binding.tvCurrentTargetTime.text = "Target: $formattedTime"
        binding.cvCurrentTarget.visibility = View.VISIBLE
    }
    /* ----------------------------- displayCurrentTarget ----------------------------- */

    /* ----------------------------- resetTimer ----------------------------- */
    private fun resetTimer() {
        startTime = 0L
        elapsedTime = 0L
        elapsedTimeInSeconds = 0L
        isTimerRunning = false
        updateTimerDisplay(0)
        binding.btnStartPause.text = "Mulai"
        binding.btnStartPause.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.green, theme))
    }
    /* ----------------------------- resetTimer ----------------------------- */

    /* ----------------------------- toggleTimer ----------------------------- */
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
    /* ----------------------------- toggleTimer ----------------------------- */

    /* ----------------------------- startTimer ----------------------------- */
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
                    elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
                    updateTimerDisplay(elapsedTime)
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }
    /* ----------------------------- startTimer ----------------------------- */

    /* ----------------------------- pauseTimer ----------------------------- */
    private fun pauseTimer() {
        isTimerRunning = false
    }
    /* ----------------------------- pauseTimer ----------------------------- */

    /* ----------------------------- updateTimerDisplay ----------------------------- */
    private fun updateTimerDisplay(elapsedMillis: Long) {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        val seconds = totalSeconds % 60
        binding.tvTimer.text = String.format("%02d : %02d : %02d", hours, minutes, seconds)
    }
    /* ----------------------------- updateTimerDisplay ----------------------------- */

    /* ----------------------------- showFinishConfirmationDialog ----------------------------- */
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
    /* ----------------------------- showFinishConfirmationDialog ----------------------------- */

    /* ----------------------------- showResultDialog ----------------------------- */
    private fun showResultDialog(resultMessage: String, isAchieved: Boolean) {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.custom_result_dialog, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvResultTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvResultMessage)
        val btnOk = dialogView.findViewById<Button>(R.id.btnResultOk)
        val imgResult = dialogView.findViewById<ImageView>(R.id.imgResult)
        tvTitle.text = "Hasil"
        tvMessage.text = resultMessage
        if (isAchieved) {
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
    /* ----------------------------- showResultDialog ----------------------------- */

    /* ----------------------------- finishTask ----------------------------- */
    private fun finishTask() {
        if (isTimerRunning) {
            pauseTimer()
        }
        val isTargetAchieved = elapsedTimeInSeconds >= targetTimeInSeconds
        saveTimerProgress(
            expectedSeconds = targetTimeInSeconds.toInt(),
            actualSeconds = elapsedTimeInSeconds.toInt(),
            isAchieved = isTargetAchieved
        )
        val resultMessage = if (isTargetAchieved) {
            "Selamat! Target tercapai."
        } else {
            "Target belum tercapai. Anda masih kurang ${formatTimeDifference(targetTimeInSeconds - elapsedTimeInSeconds)}"
        }
        showResultDialog(resultMessage, isTargetAchieved)
    }
    /* ----------------------------- finishTask ----------------------------- */

    /* ----------------------------- formatTimeDifference ----------------------------- */
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
    /* ----------------------------- formatTimeDifference ----------------------------- */

    /* ----------------------------- resetToInitialState ----------------------------- */
    private fun resetToInitialState() {
        binding.cvCurrentTarget.visibility = View.GONE
        binding.llTimerControls.visibility = View.GONE
        resetTimer()
        binding.btnCreateTarget.visibility = View.VISIBLE
        binding.hoursPicker.value = 0
        binding.minutesPicker.value = 0
        binding.secondsPicker.value = 0
    }
    /* ----------------------------- resetToInitialState ----------------------------- */

    /* ----------------------------- saveTimerProgress ----------------------------- */
    private fun saveTimerProgress(expectedSeconds: Int, actualSeconds: Int, isAchieved: Boolean) {
        val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "Menyimpan progress..."
        loadingDialog.setCancelable(false)
        loadingDialog.show()
        lifecycleScope.launch {
            try {
                val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
                val token = sharedPreferences.getString("token", null)
                if (token == null) {
                    loadingDialog.dismissWithAnimation()
                    SweetAlertDialog(this@TaskLogActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("Token autentikasi tidak ditemukan")
                        .show()
                    return@launch
                }
                Log.d("TaskLog", "Mengirim data: expected=$expectedSeconds, actual=$actualSeconds, achieved=$isAchieved")
                val request = StoreProgressRequest(
                    expectedTarget = expectedSeconds,
                    actualTarget = actualSeconds,
                    isAchieved = isAchieved
                )
                val authToken = "Bearer $token"
                val response = ApiClient.apiService.storeProgress(authToken, request)
                loadingDialog.dismissWithAnimation()
                if (response.isSuccessful) {
                    Log.d("TaskLog", "Berhasil menyimpan progress")
                    SweetAlertDialog(this@TaskLogActivity, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Berhasil!")
                        .setContentText("Progress berhasil disimpan")
                        .setConfirmClickListener { it.dismissWithAnimation() }
                        .show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("TaskLog", "Error response code: ${response.code()}")
                    Log.e("TaskLog", "Error body: $errorBody")
                    SweetAlertDialog(this@TaskLogActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Gagal!")
                        .setContentText("Gagal menyimpan progress: ${response.message() ?: "Internal Server Error"}")
                        .show()
                }
            } catch (e: Exception) {
                loadingDialog.dismissWithAnimation()
                Log.e("TaskLog", "Exception: ${e.message}", e)
                SweetAlertDialog(this@TaskLogActivity, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Error")
                    .setContentText("Terjadi kesalahan: ${e.message}")
                    .show()
            }
        }
    }
    /* ----------------------------- saveTimerProgress ----------------------------- */

    /* ----------------------------- onDestroy ----------------------------- */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
    /* ----------------------------- onDestroy ----------------------------- */
}
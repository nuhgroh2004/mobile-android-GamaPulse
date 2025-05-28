package com.example.gamapulse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gamapulse.model.StoreProgressRequest
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    private val binder = LocalBinder()
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    // Timer state variables
    var startTime: Long = 0L
    var elapsedTime: Long = 0L
    var elapsedTimeInSeconds: Long = 0L
    var targetTimeInSeconds: Long = 0L
    var isTimerRunning = false

    // 12 hours in seconds - auto submit limit
    private val MAX_TIMER_DURATION = 12 * 60 * 60L

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TimerService::WakeLock"
        )
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            startTime = intent.getLongExtra("START_TIME", 0L)
            elapsedTime = intent.getLongExtra("ELAPSED_TIME", 0L)
            elapsedTimeInSeconds = intent.getLongExtra("ELAPSED_TIME_SECONDS", 0L)
            targetTimeInSeconds = intent.getLongExtra("TARGET_TIME_SECONDS", 0L)
            isTimerRunning = true

            startForeground(NOTIFICATION_ID, createNotification())
            wakeLock?.acquire(TimeUnit.HOURS.toMillis(24))

            // Start the timer updates in the service
            startTimerUpdates()
        }
        return START_STICKY
    }

    private fun startTimerUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    elapsedTime = SystemClock.elapsedRealtime() - startTime
                    elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime)

                    // Update notification periodically
                    if (elapsedTimeInSeconds % 60 == 0L) {
                        updateNotification()
                    }

                    // Auto-submission after 12 hours
                    if (elapsedTimeInSeconds >= MAX_TIMER_DURATION) {
                        autoSubmitProgress()
                        return
                    }

                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun autoSubmitProgress() {
        isTimerRunning = false
        val isTargetAchieved = elapsedTimeInSeconds >= targetTimeInSeconds

        serviceScope.launch {
            saveTimerProgress(
                targetTimeInSeconds.toInt(),
                elapsedTimeInSeconds.toInt(),
                isTargetAchieved
            )
            stopSelf()
        }
    }

    private suspend fun saveTimerProgress(expectedSeconds: Int, actualSeconds: Int, isAchieved: Boolean) {
        try {
            val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)

            if (token == null) {
                Log.e("TimerService", "Authentication token not found")
                return
            }

            Log.d("TimerService", "Auto-submitting progress: expected=$expectedSeconds, actual=$actualSeconds, achieved=$isAchieved")

            val request = StoreProgressRequest(
                expectedTarget = expectedSeconds,
                actualTarget = actualSeconds,
                isAchieved = isAchieved
            )

            val authToken = "Bearer $token"
            val response = ApiClient.apiService.storeProgress(authToken, request)

            if (response.isSuccessful) {
                Log.d("TimerService", "Successfully auto-submitted progress")
            } else {
                Log.e("TimerService", "Failed to auto-submit: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Exception during auto-submission: ${e.message}", e)
        }
    }

    fun startTimer(startTime: Long, elapsedTime: Long, elapsedTimeInSeconds: Long, targetTimeInSeconds: Long) {
        this.startTime = startTime
        this.elapsedTime = elapsedTime
        this.elapsedTimeInSeconds = elapsedTimeInSeconds
        this.targetTimeInSeconds = targetTimeInSeconds
        this.isTimerRunning = true

        // Store timer state in preferences
        val sharedPreferences = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong("START_TIME", startTime)
            putLong("ELAPSED_TIME", elapsedTime)
            putLong("ELAPSED_TIME_SECONDS", elapsedTimeInSeconds)
            putLong("TARGET_TIME_SECONDS", targetTimeInSeconds)
            putBoolean("IS_TIMER_RUNNING", true)
            putLong("START_TIMESTAMP", System.currentTimeMillis())
            apply()
        }

        startTimerUpdates()

        // Update notification
        updateNotification()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    fun pauseTimer() {
        isTimerRunning = false

        // Update stored state
        val sharedPreferences = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("IS_TIMER_RUNNING", false)
            apply()
        }
    }

    fun stopTimer() {
        isTimerRunning = false

        // Clear stored state
        val sharedPreferences = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }

        stopForeground(true)
        stopSelf()
        wakeLock?.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Channel for Timer Service notifications"
            channel.setSound(null, null)

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val hours = TimeUnit.SECONDS.toHours(elapsedTimeInSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(elapsedTimeInSeconds) % 60
        val seconds = elapsedTimeInSeconds % 60
        val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val pendingIntent: PendingIntent =
            Intent(this, TaskLogActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer Berjalan")
            .setContentText("Waktu: $timeStr")
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        job.cancel()
        wakeLock?.release()
    }

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1001
    }
}
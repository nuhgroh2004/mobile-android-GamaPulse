package com.example.gamapulse

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import java.util.concurrent.TimeUnit

class TaskLogService : Service() {
    private val binder = LocalBinder()
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    private var isTimerRunning = false
    private var targetTimeInSeconds = 0L
    private var timerThread: Thread? = null

    inner class LocalBinder : Binder() {
        fun getService(): TaskLogService = this@TaskLogService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val target = intent.getLongExtra(EXTRA_TARGET_TIME, 0L)
                startTimer(target)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimerService()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows ongoing timer progress"
                setShowBadge(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Tambahkan fungsi reset di TaskLogService
    fun resetTimer() {
        startTime = 0L
        elapsedTime = 0L
        isTimerRunning = false
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, TaskLogActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, TaskLogService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TaskLogService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val targetFormatted = formatTime(TimeUnit.SECONDS.toMillis(targetTimeInSeconds))

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer Running")
            .setContentText("${formatTime(elapsedTime)} / Target: $targetFormatted")
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startTimer(targetSeconds: Long) {
        targetTimeInSeconds = targetSeconds
        if (startTime == 0L || elapsedTime > 0) {
            startTime = SystemClock.elapsedRealtime()
            elapsedTime = 0L
        } else {
            startTime = SystemClock.elapsedRealtime() - elapsedTime
        }

        isTimerRunning = true
        startForegroundService()

        timerThread = Thread {
            while (isTimerRunning) {
                elapsedTime = SystemClock.elapsedRealtime() - startTime
                updateNotification(elapsedTime)
                onTick?.invoke(elapsedTime)
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        timerThread?.start()
    }

    private fun pauseTimer() {
        isTimerRunning = false
        timerThread?.interrupt()
    }

    private fun resumeTimer() {
        startTimer(targetTimeInSeconds)
    }

    private fun stopTimerService() {
        isTimerRunning = false
        timerThread?.interrupt()
        stopForeground(true)
        stopSelf()
    }

    private fun updateNotification(elapsedMillis: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val actionIntent = Intent(this, TaskLogService::class.java)
        val actionPendingIntent: PendingIntent
        val actionTitle: String
        val actionIcon: Int

        if (isTimerRunning) {
            actionIntent.action = ACTION_PAUSE
            actionTitle = "Pause"
            actionIcon = R.drawable.ic_pause
        } else {
            actionIntent.action = ACTION_RESUME
            actionTitle = "Resume"
            actionIcon = R.drawable.ic_play
        }

        actionPendingIntent = PendingIntent.getService(
            this, 1, actionIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TaskLogService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val targetFormatted = formatTime(TimeUnit.SECONDS.toMillis(targetTimeInSeconds))

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer Running")
            .setContentText("${formatTime(elapsedMillis)} / Target: $targetFormatted")
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(actionIcon, actionTitle, actionPendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatTime(elapsedMillis: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getElapsedTime(): Long = elapsedTime
    fun isRunning(): Boolean = isTimerRunning
    fun getTargetTime(): Long = targetTimeInSeconds

    var onTick: ((Long) -> Unit)? = null

    override fun onDestroy() {
        super.onDestroy()
        isTimerRunning = false
        timerThread?.interrupt()
    }

    companion object {
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_TARGET_TIME = "extra_target_time"
    }
}
package com.nwe.recipely.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nwe.recipely.MainActivity
import com.nwe.recipely.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class TimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickJob: Job? = null

    private var stepNumber = 0
    private var totalSeconds = 0
    private var remaining = 0
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                stepNumber = intent.getIntExtra(EXTRA_STEP, 1)
                totalSeconds = intent.getIntExtra(EXTRA_SECONDS, 0)
                remaining = totalSeconds
                running = true
                startForegroundOngoing()
                publish()
                startTicking()
            }
            ACTION_TOGGLE -> {
                if (running) {
                    running = false
                    tickJob?.cancel()
                    updateOngoingNotification()
                    publish()
                } else {
                    running = true
                    publish()
                    startTicking()
                }
            }
            ACTION_STOP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                publish()
                updateOngoingNotification()
            }
            if (remaining <= 0) onFinished()
        }
    }

    private fun onFinished() {
        running = false
        CookTimer.publish(null)
        val nm = NotificationManagerCompat.from(this)
        nm.notify(
            NOTIF_DONE_ID,
            NotificationCompat.Builder(this, CHANNEL_DONE)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.cook_notif_done, stepNumber))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build(),
        )
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopTimer() {
        tickJob?.cancel()
        running = false
        CookTimer.publish(null)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publish() {
        CookTimer.publish(TimerUiState(stepNumber, totalSeconds, remaining, running))
    }

    private fun startForegroundOngoing() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(this, NOTIF_ONGOING_ID, buildOngoingNotification(), type)
    }

    private fun updateOngoingNotification() {
        if (!running && remaining <= 0) return
        NotificationManagerCompat.from(this).notify(NOTIF_ONGOING_ID, buildOngoingNotification())
    }

    private fun buildOngoingNotification(): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, TimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.cook_notif_running, stepNumber, formatTime(remaining)))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.cook_notif_stop), stopIntent)
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, getString(R.string.cook_notif_channel_ongoing), NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, getString(R.string.cook_notif_channel_done), NotificationManager.IMPORTANCE_HIGH)
                .apply { enableVibration(true) }
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val PKG = "com.nwe.recipely.timer"
        const val ACTION_START = "$PKG.START"
        const val ACTION_TOGGLE = "$PKG.TOGGLE"
        const val ACTION_STOP = "$PKG.STOP"
        const val EXTRA_STEP = "step"
        const val EXTRA_SECONDS = "seconds"
        private const val CHANNEL_ONGOING = "cook_timer_ongoing"
        private const val CHANNEL_DONE = "cook_timer_done"
        private const val NOTIF_ONGOING_ID = 4711
        private const val NOTIF_DONE_ID = 4712

        fun start(context: Context, stepNumber: Int, seconds: Int) {
            val intent = Intent(context, TimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_STEP, stepNumber)
                .putExtra(EXTRA_SECONDS, seconds)
            ContextCompat.startForegroundService(context, intent)
        }

        fun toggle(context: Context) {
            context.startService(Intent(context, TimerService::class.java).setAction(ACTION_TOGGLE))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, TimerService::class.java).setAction(ACTION_STOP))
        }

        fun formatTime(totalSeconds: Int): String {
            val m = totalSeconds / 60
            val s = totalSeconds % 60
            return String.format(Locale.ROOT, "%02d:%02d", m, s)
        }
    }
}

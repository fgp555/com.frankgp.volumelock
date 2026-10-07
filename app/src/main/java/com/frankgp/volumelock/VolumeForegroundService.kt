package com.frankgp.volumelock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class VolumeForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "volume_lock_control_channel"
        const val NOTIFICATION_ID = 1003

        const val ACTION_VOL_UP = "com.frankgp.volumelock.ACTION_VOL_UP"
        const val ACTION_VOL_DOWN = "com.frankgp.volumelock.ACTION_VOL_DOWN"
        const val ACTION_OPEN_DIALOG = "com.frankgp.volumelock.ACTION_OPEN_DIALOG"

        fun startService(context: Context) {
            val intent = Intent(context, VolumeForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VolumeForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        when (intent?.action) {
            ACTION_VOL_UP -> {
                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (current < max) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current + 1, 0)
                }
            }
            ACTION_VOL_DOWN -> {
                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (current > 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current - 1, 0)
                }
            }
            ACTION_OPEN_DIALOG -> {
                val dialogIntent = Intent(this, VolumeDialogActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                }
                startActivity(dialogIntent)
            }
        }

        createNotificationChannel()
        val notification = buildNotification(audioManager)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FGP Volume Lock Control de Volumen"
            val descriptionText = "Control de volumen en la pantalla de bloqueo"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(audioManager: AudioManager): Notification {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val remoteViews = RemoteViews(packageName, R.layout.notification_volume).apply {
            setTextViewText(R.id.notification_volume_text, "$current / $max")
            setProgressBar(R.id.notification_progress, max, current, false)

            val openIntent = Intent(this@VolumeForegroundService, VolumeForegroundService::class.java).apply {
                action = ACTION_OPEN_DIALOG
            }
            setOnClickPendingIntent(
                R.id.notification_title,
                PendingIntent.getService(this@VolumeForegroundService, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val downIntent = Intent(this@VolumeForegroundService, VolumeForegroundService::class.java)
                .setAction(ACTION_VOL_DOWN)
            setOnClickPendingIntent(
                R.id.btn_down,
                PendingIntent.getService(this@VolumeForegroundService, 1, downIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val upIntent = Intent(this@VolumeForegroundService, VolumeForegroundService::class.java)
                .setAction(ACTION_VOL_UP)
            setOnClickPendingIntent(
                R.id.btn_up,
                PendingIntent.getService(this@VolumeForegroundService, 2, upIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        }

        val openDialogIntent = Intent(this, VolumeForegroundService::class.java).apply {
            action = ACTION_OPEN_DIALOG
        }
        val openPendingIntent = PendingIntent.getService(
            this, 0, openDialogIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_volume) // Proper monochrome icon for status bar / notifications
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}

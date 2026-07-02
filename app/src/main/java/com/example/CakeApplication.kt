package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import com.example.database.CakeDatabase
import com.example.repository.CakeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CakeApplication : Application() {

    // Using Simple Constructor Injection (Dependency Container / AppContainer Pattern)
    // This is robust, testable, fast-compiling, and prevents KSP version mismatch issues.
    val database: CakeDatabase by lazy { CakeDatabase.getDatabase(this) }
    val repository: CakeRepository by lazy { CakeRepository(database.cakeDao()) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startNotificationObserver()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Regular Reminders Channel
            val name = "Cake Delivery Reminders"
            val descriptionText = "Notifications for upcoming cake deliveries and payments"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("cake_reminders_channel", name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)

            // Alarm Channel (Plays Alarm Sound, Ringer)
            val alarmChannelName = "Cake Delivery Alarms"
            val alarmDescriptionText = "Alarms for urgent upcoming cake deliveries (2 hours before)"
            val alarmChannel = NotificationChannel("cake_alarms_channel", alarmChannelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = alarmDescriptionText
                enableVibration(true)
                try {
                    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    setSound(alarmSound, android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    /**
     * Periodically checks Room Database for pending notifications and displays them.
     * This provides real notification functionality without relying on external servers.
     */
    private fun startNotificationObserver() {
        applicationScope.launch {
            while (true) {
                try {
                    val pending = repository.getPendingNotifications(System.currentTimeMillis())
                    for (notification in pending) {
                        val channelId = if (notification.notificationType == "2_HOURS_BEFORE") {
                            "cake_alarms_channel"
                        } else {
                            "cake_reminders_channel"
                        }
                        showSystemNotification(notification.id, notification.title, notification.message, channelId)
                        repository.markNotificationAsSent(notification.id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Check every 15 seconds
                delay(15000)
            }
        }
    }

    private fun showSystemNotification(id: Int, title: String, message: String, channelId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (channelId == "cake_alarms_channel") {
            try {
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                builder.setSound(alarmSound)
                builder.setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            notificationManager.notify(id, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ requires POST_NOTIFICATIONS permission
            e.printStackTrace()
        }
    }
}

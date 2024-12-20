package com.geofencedattendance

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.facebook.react.bridge.ReactApplicationContext


class RNNotificationManager (val context:Context) {

    fun createChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "GEOFENCE_CHANNEL"
            val name: CharSequence = "Geofence Alerts"
            val description = "Notifications for geofencing events"
            val importance: Int = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance)
            channel.description = description

            val notificationManager = context.getSystemService(
                NotificationManager::class.java
            )
            notificationManager?.createNotificationChannel(channel)
        }
    }


    fun send(isEnter : Boolean =  true) {
        val message = if (isEnter) {
            "You have marked your attendance"
        } else {
            "You are logged out"
        }

        // Create an Intent that will open the app when the notification is clicked
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            12345,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "GEOFENCE_CHANNEL")
            .setContentTitle("Geofence Alert")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

        notificationManager.notify(1, builder.build())
    }
}
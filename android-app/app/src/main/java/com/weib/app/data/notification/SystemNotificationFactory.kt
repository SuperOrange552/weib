package com.weib.app.data.notification

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.weib.app.MainActivity
import com.weib.app.R

object SystemNotificationFactory {
    const val SECURITY = "weib_security"
    const val MESSAGE = "weib_message"
    const val BUSINESS = "weib_business"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(SECURITY, "账号安全", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(MESSAGE, "聊天消息", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(BUSINESS, "业务通知", NotificationManager.IMPORTANCE_DEFAULT)
        ))
    }

    fun show(context: Context, id: Long, type: String, title: String) {
        createChannels(context)
        val channel = if (type == "CHAT_MESSAGE") MESSAGE else BUSINESS
        val intent = Intent(context, MainActivity::class.java).putExtra("notification_type", type)
        val pending = PendingIntent.getActivity(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("微招").setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pending).build()
        context.getSystemService(NotificationManager::class.java).notify(id.toInt(), notification)
    }
}

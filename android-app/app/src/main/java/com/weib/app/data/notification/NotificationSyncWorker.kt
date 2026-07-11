package com.weib.app.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.weib.app.data.AppRepository

class NotificationSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val prefs = applicationContext.getSharedPreferences("notification_sync", Context.MODE_PRIVATE)
        val after = prefs.getLong("last_event_id", 0)
        val response = AppRepository(applicationContext).notificationEvents(after)
        if (response.code == 401) return Result.success()
        val events = response.data?.asJsonArray ?: return Result.success()
        var last = after
        events.forEach { item ->
            val event = item.asJsonObject
            val id = event["id"].asLong
            SystemNotificationFactory.show(applicationContext, id, event["eventType"].asString, event["title"].asString)
            if (id > last) last = id
        }
        prefs.edit().putLong("last_event_id", last).apply()
        Result.success()
    }.getOrElse { Result.retry() }
}

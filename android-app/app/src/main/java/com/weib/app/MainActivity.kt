package com.weib.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weib.app.ui.WeibApp
import com.weib.app.ui.theme.WeibTheme
import androidx.work.*
import com.weib.app.data.notification.NotificationSyncWorker
import com.weib.app.data.notification.SystemNotificationFactory
import java.util.concurrent.TimeUnit
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemNotificationFactory.createChannels(this)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weib_notification_sync", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<NotificationSyncWorker>(15, TimeUnit.MINUTES).build()
        )
        setContent {
            WeibTheme {
                val viewModel: AppViewModel = viewModel()
                WeibApp(viewModel)
            }
        }
    }
}

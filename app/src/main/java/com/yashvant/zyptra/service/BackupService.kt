package com.yashvant.zyptra.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yashvant.zyptra.R
import com.yashvant.zyptra.data.model.AppInfo
import com.yashvant.zyptra.data.repository.AppRepository
import com.yashvant.zyptra.data.storage.GoogleDriveStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BackupService : Service() {
    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    lateinit var driveStorage: GoogleDriveStorage

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        private const val CHANNEL_ID = "backup_channel"
        private const val NOTIFICATION_ID = 1
        const val APPS_TO_BACKUP = "apps_to_backup"

        fun startBackup(context: Context, apps: ArrayList<AppInfo>) {
            // Check if we have notification permission before starting the service
            if (!PermissionUtil.hasNotificationPermission(context)) {
                return
            }

            val intent = Intent(context, BackupService::class.java).apply {
                putParcelableArrayListExtra(APPS_TO_BACKUP, apps)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        notificationBuilder = createNotificationBuilder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appsToBackup = intent?.getParcelableArrayListExtra<AppInfo>(APPS_TO_BACKUP)
        if (appsToBackup != null && PermissionUtil.hasNotificationPermission(this)) {
            startForeground(NOTIFICATION_ID, notificationBuilder.build())
            performBackup(appsToBackup)
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Backup Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows backup progress for apps"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Backup")
            .setContentText("Preparing backup...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(100, 0, true)
            .setOngoing(true)
            .setAutoCancel(false)
    }

    private fun updateNotification(title: String, message: String, progress: Int, maxProgress: Int) {
        if (!PermissionUtil.hasNotificationPermission(this)) return

        val notification = notificationBuilder
            .setContentTitle(title)
            .setContentText(message)
            .setProgress(maxProgress, progress, false)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showSuccessNotification(appsCount: Int) {
        if (!PermissionUtil.hasNotificationPermission(this)) return

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Backup Complete")
            .setContentText("Successfully backed up $appsCount apps")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun showErrorNotification(error: String) {
        if (!PermissionUtil.hasNotificationPermission(this)) return

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Backup Failed")
            .setContentText(error)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun performBackup(apps: List<AppInfo>) {
        serviceScope.launch {
            val totalApps = apps.size
            try {
                apps.forEachIndexed { index, app ->
                    updateNotification(
                        "Backing up apps",
                        "Backing up ${app.appName} (${index + 1}/$totalApps)",
                        index + 1,
                        totalApps
                    )
                    val backupFile = appRepository.backupApp(app).getOrThrow()
                    driveStorage.uploadFile(backupFile, "${app.packageName}_${app.versionName}.apk")
                        .getOrThrow()
                }
                showSuccessNotification(totalApps)
            } catch (e: Exception) {
                showErrorNotification(e.message ?: "Unknown error occurred")
            } finally {
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                // Handle potential IllegalStateException
            }
        }
    }
}

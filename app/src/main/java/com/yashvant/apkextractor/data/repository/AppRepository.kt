package com.yashvant.apkextractor.data.repository

import CloudStorage
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import com.yashvant.apkextractor.data.storage.CloudStorage
import com.yashvant.apkextractor.ui.screens.AppInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class AppRepository @Inject constructor(
    private val context: Context,
    private val cloudStorage: CloudStorage
) {
    suspend fun getInstalledApps(): Flow<List<AppInfo>> = flow {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { !isSystemApp(it) }
            .mapNotNull { appInfo -> // Use mapNotNull to filter out null values
                packageManager.getPackageInfo(appInfo.packageName, 0).versionName?.let {
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        versionName = it,
                        apkPath = appInfo.sourceDir,
                        dataPath = appInfo.dataDir,
                        size = File(appInfo.sourceDir).length(),
                        icon = packageManager.getApplicationIcon(appInfo.packageName).toBitmap()
                    )
                }
            }
        emit(installedApps)
    }

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    suspend fun backupApp(appInfo: AppInfo): Result<Unit> = runCatching {
        // Create backup zip containing APK and data
        val backupFile = createBackupZip(appInfo)

        // Upload to cloud storage
        cloudStorage.uploadFile(backupFile, "${appInfo.packageName}_backup.zip")
    }

    private suspend fun createBackupZip(appInfo: AppInfo): File {
        // Implementation for creating zip file containing app APK and data
        // Return a File object
        return File("path/to/backup.zip")
    }
}
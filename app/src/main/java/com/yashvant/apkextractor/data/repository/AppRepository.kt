package com.yashvant.apkextractor.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import com.yashvant.apkextractor.data.model.AppInfo
import com.yashvant.apkextractor.data.storage.CloudStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
            .mapNotNull { appInfo ->
                val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                val icon = try {
                    packageManager.getApplicationIcon(appInfo.packageName).toBitmap()
                } catch (e: Exception) {
                    null
                }

                AppInfo(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    versionName = packageInfo.versionName ?: "",
                    apkPath = appInfo.sourceDir,
                    dataPath = appInfo.dataDir,
                    size = File(appInfo.sourceDir).length(),
                    icon = icon
                )
            }
            .sortedBy { it.appName }
        emit(installedApps)
    }.flowOn(Dispatchers.IO)

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    suspend fun backupApp(appInfo: AppInfo): Result<File> = runCatching {
        // Create a copy of the APK in the app's cache directory
        val apkFile = File(appInfo.apkPath)
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, "${appInfo.packageName}_${appInfo.versionName}.apk")

        if (backupFile.exists()) {
            backupFile.delete()
        }

        apkFile.copyTo(backupFile, overwrite = true)
        backupFile
    }

    suspend fun getBackedUpApps(): Flow<List<AppInfo>> = flow {
        val backedUpApps = cloudStorage.listFiles().getOrNull()?.map { cloudFile ->
            // Parse filename to extract package name and version
            val fileName = cloudFile.name
            val packageName = fileName.substringBefore("_")
            val versionName = fileName.substringAfter("_").substringBefore(".apk")

            AppInfo(
                packageName = packageName,
                appName = packageName, // Use package name as app name since we don't have the original name
                versionName = versionName,
                apkPath = "", // Will be populated when downloaded
                dataPath = "",
                size = cloudFile.size
            )
        } ?: emptyList()

        emit(backedUpApps)
    }.flowOn(Dispatchers.IO)

    suspend fun downloadApp(appInfo: AppInfo): Result<File> = runCatching {
        val downloadDir = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }
        val downloadFile = File(downloadDir, "${appInfo.packageName}_${appInfo.versionName}.apk")

        cloudStorage.downloadFile(appInfo.packageName, downloadFile).getOrThrow()
    }
}


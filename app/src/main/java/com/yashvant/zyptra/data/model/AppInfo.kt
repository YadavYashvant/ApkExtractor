package com.yashvant.zyptra.data.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val apkPath: String,
    val dataPath: String,
    val size: Long,
    val icon: Bitmap? = null,
    val downloadUrl: String? = null,
    val lastBackupTime: Long? = null
) : Parcelable


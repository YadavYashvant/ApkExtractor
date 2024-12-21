import android.graphics.Bitmap

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val apkPath: String,
    val dataPath: String,
    val size: Long,
    val icon: Bitmap? = null
) 
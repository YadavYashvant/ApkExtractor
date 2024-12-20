package com.yashvant.apkextractor.ui.screens

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.documentfile.provider.DocumentFile
import java.io.FileInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkExtractorApp() {
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var selectedDirectory by remember { mutableStateOf<String?>(null) }
    var hasPermissions by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
        if (hasPermissions) {
            apps = getInstalledApps(context)
        }
    }

    val directoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            selectedDirectory = it.toString()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intent)
            } else {
                hasPermissions = true
                apps = getInstalledApps(context)
            }
        } else {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APK Extractor") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (!hasPermissions) {
                Text(
                    "Please grant storage permissions to use the app",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Text(
                    text = "Select an App",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(apps) { app ->
                        AppListItem(
                            app = app,
                            isSelected = app == selectedApp,
                            onSelect = { selectedApp = it }
                        )
                    }
                }

                Button(
                    onClick = { directoryLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = selectedDirectory?.let {
                            "Selected Directory: ${Uri.parse(it).lastPathSegment}"
                        } ?: "Choose Export Directory"
                    )
                }

                Button(
                    onClick = {
                        selectedApp?.let { app ->
                            selectedDirectory?.let { dir ->
                                extractApk(context, app, dir)
                            }
                        }
                    },
                    enabled = selectedApp != null && selectedDirectory != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Generate APK")
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelect: (AppInfo) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(app) }
            .padding(16.dp)
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = app.name,
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


private fun getInstalledApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    return packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        .filter { packageInfo ->
            packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null
        }
        .map { packageInfo ->
            AppInfo(
                name = packageInfo.applicationInfo!!.loadLabel(packageManager).toString(),
                packageName = packageInfo.packageName,
                icon = packageInfo.applicationInfo!!.loadIcon(packageManager)
            )
        }
        .sortedBy { it.name }
}


private fun extractApk(
    context: Context,
    app: AppInfo,
    destinationUri: String
) {
    try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
        val sourceApk = appInfo.sourceDir

        val destinationDirectory = DocumentFile.fromTreeUri(context, Uri.parse(destinationUri))
            ?: throw IllegalStateException("Cannot access destination directory")

        val apkFile = destinationDirectory.createFile(
            "application/vnd.android.package-archive",
            "${app.name}_${app.packageName}.apk"
        ) ?: throw IllegalStateException("Cannot create destination file")

        context.contentResolver.openOutputStream(apkFile.uri)?.use { output ->
            FileInputStream(sourceApk).use { input ->
                input.copyTo(output)
            }
        }

        AlertDialog.Builder(context)
            .setTitle("APK Extracted")
            .setMessage("APK saved successfully")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    } catch (e: Exception) {
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("Failed to extract APK: ${e.message}")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)
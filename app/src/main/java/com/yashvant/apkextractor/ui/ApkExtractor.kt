package com.yashvant.apkextractor.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkExtractorApp() {
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var selectedDirectory by remember { mutableStateOf<String?>(null) }

    // Get list of installed apps
    LaunchedEffect(Unit) {
        apps = getInstalledApps(context)
    }

    // Directory selection launcher
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
            // App Selection Section
            Text(
                text = "Select an App",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn {
                items(apps) { app ->
                    AppListItem(
                        app = app,
                        isSelected = app == selectedApp,
                        onSelect = { selectedApp = it }
                    )
                }
            }

            // Directory Selection
            Button(
                onClick = {
                    directoryLauncher.launch(null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedDirectory?.let {
                        "Selected Directory: $it"
                    } ?: "Choose Export Directory"
                )
            }

            // Generate APK Button
            Button(
                onClick = {
                    selectedApp?.let { app ->
                        selectedDirectory?.let { dir ->
                            extractApk(context, app, dir)
                        } ?: run {
                            // Show error that directory is not selected
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

// Utility Functions
private fun getInstalledApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .map { appInfo ->
            AppInfo(
                name = appInfo.loadLabel(packageManager).toString(),
                packageName = appInfo.packageName,
                icon = appInfo.loadIcon(packageManager)
            )
        }
        .sortedBy { it.name }
}

private fun extractApk(
    context: Context,
    app: AppInfo,
    destinationUri: String
) {
    val packageManager = context.packageManager
    val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
    val sourceApk = appInfo.sourceDir

    try {
        val destinationFile = File(
            URI.create(destinationUri).path,
            "${app.name}_${app.packageName}.apk"
        )

        FileInputStream(sourceApk).use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }

        // Show success dialog
        AlertDialog.Builder(context)
            .setTitle("APK Extracted")
            .setMessage("APK saved to ${destinationFile.absolutePath}")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    } catch (e: Exception) {
        // Show error dialog
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("Failed to extract APK: ${e.message}")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

// Data Classes
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)
package com.yashvant.apkextractor.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.yashvant.apkextractor.data.model.AppInfo
import com.yashvant.apkextractor.data.storage.GoogleDriveStorage
import com.yashvant.apkextractor.ui.viewmodel.AppBackupViewModel
import com.yashvant.apkextractor.ui.viewmodel.UiState

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelect() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            app.icon?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "App icon",
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${app.packageName} (${app.versionName})",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppBackupViewModel = hiltViewModel(),
    onSignInRequest: (Intent) -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val backupMode by viewModel.backupMode.collectAsState()
    val backedUpApps by viewModel.backedUpApps.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APK Backup") },
                actions = {
                    if (authState is GoogleDriveStorage.AuthState.Authenticated) {
                        IconButton(onClick = { viewModel.signOut() }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, "Sign Out")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (authState) {
                is GoogleDriveStorage.AuthState.NotAuthenticated -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    onSignInRequest(googleSignInClient.signInIntent)
                                }
                            ) {
                                Text("Sign in with Google Drive", style = MaterialTheme.typography.titleLarge)
                            }
                            Text(
                                "Sign in to backup and restore your apps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is GoogleDriveStorage.AuthState.Authenticated -> {
                    when (uiState) {
                        is UiState.Initial -> {
                            if (backupMode) {
                                // App selection screen
                                Column {
                                    Text(
                                        text = "Select Apps to Backup",
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                    LazyColumn(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(apps) { app ->
                                            AppListItem(
                                                app = app,
                                                isSelected = selectedApps.contains(app),
                                                onSelect = { viewModel.toggleAppSelection(app) }
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { viewModel.startBackup() },
                                        enabled = selectedApps.isNotEmpty(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text("Confirm Backup (${selectedApps.size} apps)")
                                    }
                                }
                            } else {
                                // Restore screen
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Your Backed Up Apps",
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        TextButton(onClick = { viewModel.toggleBackupMode() }) {
                                            Text("Switch to Backup")
                                        }
                                    }
                                    if (backedUpApps.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No backed up apps found",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            items(backedUpApps) { app ->
                                                BackedUpAppItem(
                                                    app = app,
                                                    onDownload = { viewModel.downloadAndInstallApp(app, context) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is UiState.BackupInProgress -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Backing up selected apps...",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                        is UiState.BackupComplete -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Backup completed successfully!",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Button(
                                        onClick = { viewModel.toggleBackupMode() }
                                    ) {
                                        Text("View Backed Up Apps")
                                    }
                                }
                            }
                        }
                        is UiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = (uiState as UiState.Error).message,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(
                                        onClick = { viewModel.resetState() }
                                    ) {
                                        Text("Try Again")
                                    }
                                }
                            }
                        }
                    }
                }
                is GoogleDriveStorage.AuthState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Authentication Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Authentication Error: ${(authState as GoogleDriveStorage.AuthState.Error).exception.message}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    onSignInRequest(googleSignInClient.signInIntent)
                                }
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackedUpAppItem(
    app: AppInfo,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Version: ${app.versionName}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (app.lastBackupTime != null) {
                    Text(
                        text = "Backed up: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(app.lastBackupTime))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.AutoMirrored.Filled.Send, "Download APK")
            }
        }
    }
}


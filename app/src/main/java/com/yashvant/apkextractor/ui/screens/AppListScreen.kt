package com.yashvant.apkextractor.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
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

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Backing up apps...",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
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

    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
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

                    if (authState is GoogleDriveStorage.AuthState.Authenticated) {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Local Apps") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    viewModel.loadBackedUpApps()
                                },
                                text = { Text("Backed Up Apps") }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (authState is GoogleDriveStorage.AuthState.Authenticated && selectedApps.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            if (selectedTab == 0) {
                                viewModel.backupSelectedApps()
                            } else {
                                viewModel.installSelectedApps(context)
                            }
                        }
                    ) {
                        Icon(
                            if (selectedTab == 0) Icons.AutoMirrored.Filled.Send else Icons.Filled.CheckCircle,
                            contentDescription = if (selectedTab == 0) "Backup" else "Install"
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { padding ->
            when (authState) {
                is GoogleDriveStorage.AuthState.Authenticated -> {
                    Box(modifier = Modifier.padding(padding)) {
                        when {
                            uiState is UiState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            uiState is UiState.Error -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Warning, "Error")
                                    Text((uiState as UiState.Error).message)
                                }
                            }
                            else -> {
                                if (selectedTab == 0) {
                                    LazyColumn {
                                        items(apps) { app ->
                                            AppListItem(
                                                app = app,
                                                isSelected = selectedApps.contains(app),
                                                onSelect = { viewModel.toggleAppSelection(app) }
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn {
                                        items(backedUpApps) { app ->
                                            AppListItem(
                                                app = app,
                                                isSelected = selectedApps.contains(app),
                                                onSelect = { viewModel.toggleAppSelection(app) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                                    .build()
                                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                onSignInRequest(googleSignInClient.signInIntent)
                            }
                        ) {
                            Text("Sign in with Google")
                        }
                    }
                }
            }
        }

        // Add loading overlay when backup is in progress
        if (uiState is UiState.BackupInProgress) {
            LoadingOverlay()
        }

        // Show snackbar for errors
        if (uiState is UiState.Error) {
            LaunchedEffect(uiState) {
                val message = (uiState as UiState.Error).message
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
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


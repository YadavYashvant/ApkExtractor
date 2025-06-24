package com.yashvant.apkextractor.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.yashvant.apkextractor.data.model.AppInfo
import com.yashvant.apkextractor.data.repository.AppRepository
import com.yashvant.apkextractor.data.storage.GoogleDriveStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppBackupViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val driveStorage: GoogleDriveStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedApps = MutableStateFlow<Set<AppInfo>>(emptySet())
    val selectedApps: StateFlow<Set<AppInfo>> = _selectedApps.asStateFlow()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _backupMode = MutableStateFlow(true)
    val backupMode: StateFlow<Boolean> = _backupMode.asStateFlow()

    private val _backedUpApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val backedUpApps: StateFlow<List<AppInfo>> = _backedUpApps.asStateFlow()

    val authState = driveStorage.authState

    init {
        viewModelScope.launch {
            authState.collect { state ->
                when (state) {
                    is GoogleDriveStorage.AuthState.Authenticated -> {
                        _backupMode.value = true
                        loadInstalledApps()
                        loadBackedUpApps()
                    }
                    else -> {
                        _selectedApps.value = emptySet()
                        _apps.value = emptyList()
                        _backedUpApps.value = emptyList()
                    }
                }
            }
        }
    }

    fun toggleBackupMode() {
        _backupMode.value = !_backupMode.value
        if (!_backupMode.value) {
            loadBackedUpApps()
        } else {
            loadInstalledApps()
        }
    }

    fun toggleAppSelection(app: AppInfo) {
        val currentSelection = _selectedApps.value.toMutableSet()
        if (currentSelection.contains(app)) {
            currentSelection.remove(app)
        } else {
            currentSelection.add(app)
        }
        _selectedApps.value = currentSelection
    }

    fun startBackup() {
        viewModelScope.launch {
            _uiState.value = UiState.BackupInProgress
            try {
                _selectedApps.value.forEach { app ->
                    val backupFile = appRepository.backupApp(app).getOrThrow()
                    driveStorage.uploadFile(backupFile, "${app.packageName}_${app.versionName}.apk")
                        .getOrThrow()
                }
                _uiState.value = UiState.BackupComplete
                _selectedApps.value = emptySet()
                loadBackedUpApps()
                _backupMode.value = false
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Backup failed")
            }
        }
    }

    fun downloadAndInstallApp(app: AppInfo, context: Context) {
        viewModelScope.launch {
            _uiState.value = UiState.BackupInProgress
            try {
                val downloadedFile = appRepository.downloadApp(app).getOrThrow()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    downloadedFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
                _uiState.value = UiState.Initial
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Download failed")
            }
        }
    }

    fun handleSignInResult(data: Intent?) {
        driveStorage.handleSignInResult(data)
    }

    fun signOut() {
        driveStorage.signOut()
        _selectedApps.value = emptySet()
        _apps.value = emptyList()
        _backedUpApps.value = emptyList()
        _backupMode.value = true
    }

    fun resetState() {
        _uiState.value = UiState.Initial
        if (_backupMode.value) {
            loadInstalledApps()
        } else {
            loadBackedUpApps()
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            appRepository.getInstalledApps()
                .collect { appList ->
                    _apps.value = appList
                }
        }
    }

    private fun loadBackedUpApps() {
        viewModelScope.launch {
            appRepository.getBackedUpApps()
                .collect { appList ->
                    _backedUpApps.value = appList
                }
        }
    }

    companion object {
        const val RC_SIGN_IN = 9001
    }
}

sealed class UiState {
    object Initial : UiState()
    object BackupInProgress : UiState()
    object BackupComplete : UiState()
    data class Error(val message: String) : UiState()
}


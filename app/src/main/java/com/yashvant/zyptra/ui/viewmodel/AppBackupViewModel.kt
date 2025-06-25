package com.yashvant.zyptra.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yashvant.zyptra.data.model.AppInfo
import com.yashvant.zyptra.data.repository.AppRepository
import com.yashvant.zyptra.data.storage.GoogleDriveStorage
import com.yashvant.zyptra.service.BackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppBackupViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val driveStorage: GoogleDriveStorage
) : ViewModel() {

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

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
                _isInitialLoading.value = false
            }
        }
    }

    suspend fun toggleBackupMode() {
        _backupMode.value = !_backupMode.value
        _selectedApps.value = emptySet()
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

    fun startBackup(context: Context) {
        viewModelScope.launch {
            _uiState.value = UiState.BackupInProgress
            try {
                val selectedAppsList = ArrayList(_selectedApps.value)
                BackupService.startBackup(context, selectedAppsList)
                _selectedApps.value = emptySet()
                _uiState.value = UiState.Initial
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Backup failed")
            }
        }
    }

    fun downloadAndInstallApp(app: AppInfo, context: Context) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val downloadDir = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }
                val downloadFile = File(downloadDir, "${app.packageName}_${app.versionName}.apk")

                app.downloadUrl?.let { fileId ->
                    driveStorage.downloadFile(fileId, downloadFile).getOrThrow()
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        downloadFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(intent)
                } ?: throw IllegalStateException("Download URL not available")

                _uiState.value = UiState.Initial
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to download and install app")
            }
        }
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                _isInitialLoading.value = true
                driveStorage.handleSignInResult(data)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Sign in failed")
            } finally {
                _isInitialLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            driveStorage.signOut()
            _selectedApps.value = emptySet()
            _apps.value = emptyList()
            _backedUpApps.value = emptyList()
        }
    }

    suspend fun resetState() {
        _uiState.value = UiState.Initial
        if (_backupMode.value) {
            loadInstalledApps()
        } else {
            loadBackedUpApps()
        }
    }

    private suspend fun loadInstalledApps() {
        _uiState.value = UiState.Loading
        try {
            appRepository.getInstalledApps().collect { apps ->
                _apps.value = apps
            }
            _uiState.value = UiState.Initial
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Failed to load installed apps")
        }
    }

    fun loadBackedUpApps() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                driveStorage.listFiles().getOrNull()?.map { cloudFile ->
                    AppInfo(
                        packageName = cloudFile.name.substringBefore("_"),
                        appName = cloudFile.name.substringBefore("_"),
                        versionName = cloudFile.name.substringAfter("_").substringBefore(".apk"),
                        apkPath = "",
                        dataPath = "",
                        size = cloudFile.size,
                        downloadUrl = cloudFile.id,
                        lastBackupTime = cloudFile.modifiedTime
                    )
                }?.let { apps ->
                    _backedUpApps.value = apps
                }
                _uiState.value = UiState.Initial
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load backed up apps")
            }
        }
    }

    fun backupSelectedApps() {
        viewModelScope.launch {
            _uiState.value = UiState.BackupInProgress
            try {
                val selectedAppsList = ArrayList(_selectedApps.value)
                selectedAppsList.forEach { app ->
                    val backupFile = appRepository.backupApp(app).getOrThrow()
                    driveStorage.uploadFile(backupFile, "${app.packageName}_${app.versionName}.apk")
                }
                _selectedApps.value = emptySet()
                loadBackedUpApps()
                _uiState.value = UiState.Initial
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Backup failed")
            }
        }
    }

    fun installSelectedApps(context: Context) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                _selectedApps.value.forEach { app ->
                    downloadAndInstallApp(app, context)
                }
                _selectedApps.value = emptySet()
                _uiState.value = UiState.Initial
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Installation failed")
            }
        }
    }

    companion object {
        const val RC_SIGN_IN = 9001
    }
}

sealed class UiState {
    object Initial : UiState()
    object Loading : UiState()
    object BackupInProgress : UiState()
    data class Error(val message: String) : UiState()
}


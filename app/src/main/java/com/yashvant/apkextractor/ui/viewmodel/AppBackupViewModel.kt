import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppBackupViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val googleDriveStorage: GoogleDriveStorage
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _backupStatus = MutableStateFlow<Map<String, BackupStatus>>(emptyMap())
    val backupStatus: StateFlow<Map<String, BackupStatus>> = _backupStatus.asStateFlow()

    val authState = googleDriveStorage.authState.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            appRepository.getInstalledApps()
                .collect { appList ->
                    _apps.value = appList
                }
        }
    }

    fun backupApp(appInfo: AppInfo) {
        viewModelScope.launch {
            _backupStatus.update { it + (appInfo.packageName to BackupStatus.InProgress) }
            
            appRepository.backupApp(appInfo)
                .onSuccess {
                    _backupStatus.update { it + (appInfo.packageName to BackupStatus.Success) }
                }
                .onFailure { error ->
                    _backupStatus.update { it + (appInfo.packageName to BackupStatus.Error(error)) }
                }
        }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            googleDriveStorage.signIn(activity)
        }
    }

    fun handleSignInResult(data: Intent?) {
        googleDriveStorage.handleSignInResult(data)
    }
}

sealed class BackupStatus {
    object InProgress : BackupStatus()
    object Success : BackupStatus()
    data class Error(val throwable: Throwable) : BackupStatus()
} 
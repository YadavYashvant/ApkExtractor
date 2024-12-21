import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppListScreen(
    viewModel: AppBackupViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()

    Column {
        val mContext = LocalContext.current
        when (val state = authState) {
            is GoogleDriveStorage.AuthState.NotAuthenticated -> {
                Button(
                    onClick = { 
                        val activity = mContext as? Activity
                        activity?.let { viewModel.signIn(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Sign in with Google Drive")
                }
            }
            is GoogleDriveStorage.AuthState.Authenticated -> {
                Text(
                    text = "Signed in as: ${state.email}",
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn {
                    items(apps) { appInfo ->
                        AppListItem(
                            appInfo = appInfo,
                            backupStatus = backupStatus[appInfo.packageName],
                            onBackupClick = { viewModel.backupApp(appInfo) }
                        )
                    }
                }
            }
            is GoogleDriveStorage.AuthState.Error -> {
                Text(
                    text = "Error: ${state.exception.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun AppListItem(
    appInfo: AppInfo,
    backupStatus: BackupStatus?,
    onBackupClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            appInfo.icon?.let { icon ->
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onBackupClick,
                enabled = backupStatus !is BackupStatus.InProgress
            ) {
                when (backupStatus) {
                    is BackupStatus.InProgress -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                    is BackupStatus.Success -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )
                    is BackupStatus.Error -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )
                    null -> Text("Backup")
                    else -> {}
                }
            }
        }
    }
} 
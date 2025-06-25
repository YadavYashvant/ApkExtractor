package com.yashvant.zyptra.data.storage

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.yashvant.zyptra.R
import com.yashvant.zyptra.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveStorage @Inject constructor(
    private val context: Context
) : CloudStorage {
    private val scopes = listOf(Config.DRIVE_SCOPE)
    private var driveService: Drive? = null
    private var backupFolderId: String? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun signIn(activity: Activity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(scopes[0]))
            .requestIdToken(Config.OAUTH_CLIENT_ID)
            .build()

        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        // Clear any previous sign-in state and launch the sign-in flow
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    suspend fun setGoogleAccount(account: GoogleSignInAccount) {
        setupDriveService(account)
        _authState.value = AuthState.Authenticated(account.email ?: "")
    }

    suspend fun handleSignInResult(data: Intent?) {
        try {
            // Handle existing account case
            val existingAccount = data?.getParcelableExtra<GoogleSignInAccount>("googleAccount")
            if (existingAccount != null) {
                setupDriveService(existingAccount)
                _authState.value = AuthState.Authenticated(existingAccount.email ?: "")
                return
            }

            // Handle new sign-in result
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            setupDriveService(account)
            _authState.value = AuthState.Authenticated(account.email ?: "")
        } catch (e: ApiException) {
            _authState.value = AuthState.Error(e)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e)
        }
    }

    private suspend fun ensureBackupFolder(): String = withContext(Dispatchers.IO) {
        try {
            backupFolderId?.let { return@withContext it }

            val folderList = driveService?.files()?.list()
                ?.setQ("mimeType='application/vnd.google-apps.folder' and name='AppBackups' and trashed=false")
                ?.setFields("files(id, name)")
                ?.execute()
                ?.files

            backupFolderId = if (!folderList.isNullOrEmpty()) {
                folderList[0].id
            } else {
                val folderMetadata = DriveFile().apply {
                    name = "AppBackups"
                    mimeType = "application/vnd.google-apps.folder"
                }
                driveService?.files()?.create(folderMetadata)
                    ?.setFields("id")
                    ?.execute()
                    ?.id
            }

            return@withContext backupFolderId ?: throw IllegalStateException("Could not create or find backup folder")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create or find backup folder: ${e.message}")
        }
    }

    override suspend fun uploadFile(file: File, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (driveService == null) throw IllegalStateException("Drive service not initialized. Please sign in first.")

            try {
                val folderId = ensureBackupFolder()
                val fileMetadata = DriveFile().apply {
                    name = fileName
                    parents = listOf(folderId)
                }

                val mediaContent = FileContent("application/vnd.android.package-archive", file)

                val uploadedFile = driveService?.files()?.create(fileMetadata, mediaContent)
                    ?.setFields("id, name")
                    ?.execute()
                    ?: throw IllegalStateException("Failed to upload file")

                uploadedFile.id
            } catch (e: Exception) {
                throw IllegalStateException("Failed to upload file: ${e.message}")
            }
        }
    }

    override suspend fun downloadFile(fileId: String, destinationFile: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (driveService == null) throw IllegalStateException("Drive service not initialized. Please sign in first.")

            driveService?.files()?.get(fileId)?.executeMediaAndDownloadTo(FileOutputStream(destinationFile))
            destinationFile
        }
    }

    override suspend fun listFiles(): Result<List<CloudFile>> = withContext(Dispatchers.IO) {
        runCatching {
            if (driveService == null) throw IllegalStateException("Drive service not initialized. Please sign in first.")

            val folderId = ensureBackupFolder()
            val result = driveService?.files()?.list()
                ?.setQ("'$folderId' in parents and trashed=false")
                ?.setFields("files(id, name, size, modifiedTime)")
                ?.execute()

            result?.files?.map { file ->
                CloudFile(
                    id = file.id,
                    name = file.name,
                    size = file.getSize() ?: 0L,
                    modifiedTime = file.modifiedTime?.value ?: 0L
                )
            } ?: emptyList()
        }
    }

    private suspend fun setupDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            scopes
        ).apply {
            selectedAccount = account.account
        }

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(context.getString(R.string.app_name))
            .build()

        // Initialize backup folder
        ensureBackupFolder()
    }

    fun signOut() {
        driveService = null
        backupFolderId = null
        _authState.value = AuthState.NotAuthenticated
    }

    sealed class AuthState {
        object NotAuthenticated : AuthState()
        data class Authenticated(val email: String) : AuthState()
        data class Error(val exception: Exception) : AuthState()
    }

    companion object {
        const val RC_SIGN_IN = 9001
    }
}


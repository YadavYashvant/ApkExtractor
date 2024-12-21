package com.yashvant.apkextractor.data.storage

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
import com.yashvant.apkextractor.R
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
    private val scope = "https://www.googleapis.com/auth/drive.file"
    private var driveService: Drive? = null
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun signIn(activity: Activity) {
        val googleSignInClient = GoogleSignIn.getClient(
            activity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(scope))
                .build()
        )

        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            setupDriveService(account)
            _authState.value = AuthState.Authenticated(account.email ?: "")
        } catch (e: ApiException) {
            _authState.value = AuthState.Error(e)
        }
    }

    private fun setupDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(scope)
        ).apply {
            selectedAccount = account.account
        }

        driveService = Drive.Builder(
//            AndroidHttp.newCompatibleTransport(),
            NetHttpTransport.Builder().build(),
            GsonFactory(),
            credential
        )
            .setApplicationName(context.getString(R.string.app_name))
            .build()
    }

    override suspend fun uploadFile(file: File, fileName: String): Result<String> = withContext(
        Dispatchers.IO) {
        runCatching {
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = fileName
                parents = listOf("appDataFolder")
            }

            val mediaContent = FileContent("application/zip", file)
            
            driveService?.files()?.create(fileMetadata, mediaContent)
                ?.setFields("id")
                ?.execute()
                ?.id ?: throw IllegalStateException("Drive service not initialized")
        }
    }

    override suspend fun downloadFile(fileId: String, destinationFile: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val outputStream = FileOutputStream(destinationFile)
            driveService?.files()?.get(fileId)?.executeMediaAndDownloadTo(outputStream)
            outputStream.close()
            destinationFile
        }
    }

    override suspend fun listFiles(): Result<List<CloudFile>> = withContext(Dispatchers.IO) {
        runCatching {
            driveService?.files()?.list()
                ?.setSpaces("appDataFolder")
                ?.setFields("files(id, name, size, modifiedTime)")
                ?.execute()
                ?.files
                ?.map { file ->
                    CloudFile(
                        id = file.id,
                        name = file.name,
                        size = file.getSize() ?: 0,
                        modifiedTime = file.modifiedTime?.value ?: 0
                    )
                } ?: emptyList()
        }
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
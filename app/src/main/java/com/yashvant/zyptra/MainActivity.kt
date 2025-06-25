package com.yashvant.zyptra

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.yashvant.zyptra.ui.screens.AppListScreen
import com.yashvant.zyptra.ui.theme.ApkExtractorTheme
import com.yashvant.zyptra.ui.viewmodel.AppBackupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppBackupViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, notifications will work
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Check for existing signed-in account
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            viewModel.viewModelScope.launch {
                viewModel.handleSignInResult(Intent().apply {
                    putExtra("googleAccount", account)
                })
            }
        }

        // Initialize sign-in launcher
        val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.handleSignInResult(result.data)
        }

        setContent {
            ApkExtractorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppListScreen(
                        onSignInRequest = { intent ->
                            signInLauncher.launch(intent)
                        }
                    )
                }
            }
        }
    }
}

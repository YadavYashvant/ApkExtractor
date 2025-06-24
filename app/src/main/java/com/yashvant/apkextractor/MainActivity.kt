package com.yashvant.apkextractor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yashvant.apkextractor.ui.screens.AppListScreen
import com.yashvant.apkextractor.ui.theme.ApkExtractorTheme
import com.yashvant.apkextractor.ui.viewmodel.AppBackupViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppBackupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

package com.yashvant.apkextractor

import AppBackupViewModel
import AppListScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yashvant.apkextractor.ui.screens.ApkExtractorApp
import com.yashvant.apkextractor.ui.theme.ApkExtractorTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppBackupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApkExtractorTheme{
                AppListScreen()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GoogleDriveStorage.RC_SIGN_IN) {
            viewModel.handleSignInResult(data)
        }
    }
}


/*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApkExtractorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ApkExtractorApp()
                }
            }
        }
    }
}*/

package com.mhss.app.mybrain.presentation.auth

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.mhss.app.mybrain.presentation.app_lock.AuthScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthRoute(
    viewModel: AuthViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val authMessage by viewModel.authMessage.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()

    LaunchedEffect(authMessage) {
        authMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearAuthMessage()
        }
    }

    LaunchedEffect(syncResult) {
        when (syncResult) {
            true -> Toast.makeText(context, "Synkning klar", Toast.LENGTH_SHORT).show()
            false -> Toast.makeText(context, "Synkning misslyckades", Toast.LENGTH_SHORT).show()
            null -> {}
        }
    }
}

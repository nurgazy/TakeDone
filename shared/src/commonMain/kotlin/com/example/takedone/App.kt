package com.example.takedone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.takedone.auth.network.AuthRepositoryImpl
import com.example.takedone.auth.network.HttpClientFactory
import com.example.takedone.auth.storage.SettingsTokenStorage
import com.example.takedone.auth.ui.AuthScreen
import com.example.takedone.auth.ui.AuthState
import com.example.takedone.auth.ui.AuthViewModel
import com.example.takedone.auth.ui.LoginScreen
import com.example.takedone.auth.ui.RegisterScreen
import com.example.takedone.auth.ui.UserProfileScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel = remember {
            val tokenStorage = SettingsTokenStorage()
            val httpClient = HttpClientFactory.create(tokenStorage)
            val repository = AuthRepositoryImpl(httpClient, tokenStorage)
            AuthViewModel(repository)
        }

        val uiState by viewModel.uiState.collectAsState()
        val currentScreen by viewModel.currentScreen.collectAsState()

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState is AuthState.Authenticated -> {
                    val user = (uiState as AuthState.Authenticated).user
                    UserProfileScreen(
                        user = user,
                        onLogoutClick = { viewModel.logout() }
                    )
                }
                currentScreen == AuthScreen.REGISTER -> {
                    RegisterScreen(
                        state = uiState,
                        onRegisterClick = { name, email, password ->
                            viewModel.register(name, email, password)
                        },
                        onNavigateToLogin = {
                            viewModel.navigateTo(AuthScreen.LOGIN)
                        }
                    )
                }
                currentScreen == AuthScreen.LOGIN -> {
                    LoginScreen(
                        state = uiState,
                        onLoginClick = { email, password ->
                            viewModel.login(email, password)
                        },
                        onNavigateToRegister = {
                            viewModel.navigateTo(AuthScreen.REGISTER)
                        }
                    )
                }
                uiState is AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                else -> {
                    LoginScreen(
                        state = uiState,
                        onLoginClick = { email, password ->
                            viewModel.login(email, password)
                        },
                        onNavigateToRegister = {
                            viewModel.navigateTo(AuthScreen.REGISTER)
                        }
                    )
                }
            }
        }
    }
}

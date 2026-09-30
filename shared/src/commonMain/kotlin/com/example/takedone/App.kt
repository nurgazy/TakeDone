package com.example.takedone

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private val PrimaryColor = Color(0xFF4F46E5)
private val PrimaryContainerColor = Color(0xFFEEF2FF)
private val OnPrimaryContainerColor = Color(0xFF312E81)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    primaryContainer = PrimaryContainerColor,
    onPrimaryContainer = OnPrimaryContainerColor,
    surface = Color(0xFFF9FAFB),
    surfaceContainerHigh = Color.White,
    surfaceContainer = Color(0xFFF3F4F6)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    surface = Color(0xFF111827),
    surfaceContainerHigh = Color(0xFF1F2937),
    surfaceContainer = Color(0xFF374151)
)

@Composable
@Preview
fun App() {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme) {
        val viewModel = remember {
            val tokenStorage = SettingsTokenStorage()
            val httpClient = HttpClientFactory.create(tokenStorage)
            val repository = AuthRepositoryImpl(httpClient, tokenStorage)
            AuthViewModel(repository)
        }

        val uiState by viewModel.uiState.collectAsState()
        val currentScreen by viewModel.currentScreen.collectAsState()

        Surface(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
}

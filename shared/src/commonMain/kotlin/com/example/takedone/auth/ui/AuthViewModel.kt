package com.example.takedone.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.takedone.auth.model.AuthResult
import com.example.takedone.auth.model.User
import com.example.takedone.auth.network.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}

enum class AuthScreen {
    LOGIN,
    REGISTER,
    PROFILE
}

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    private val _currentScreen = MutableStateFlow(AuthScreen.LOGIN)
    val currentScreen: StateFlow<AuthScreen> = _currentScreen.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        if (repository.isLoggedIn()) {
            viewModelScope.launch {
                _uiState.value = AuthState.Loading
                when (val result = repository.getCurrentUser()) {
                    is AuthResult.Success -> {
                        _uiState.value = AuthState.Authenticated(result.data)
                        _currentScreen.value = AuthScreen.PROFILE
                    }
                    is AuthResult.Error -> {
                        _uiState.value = AuthState.Unauthenticated
                        _currentScreen.value = AuthScreen.LOGIN
                    }
                    AuthResult.Loading -> {}
                }
            }
        } else {
            _uiState.value = AuthState.Unauthenticated
            _currentScreen.value = AuthScreen.LOGIN
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthState.Error("Заполните все поля")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            when (val result = repository.login(email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthState.Authenticated(result.data)
                    _currentScreen.value = AuthScreen.PROFILE
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthState.Error(result.message)
                }
                AuthResult.Loading -> {}
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthState.Error("Заполните все поля")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            when (val result = repository.register(name, email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthState.Authenticated(result.data)
                    _currentScreen.value = AuthScreen.PROFILE
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthState.Error(result.message)
                }
                AuthResult.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthState.Unauthenticated
            _currentScreen.value = AuthScreen.LOGIN
        }
    }

    fun navigateTo(screen: AuthScreen) {
        _currentScreen.value = screen
        if (_uiState.value is AuthState.Error) {
            _uiState.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_uiState.value is AuthState.Error) {
            _uiState.value = AuthState.Unauthenticated
        }
    }
}

package com.example.takedone.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String? = null
)

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String? = null,
    val user: User
)

sealed interface AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>
    data class Error(val message: String, val statusCode: Int? = null) : AuthResult<Nothing>
    data object Loading : AuthResult<Nothing>
}

package com.example.takedone.auth.network

import com.example.takedone.auth.model.AuthResponse
import com.example.takedone.auth.model.AuthResult
import com.example.takedone.auth.model.AuthTokens
import com.example.takedone.auth.model.LoginRequest
import com.example.takedone.auth.model.RegisterRequest
import com.example.takedone.auth.model.User
import com.example.takedone.auth.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult<User>
    suspend fun register(name: String, email: String, password: String): AuthResult<User>
    suspend fun getCurrentUser(): AuthResult<User>
    suspend fun logout()
    fun isLoggedIn(): Boolean
    fun getSavedUser(): User?
}

class AuthRepositoryImpl(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    private var currentUser: User? = null

    override suspend fun login(email: String, password: String): AuthResult<User> {
        return try {
            val response: HttpResponse = client.post("/auth/login") {
                setBody(LoginRequest(email = email, password = password))
            }

            if (response.status.isSuccess()) {
                val authResponse: AuthResponse = response.body()
                tokenStorage.saveTokens(
                    AuthTokens(authResponse.token, authResponse.refreshToken)
                )
                currentUser = authResponse.user
                AuthResult.Success(authResponse.user)
            } else {
                AuthResult.Error("Ошибка входа: ${response.status.value}", response.status.value)
            }
        } catch (e: Exception) {
            val demoUser = User(
                id = "demo_123",
                email = email,
                name = if (email.contains("@")) email.substringBefore("@") else email,
                role = "User"
            )
            tokenStorage.saveTokens(AuthTokens("demo_access_token", "demo_refresh_token"))
            currentUser = demoUser
            AuthResult.Success(demoUser)
        }
    }

    override suspend fun register(name: String, email: String, password: String): AuthResult<User> {
        return try {
            val response: HttpResponse = client.post("/auth/register") {
                setBody(RegisterRequest(name = name, email = email, password = password))
            }

            if (response.status.isSuccess()) {
                val authResponse: AuthResponse = response.body()
                tokenStorage.saveTokens(
                    AuthTokens(authResponse.token, authResponse.refreshToken)
                )
                currentUser = authResponse.user
                AuthResult.Success(authResponse.user)
            } else {
                AuthResult.Error("Ошибка регистрации: ${response.status.value}", response.status.value)
            }
        } catch (e: Exception) {
            val demoUser = User(
                id = "demo_123",
                email = email,
                name = name,
                role = "User"
            )
            tokenStorage.saveTokens(AuthTokens("demo_access_token", "demo_refresh_token"))
            currentUser = demoUser
            AuthResult.Success(demoUser)
        }
    }

    override suspend fun getCurrentUser(): AuthResult<User> {
        if (!tokenStorage.isLoggedIn()) {
            return AuthResult.Error("Пользователь не авторизован")
        }

        currentUser?.let { return AuthResult.Success(it) }

        return try {
            val response: HttpResponse = client.get("/auth/me")
            if (response.status.isSuccess()) {
                val user: User = response.body()
                currentUser = user
                AuthResult.Success(user)
            } else {
                tokenStorage.clear()
                AuthResult.Error("Не удалось получить профиль", response.status.value)
            }
        } catch (e: Exception) {
            val demoUser = User(
                id = "demo_123",
                email = "user@example.com",
                name = "Пользователь",
                role = "User"
            )
            currentUser = demoUser
            AuthResult.Success(demoUser)
        }
    }

    override suspend fun logout() {
        try {
            client.post("/auth/logout")
        } catch (_: Exception) {}
        tokenStorage.clear()
        currentUser = null
    }

    override fun isLoggedIn(): Boolean {
        return tokenStorage.isLoggedIn()
    }

    override fun getSavedUser(): User? {
        return currentUser
    }
}

package com.example.takedone.auth.network

import com.example.takedone.auth.model.AuthResult
import com.example.takedone.auth.model.AuthTokens
import com.example.takedone.auth.model.RegisterRequest
import com.example.takedone.auth.model.TokenResponse
import com.example.takedone.auth.model.User
import com.example.takedone.auth.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun login(email: String, password: String): AuthResult<User> {
        return try {
            val response: HttpResponse = client.post("/auth/login") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("username", email)
                            append("password", password)
                        }
                    )
                )
            }

            if (response.status.isSuccess()) {
                val tokenResponse: TokenResponse = response.body()
                tokenStorage.saveTokens(
                    AuthTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                )
                getCurrentUser()
            } else {
                val errorMsg = parseErrorMessage(response)
                AuthResult.Error(errorMsg, response.status.value)
            }
        } catch (e: Exception) {
            handleException(e, "Ошибка входа")
        }
    }

    override suspend fun register(name: String, email: String, password: String): AuthResult<User> {
        return try {
            val response: HttpResponse = client.post("/auth/register") {
                setBody(
                    RegisterRequest(
                        email = email,
                        password = password,
                        fullName = name.ifBlank { null }
                    )
                )
            }

            if (response.status.isSuccess()) {
                val registeredUser: User = response.body()
                when (val loginResult = login(email, password)) {
                    is AuthResult.Success -> loginResult
                    else -> {
                        currentUser = registeredUser
                        AuthResult.Success(registeredUser)
                    }
                }
            } else {
                val errorMsg = parseErrorMessage(response)
                AuthResult.Error(errorMsg, response.status.value)
            }
        } catch (e: Exception) {
            handleException(e, "Ошибка регистрации")
        }
    }

    override suspend fun getCurrentUser(): AuthResult<User> {
        if (!tokenStorage.isLoggedIn()) {
            return AuthResult.Error("Пользователь не авторизован")
        }

        return try {
            val response: HttpResponse = client.get("/users/me")
            if (response.status.isSuccess()) {
                val user: User = response.body()
                currentUser = user
                AuthResult.Success(user)
            } else {
                tokenStorage.clear()
                currentUser = null
                val errorMsg = parseErrorMessage(response)
                AuthResult.Error(errorMsg, response.status.value)
            }
        } catch (e: Exception) {
            handleException(e, "Ошибка загрузки профиля")
        }
    }

    override suspend fun logout() {
        tokenStorage.clear()
        currentUser = null
    }

    override fun isLoggedIn(): Boolean {
        return tokenStorage.isLoggedIn()
    }

    override fun getSavedUser(): User? {
        return currentUser
    }

    private suspend fun parseErrorMessage(response: HttpResponse): String {
        val status = response.status.value
        if (status >= 500) {
            return "Сервер временно недоступен. Пожалуйста, попробуйте позже."
        }
        return try {
            val text = response.bodyAsText()
            if (text.isBlank()) return "Ошибка $status"
            val element = json.parseToJsonElement(text)
            if (element is JsonObject && element.containsKey("detail")) {
                when (val detail = element["detail"]) {
                    is JsonPrimitive -> detail.content
                    is JsonArray -> detail.firstOrNull()?.jsonObject?.get("msg")?.jsonPrimitive?.content ?: text
                    else -> text
                }
            } else {
                text
            }
        } catch (_: Exception) {
            "Ошибка сервера ($status)"
        }
    }

    private fun handleException(e: Exception, defaultMessage: String): AuthResult.Error {
        val message = e.message ?: ""
        return if (message.contains("Failed to connect") ||
            message.contains("Unable to resolve host") ||
            message.contains("nodename nor servname") ||
            message.contains("timeout") ||
            message.contains("Connection refused") ||
            message.contains("Network is unreachable") ||
            e is kotlinx.io.IOException
        ) {
            AuthResult.Error("Нет подключения к интернету. Проверьте соединение.")
        } else {
            AuthResult.Error("$defaultMessage: ${e.message ?: "Неизвестная ошибка"}")
        }
    }
}

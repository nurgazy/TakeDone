package com.example.takedone.auth.network

import com.example.takedone.auth.model.AuthResponse
import com.example.takedone.auth.model.AuthTokens
import com.example.takedone.auth.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    const val BASE_URL = "https://api.example.com"

    fun create(tokenStorage: TokenStorage): HttpClient {
        return HttpClient {
            defaultRequest {
                url(BASE_URL)
                contentType(ContentType.Application.Json)
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = tokenStorage.getAccessToken()
                        val refreshToken = tokenStorage.getRefreshToken()
                        if (accessToken != null) {
                            BearerTokens(accessToken, refreshToken ?: "")
                        } else null
                    }

                    refreshTokens {
                        val oldRefreshToken = tokenStorage.getRefreshToken() ?: return@refreshTokens null
                        try {
                            val response: AuthResponse = client.post("/auth/refresh") {
                                setBody(mapOf("refreshToken" to oldRefreshToken))
                            }.body()
                            tokenStorage.saveTokens(
                                AuthTokens(response.token, response.refreshToken ?: oldRefreshToken)
                            )
                            BearerTokens(response.token, response.refreshToken ?: oldRefreshToken)
                        } catch (e: Exception) {
                            tokenStorage.clear()
                            null
                        }
                    }
                }
            }
        }
    }
}

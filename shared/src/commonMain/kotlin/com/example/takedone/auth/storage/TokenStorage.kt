package com.example.takedone.auth.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import com.example.takedone.auth.model.AuthTokens

interface TokenStorage {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(tokens: AuthTokens)
    fun clear()
    fun isLoggedIn(): Boolean
}

class SettingsTokenStorage(
    private val settings: Settings = Settings()
) : TokenStorage {

    companion object {
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
    }

    override fun getAccessToken(): String? {
        return settings[KEY_ACCESS_TOKEN]
    }

    override fun getRefreshToken(): String? {
        return settings[KEY_REFRESH_TOKEN]
    }

    override fun saveTokens(tokens: AuthTokens) {
        settings[KEY_ACCESS_TOKEN] = tokens.accessToken
        tokens.refreshToken?.let {
            settings[KEY_REFRESH_TOKEN] = it
        }
    }

    override fun clear() {
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
    }

    override fun isLoggedIn(): Boolean {
        val token = getAccessToken()
        return !token.isNullOrEmpty()
    }
}

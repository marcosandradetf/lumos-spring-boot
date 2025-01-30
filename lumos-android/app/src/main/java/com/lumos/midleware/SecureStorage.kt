package com.lumos.midleware

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(private val context: Context) {

    private val PREFS_NAME = "secure_prefs"
    private val KEY_ACCESS_TOKEN = "accessToken"
    private val KEY_REFRESH_TOKEN = "refreshToken"

    private fun getSharedPreferences() =
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun saveTokens(accessToken: String, refreshToken: String) {
        val prefs = getSharedPreferences()
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? =
        getSharedPreferences().getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? =
        getSharedPreferences().getString(KEY_REFRESH_TOKEN, null)

    fun clearTokens() {
        getSharedPreferences().edit().clear().apply()
    }

    fun saveAccessToken(newAccessToken: String) {
        val prefs = getSharedPreferences()
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, newAccessToken)
            .apply()
    }
}

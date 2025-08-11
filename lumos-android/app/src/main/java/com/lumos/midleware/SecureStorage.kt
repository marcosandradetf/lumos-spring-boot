package com.lumos.midleware

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.AEADBadTagException
import androidx.core.content.edit

class SecureStorage(private val context: Context) {

//    private val PREFS_NAME = "secure_prefs"
//    private val KEY_ACCESS_TOKEN = "accessToken"
//    private val KEY_REFRESH_TOKEN = "refreshToken"
//    private val KEY_USER_UUID = "userUUID"

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_ACCESS_TOKEN = "accessToken"
        private const val KEY_REFRESH_TOKEN = "refreshToken"
        private const val KEY_USER_UUID = "userUUID"
        private const val KEY_ROLES = "roles"
        private const val KEY_TEAMS = "teams"
    }

    // Função modificada para tratar erro específico do Keystore
    private fun getSharedPreferences(): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: AEADBadTagException) {
            Log.e("SecureStorage", "Erro de autenticação - dados corrompidos. Resetando prefs.", e)
            // Limpa os dados e usa SharedPreferences simples
            val fallbackPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            fallbackPrefs.edit { clear() }
            fallbackPrefs
        } catch (e: Exception) {
            Log.e("SecureStorage", "Erro ao acessar EncryptedSharedPreferences. Usando fallback.", e)
            // Também usa SharedPreferences simples como fallback
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }


    fun saveTokens(accessToken: String, refreshToken: String, uuid: String) {
        val prefs = getSharedPreferences()
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_UUID, uuid)
        }
        Log.d("SecureStorage", "Tokens salvos: accessToken=$accessToken, refreshToken=$refreshToken, uuid=$uuid")
    }

    fun saveAssignments(roles: Set<String>, teams: Set<String>) {
        val prefs = getSharedPreferences()
        prefs.edit {
            putStringSet(KEY_ROLES, roles)
                .putStringSet(KEY_TEAMS, teams)
        }
    }

    fun getAccessToken(): String? =
        getSharedPreferences().getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? =
        getSharedPreferences().getString(KEY_REFRESH_TOKEN, null)

    fun getUserUuid(): String? =
        getSharedPreferences().getString(KEY_USER_UUID, null)

    fun getRoles(): Set<String> =
        getSharedPreferences().getStringSet(KEY_ROLES, null) ?: emptySet()

    fun getTeams(): Set<String> =
        getSharedPreferences().getStringSet(KEY_TEAMS, null) ?: emptySet()

    fun saveAccessToken(newAccessToken: String) {
        val prefs = getSharedPreferences()
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, newAccessToken)
        }
    }

    fun saveRefreshToken(newRefreshToken: String) {
        val prefs = getSharedPreferences()
        prefs.edit {
            putString(KEY_REFRESH_TOKEN, newRefreshToken)
        }
    }

    fun clearAll() {
        getSharedPreferences().edit { clear() }
    }

    fun setLastUpdateCheck() {
        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences()

        prefs.edit {
            putLong("last_update_check", now)
        }
    }

    fun getLastUpdateCheck(): Long {
        return getSharedPreferences().getLong("last_update_check", 0L)
    }

    fun setTeamId(teamId: Long) {
        val prefs = getSharedPreferences()

        prefs.edit {
            putLong("team_id", teamId)
        }
    }

    fun getTeamId(): Long {
        val prefs = getSharedPreferences()
        return prefs.getLong("team_id", 0L)
    }

}

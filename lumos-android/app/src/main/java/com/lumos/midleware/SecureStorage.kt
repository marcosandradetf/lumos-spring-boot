package com.lumos.midleware

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecureStorage(private val context: Context) {
    private val _appLocked = MutableStateFlow(isAppLocked())
    val appLocked: StateFlow<Boolean> = _appLocked.asStateFlow()

    companion object {
        private const val SECURE_PREFS_NAME = "secure_prefs"
        private const val NORMAL_PREFS_NAME = "normal_prefs"
        private const val MIGRATION_FLAG = "migration_done"
        private const val KEY_ACCESS_TOKEN = "accessToken"
        private const val KEY_REFRESH_TOKEN = "refreshToken"
        private const val KEY_USER_UUID = "userUUID"
        private const val KEY_USER_MAIL = "userMail"

        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ROLES = "roles"
        private const val KEY_TEAMS = "teams"
        private const val KEY_OPERATIONAL_USERS = "operational_users"
        private const val KEY_TEAM_ID = "team_id"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_LAST_TEAM_CHECK = "last_team_check"

        private const val KEY_AUTO_CALCULATE_ITEMS_PRE_MEASUREMENT =
            "key_auto_calculate_items_pre_measurement"
        private const val SUBSCRIBED_TEAM_TOPIC = "subscribed-team-topic"
        private const val UPDATE_TYPE = "update-type"
    }

    // Cache de instâncias
    @Volatile
    private var securePrefsInstance: SharedPreferences? = null

    @Volatile
    private var normalPrefsInstance: SharedPreferences? = null
    private var lastKnownAccessToken: String? = null
    private var lastKnownRefreshToken: String? = null


    private fun getSecurePrefs(): SharedPreferences {
        securePrefsInstance?.let { return it }

        synchronized(this) {
            securePrefsInstance?.let { return it }

            val encryptedPrefs = try {
                EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFS_NAME,
                    MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.e(
                    "SecureStorage",
                    "Falha ao abrir EncryptedSharedPreferences. Criando fallback com tokens em memória.",
                    e
                )
                null
            }

            if (encryptedPrefs != null) {
                // Atualiza cache em memória
                val accessToken = encryptedPrefs.getString(KEY_ACCESS_TOKEN, lastKnownAccessToken)
                val refreshToken =
                    encryptedPrefs.getString(KEY_REFRESH_TOKEN, lastKnownRefreshToken)

                lastKnownAccessToken = accessToken
                lastKnownRefreshToken = refreshToken

                // Escreve no EncryptedSharedPreferences se houver algo no cache que ainda não existe
                encryptedPrefs.edit {
                    lastKnownAccessToken?.let { putString(KEY_ACCESS_TOKEN, it) }
                    lastKnownRefreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
                }

                securePrefsInstance = encryptedPrefs
                return encryptedPrefs
            }

            // Fallback persistente com tokens em memória
            val fallbackPrefs = context.getSharedPreferences(
                SECURE_PREFS_NAME + "_tokens_fallback",
                Context.MODE_PRIVATE
            ).apply {
                edit {
                    lastKnownAccessToken?.let { putString(KEY_ACCESS_TOKEN, it) }
                    lastKnownRefreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
                }
            }

            securePrefsInstance = fallbackPrefs
            return fallbackPrefs
        }
    }

    private fun getNormalPrefs(): SharedPreferences {
        normalPrefsInstance?.let { return it }

        synchronized(this) {
            normalPrefsInstance?.let { return it }
            val prefs = context.getSharedPreferences(NORMAL_PREFS_NAME, Context.MODE_PRIVATE)
            normalPrefsInstance = prefs
            migrateIfNeeded()
            return prefs
        }
    }

    // Migração única: dados não sensíveis do secure_prefs para normal_prefs
    private fun migrateIfNeeded() {
        val normalPrefs = normalPrefsInstance ?: return

        // Garante inicialização do securePrefs
        val securePrefs = securePrefsInstance ?: getSecurePrefs()

        if (normalPrefs.getBoolean(MIGRATION_FLAG, false)) return

        val editor = normalPrefs.edit()
        var migrated = false

        // UUID
        securePrefs.getString(KEY_USER_UUID, null)?.let {
            editor.putString(KEY_USER_UUID, it)
            migrated = true
        }

        // Roles
        securePrefs.getStringSet(KEY_ROLES, null)?.let {
            editor.putStringSet(KEY_ROLES, it)
            migrated = true
        }

        // Teams
        securePrefs.getStringSet(KEY_TEAMS, null)?.let {
            editor.putStringSet(KEY_TEAMS, it)
            migrated = true
        }

        // Operational users
        securePrefs.getStringSet(KEY_OPERATIONAL_USERS, null)?.let {
            editor.putStringSet(KEY_OPERATIONAL_USERS, it)
            migrated = true
        }

        // Team ID
        if (securePrefs.contains(KEY_TEAM_ID)) {
            editor.putLong(KEY_TEAM_ID, securePrefs.getLong(KEY_TEAM_ID, 0L))
            migrated = true
        }

        // Last update check
        if (securePrefs.contains(KEY_LAST_UPDATE_CHECK)) {
            editor.putLong(KEY_LAST_UPDATE_CHECK, securePrefs.getLong(KEY_LAST_UPDATE_CHECK, 0L))
            migrated = true
        }

        if (migrated) {
            editor.putBoolean(MIGRATION_FLAG, true).apply()
            Log.d("SecureStorage", "Migração de dados não sensíveis concluída.")
        }
    }

    // --- Tokens (seguro) ---
    fun saveTokens(accessToken: String, refreshToken: String) {
        lastKnownAccessToken = accessToken
        lastKnownRefreshToken = refreshToken

        getSecurePrefs().edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    fun getAccessToken(): String? = getSecurePrefs().getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = getSecurePrefs().getString(KEY_REFRESH_TOKEN, null)

    // --- Dados normais ---
    fun saveUserUuid(uuid: String) {
        getNormalPrefs().edit { putString(KEY_USER_UUID, uuid) }
    }

    fun setFullName(name: String) {
        getNormalPrefs().edit { putString(KEY_USER_NAME, name) }
    }

    fun getUserUuid(): String? = getNormalPrefs().getString(KEY_USER_UUID, null)

    fun getFullName(): String? = getNormalPrefs().getString(KEY_USER_NAME, null)

    fun saveRoles(roles: Set<String>) {
        getNormalPrefs().edit { putStringSet(KEY_ROLES, roles) }
    }

    fun getRoles(): Set<String> = getNormalPrefs().getStringSet(KEY_ROLES, emptySet()) ?: emptySet()

    fun saveTeams(teams: Set<String>) {
        getNormalPrefs().edit { putStringSet(KEY_TEAMS, teams) }
    }

    fun getTeams(): Set<String> = getNormalPrefs().getStringSet(KEY_TEAMS, emptySet()) ?: emptySet()

    fun saveOperationalUsers(users: Set<String>) {
        getNormalPrefs().edit { putStringSet(KEY_OPERATIONAL_USERS, users) }
    }

    fun getOperationalUsers(): Set<String> =
        getNormalPrefs().getStringSet(KEY_OPERATIONAL_USERS, emptySet()) ?: emptySet()

    fun setTeamId(teamId: Long) {
        getNormalPrefs().edit { putLong(KEY_TEAM_ID, teamId) }
    }

    fun getTeamId(): Long = getNormalPrefs().getLong(KEY_TEAM_ID, 0L)

    fun setLastUpdateCheck() {
        val now = System.currentTimeMillis()
        getNormalPrefs().edit { putLong(KEY_LAST_UPDATE_CHECK, now) }
    }

    fun getLastUpdateCheck(): Long = getNormalPrefs().getLong(KEY_LAST_UPDATE_CHECK, 0L)

    fun setLastTeamCheck() {
        val now = System.currentTimeMillis()
        getNormalPrefs().edit { putLong(KEY_LAST_TEAM_CHECK, now) }
    }

    fun getLastTeamCheck(): Long = getNormalPrefs().getLong(KEY_LAST_TEAM_CHECK, 0L)

    // Limpar tudo (tokens no seguro + dados normais)
    fun clearAll() {
        getSecurePrefs().edit { clear() }
        getNormalPrefs().edit { clear() }
    }

    fun getAutoCalculate(): Boolean {
        return getNormalPrefs().getBoolean(KEY_AUTO_CALCULATE_ITEMS_PRE_MEASUREMENT, false)
    }

    fun toggleAutoCalculate(value: Boolean) {
        getNormalPrefs().edit { putBoolean(KEY_AUTO_CALCULATE_ITEMS_PRE_MEASUREMENT, value) }
    }

    fun saveEmail(email: String) {
        getNormalPrefs().edit { putString(KEY_USER_MAIL, email) }
    }

    fun getEmail(): String? = getNormalPrefs().getString(KEY_USER_MAIL, null)

    fun getTeamSubscribedTopic(): String? = getNormalPrefs().getString(SUBSCRIBED_TEAM_TOPIC, null)

    fun setTeamSubscribedTopic(topic: String) {
        getNormalPrefs().edit { putString(SUBSCRIBED_TEAM_TOPIC, topic) }
    }

    fun setUpdateType(updateType: String) {
        getNormalPrefs().edit { putString(UPDATE_TYPE, updateType) }
    }

    fun getUpdateType() = getNormalPrefs().getString(UPDATE_TYPE, null) ?: "FLEXIBLE"
    fun setActionsIds(ids: Set<String>) {
        getNormalPrefs().edit {
            putStringSet("actions-ids", ids)
        }
    }

    fun getActionIds(): Set<String> =
        getNormalPrefs().getStringSet("actions-ids", emptySet()) ?: emptySet()

    fun setForceUpdate(forceUpdate: Boolean) {
        getNormalPrefs().edit {
            putBoolean("force-update", forceUpdate)
        }
    }

    fun getForceUpdate() =
        getNormalPrefs().getBoolean("force-update", false)

    fun setMinBuild(minBuild: Long) {
        getNormalPrefs().edit {
            putLong("min-build", minBuild)
        }
    }

    fun getMinBuild() =
        getNormalPrefs().getLong("min-build", 0L)

    fun setTenant(tenant: String) = getNormalPrefs().edit {
        putString("tenant", tenant)
    }

    fun getTenant() = getNormalPrefs().getString("tenant", null)

    fun setAppLocked(locked: Boolean) {
        getNormalPrefs().edit { putBoolean("app_locked", locked) }
        _appLocked.value = locked
    }

    private fun isAppLocked(): Boolean =
        getNormalPrefs().getBoolean("app_locked", false)


}

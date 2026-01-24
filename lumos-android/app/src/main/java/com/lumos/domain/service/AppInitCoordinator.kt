package com.lumos.domain.service

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.lumos.BuildConfig
import com.lumos.midleware.SecureStorage
import com.lumos.repository.RemoteConfigRepository

class AppInitCoordinator(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val actionExecutor: DefaultRemoteActionExecutor,
    private val secureStorage: SecureStorage
) {
    private var lastFetchTime = 0L
    private val ttl = 6 * 60 * 60 * 1000 // 6h

    suspend fun onAppStart() {
        fetch(force = true)
    }

    suspend fun onForeground() {
        fetch(force = false)
    }

    private suspend fun fetch(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastFetchTime < ttl) return

        try {
            val config = remoteConfigRepository.fetchConfig(
                appId = "lumos-android",
                platform = "android",
                build = BuildConfig.VERSION_CODE.toLong()
            )

            // Atualizações de estado são idempotentes
            secureStorage.setForceUpdate(config.forceUpdate)
            secureStorage.setUpdateType(config.updateType)
            secureStorage.setMinBuild(config.minBuild)

            // Marca fetch como válido assim que o backend respondeu
            lastFetchTime = now

            val executedIds = secureStorage.getActionIds().toMutableSet()

            config.actions.forEach { action ->
                if (executedIds.contains(action.id)) return@forEach

                try {
                    actionExecutor.execute(action)
                    executedIds.add(action.id)
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }

            secureStorage.setActionsIds(executedIds)

        } catch (e: java.io.IOException) {
            // offline / timeout → esperado
        } catch (e: retrofit2.HttpException) {
            // erro de backend → esperado
        } catch (e: Exception) {
            // bug
            FirebaseCrashlytics.getInstance().recordException(e)
        }

    }


}


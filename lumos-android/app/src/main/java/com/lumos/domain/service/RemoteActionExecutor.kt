package com.lumos.domain.service;

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.lumos.BuildConfig
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.RemoteAction
import com.lumos.midleware.SecureStorage
import com.lumos.repository.RemoteConfigRepository
import com.lumos.utils.SessionManager
import com.lumos.worker.SyncManager.enqueueSync
import com.lumos.worker.SyncTypes

interface RemoteActionExecutor {
    suspend fun execute(action: RemoteAction)
}

class DefaultRemoteActionExecutor(
    private val db: AppDatabase,
    private val secureStorage: SecureStorage,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val appContext: Context
) : RemoteActionExecutor {

    override suspend fun execute(action: RemoteAction) {

        // 1️⃣ Validação de versão
        action.minAppBuild?.let { minBuild ->
            if (BuildConfig.VERSION_CODE < minBuild) return
        }

        // 2️⃣ Avaliação de conditions
        if (!conditionsSatisfied(action.conditions)) return

        // 3️⃣ Dispatch por tipo (whitelist)
        when (action.type) {

            "SET_TENANT" -> {
                secureStorage.setTenant(action.target)
            }

            "CLEAR_TABLE" -> clearTable(action)

            "RUN_WORKER" -> runWorker(action)

            "SEND_PAYLOAD_INSTALLATION" -> sendPayloadInstallation(action)

            "SEND_PAYLOAD_MAINTENANCE" -> sendPayloadMaintenance(action)

            "SEND_PAYLOAD_QUEUE" -> sendPayloadQueue(action)

            "LOCK_APP" -> {
                secureStorage.setAppLocked(true)
            }

            "UNLOCK_APP" -> {
                secureStorage.setAppLocked(false)
            }

            else -> {
                // Tipo desconhecido → ignora silenciosamente
                FirebaseCrashlytics.getInstance()
                    .log("Unknown remote action type: ${action.type}")
            }
        }
    }

    private suspend fun conditionsSatisfied(conditions: Map<String, Any>?): Boolean {
        if (conditions == null) return true

        return conditions.all { (key, value) ->
            when (key) {
                "tenant" -> {
                    secureStorage.getTenant() == value as String
                }

                "loggedIn" ->
                    SessionManager.loggedIn.value == value as Boolean

                "hasPendingSync" ->
                    db.queueDao().countPendingItemsByTypes(
                        listOf(
                            SyncTypes.POST_DIRECT_EXECUTION,
                            SyncTypes.FINISHED_DIRECT_EXECUTION,
                            SyncTypes.SUBMIT_PRE_MEASUREMENT_INSTALLATION,
                            SyncTypes.SUBMIT_PRE_MEASUREMENT_INSTALLATION_STREET,
                        )
                    ) > 0 == value as Boolean

                else -> false
            }
        }
    }

    private suspend fun clearTable(action: RemoteAction) {
        when (action.target) {
            "team" -> db.teamDao().deleteTeams()
            "user" -> db.teamDao().deleteUsers()
            "stock" -> db.stockDao().deleteStock()
            "pre-measurement-installation" -> {
                val ids = action.payload
                    ?.get("ids")
                    ?.let { raw ->
                        when (raw) {
                            is List<*> -> raw.filterIsInstance<String>()
                            else -> null
                        }
                    }
                    ?: return
                db.preMeasurementInstallationDao().deleteAllInstallations(ids)
            }

            "direct-execution" -> {
                val ids = action.payload
                    ?.get("ids")
                    ?.let { raw ->
                        when (raw) {
                            is List<*> -> raw.filterIsInstance<Long>()
                            else -> null
                        }
                    }
                    ?: return
                db.directExecutionDao().deleteAllInstallations(ids)
            }

            else -> {
                FirebaseCrashlytics.getInstance()
                    .log("Invalid CLEAR_TABLE target: ${action.target}")
            }
        }
    }

    private fun runWorker(action: RemoteAction) {
        val workerName = action.target

        when (workerName) {
            "SYNC_QUEUE" ->
                enqueueSync(appContext, true)

            else -> {
                FirebaseCrashlytics.getInstance()
                    .log("Unknown worker target: $workerName")
                return
            }
        }
    }

    private suspend fun sendPayloadQueue(action: RemoteAction) {
        val payload = action.payload ?: return

        try {
            val status = (payload["status"] as? String) ?: return
            val queue = db.queueDao().getItems(status)
            remoteConfigRepository.sendPayload(queue, "queue")
        } catch (e: java.io.IOException) {
            // offline / timeout → esperado
        } catch (e: retrofit2.HttpException) {
            // erro de backend → esperado
        } catch (e: Exception) {
            // bug
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private suspend fun sendPayloadInstallation(action: RemoteAction) {
        val payload = action.payload ?: return

        // Exemplo: { "status": "FINISHED" }
        val status = (payload["status"] as? String) ?: return

//        remoteConfigRepository.sendInstallationPayload(status)
    }

    private suspend fun sendPayloadMaintenance(action: RemoteAction) {
        val payload = action.payload ?: return

        // Exemplo: { "status": "FINISHED" }
        val status = (payload["status"] as? String) ?: return

//        remoteConfigRepository.sendMaintenancePayload(status)
    }


}


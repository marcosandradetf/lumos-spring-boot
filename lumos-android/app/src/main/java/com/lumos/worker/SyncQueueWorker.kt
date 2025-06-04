package com.lumos.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lumos.R
import com.lumos.data.api.ApiService
import com.lumos.data.api.ContractApi
import com.lumos.data.api.ExecutionApi
import com.lumos.data.api.PreMeasurementApi
import com.lumos.data.api.RequestResult
import com.lumos.data.api.UpdateEntity
import com.lumos.data.api.UserExperience
import com.lumos.data.database.AppDatabase
import com.lumos.data.database.QueueDao
import com.lumos.data.repository.ContractRepository
import com.lumos.data.repository.ExecutionRepository
import com.lumos.data.repository.GenericRepository
import com.lumos.data.repository.PreMeasurementRepository
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.midleware.SecureStorage
import com.lumos.utils.ConnectivityUtils
import com.lumos.utils.Utils.parseToAny


object SyncStatus {
    const val PENDING = "PENDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
}

object SyncTypes {
    const val SYNC_CONTRACT_ITEMS = "SYNC_CONTRACT_ITEMS"
    const val SYNC_CONTRACTS = "SYNC_CONTRACTS"
    const val SYNC_STOCK = "SYNC_STOCK"
    const val POST_PRE_MEASUREMENT = "POST_PRE_MEASUREMENT"
    const val SYNC_EXECUTIONS = "SYNC_EXECUTIONS"
    const val POST_EXECUTION = "POST_EXECUTION"
    const val POST_GENERIC = "POST_GENERIC"
    const val GET_GENERIC = "GET_GENERIC"

}


class SyncQueueWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val queueDao: QueueDao
    private val secureStorage: SecureStorage = SecureStorage(appContext)

    private val preMeasurementRepository: PreMeasurementRepository
    private val contractRepository: ContractRepository
    private val executionRepository: ExecutionRepository
    private val genericRepository: GenericRepository


    init {
        val api = ApiService(appContext, secureStorage)
        val db = AppDatabase.getInstance(appContext)

        queueDao = db.queueDao()

        val preMeasurementApi = api.createApi(PreMeasurementApi::class.java)
        val contractApi = api.createApi(ContractApi::class.java)
        val executionApi = api.createApi(ExecutionApi::class.java)

        preMeasurementRepository = PreMeasurementRepository(
            db,
            preMeasurementApi,
            appContext
        )

        contractRepository = ContractRepository(
            db = db,
            api = contractApi
        )

        executionRepository = ExecutionRepository(
            db = db,
            api = executionApi
        )

        genericRepository = GenericRepository(
            api = api
        )
    }

    override suspend fun doWork(): Result {
        setForeground(ForegroundInfo(1, createNotification()))

        val uuid = secureStorage.getUserUuid() ?: return Result.failure()
        val pendingItems = queueDao.getItemsToProcess()

        var shouldRetry = false
        var hasFailures = false

        for (item in pendingItems) {
            queueDao.update(item.copy(status = SyncStatus.IN_PROGRESS))

            val result = when (item.type) {
                SyncTypes.POST_PRE_MEASUREMENT -> postPreMeasurement(item, uuid)
                SyncTypes.SYNC_CONTRACT_ITEMS -> syncContractItems(item)
                SyncTypes.SYNC_CONTRACTS -> syncContract(item)
                SyncTypes.SYNC_EXECUTIONS -> syncExecutions(item)
                SyncTypes.POST_GENERIC -> postGeneric(item)
                SyncTypes.POST_EXECUTION -> postExecution(item)
//                SyncTypes.UPLOAD_STREET_PHOTOS -> uploadStreetPhotos(item)
                else -> {
                    Log.e("SyncWorker", "Tipo desconhecido: ${item.type}")
                    queueDao.update(item.copy(status = SyncStatus.FAILED))
                    Result.failure()
                }
            }

            if (result == Result.failure()) hasFailures = true
            if (result == Result.retry()) shouldRetry = true
        }

        return when {
            shouldRetry -> Result.retry()
            hasFailures -> Result.success()
            else -> Result.success()
        }
    }

    private suspend fun postGeneric(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            if (item.equal == null || item.table == null || item.where == null || item.field == null || item.set == null) {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                return Result.success()
            }

            if (ConnectivityUtils.isNetworkGood(applicationContext)) {
                Log.e("postGeneric", "Internet")
                queueDao.update(inProgressItem)
                val request = UpdateEntity(
                    table = item.table,
                    field = item.field,
                    set = parseToAny(item.set),
                    where = item.where,
                    equal = parseToAny(item.equal)
                )
                val success = genericRepository.setEntity(request)
                if (success) {
                    queueDao.deleteById(item.id)
                    Result.success()
                } else {
                    queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                    Result.retry()
                }

            } else {
                Log.e("postGeneric", "Sem Internet")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("postGeneric", "Erro ao sincronizar: ${e.message}")
            queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
            Result.failure()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "sync_worker_channel"
        val channelName = "Sync Worker Notifications"


        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificações do serviço de sincronização"
        }

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)


        return NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Sincronizando dados")
            .setContentText("O aplicativo está sincronizando dados em segundo plano.")
            .setSmallIcon(R.drawable.ic_lumos) // ícone precisa existir!
            .setOngoing(true)
            .build()
    }


    private suspend fun postPreMeasurement(item: SyncQueueEntity, uuid: String): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (!ConnectivityUtils.isNetworkGood(applicationContext)) {
                return Result.retry()
            }

            if (item.relatedId == null) {
                return Result.failure()
            }

            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            queueDao.update(inProgressItem)
            val contract = preMeasurementRepository.getPreMeasurement(item.relatedId)
            val streets = preMeasurementRepository.getStreets(item.relatedId)
            val items = preMeasurementRepository.getItems(item.relatedId)

            val success = preMeasurementRepository.sendMeasurementToBackend(
                contract,
                streets,
                items,
                uuid,
                applicationContext
            )

            return if (success == 0) {
                queueDao.deleteById(item.id)
                Result.success()
            } else if (success == -1) {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                Result.retry() // ainda quer tentar mais uma vez
            } else {
                Result.retry()
            }

        } catch (e: Exception) {
            // Marcar falha ou retry conforme necessidade
            queueDao.update(item.copy(status = SyncStatus.FAILED))
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Erro ao enviar pré-mediçao",
                body = "Tente novamente e caso se repita, contate o Suporte e informe o seguinte erro: 'Código 1 - Exception na classe SyncPreMeasurement'",
            )
            Result.failure()
        }
    }

    private suspend fun syncContractItems(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            if (ConnectivityUtils.isNetworkGood(applicationContext)) {
                Log.e("SyncStock", "Internet")
                queueDao.update(inProgressItem)
                val response = contractRepository.syncContractItems(applicationContext)
                checkResponse(response, item)

            } else {
                Log.e("SyncStock", "Sem Internet")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncStock", "Erro ao sincronizar: ${e.message}")
            Result.failure()
        }
    }

    private suspend fun syncContract(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            if (ConnectivityUtils.isNetworkGood(applicationContext)) {
                Log.e("syncContract", "Internet")
                queueDao.update(inProgressItem)
                val success = contractRepository.syncContracts()
                if (success) {
                    queueDao.deleteById(item.id)
                    Result.success()
                } else {
                    queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                    Result.retry()
                }

            } else {
                Log.e("syncContract", "Sem Internet")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("syncContract", "Erro ao sincronizar: ${e.message}")
            queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
            Result.failure()
        }
    }

    private suspend fun syncExecutions(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            val uuid = secureStorage.getUserUuid()
            if (uuid == null) {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                return Result.success()
            }

            if (!ConnectivityUtils.isNetworkGood(applicationContext)) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val success = executionRepository.syncExecutions(uuid)

            return if (success) {
                queueDao.deleteById(item.id)
                Result.success()
            } else {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                Result.retry() // ainda quer tentar mais uma vez
            }

        } catch (e: Exception) {
            Log.e("syncExecutions", "Erro ao sincronizar: ${e.message}")
            queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
            Result.failure()
        }
    }

    private suspend fun postExecution(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (item.relatedId == null) {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                return Result.success()
            }

            if (!ConnectivityUtils.isNetworkGood(applicationContext)) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val success = executionRepository.postExecution(item.relatedId, applicationContext)

            return if (success) {
                queueDao.deleteById(item.id)
                Result.success()
            } else {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                Result.retry() // ainda quer tentar mais uma vez
            }

        } catch (e: Exception) {
            Log.e("postExecution", "Erro ao sincronizar: ${e.message}")
            queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
            Result.failure()
        }
    }


    suspend fun checkResponse(response:  RequestResult<Unit>, inProgressItem: SyncQueueEntity): Result {
        return when (response) {
            is RequestResult.Success -> Result.success()
            is RequestResult.NoInternet -> Result.retry()
            is RequestResult.Timeout -> Result.retry()
            is RequestResult.ServerError -> {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                Result.retry()
            }
            is RequestResult.UnknownError -> {

                // Marca como failed para evitar retries automáticos
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))

                // Pode enviar para um sistema de logs, Crashlytics etc
//                Crashlytics.logException(Exception("Erro desconhecido na sync: $inProgressItem"))

                return Result.failure()
            }

        }
    }


}

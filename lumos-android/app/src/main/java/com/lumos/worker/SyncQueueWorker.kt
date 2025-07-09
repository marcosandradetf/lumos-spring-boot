package com.lumos.worker

import android.app.Application
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
import com.lumos.data.api.NotificationType
import com.lumos.data.api.PreMeasurementApi
import com.lumos.data.api.RequestResult
import com.lumos.data.api.UpdateEntity
import com.lumos.data.api.UserExperience
import com.lumos.data.database.AppDatabase
import com.lumos.data.database.QueueDao
import com.lumos.data.repository.ContractRepository
import com.lumos.data.repository.DirectExecutionRepository
import com.lumos.data.repository.IndirectExecutionRepository
import com.lumos.data.repository.GenericRepository
import com.lumos.data.repository.MaintenanceRepository
import com.lumos.data.repository.PreMeasurementRepository
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.midleware.SecureStorage
import com.lumos.navigation.Routes
import com.lumos.utils.ConnectivityUtils
import com.lumos.utils.Utils
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
    const val SYNC_EXECUTIONS = "SYNC_EXECUTIONS"

    const val POST_GENERIC = "POST_GENERIC"
    const val POST_PRE_MEASUREMENT = "POST_PRE_MEASUREMENT"
    const val POST_INDIRECT_EXECUTION = "POST_INDIRECT_EXECUTION"
    const val POST_DIRECT_EXECUTION = "POST_DIRECT_EXECUTION"
    const val POST_MAINTENANCE = "POST_MAINTENANCE"

    const val FINISHED_DIRECT_EXECUTION = "FINISHED_DIRECT_EXECUTION"

}


class SyncQueueWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val app = context.applicationContext as Application

    private val queueDao: QueueDao
    private val secureStorage: SecureStorage = SecureStorage(app.applicationContext)

    private val preMeasurementRepository: PreMeasurementRepository
    private val contractRepository: ContractRepository
    private val indirectExecutionRepository: IndirectExecutionRepository
    private val directExecutionRepository: DirectExecutionRepository
    private val genericRepository: GenericRepository
    private val maintenanceRepository: MaintenanceRepository


    init {
        val api = ApiService(app.applicationContext, secureStorage)
        val db = AppDatabase.getInstance(app.applicationContext)

        queueDao = db.queueDao()

        val preMeasurementApi = api.createApi(PreMeasurementApi::class.java)
        val contractApi = api.createApi(ContractApi::class.java)
        val executionApi = api.createApi(ExecutionApi::class.java)

        preMeasurementRepository = PreMeasurementRepository(
            db,
            preMeasurementApi,
            app
        )

        contractRepository = ContractRepository(
            db = db,
            api = contractApi,
            app = app
        )

        indirectExecutionRepository = IndirectExecutionRepository(
            db = db,
            api = executionApi,
            secureStorage = secureStorage,
            app = app
        )

        directExecutionRepository = DirectExecutionRepository(
            db = db,
            api = executionApi,
            secureStorage = secureStorage,
            app = app
        )

        genericRepository = GenericRepository(
            api = api
        )

        maintenanceRepository = MaintenanceRepository(
            db = db,
            api = api,
            secureStorage = secureStorage,
            app = app
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
                SyncTypes.SYNC_CONTRACT_ITEMS -> syncContractItems(item)
                SyncTypes.SYNC_CONTRACTS -> syncContract(item)
                SyncTypes.SYNC_EXECUTIONS -> syncExecutions(item)
                SyncTypes.SYNC_STOCK -> syncStock(item)

                SyncTypes.POST_PRE_MEASUREMENT -> postPreMeasurement(item, uuid)
                SyncTypes.POST_GENERIC -> postGeneric(item)
                SyncTypes.POST_INDIRECT_EXECUTION -> postIndirectExecution(item)
                SyncTypes.POST_DIRECT_EXECUTION -> postDirectExecution(item)

                SyncTypes.FINISHED_DIRECT_EXECUTION -> finishedDirectExecution(item)
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

            if (ConnectivityUtils.isNetworkGood(applicationContext) && ConnectivityUtils.hasRealInternetConnection()) {
                Log.e("postGeneric", "Internet")
                queueDao.update(inProgressItem)
                val request = UpdateEntity(
                    table = item.table,
                    field = item.field,
                    set = parseToAny(item.set),
                    where = item.where,
                    equal = parseToAny(item.equal)
                )
                val response = genericRepository.setEntity(request)
                checkResponse(response, item)

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
            if (!ConnectivityUtils.isNetworkGood(applicationContext) && !ConnectivityUtils.hasRealInternetConnection()) {
                return Result.retry()
            }

            if (item.relatedId == null) {
                return Result.failure()
            }

            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            queueDao.update(inProgressItem)
            val streets = preMeasurementRepository.getAllStreets(item.relatedId)
            val items = preMeasurementRepository.getItems(item.relatedId)

            val response = preMeasurementRepository.sendMeasurementToBackend(
                item.relatedId,
                streets,
                items,
                uuid
            )

            checkResponse(
                response,
                item
            )

        } catch (e: Exception) {
            // Marcar falha ou retry conforme necessidade
            queueDao.update(
                inProgressItem.copy(
                    status = SyncStatus.FAILED,
                    errorMessage = e.message
                )
            )
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Erro ao enviar pré-mediçao",
                body = "Verifique o erro em Perfil - Sincronizações",
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

            if (ConnectivityUtils.isNetworkGood(applicationContext) && ConnectivityUtils.hasRealInternetConnection()) {
                Log.e("SyncStock", "Internet")
                queueDao.update(inProgressItem)
                val response = contractRepository.syncContractItems()
                checkResponse(response, item)

            } else {
                Log.e("SyncStock", "Sem Internet")
                Result.retry()
            }
        } catch (e: Exception) {
            queueDao.update(
                inProgressItem.copy(
                    status = SyncStatus.FAILED,
                    errorMessage = e.message
                )
            )
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Erro ao enviar pré-mediçao",
                body = "Verifique o erro em Perfil - Sincronizações",
            )
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

            if (ConnectivityUtils.isNetworkGood(applicationContext) && ConnectivityUtils.hasRealInternetConnection()) {
                Log.e("syncContract", "Internet")
                queueDao.update(inProgressItem)
                val response = contractRepository.syncContracts()
                checkResponse(response, item)

            } else {
                Log.e("syncContract", "Sem Internet")
                Result.retry()
            }
        } catch (e: Exception) {
            queueDao.update(
                inProgressItem.copy(
                    status = SyncStatus.FAILED,
                    errorMessage = e.message
                )
            )
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Erro ao enviar pré-mediçao",
                body = "Verifique o erro em Perfil - Sincronizações",
            )
            Result.failure()
        }
    }

    private suspend fun syncExecutions(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (!ConnectivityUtils.isNetworkGood(applicationContext) && !ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = indirectExecutionRepository.syncExecutions()
            checkResponse(response, item)

        } catch (e: Exception) {
            queueDao.update(
                inProgressItem.copy(
                    status = SyncStatus.FAILED,
                    errorMessage = e.message
                )
            )
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Erro ao enviar pré-mediçao",
                body = "Verifique o erro em Perfil - Sincronizações",
            )
            Result.failure()
        }
    }

    private suspend fun syncStock(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (!ConnectivityUtils.isNetworkGood(applicationContext) && !ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = maintenanceRepository.callGetStock()
            checkResponse(response, item)

        } catch (e: Exception) {
            queueDao.update(
                inProgressItem.copy(
                    status = SyncStatus.FAILED,
                    errorMessage = e.message
                )
            )
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Problema ao sincronizar estoque",
                body = "Clique para saber mais",
                action = Routes.SYNC,
                time = Utils.dateTime.toString(),
                type = NotificationType.WARNING
            )
            Result.failure()
        }
    }

    private suspend fun postIndirectExecution(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (item.relatedId == null) {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                return Result.success()
            }

            if (!ConnectivityUtils.isNetworkGood(applicationContext) && !ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = indirectExecutionRepository.postExecution(item.relatedId)
            checkResponse(response, item)

        } catch (e: Exception) {
            queueDao.update(
                inProgressItem.copy(
                    status = SyncStatus.FAILED,
                    errorMessage = e.message
                )
            )
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Erro ao enviar pré-mediçao",
                body = "Verifique o erro em Perfil - Sincronizações",
            )
            Result.failure()
        }
    }

    private suspend fun postDirectExecution(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (item.relatedId == null) {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                return Result.success()
            }

            if (!ConnectivityUtils.isNetworkGood(applicationContext) && !ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = directExecutionRepository.postDirectExecution(item.relatedId)
            checkResponse(response, item)

        } catch (e: Exception) {
            queueDao.update(
                inProgressItem.copy(
                    status = SyncStatus.FAILED,
                    errorMessage = e.message
                )
            )
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Erro ao enviar pré-mediçao",
                body = "Verifique o erro em Perfil - Sincronizações",
            )
            Result.failure()
        }
    }

    private suspend fun finishedDirectExecution(
        item: SyncQueueEntity,
    ): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (item.relatedId == null) {
                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
                return Result.success()
            }

            if (!ConnectivityUtils.isNetworkGood(applicationContext) && !ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = directExecutionRepository.finishedDirectExecution(item.relatedId)
            checkResponse(response, item)

        } catch (e: Exception) {
            queueDao.update(
                inProgressItem.copy(
                    status = SyncStatus.FAILED,
                    errorMessage = e.message
                )
            )
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Erro ao enviar execução",
                body = "Verifique o erro no caminho Mais -> Perfil -> Tarefas em Sincronizações",
            )
            Result.failure()
        }
    }

    private suspend fun checkResponse(
        response: RequestResult<*>,
        inProgressItem: SyncQueueEntity,
    ): Result {
        val message =
            when (inProgressItem.type) {
                SyncTypes.POST_PRE_MEASUREMENT -> "Falha ao enviar pré-medição"
                SyncTypes.POST_DIRECT_EXECUTION -> "Falha ao enviar execução"
                SyncTypes.POST_MAINTENANCE -> "Falha ao enviar manutenção"
                SyncTypes.POST_INDIRECT_EXECUTION -> "Falha ao enviar execução"
                else -> "Problema ao comunicar com servidor"
            }


        return when (response) {
            is RequestResult.Success -> {
                if (inProgressItem.type == SyncTypes.POST_PRE_MEASUREMENT) {
                    val data = response.data as? String
                    Log.e("data", data.toString())
                    if (data != "0") return Result.retry()
                }

                queueDao.deleteById(inProgressItem.id)
                Result.success()
            }

            is RequestResult.SuccessEmptyBody -> {
                queueDao.deleteById(inProgressItem.id)
                Result.success()
            }

            is RequestResult.NoInternet -> {
                queueDao.update(inProgressItem.copy(errorMessage = "Durante uma tentativa de comunicação com o servidor, este dispositivo estava sem internet. Tente novamente."))
                Result.retry()
            }

            is RequestResult.Timeout -> {
                queueDao.update(inProgressItem.copy(errorMessage = "Timeout - Solicitação excedeu o tempo de espera. Tente novamente agora."))
                Result.retry()
            }

            is RequestResult.ServerError -> {
                queueDao.update(
                    inProgressItem.copy(
                        status = SyncStatus.FAILED,
                        errorMessage = response.message,
                    )
                )
                UserExperience.sendNotification(
                    context = applicationContext,
                    title = message,
                    body = "Clique para saber mais",
                    action = Routes.SYNC,
                    time = Utils.dateTime.toString(),
                    type = NotificationType.WARNING
                )
                Result.retry()
            }

            is RequestResult.UnknownError -> {

                // Marca como failed para evitar retries automáticos
                queueDao.update(
                    inProgressItem.copy(
                        status = SyncStatus.FAILED,
                        errorMessage = response.error.message ?: response.error.toString()
                    )
                )
                UserExperience.sendNotification(
                    context = applicationContext,
                    title = message,
                    body = "Clique para saber mais",
                    action = Routes.SYNC,
                    time = Utils.dateTime.toString(),
                    type = NotificationType.WARNING
                )

                // Pode enviar para um sistema de logs, Crashlytics etc
//                Crashlytics.logException(Exception("Erro desconhecido na sync: $inProgressItem"))

                return Result.retry()
            }

        }
    }


}

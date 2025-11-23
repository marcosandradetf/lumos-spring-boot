package com.lumos.worker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.lumos.MainActivity
import com.lumos.R
import com.lumos.api.ApiService
import com.lumos.api.ContractApi
import com.lumos.api.DirectExecutionApi
import com.lumos.api.NotificationType
import com.lumos.api.PreMeasurementApi
import com.lumos.api.RequestResult
import com.lumos.api.UpdateEntity
import com.lumos.api.UserExperience
import com.lumos.data.database.AppDatabase
import com.lumos.data.database.QueueDao
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.midleware.SecureStorage
import com.lumos.navigation.Routes
import com.lumos.repository.ContractRepository
import com.lumos.repository.DirectExecutionRepository
import com.lumos.repository.GenericRepository
import com.lumos.repository.MaintenanceRepository
import com.lumos.repository.PreMeasurementRepository
import com.lumos.repository.StockRepository
import com.lumos.repository.TeamRepository
import com.lumos.utils.ConnectivityUtils
import com.lumos.utils.SyncLoading
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
    const val SYNC_CONTRACT_ITEM_BALANCE = "SYNC_CONTRACT_ITEM_BALANCE"
    const val SYNC_EXECUTIONS = "SYNC_EXECUTIONS"

    const val POST_GENERIC = "POST_GENERIC"
    const val POST_PRE_MEASUREMENT = "POST_PRE_MEASUREMENT"
    const val POST_INDIRECT_EXECUTION = "POST_INDIRECT_EXECUTION"
    const val POST_DIRECT_EXECUTION = "POST_DIRECT_EXECUTION"
    const val POST_MAINTENANCE = "POST_MAINTENANCE"
    const val POST_ORDER = "POST_ORDER"
    const val POST_MAINTENANCE_STREET = "POST_MAINTENANCE_STREET"
    const val UPDATE_TEAM = "UPDATE_TEAM"
    const val FINISHED_DIRECT_EXECUTION = "FINISHED_DIRECT_EXECUTION"

    const val SUBMIT_PRE_MEASUREMENT_INSTALLATION_STREET = "SUBMIT_PRE_MEASUREMENT_INSTALLATION_STREET"
    // PreMeasurementInstallation
    const val SUBMIT_PRE_MEASUREMENT_INSTALLATION = "SUBMIT_PRE_MEASUREMENT_INSTALLATION"

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
    private val directExecutionRepository: DirectExecutionRepository
    private val genericRepository: GenericRepository
    private val stockRepository: StockRepository
    private val maintenanceRepository: MaintenanceRepository
    private val teamRepository: TeamRepository

    init {
        val api = ApiService(app.applicationContext, secureStorage)
        val db = AppDatabase.getInstance(app.applicationContext)

        queueDao = db.queueDao()

        val preMeasurementApi = api.createApi(PreMeasurementApi::class.java)
        val contractApi = api.createApi(ContractApi::class.java)
        val executionApi = api.createApi(DirectExecutionApi::class.java)

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

        directExecutionRepository = DirectExecutionRepository(
            db = db,
            api = executionApi,
            secureStorage = secureStorage,
            app = app
        )

        genericRepository = GenericRepository(
            api = api
        )

        stockRepository = StockRepository(
            db = db,
            api = api,
            secureStorage = secureStorage,
            app = app
        )

        maintenanceRepository = MaintenanceRepository(
            db = db,
            api = api,
            app = app,
            secureStorage = secureStorage
        )

        teamRepository = TeamRepository(
            db = db,
            api = api,
            secureStorage = secureStorage,
            app = app
        )
    }

    override suspend fun doWork(): Result {
        setForeground(foregroundInfo())

        SyncLoading.publish(true)
        val pendingItems = queueDao.getItemsToProcess()

        for (item in pendingItems) {
            queueDao.update(item.copy(status = SyncStatus.IN_PROGRESS))

            val result = when (item.type) {
                SyncTypes.SYNC_CONTRACT_ITEMS -> syncContractItems(item)
                SyncTypes.SYNC_CONTRACTS -> syncContract(item)
//                SyncTypes.SYNC_EXECUTIONS -> syncExecutions(item)
                SyncTypes.SYNC_STOCK -> syncStock(item)

                SyncTypes.POST_PRE_MEASUREMENT -> postPreMeasurement(item)
                SyncTypes.POST_GENERIC -> postGeneric(item)
//                SyncTypes.POST_INDIRECT_EXECUTION -> postIndirectExecution(item)
                SyncTypes.POST_DIRECT_EXECUTION -> postDirectExecution(item)
                SyncTypes.POST_ORDER -> postOrder(item)
                SyncTypes.POST_MAINTENANCE_STREET -> postMaintenanceStreet(item)
                SyncTypes.POST_MAINTENANCE -> postMaintenance(item)
                SyncTypes.UPDATE_TEAM -> updateTeam(item)

                SyncTypes.FINISHED_DIRECT_EXECUTION -> finishedDirectExecution(item)
                // SyncTypes.UPLOAD_STREET_PHOTOS -> uploadStreetPhotos(item)
                else -> {
                    Log.e("SyncWorker", "Tipo desconhecido: ${item.type}")
                    queueDao.update(item.copy(status = SyncStatus.FAILED))
                    SyncLoading.publish(false)
                    Result.failure()
                }
            }

            if(result == Result.retry()) {
                // Rede/servidor indisponível → retry global seguro
                SyncLoading.publish(false)
                return Result.retry()
            }
        }

        SyncLoading.publish(false)
        return Result.success()
    }

    private fun foregroundInfo(): ForegroundInfo {
        val channelId = "sync_worker_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Sincronização",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Sincronizando dados")
            .setContentText("Enviando informações ao servidor…")
            .setSmallIcon(R.drawable.ic_lumos) // ícone válido
            .setOngoing(true)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0

        return ForegroundInfo(1001, notification, type)
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

            if (ConnectivityUtils.hasRealInternetConnection()) {
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




    private suspend fun postPreMeasurement(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (!ConnectivityUtils.hasRealInternetConnection()) {
                return Result.retry()
            }

            if (item.relatedUuid == null) {
                return Result.failure()
            }

            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            queueDao.update(inProgressItem)
            val preMeasurement = preMeasurementRepository.getPreMeasurement(item.relatedUuid)
            val streets = preMeasurementRepository.getAllStreets(item.relatedUuid)
            val items = preMeasurementRepository.getItems(item.relatedUuid)

            val response = preMeasurementRepository.sendMeasurementToBackend(
                preMeasurement,
                streets,
                items
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

            if (ConnectivityUtils.hasRealInternetConnection()) {
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

            if (ConnectivityUtils.hasRealInternetConnection()) {
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

//    private suspend fun syncExecutions(item: SyncQueueEntity): Result {
//        val inProgressItem = item.copy(
//            status = SyncStatus.IN_PROGRESS,
//            attemptCount = item.attemptCount + 1
//        )
//
//        return try {
//            if (!ConnectivityUtils.hasRealInternetConnection()) return Result.retry()
//
//            queueDao.update(inProgressItem)
//            // Atualiza o item com novo status e tentativa
//
//            // Checa limite de tentativas antes de continuar
//            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
//                return Result.success() // não tenta mais esse
//            }
//
//            val response = indirectExecutionRepository.syncExecutions()
//            checkResponse(response, item)
//
//        } catch (e: Exception) {
//            queueDao.update(
//                inProgressItem.copy(
//                    status = SyncStatus.FAILED,
//                    errorMessage = e.message
//                )
//            )
//            UserExperience.sendNotification(
//                context = applicationContext,
//                title = "Erro ao enviar pré-mediçao",
//                body = "Verifique o erro em Perfil - Sincronizações",
//            )
//            Result.failure()
//        }
//    }

    private suspend fun syncStock(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (!ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = stockRepository.callGetStock()
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

//    private suspend fun postIndirectExecution(item: SyncQueueEntity): Result {
//        val inProgressItem = item.copy(
//            status = SyncStatus.IN_PROGRESS,
//            attemptCount = item.attemptCount + 1
//        )
//
//        return try {
//            if (item.relatedId == null) {
//                queueDao.update(inProgressItem.copy(status = SyncStatus.FAILED))
//                return Result.success()
//            }
//
//            if (!ConnectivityUtils.hasRealInternetConnection()) return Result.retry()
//
//            queueDao.update(inProgressItem)
//            // Atualiza o item com novo status e tentativa
//
//            // Checa limite de tentativas antes de continuar
//            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
//                return Result.success() // não tenta mais esse
//            }
//
//            val response = indirectExecutionRepository.postExecution(item.relatedId)
//            checkResponse(response, item)
//
//        } catch (e: Exception) {
//            queueDao.update(
//                inProgressItem.copy(
//                    status = SyncStatus.FAILED,
//                    errorMessage = e.message
//                )
//            )
//            UserExperience.sendNotification(
//                context = applicationContext,
//                title = "Erro ao enviar pré-mediçao",
//                body = "Verifique o erro em Perfil - Sincronizações",
//            )
//            Result.failure()
//        }
//    }

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

            if (!ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = directExecutionRepository.submitStreet(item.relatedId)
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
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.failure()
        }
    }

    private suspend fun postOrder(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (item.relatedUuid == null) {
                queueDao.update(
                    inProgressItem.copy(
                        status = SyncStatus.FAILED,
                        errorMessage = "Worker - Código não encontrado."
                    )
                )
                return Result.success()
            }

            if (!ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = stockRepository.callPostOrder(item.relatedUuid)
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
                title = "Falha ao enviar requisição de materiais",
                body = "Clique para saber mais",
            )
            Result.failure()
        }

    }

    private suspend fun postMaintenanceStreet(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (item.relatedUuid == null) {
                queueDao.update(
                    inProgressItem.copy(
                        status = SyncStatus.FAILED,
                        errorMessage = "Worker - Código não encontrado."
                    )
                )
                return Result.success()
            }

            if (!ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = maintenanceRepository.callPostMaintenanceStreet(item.relatedUuid)
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
                title = "Falha ao rua finalizada da manutenção",
                body = "Clique para saber mais",
            )

            FirebaseCrashlytics.getInstance().recordException(e)

            Result.failure()
        }

    }

    private suspend fun postMaintenance(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (item.relatedUuid == null) {
                queueDao.update(
                    inProgressItem.copy(
                        status = SyncStatus.FAILED,
                        errorMessage = "Worker - Código não encontrado."
                    )
                )
                return Result.success()
            }

            if (!ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = maintenanceRepository.callPostMaintenance(item.relatedUuid)
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
                title = "Falha ao enviar manutenção",
                body = "Clique para saber mais",
            )
            FirebaseCrashlytics.getInstance().recordException(e)
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

            if (!ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = directExecutionRepository.submitDirectExecution(item.relatedId)
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
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.failure()
        }
    }

    private suspend fun updateTeam(item: SyncQueueEntity): Result {
        val inProgressItem = item.copy(
            status = SyncStatus.IN_PROGRESS,
            attemptCount = item.attemptCount + 1
        )

        return try {
            if (!ConnectivityUtils.hasRealInternetConnection()) return Result.retry()

            queueDao.update(inProgressItem)
            // Atualiza o item com novo status e tentativa

            // Checa limite de tentativas antes de continuar
            if (inProgressItem.attemptCount >= 5 && item.status == SyncStatus.FAILED) {
                return Result.success() // não tenta mais esse
            }

            val response = teamRepository.callPostUpdateTeam()
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
                SyncTypes.POST_ORDER -> "Falha ao enviar requisição de materiais"
                SyncTypes.POST_MAINTENANCE_STREET -> "Falha ao enviar rua finalizada da manutenção"
                else -> "Problema ao comunicar com servidor"
            }


        return when (response) {
            is RequestResult.Success -> {
                if (inProgressItem.type == SyncTypes.POST_PRE_MEASUREMENT) {
                    val data = response.data
                    Log.e("response", response.data.toString())
                    if (data.toString() != "0") return Result.retry()

                    preMeasurementRepository.finishPreMeasurement(inProgressItem.relatedUuid!!)
                }

                queueDao.deleteById(
                    id = inProgressItem.id
                )
                Result.success()
            }

            is RequestResult.SuccessEmptyBody -> {
                queueDao.deleteById(
                    id = inProgressItem.id
                )
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

                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra("destination", Routes.SYNC) // 👉 identificando a rota
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )


                UserExperience.sendNotification(
                    context = applicationContext,
                    title = message,
                    body = response.message ?: "Clique para saber mais",
                    intent = pendingIntent,
                    action = Routes.SYNC,
                    time = Utils.dateTime.toString(),
                    type = NotificationType.WARNING
                )

                FirebaseCrashlytics.getInstance().recordException(Exception("$message - $inProgressItem: $message"))

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
                FirebaseCrashlytics.getInstance().recordException(Exception("$message - $inProgressItem: $message"))

                return Result.retry()
            }

        }
    }


}

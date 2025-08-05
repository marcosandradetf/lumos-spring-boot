package com.lumos.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lumos.data.api.ApiService
import com.lumos.data.api.DownloadApi
import com.lumos.data.api.UserExperience
import com.lumos.midleware.SecureStorage
import com.lumos.utils.ConnectivityUtils
import java.io.File
import java.io.FileOutputStream

class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private var api: DownloadApi

    init {
        val secureStorage = SecureStorage(context)
        val apiService = ApiService(context, secureStorage)
        api = apiService.createApi(DownloadApi::class.java) // Agora pode ser atribuído corretamente
    }

    override suspend fun doWork(): Result {
        return try {
            // Obtendo a URL do arquivo a ser baixado
            val url = inputData.getString("file_url") ?: return Result.failure()
            val fileName = url.substringAfterLast("/")

            // Verifica a conectividade antes de tentar o download
            if (ConnectivityUtils.hasRealInternetConnection()) {
                // Realiza o download do arquivo
                val response = api.downloadFile(url)
                if (!response.isSuccessful || response.body() == null) {
                    Log.e("DownloadWorker", "Falha ao baixar arquivo: ${response.code()}")
                    return Result.failure()
                }

                // Salvando o arquivo no armazenamento privado do app
                val file = File(applicationContext.filesDir, fileName)
                response.body()?.byteStream()?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                // Exibe a notificação de sucesso
                showNotification(file)

                // Retorna o caminho do arquivo como resultado
                return Result.success(workDataOf("file_path" to file.absolutePath))
            } else {
                Log.e("DownloadWorker", "Sem Internet")
                return Result.retry() // Tenta novamente em caso de falha na conectividade
            }
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Erro ao baixar arquivo: ${e.message}")
            return Result.failure() // Retenta em caso de falha
        }
    }

    private fun showNotification(file: File) {
        // Criação do intent para abrir o arquivo armazenado no armazenamento privado
        val uri =
            Uri.fromFile(file) // Usando Uri diretamente, já que o arquivo está no armazenamento privado

        // Criação do intent para abrir o arquivo
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri // Deixe o Android determinar o tipo MIME automaticamente
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Criação de um PendingIntent para o click na notificação
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Envio da notificação
        UserExperience.sendNotification(
            context = applicationContext, // Deve usar o contexto adequado
            title = "Download Finalizado",
            body = "Clique para abrir o arquivo",
            intent = pendingIntent
        )
    }
}

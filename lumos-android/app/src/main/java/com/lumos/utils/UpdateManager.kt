package com.lumos.utils

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object UpdateManager {

    data class UpdateInfo(
        val latestVersion: String,
        val apkUrl: String,
        val forceUpdate: Boolean,
        val releaseNotes: String
    )


    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute() // síncrono, seguro em IO

        if (!response.isSuccessful) {
            throw IOException("Failed to download file")
        }

        val body = response.body ?: throw IOException("Empty response body")
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IOException("External files directory not available")

        val file = File(dir, "update.apk")
        val outputStream = FileOutputStream(file)

        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        var totalBytesRead = 0L

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                val progress = if (contentLength > 0) {
                    (totalBytesRead * 100 / contentLength).toInt()
                } else -1
                onProgress(progress)
            }
            outputStream.flush()
            return@withContext file
        } catch (e: Exception) {
            throw e
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }



}

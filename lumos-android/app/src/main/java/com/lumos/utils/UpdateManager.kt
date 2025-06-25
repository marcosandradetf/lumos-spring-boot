package com.lumos.utils

import android.content.Context
import android.os.Environment
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


    fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit,
        onComplete: (File) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val client = OkHttpClient()

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError(IOException("Failed to download file"))
                    return
                }

                val body = response.body
                if (body == null) {
                    onError(IOException("Empty response body"))
                    return
                }

                val contentLength = body.contentLength()

                val inputStream = body.byteStream()
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "update.apk")
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
                        } else {
                            -1
                        }
                        onProgress(progress)
                    }
                    outputStream.flush()
                    onComplete(file)
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    inputStream.close()
                    outputStream.close()
                }
            }
        })
    }


}

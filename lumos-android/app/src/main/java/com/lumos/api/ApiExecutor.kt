package com.lumos.api

import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ApiExecutor {
    suspend fun <T> execute(call: suspend () -> Response<T>): RequestResult<T> {
        return try {
            val response = call()

            if (response.isSuccessful) {
                val body = response.body()

                if (response.code() == 204 || body == null) {
                    RequestResult.SuccessEmptyBody
                } else {
                    RequestResult.Success(body)
                }

            } else {
                val errorBody = response.errorBody()?.string()
                val payload = try {
                    JSONObject(errorBody ?: "")
                } catch (e: Exception) {
                    null
                }

                val errorCode = payload?.optString("error")?.takeIf { it.isNotBlank() }
                val errorMessage = payload?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: errorCode
                    ?: response.message()

                RequestResult.ServerError(
                    response.code(),
                    errorMessage,
                    errorCode
                )
            }

        } catch (e: UnknownHostException) {
            RequestResult.NoInternet

        } catch (e: SocketTimeoutException) {
            RequestResult.Timeout

        } catch (e: IOException) {
            RequestResult.NoInternet

        } catch (e: Exception) {
            RequestResult.UnknownError(e)
        }
    }

}

sealed class RequestResult<out T> {
    data class Success<out T>(val data: T) : RequestResult<T>()
    data object SuccessEmptyBody : RequestResult<Nothing>()
    data object NoInternet : RequestResult<Nothing>()
    data object Timeout : RequestResult<Nothing>()
    data class ServerError(
        val code: Int,
        val message: String?,
        val errorCode: String? = null
    ) : RequestResult<Nothing>()
    data class UnknownError(val error: Throwable) : RequestResult<Nothing>()
}


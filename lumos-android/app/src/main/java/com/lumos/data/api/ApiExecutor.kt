package com.lumos.data.api

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
                val errorMessage = errorBody?.ifBlank { null } ?: response.message()

                RequestResult.ServerError(
                    response.code(),
                    errorMessage
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
    data class ServerError(val code: Int, val message: String?) : RequestResult<Nothing>()
    data class UnknownError(val error: Throwable) : RequestResult<Nothing>()
}


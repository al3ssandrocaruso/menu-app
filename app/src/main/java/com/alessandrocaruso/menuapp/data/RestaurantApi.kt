package com.alessandrocaruso.menuapp.data

import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkError
import com.android.volley.NoConnectionError
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A transport-level failure, classified so the UI can show a message a user can act on. */
class ApiException(
    val kind: Kind,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(kind.name, cause) {
    enum class Kind { NoConnection, Timeout, NotFound, Server, Unknown }
}

/**
 * Read-only HTTP access to the restaurant data set.
 *
 * Every call is a `suspend` function: Volley still performs the request on its own network
 * threads, and the coroutine is resumed when the response arrives. Cancelling the caller's
 * coroutine cancels the in-flight request, so navigating away does not leave work running.
 *
 * The [RequestQueue] is injected rather than created per call — one queue for the process means
 * one thread pool and one shared HTTP cache, instead of a fresh pool (and a leaked disk cache)
 * for every request.
 */
class RestaurantApi(
    private val queue: RequestQueue,
    private val baseUrl: String,
) {

    /** Fetches the list of restaurant previews shown on the home screen. */
    suspend fun fetchPreviews(): String = get("restaurants/allpreviews")

    /** Fetches the full menu document for [restaurantId]. */
    suspend fun fetchMenu(restaurantId: String): String = get("menus/$restaurantId")

    private suspend fun get(path: String): String = suspendCancellableCoroutine { continuation ->
        val request = StringRequest(
            Request.Method.GET,
            baseUrl + path,
            { response -> continuation.resume(response) },
            { error -> continuation.resumeWithException(error.toApiException()) },
        ).apply {
            tag = this@RestaurantApi
            retryPolicy = DefaultRetryPolicy(
                TIMEOUT_MS,
                MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT,
            )
        }

        continuation.invokeOnCancellation { request.cancel() }
        queue.add(request)
    }

    private fun VolleyError.toApiException(): ApiException {
        val status = networkResponse?.statusCode
        val kind = when {
            status == 404 -> ApiException.Kind.NotFound
            this is NoConnectionError || this is NetworkError -> ApiException.Kind.NoConnection
            this is TimeoutError -> ApiException.Kind.Timeout
            this is ServerError || this is AuthFailureError -> ApiException.Kind.Server
            this is ParseError -> ApiException.Kind.Unknown
            else -> ApiException.Kind.Unknown
        }
        return ApiException(kind, status, this)
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val MAX_RETRIES = 1
    }
}

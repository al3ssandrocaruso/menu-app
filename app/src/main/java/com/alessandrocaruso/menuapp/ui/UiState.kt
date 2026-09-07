package com.alessandrocaruso.menuapp.ui

import com.alessandrocaruso.menuapp.data.ApiException
import com.alessandrocaruso.menuapp.data.MenuParseException

/**
 * Every asynchronous screen is in exactly one of these states.
 *
 * Modelling loading and failure explicitly is what lets the UI show a spinner and a retry button;
 * the original code had no representation for either, so a failed request left a blank screen.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val kind: ErrorKind) : UiState<Nothing>
}

/** Failure categories the UI has a distinct message for. */
enum class ErrorKind {
    NoConnection,
    Timeout,
    NotFound,
    Server,
    /** The response arrived but did not match the expected schema. */
    Malformed,
    Unknown,
}

/** Maps data-layer failures onto the categories the UI knows how to explain. */
fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is ApiException -> when (kind) {
        ApiException.Kind.NoConnection -> ErrorKind.NoConnection
        ApiException.Kind.Timeout -> ErrorKind.Timeout
        ApiException.Kind.NotFound -> ErrorKind.NotFound
        ApiException.Kind.Server -> ErrorKind.Server
        ApiException.Kind.Unknown -> ErrorKind.Unknown
    }
    is MenuParseException -> ErrorKind.Malformed
    else -> ErrorKind.Unknown
}

package com.rudderstack.kotlin.internals.network

/**
 * Sealed class representing the result of an operation that can either be a success or a failure.
 *
 * This class is used to encapsulate the outcome of an operation, providing a way to handle both successful and
 * erroneous results in a type-safe manner. The result can be either a `Success` or a `Failure`.
 *
 * @param T The type of the successful result.
 */
sealed class Result<out T> {

    /**
     * Represents a successful result of an operation.
     *
     * @param response The successful result of the operation. The type is defined by the generic type parameter [T].
     */
    class Success<out T>(val response: T) : Result<T>()

    /**
     * Represents a failed result of an operation.
     *
     * @param status The error status associated with the failure. This provides context about the nature of the failure.
     * @param error The exception or throwable that caused the failure. This provides details about what went wrong.
     */
    class Failure(val status: ErrorStatus, val error: Throwable) : Result<Nothing>()
}

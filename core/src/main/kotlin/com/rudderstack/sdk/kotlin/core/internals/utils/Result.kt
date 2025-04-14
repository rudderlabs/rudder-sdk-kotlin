package com.rudderstack.sdk.kotlin.core.internals.utils

/**
 * A sealed class representing a result that can either be a success or a failure.
 *
 * @param T The type of the success result.
 * @param E The type of the failure error.
 */
sealed class Result<out T, out E> {

    /**
     * Represents a successful result.
     *
     * @param response The successful response of type [T].
     */
    class Success<out T>(
        val response: T
    ) : Result<T, Nothing>()

    /**
     * Represents a failure result.
     *
     * @param error The error result of type [E].
     */
    class Failure<out E>(
        val error: E
    ) : Result<Nothing, E>()

    /**
     * Returns this result if it is a [Success], otherwise returns the result of the
     * [fallback] function.
     *
     * @param R The type of the fallback success result.
     * @param fallback A function that returns a fallback result of type [Result] in case of failure.
     * @return This result if it is a [Success], otherwise the result of [fallback].
     */
    fun <R> orElse(fallback: () -> Result<R, @UnsafeVariance E>): Result<Any?, E> {
        return when (this) {
            is Success -> this
            is Failure -> fallback()
        }
    }
}

package com.rudderstack.core.internals.web

sealed class Result<out T>
class Success<out T>(val response: T): Result<T>()
class Failure(val status: NetworkStatus, val error: Throwable): Result<Nothing>()

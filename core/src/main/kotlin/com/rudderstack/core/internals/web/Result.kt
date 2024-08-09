package com.rudderstack.core.internals.web

sealed class Result<out T>
class Success<out T>(val response: T): Result<T>()
class Failure(val status: ErrorStatus, val error: Throwable): Result<Nothing>()

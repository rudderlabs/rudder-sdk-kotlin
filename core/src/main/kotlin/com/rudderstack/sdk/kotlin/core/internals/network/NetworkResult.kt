package com.rudderstack.sdk.kotlin.core.internals.network

import com.rudderstack.sdk.kotlin.core.internals.utils.Result

/**
 * The result of a network operation.
 */
typealias NetworkResult = Result<String, NetworkErrorStatus>

package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.internals.utils.Result

/**
 * The result of a destination initialisation returned in `onDestinationReady` callback.
 */
typealias DestinationResult = Result<Unit, Exception>

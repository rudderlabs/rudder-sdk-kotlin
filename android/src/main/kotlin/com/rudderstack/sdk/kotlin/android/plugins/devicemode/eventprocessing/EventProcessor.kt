package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.Event

interface EventProcessor {

    fun process(event: Event, key: String, destination: Destination): Event?
}

package com.rudderstack.sdk.kotlin.android.utils

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject

fun provideEvent(): Event = TrackEvent(
    event = "Sample Event",
    properties = emptyJsonObject,
)

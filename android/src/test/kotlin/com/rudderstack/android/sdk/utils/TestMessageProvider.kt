package com.rudderstack.android.sdk.utils

import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject

fun provideEvent(): Message = TrackEvent(
    event = "Sample Event",
    properties = emptyJsonObject,
)

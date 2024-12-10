package com.rudderstack.android.sdk.utils

import com.rudderstack.sdk.kotlin.core.internals.models.Message
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject

fun provideEvent(): Message = TrackEvent(
    event = "Sample Event",
    properties = emptyJsonObject,
)

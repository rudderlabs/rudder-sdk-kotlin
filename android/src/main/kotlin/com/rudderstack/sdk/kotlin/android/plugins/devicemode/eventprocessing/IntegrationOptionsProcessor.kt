package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.utils.getBoolean
import com.rudderstack.sdk.kotlin.android.utils.isFalseOrNull
import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.Event

internal class IntegrationOptionsProcessor : EventProcessor {

    override fun process(event: Event, key: String, destination: Destination): Event? {
        val integrationOptions = event.integrations
        val isDestinationDisabled = integrationOptions.getBoolean(key) == false || (
            integrationOptions.getBoolean("All")
                .isFalseOrNull() && integrationOptions.getBoolean(key).isFalseOrNull()
            )
        return if (isDestinationDisabled) {
            null
        } else {
            event
        }
    }
}

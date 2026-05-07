package com.rudderstack.testapp.spy

import android.content.Context
import com.rudderstack.scenarioengine.domain.spy.SpyObservation
import com.rudderstack.testapp.ipc.Events
import kotlinx.serialization.json.Json

/**
 * Wire [SpyPlugin] observations onto the SUT → driver broadcast channel.
 *
 * Observations ride the existing [com.rudderstack.scenarioengine.ipc.Commands.EVENT_TYPE_SDK_EVENT]
 * intent type — same channel the design doc reserves for unsolicited spy traffic. The driver-side
 * `BroadcastSpyOracle` deserializes the JSON payload back into a [SpyObservation].
 *
 * Construction-once, share-across-plugins: a single sink instance is fine for every
 * [SpyPlugin] in a given [com.rudderstack.testapp.TestApp.spyPluginRegistry] — the observation
 * itself carries its `tag`, so the driver can route after the fact.
 */
internal class BroadcastSpySink(private val context: Context) : SpySink {

    override fun emit(observation: SpyObservation) {
        val payload = Json.encodeToString(SpyObservation.serializer(), observation)
        Events.sendSdkEvent(context, payload)
    }
}

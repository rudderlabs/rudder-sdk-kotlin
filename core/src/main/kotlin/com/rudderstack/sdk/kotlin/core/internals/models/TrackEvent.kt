package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.provideEmptyUserIdentityState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

/**
 * Represents a track event in RudderStack.
 *
 * This data class encapsulates the properties required for a track event.
 *
 * @property event The name of the track event.
 * @property properties The properties associated with the track event.
 * @property options Additional options for the event, encapsulated in a [RudderOption] instance.
 * @property userIdentityState The [UserIdentity] information associated with the track event.
 */
@Serializable
@SerialName("track")
data class TrackEvent(
    var event: String,
    var properties: Properties,
    @Transient override var options: RudderOption = RudderOption(),
    @Transient override var userIdentityState: UserIdentity = provideEmptyUserIdentityState()
) : Event() {

    override var type: EventType = EventType.Track
    override var messageId: String = super.messageId
    override var context: AnalyticsContext = super.context
    override var originalTimestamp: String = super.originalTimestamp
    override val sentAt: String = super.sentAt
    override var userId: String = super.userId
    override lateinit var integrations: JsonObject
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType
}

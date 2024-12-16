package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.provideEmptyUserIdentityState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

/**
 * Represents an identify event in RudderStack.
 *
 * This data class encapsulates the optional [options] and [userIdentityState] related to the identify event.
 * An identify event enables setting `userId`, `traits`, and `externalIds`, which are persisted in storage
 * and must be provided within [userIdentityState].
 *
 * @param options Additional configurations for the event, contained within a [RudderOption] instance.
 * @param userIdentityState The [UserIdentity] information associated with the identify event.
 * @param event The type of event, which is always [MessageType.Identify].
 */
@Serializable
@SerialName("identify")
data class IdentifyEvent(
    @Transient override var options: RudderOption = RudderOption(),
    @Transient override var userIdentityState: UserIdentity = provideEmptyUserIdentityState()
) : Event() {

    override var type: MessageType = MessageType.Identify
    override var messageId: String = super.messageId
    override var context: AnalyticsContext = super.context
    override var originalTimestamp: String = super.originalTimestamp
    override val sentAt: String = super.sentAt
    override var userId: String = super.userId
    override lateinit var integrations: JsonObject
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType
    val event = MessageType.Identify
}

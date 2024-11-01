package com.rudderstack.kotlin.sdk.internals.models

import com.rudderstack.kotlin.sdk.internals.models.useridentity.UserIdentity
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.utils.provideEmptyUserIdentityState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents an identify event message in RudderStack.
 *
 * This data class encapsulates the optional [options] and [userIdentityState] related to the identify event.
 * An identify event enables setting `userId`, `traits`, and `externalIds`, which are persisted in storage
 * and must be provided within [userIdentityState].
 *
 * @param options Additional configurations for the event, contained within a [RudderOption] instance.
 * @param userIdentityState The [UserIdentity] information associated with the identify event.
 */
@Serializable
@SerialName("identify")
data class IdentifyEvent(
    @Transient override var options: RudderOption = RudderOption(),
    @Transient override var userIdentityState: UserIdentity = provideEmptyUserIdentityState()
) : Message() {

    override var type: MessageType = MessageType.Identify
    override var messageId: String = super.messageId
    override var context: AnalyticsContext = super.context
    override var originalTimestamp: String = super.originalTimestamp
    override val sentAt: String = super.sentAt
    override var userId: String = super.userId
    override lateinit var integrations: Map<String, Boolean>
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType
}

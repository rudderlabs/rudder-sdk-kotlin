package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.provideEmptyUserIdentityState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

/**
 * Represents an alias event  in RudderStack.
 *
 * This data class encapsulates the [previousId], optional [options], and [userIdentityState] relevant to the alias event.
 * The alias call links different identities associated with a known user, allowing for a seamless identity transition.
 * The new `userId` should be specified in [userIdentityState].
 * The `previousId` may be provided by the user, default to the last known userId, or use the anonymousId if no prior ID is available.
 *
 * @param previousId The previous ID tied to the user, which may be a user-provided value or fall back on prior identifiers.
 * @param options Additional event configuration options, encapsulated within a [RudderOption] instance.
 * @param userIdentityState The [UserIdentity] information related to the user's identity for this event.
 */
@Serializable
@SerialName("alias")
data class AliasEvent(
    val previousId: String,
    @Transient override var options: RudderOption = RudderOption(),
    @Transient override var userIdentityState: UserIdentity = provideEmptyUserIdentityState()
) : Event() {

    override var type: MessageType = MessageType.Alias
    override var messageId: String = super.messageId
    override var context: AnalyticsContext = super.context
    override var originalTimestamp: String = super.originalTimestamp
    override val sentAt: String = super.sentAt
    override var userId: String = super.userId
    override lateinit var integrations: JsonObject
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType
}

package com.rudderstack.kotlin.sdk.internals.models

import com.rudderstack.kotlin.sdk.internals.models.useridentity.UserIdentity
import com.rudderstack.kotlin.sdk.internals.models.useridentity.provideEmptyUserIdentityState
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("identify")
internal data class IdentifyEvent(
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

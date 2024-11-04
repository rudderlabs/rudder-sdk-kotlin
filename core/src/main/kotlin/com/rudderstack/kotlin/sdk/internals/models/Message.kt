package com.rudderstack.kotlin.sdk.internals.models

import com.rudderstack.kotlin.sdk.internals.models.exception.UnknownMessageKeyException
import com.rudderstack.kotlin.sdk.internals.models.useridentity.UserIdentity
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.utils.DateTimeUtils
import com.rudderstack.kotlin.sdk.internals.utils.addAnonymousIdToTraits
import com.rudderstack.kotlin.sdk.internals.utils.addPersistedValues
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.internals.utils.provideEmptyUserIdentityState
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

typealias AnalyticsContext = JsonObject
typealias Properties = JsonObject
typealias RudderTraits = JsonObject

/*
 * Default timestamp value of sentAt field in Message class.
 * CAUTION: Do not modify this variable's value as it is used by JsonSentAtUpdater to replace sentAt with updated value of timestamp.
 * Do not use it anywhere else.
 */
internal const val DEFAULT_SENT_AT_TIMESTAMP = "{{ RSA_DEF_SENT_AT_TS }}"

/**
 * Represents an empty JSON object.
 */
val emptyJsonObject = JsonObject(emptyMap())

/**
 * Enum class representing the type of message being handled by RudderStack.
 */
@Serializable
enum class MessageType {

    /**
     * Indicates a track message type.
     */
    @SerialName("track")
    Track,

    /**
     * Indicates a flush message type.
     */
    @SerialName("flush")
    Flush,

    @SerialName("screen")
    Screen,

    @SerialName("group")
    Group,

    @SerialName("identify")
    Identify
}

/**
 * Base class for all message types in RudderStack, designed for representing
 * and handling different kinds of events in a unified way.
 *
 * This sealed class is the parent of specific message types like [FlushEvent] and [TrackEvent],
 * ensuring type safety and enabling polymorphism for handling different message types.
 *
 * @property type The type of the message, represented as a [MessageType] enum.
 * @property messageId A unique identifier for the message.
 * @property originalTimestamp The original timestamp when the message was created.
 * @property context The analytics context associated with the message.
 * @property userId The user ID associated with the message.
 * @property sentAt The timestamp when the message is sent to the server.
 * @property integrations The integrations options associated with the message.
 * @property anonymousId The anonymous ID is the Pseudo-identifier for the user in cases where userId is absent.
 * @property channel The platform type associated with the message.
 * @property options RudderOption associated with the message, represented as a [RudderOption] instance. This is transient.
 */
@Serializable(with = BaseMessageSerializer::class)
sealed class Message {

    abstract var type: MessageType
    open var messageId: String = UUID.randomUUID().toString()
    open var originalTimestamp: String = DateTimeUtils.now()
    open var context: AnalyticsContext = emptyJsonObject
    open var userId: String = String.empty()
    internal open var userIdentityState: UserIdentity = provideEmptyUserIdentityState()

    // this sentAt timestamp value will be updated just before sending the payload to server
    // CAUTION: Do not change the default value for this param.
    open val sentAt: String = DEFAULT_SENT_AT_TIMESTAMP
    abstract var integrations: Map<String, Boolean>
    abstract var anonymousId: String
    abstract var channel: PlatformType

    @Transient
    abstract var options: RudderOption

    internal fun updateData(platform: PlatformType) {
        this.addAnonymousIdToTraits()
        this.channel = platform
        this.updateOption()
        this.addPersistedValues()
    }

    /**
     * Copy function to create a deep copy of the message.
     *
     * @param T
     * @return
     */
    fun <T : Message> copy(): T {
        val original = this
        val copy = when (this) {
            is TrackEvent -> TrackEvent(
                event = this.event,
                properties = this.properties,
                options = this.options,
            )

            is ScreenEvent -> ScreenEvent(
                screenName = this.screenName,
                properties = this.properties,
                options = this.options,
            )

            is GroupEvent -> GroupEvent(
                groupId = this.groupId,
                traits = this.traits,
                options = this.options
            )

            is IdentifyEvent -> IdentifyEvent(
                options = this.options,
            )

            is FlushEvent -> FlushEvent()
        }.apply {
            messageId = original.messageId
            originalTimestamp = original.originalTimestamp
            context = original.context
            integrations = original.integrations
            anonymousId = original.anonymousId
            channel = original.channel
            userId = original.userId
        }
        @Suppress("UNCHECKED_CAST")
        return copy as T // This is ok because resultant type will be same as input type
    }
}

/**
 * Represents a flush event message in RudderStack.
 *
 * This data class encapsulates the properties required for a flush message.
 *
 * @property messageName The name of the message associated with the flush event.
 */
@Serializable
@SerialName("flush")
data class FlushEvent(
    var messageName: String = String.empty(),
) : Message() {

    override var type: MessageType = MessageType.Flush
    override lateinit var integrations: Map<String, Boolean>
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType

    @Transient
    override var options: RudderOption = RudderOption()
}

/**
 * Represents a track event message in RudderStack.
 *
 * This data class encapsulates the properties required for a track message.
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
) : Message() {

    override var type: MessageType = MessageType.Track
    override var messageId: String = super.messageId
    override var context: AnalyticsContext = super.context
    override var originalTimestamp: String = super.originalTimestamp
    override val sentAt: String = super.sentAt
    override var userId: String = super.userId
    override lateinit var integrations: Map<String, Boolean>
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType
}

/**
 * Represents a screen event message in RudderStack.
 *
 * This data class encapsulates the properties required for a screen message.
 *
 * @property screenName The name of the screen event.
 * @property properties The properties associated with the screen event.
 * @property options Additional options for the event, encapsulated in a [RudderOption] instance.
 * @property userIdentityState The [UserIdentity] information associated with the screen event.
 */
@Serializable
@SerialName("screen")
data class ScreenEvent(
    @SerialName("event") var screenName: String,
    var properties: Properties,
    @Transient override var options: RudderOption = RudderOption(),
    @Transient override var userIdentityState: UserIdentity = provideEmptyUserIdentityState()
) : Message() {

    override var type: MessageType = MessageType.Screen
    override var messageId: String = super.messageId
    override var context: AnalyticsContext = super.context
    override var originalTimestamp: String = super.originalTimestamp
    override val sentAt: String = super.sentAt
    override var userId: String = super.userId
    override lateinit var integrations: Map<String, Boolean>
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType
}

/**
 * Represents a group event message in RudderStack.
 *
 * This data class encapsulates the properties required for a group message.
 *
 * @property groupId The group id of group event.
 * @property traits The traits associated with the group. event.
 * @property options Additional options for the event, encapsulated in a [RudderOption] instance.
 * @property userIdentityState The [UserIdentity] information associated with the group event.
 */
@Serializable
@SerialName("group")
data class GroupEvent(
    var groupId: String,
    var traits: RudderTraits = emptyJsonObject,
    @Transient override var options: RudderOption = RudderOption(),
    @Transient override var userIdentityState: UserIdentity = provideEmptyUserIdentityState()
) : Message() {

    override var type: MessageType = MessageType.Group
    override var messageId: String = super.messageId
    override var context: AnalyticsContext = super.context
    override var originalTimestamp: String = super.originalTimestamp
    override val sentAt: String = super.sentAt
    override var userId: String = super.userId
    override lateinit var integrations: Map<String, Boolean>
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType
}

/**
 * Custom serializer for the [Message] class, determining which specific type to deserialize into
 * based on the "type" field in the JSON object.
 *
 * This serializer enables polymorphic deserialization for messages, making it possible to
 * serialize and deserialize different types of messages seamlessly.
 */
object BaseMessageSerializer : JsonContentPolymorphicSerializer<Message>(Message::class) {

    /**
     * Selects the appropriate deserializer based on the "type" field in the JSON object.
     *
     * @param element The JSON element to inspect.
     * @return The appropriate deserialization strategy for the given message type.
     */
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Message> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "track" -> TrackEvent.serializer()
            else -> throw UnknownMessageKeyException()
        }
    }
}

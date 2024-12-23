package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.models.exception.UnknownEventKeyException
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.internals.utils.addPersistedValues
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.internals.utils.provideEmptyUserIdentityState
import com.rudderstack.sdk.kotlin.core.internals.utils.updateIntegrationOptionsAndCustomCustomContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

typealias AnalyticsContext = JsonObject
typealias Properties = JsonObject
typealias RudderTraits = JsonObject

/*
 * Default timestamp value of sentAt field in Event class.
 * CAUTION: Do not modify this variable's value as it is used by JsonSentAtUpdater to replace sentAt with updated value of timestamp.
 * Do not use it anywhere else.
 */
internal const val DEFAULT_SENT_AT_TIMESTAMP = "{{ RSA_DEF_SENT_AT_TS }}"

/**
 * Represents an empty JSON object.
 */
val emptyJsonObject = JsonObject(emptyMap())

/**
 * Enum class representing the type of event being handled by RudderStack.
 */
@Serializable
enum class EventType {

    /**
     * Indicates a track event type.
     */
    @SerialName("track")
    Track,

    @SerialName("screen")
    Screen,

    @SerialName("group")
    Group,

    @SerialName("identify")
    Identify,

    @SerialName("alias")
    Alias
}

/**
 * Base class for all event types in RudderStack, designed for representing
 * and handling different kinds of events in a unified way.
 *
 * This sealed class is the parent of specific event types like [TrackEvent] [GroupEvent] [ScreenEvent],
 * ensuring type safety and enabling polymorphism for handling different event types.
 *
 * @property type The type of the event, represented as a [EventType] enum.
 * @property messageId A unique identifier for the event.
 * @property originalTimestamp The original timestamp when the event was created.
 * @property context The analytics context associated with the event.
 * @property userId The user ID associated with the event.
 * @property sentAt The timestamp when the event is sent to the server.
 * @property integrations The integrations options associated with the event.
 * @property anonymousId The anonymous ID is the Pseudo-identifier for the user in cases where userId is absent.
 * @property channel The platform type associated with the event.
 * @property options RudderOption associated with the event, represented as a [RudderOption] instance. This is transient.
 */
@Serializable(with = BaseEventSerializer::class)
sealed class Event {

    abstract var type: EventType
    open var messageId: String = generateUUID()
    open var originalTimestamp: String = DateTimeUtils.now()
    open var context: AnalyticsContext = emptyJsonObject
    open var userId: String = String.empty()
    internal open var userIdentityState: UserIdentity = provideEmptyUserIdentityState()

    // this sentAt timestamp value will be updated just before sending the payload to server
    // CAUTION: Do not change the default value for this param.
    open val sentAt: String = DEFAULT_SENT_AT_TIMESTAMP
    abstract var integrations: JsonObject
    abstract var anonymousId: String
    abstract var channel: PlatformType

    @Transient
    abstract var options: RudderOption

    internal fun updateData(platform: PlatformType) {
        this.channel = platform
        this.updateIntegrationOptionsAndCustomCustomContext()
        this.addPersistedValues()
    }

    /**
     * Copy function to create a deep copy of the event.
     *
     * @param T
     * @return
     */
    fun <T : Event> copy(): T {
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

            is AliasEvent -> AliasEvent(
                previousId = this.previousId,
                options = this.options,
            )
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
 * Represents a screen event in RudderStack.
 *
 * This data class encapsulates the properties required for a screen event.
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
) : Event() {

    override var type: EventType = EventType.Screen
    override var messageId: String = super.messageId
    override var context: AnalyticsContext = super.context
    override var originalTimestamp: String = super.originalTimestamp
    override val sentAt: String = super.sentAt
    override var userId: String = super.userId
    override lateinit var integrations: JsonObject
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType
}

/**
 * Represents a group event in RudderStack.
 *
 * This data class encapsulates the properties required for a group event.
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
) : Event() {

    override var type: EventType = EventType.Group
    override var messageId: String = super.messageId
    override var context: AnalyticsContext = super.context
    override var originalTimestamp: String = super.originalTimestamp
    override val sentAt: String = super.sentAt
    override var userId: String = super.userId
    override lateinit var integrations: JsonObject
    override lateinit var anonymousId: String
    override lateinit var channel: PlatformType
}

/**
 * Custom serializer for the [Event] class, determining which specific type to deserialize into
 * based on the "type" field in the JSON object.
 *
 * This serializer enables polymorphic deserialization for events, making it possible to
 * serialize and deserialize different types of events seamlessly.
 */
object BaseEventSerializer : JsonContentPolymorphicSerializer<Event>(Event::class) {

    /**
     * Selects the appropriate deserializer based on the "type" field in the JSON object.
     *
     * @param element The JSON element to inspect.
     * @return The appropriate deserialization strategy for the given event type.
     */
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Event> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "track" -> TrackEvent.serializer()
            "screen" -> ScreenEvent.serializer()
            "group" -> GroupEvent.serializer()
            "identify" -> IdentifyEvent.serializer()
            "alias" -> AliasEvent.serializer()
            else -> throw UnknownEventKeyException()
        }
    }
}

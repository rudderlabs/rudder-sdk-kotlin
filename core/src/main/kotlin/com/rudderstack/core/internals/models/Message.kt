package com.rudderstack.core.internals.models

import com.rudderstack.core.internals.models.exception.UnknownMessageKeyException
import com.rudderstack.core.internals.utils.DateTimeUtils
import com.rudderstack.core.internals.utils.empty
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

typealias AnalyticsContext = JsonObject
typealias Properties = JsonObject

/**
 * Represents an empty JSON object.
 */
val emptyJsonObject = JsonObject(emptyMap())

/**
 * Represents options that can be passed with a message in the RudderStack analytics context.
 *
 * @property integrations A map of integration names to a boolean indicating if they are enabled.
 * @property externalIds A list of maps representing external IDs associated with the message.
 */
@Serializable
data class RudderOption(
    val integrations: Map<String, Boolean> = emptyMap(),
    val externalIds: List<Map<String, String>> = emptyList(),
)

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
    Flush
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
 * @property newId A new identifier for the message if needed.
 */
@Serializable(with = BaseMessageSerializer::class)
sealed class Message {

    abstract var type: MessageType
    abstract var messageId: String
    abstract var originalTimestamp: String
    abstract var context: AnalyticsContext
    abstract var newId: String

    internal fun applyBaseData() {
        this.originalTimestamp = DateTimeUtils.now()
        this.context = emptyJsonObject
        this.messageId = UUID.randomUUID().toString()
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
                name = this.name,
                options = this.options,
                properties = this.properties
            )

            is FlushEvent -> FlushEvent()
        }.apply {
            messageId = original.messageId
            originalTimestamp = original.originalTimestamp
            context = original.context
            newId = original.newId
        }
        @Suppress("UNCHECKED_CAST")
        return copy as T // This is ok because resultant type will be same as input type
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (type != other.type) return false
        if (messageId != other.messageId) return false
        if (originalTimestamp != other.originalTimestamp) return false
        if (context != other.context) return false
        if (newId != other.newId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + originalTimestamp.hashCode()
        result = 31 * result + context.hashCode()
        result = 31 * result + newId.hashCode()
        return result
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
    override lateinit var messageId: String
    override lateinit var context: AnalyticsContext
    override var newId: String = ""

    override lateinit var originalTimestamp: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as FlushEvent

        if (messageName != other.messageName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + messageName.hashCode()
        return result
    }
}

/**
 * Represents a track event message in RudderStack.
 *
 * This data class encapsulates the properties required for a track message.
 *
 * @property name The name of the track event.
 * @property options Additional options for the event, encapsulated in a [RudderOption] instance.
 * @property properties The properties associated with the track event.
 */
@Serializable
@SerialName("track")
data class TrackEvent(
    var name: String,
    var options: RudderOption,
    var properties: Properties,
) : Message() {

    override var type: MessageType = MessageType.Track
    override lateinit var messageId: String
    override lateinit var context: AnalyticsContext
    override var newId: String = ""

    override lateinit var originalTimestamp: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TrackEvent

        if (properties != other.properties) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
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

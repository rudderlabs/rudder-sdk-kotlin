package com.rudderstack.core.internals.models

import com.rudderstack.core.internals.utils.DateTimeInstant
import com.rudderstack.core.internals.utils.empty
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.*

typealias AnalyticsContext = JsonObject
typealias Properties = JsonObject

val emptyJsonObject = JsonObject(emptyMap())

@Serializable
data class RudderOption(
    val integrations: Map<String, Boolean> = emptyMap(),
    val externalIds: List<Map<String, String>> = emptyList(),
)

@Serializable
enum class MessageType {

    @SerialName("track")
    Track,

    @SerialName("flush")
    Flush
}

/**
 * Principal type class for any message type and will be one of
 * @see FlushEvent
 * @see TrackEvent
 */

@Serializable(with = BaseMessageSerializer::class)
sealed class Message {

    abstract var type: MessageType
    abstract var messageId: String
    abstract var originalTimestamp: String
    abstract var context: AnalyticsContext
    abstract var newId: String

    internal fun applyBaseData() {
        this.originalTimestamp = DateTimeInstant.now()
        this.context = emptyJsonObject
        this.messageId = UUID.randomUUID().toString()
    }

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

object BaseMessageSerializer : JsonContentPolymorphicSerializer<Message>(Message::class) {

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Message> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "track" -> TrackEvent.serializer()
            else -> throw Exception("Unknown message: key 'type' not found or does not matches any message type")
        }
    }
}

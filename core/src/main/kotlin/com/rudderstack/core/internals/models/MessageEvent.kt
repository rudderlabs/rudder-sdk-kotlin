package com.rudderstack.core.internals.models

import com.rudderstack.core.internals.utils.DateTimeInstant
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

typealias Properties = JsonObject
typealias RudderOption = JsonObject


@Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
@Serializable
abstract class MessageEvent(
    open val type: EventType,
    open val messageId: String,
    open val originalTimestamp: String
) {
    fun <T : MessageEvent> copy(): T {
        val original = this
        val copy = when (this) {
            is TrackEvent -> TrackEvent(
                event = this.event,
                options = this.options,
                properties = this.properties
            ).apply {
                type = original.type
                messageId = original.messageId
                originalTimestamp = original.originalTimestamp
            }

            else -> {}
        }
        @Suppress("UNCHECKED_CAST")
        return copy as T // This is ok because resultant type will be same as input type
    }

}

@SerialName("track")
@Serializable(with = BaseEventSerializer::class)
data class TrackEvent(
    val event: String,
    val options: RudderOption?,
    val properties: Properties?,
    override var type: EventType = EventType.Track,
    override var messageId: String = UUID.randomUUID().toString(),
    override var originalTimestamp: String = DateTimeInstant.now(),
) : MessageEvent(
    type = type,
    messageId = messageId,
    originalTimestamp = originalTimestamp,
)


@Serializable
enum class EventType {
    @SerialName("track")
    Track,

    @SerialName("screen")
    Screen,

    @SerialName("alias")
    Alias,

    @SerialName("identify")
    Identify,

    @SerialName("group")
    Group
}

object BaseEventSerializer : JsonContentPolymorphicSerializer<TrackEvent>(TrackEvent::class) {

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out TrackEvent> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "track" -> TrackEvent.serializer()
            else -> throw Exception("Unknown Event: key 'type' not found or does not matches any event type")
        }
    }
}

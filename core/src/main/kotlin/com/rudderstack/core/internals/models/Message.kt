package com.rudderstack.core.internals.models

import com.rudderstack.core.internals.utils.DateTimeInstant
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.*

typealias AnalyticsContext = JsonObject
typealias Integrations = JsonObject
typealias Properties = JsonObject
typealias Traits = JsonObject

val emptyJsonObject = JsonObject(emptyMap())
val emptyJsonArray = JsonArray(emptyList())

@Serializable
data class RudderOption(
    val integrations: Map<String, Boolean> = emptyMap(),
    //   val customContexts: Map<String, Any> = emptyMap(),
    val externalIds: List<Map<String, String>> = emptyList(),
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

/**
 * Principal type class for any event type and will be one of
 * @see TrackEvent
 * @see IdentifyEvent
 * @see ScreenEvent
 * @see GroupEvent
 * @see AliasEvent
 */

@Serializable(with = BaseEventSerializer::class)
sealed class Message {

    abstract var type: EventType
    abstract var messageId: String
    abstract var originalTimestamp: String
    abstract var context: AnalyticsContext
    abstract var newId: String

    internal fun applyBaseData() {
        this.originalTimestamp = DateTimeInstant.now()
        this.context = emptyJsonObject
        this.messageId = UUID.randomUUID().toString()
    }

//    internal suspend fun applyBaseEventData(store: Store) {
//        val userInfo = store.currentState(UserInfo::class) ?: return
//
//        this.anonymousId = userInfo.anonymousId
//        this.integrations = emptyJsonObject
//
//        if (this.userId.isBlank()) {
//            // attach system userId if present
//            this.userId = userInfo.userId ?: ""
//        }
//    }

    // Create a shallow copy of this event payload
    fun <T : Message> copy(): T {
        val original = this
        val copy = when (this) {
            is TrackEvent -> TrackEvent(
                event = this.event,
                options = this.options,
                properties = this.properties
            )
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
@SerialName("track")
data class TrackEvent(
    var event: String,
    var options: RudderOption,
    var properties: Properties,
) : Message() {

    override var type: EventType = EventType.Track
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
        if (event != other.event) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + event.hashCode()
        return result
    }

}

object BaseEventSerializer : JsonContentPolymorphicSerializer<Message>(Message::class) {

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Message> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "track" -> TrackEvent.serializer()
            else -> throw Exception("Unknown Event: key 'type' not found or does not matches any event type")
        }
    }
}

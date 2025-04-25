package com.rudderstack.integration.kotlin.braze

import com.braze.enums.Gender
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.Traits
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal const val BRAZE_EXTERNAL_ID = "2d31d085-4d93-4126-b2b3-94e651810673"
internal const val EMAIL = "test@gmail.com"
internal const val FIRST_NAME = "First Name"
internal const val LAST_NAME = "Last Name"
internal val GENDER_MALE = Gender.MALE
internal val GENDER_FEMALE = Gender.FEMALE
internal const val PHONE_NUMBER = "0123456789"
internal const val CITY = "Palo Alto"
internal const val COUNTRY = "USA"
internal const val USER_ID = "<userId>"

internal object Utility {
    internal val DATE_STRING: String = Date(631172471000).toISOString()

    internal fun Any.readFileAsJsonObject(fileName: String): JsonObject {
        this::class.java.classLoader?.getResourceAsStream(fileName).let { inputStream ->
            inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
        }.let { fileAsString ->
            return Json.parseToJsonElement(fileAsString).jsonObject
        }
    }

    @OptIn(InternalRudderApi::class)
    internal fun provideTrackEvent(
        eventName: String,
        properties: JsonObject = JsonObject(emptyMap()),
    ) = TrackEvent(
        event = eventName,
        properties = properties,
        options = RudderOption(),
    ).also {
        it.applyMockedValues()
        it.updateData(PlatformType.Mobile)
    }

    private fun Event.applyMockedValues() {
        this.originalTimestamp = "<original-timestamp>"
        this.context = emptyJsonObject
        this.messageId = "<message-id>"
    }

    internal fun getCampaignObject(): JsonObject = buildJsonObject {
        put("campaign", buildJsonObject {
            put("source", "Source value")
            put("name", "Name value")
            put("ad_group", "ad_group value")
            put("ad_creative", "ad_creative value")
        })
    }

    internal fun getCustomProperties(): JsonObject = buildJsonObject {
        put("key1", "value1")
        put("key2", "value2")
        put("Product-Key-1", "Product-Value-1")
        put("Product-Key-2", "Product-Value-2")
    }

    internal fun getOrderCompletedProperties(): JsonObject = buildJsonObject {
        put("key1", "value1")
        put("key2", "value2")
        put(
            "products", buildJsonArray {
                add(buildJsonObject {
                    put("product_id", "10011")
                    put("price", 100.11)
                    put("Product-Key-1", "Product-Value-1")
                })
                add(buildJsonObject {
                    put("product_id", "20022")
                    put("price", 200.22)
                    put("Product-Key-2", "Product-Value-2")
                })
            }
        )
    }

    @OptIn(InternalRudderApi::class)
    internal fun provideIdentifyEvent(
        options: RudderOption = provideRudderOptionWithExternalId(),
        userIdentityState: UserIdentity = provideUserIdentity(),
    ) = IdentifyEvent(
        options = options,
        userIdentityState = userIdentityState,
    ).also {
        it.applyMockedValues()
        it.updateData(PlatformType.Mobile)
    }

    private fun provideRudderOptionWithExternalId(
        externalIds: List<ExternalId> = listOf(
            ExternalId(type = "brazeExternalId", id = BRAZE_EXTERNAL_ID),
        )
    ) = RudderOption(
        externalIds = externalIds
    )

    internal fun provideUserIdentity(
        anonymousId: String = "<anonymousId>",
        userId: String = USER_ID,
        traits: Traits = getStandardAndCustomTraits(),
    ) = UserIdentity(
        anonymousId = anonymousId,
        userId = userId,
        traits = traits,
    )

    private fun getStandardAndCustomTraits(): JsonObject = buildJsonObject {
        // Standard traits
        put("birthday", DATE_STRING)
        put("address", buildJsonObject {
            put("city", CITY)
            put("country", COUNTRY)
        })
        put("firstName", FIRST_NAME)
        put("lastName", LAST_NAME)
        put("gender", "Male")
        put("phone", PHONE_NUMBER)
        put("email", EMAIL)

        // Custom Traits
        put("key-1", true)
        put("key-2", 1234)
        put("key-3", 678.45)
        put("key-4", "value-4")
        put("key-5", DATE_STRING)
    }

    internal fun getSlightDifferentStandardAndCustomTraits(): JsonObject = buildJsonObject {
        // Standard traits
        put("birthday", DATE_STRING)
        put("address", buildJsonObject {
            put("city", CITY)
            put("country", COUNTRY)
        })
        put("firstName", FIRST_NAME)
        put("lastName", LAST_NAME)
        put("gender", "FeMale") // Different
        put("phone", PHONE_NUMBER)
        put("email", EMAIL)

        // Custom Traits
        put("key-1", true)
        put("key-2", 1234)
        put("key-3", 678.45)
        put("key-4", "value-43") // Different
        put("key-5", DATE_STRING)
    }

    internal fun Date.toISOString(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(this)
    }

    private const val MILLIS_TO_SECONDS_DIVISOR = 1000
    private fun Long.toSeconds() = this / MILLIS_TO_SECONDS_DIVISOR
    private val iso8601DateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    /**
     * Tries to convert the given [value] to a [Long] representing the time in milliseconds.
     *
     * @param value The value to be converted.
     * @return The time in milliseconds if the conversion is successful, otherwise `null`.
     */
    internal fun tryDateConversion(value: String): Long? {
        return runCatching {
            iso8601DateFormatter.parse(value)?.time?.toSeconds()
        }.getOrNull()
    }
}

package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Data class representing the configuration for Braze Integration.
 *
 * @property apiKey The API key for Braze. Must not be empty or blank.
 * @property customEndpoint The custom endpoint for the data center. Must not be empty or blank.
 * @property supportDedup Flag indicating whether deduplication is supported.
 * @property connectionMode The mode of connection, either HYBRID or DEVICE.
 *
 * @throws IllegalArgumentException if apiKey or customEndpoint is empty or blank.
 */
@Serializable
internal data class RudderBrazeConfig(
    @SerialName("appKey")
    val apiKey: String,
    @Serializable(with = CustomEndpointSerializer::class)
    @SerialName("dataCenter")
    val customEndpoint: String,
    val supportDedup: Boolean,
    val connectionMode: ConnectionMode,
) {

    init {
        require(apiKey.isNotBlank()) { "appKey cannot be empty or blank" }
        require(customEndpoint.isNotBlank()) { "dataCenter cannot be empty or blank" }
    }
}

/**
 * Enum class representing the connection modes for Braze Integration.
 */
@Serializable
internal enum class ConnectionMode {

    /**
     * Represents the hybrid connection mode.
     */
    @SerialName("hybrid")
    HYBRID,

    /**
     * Represents the device connection mode.
     */
    @SerialName("device")
    DEVICE
}

/**
 * Custom serializer for mapping custom endpoint identifiers to their corresponding URLs.
 */
private object CustomEndpointSerializer : KSerializer<String> {

    /**
     * Mapping of custom endpoint identifiers to their corresponding URLs.
     */
    private val customEndpointMapping = mapOf(
        "US-01" to "sdk.iad-01.braze.com",
        "US-02" to "sdk.iad-02.braze.com",
        "US-03" to "sdk.iad-03.braze.com",
        "US-04" to "sdk.iad-04.braze.com",
        "US-05" to "sdk.iad-05.braze.com",
        "US-06" to "sdk.iad-06.braze.com",
        "US-08" to "sdk.iad-08.braze.com",
        "EU-01" to "sdk.fra-01.braze.eu",
        "EU-02" to "sdk.fra-02.braze.eu",
    )

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("customEndpoint", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val customEndpoint = decoder.decodeString().uppercase()
        return customEndpointMapping[customEndpoint]
            ?: throw IllegalArgumentException("Unsupported data center: $customEndpoint")
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value) // Store as the mapped endpoint
    }
}

@Serializable
internal data class InstallAttributed(
    val campaign: Campaign? = null
)

@Serializable
internal data class Campaign(
    val source: String? = null,
    val name: String? = null,
    @SerialName("ad_group") val adGroup: String? = null,
    @SerialName("ad_creative") val adCreative: String? = null,
)

internal fun JsonObject.getCustomProperties(): CustomProperties {
    return this.parseConfig<CustomProperties>()
}

@Serializable(with = CustomPropertiesSerializer::class)
internal data class CustomProperties(
    val products: List<CustomProductsProperties> = emptyList(),
    val customProperties: JsonObject = JsonObject(emptyMap())
) {
    companion object {

        /**
         * Modify it like this:
         * ```
         * CustomProperties.knownKeys.addAll(listOf("products"))
         * ```
         */
        internal var knownKeys: MutableSet<String> = mutableSetOf("products")
    }
}

private object CustomPropertiesSerializer : KSerializer<CustomProperties> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CustomPropertiesSerializer")

    @OptIn(InternalRudderApi::class)
    override fun deserialize(decoder: Decoder): CustomProperties {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val products = jsonObject["products"]?.let {
            LenientJson.decodeFromJsonElement(ListSerializer(CustomProductsProperties.serializer()), it)
        } ?: emptyList()

        val customProperties = jsonObject.filterKeys { it !in CustomProperties.knownKeys }
        return CustomProperties(products, JsonObject(customProperties))
    }

    override fun serialize(encoder: Encoder, value: CustomProperties) {
        throw UnsupportedOperationException("Serialization is not supported")
    }
}

@Serializable(with = CustomProductsPropertiesSerializer::class)
internal data class CustomProductsProperties(
    val customProperties: JsonObject? = null,
) {

    companion object {

        /**
         * Modify it like this:
         * ```
         * CustomProductsProperties.knownKeys.addAll(listOf("product_id", "quantity", "price"))
         * ```
         */
        internal var knownKeys: MutableSet<String> = mutableSetOf("product_id", "quantity", "price")
    }
}

private object CustomProductsPropertiesSerializer : KSerializer<CustomProductsProperties> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Products")

    override fun deserialize(decoder: Decoder): CustomProductsProperties {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val customProperties = jsonObject.filterKeys { it !in CustomProductsProperties.knownKeys }
        return CustomProductsProperties(JsonObject(customProperties))
    }

    override fun serialize(encoder: Encoder, value: CustomProductsProperties) {
        throw UnsupportedOperationException("Serialization is not supported for Products")
    }
}

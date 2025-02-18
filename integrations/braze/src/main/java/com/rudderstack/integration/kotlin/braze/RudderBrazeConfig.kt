package com.rudderstack.integration.kotlin.braze

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
        throw UnsupportedOperationException("Serialization is not supported")
    }
}

/**
 * Data class representing the attribution details of an install event.
 *
 * @property campaign The campaign details associated with the install. Can be null, if user does not provide any.
 */
@Serializable
internal data class InstallAttributed(
    val campaign: Campaign? = null
)

/**
 * Data class representing campaign details for install attribution.
 *
 * @property source The source of the campaign.
 * @property name The name of the campaign.
 * @property adGroup The ad group name of the campaign. Maps to "ad_group" in JSON.
 * @property adCreative The ad creative name/ID used in the campaign. Maps to "ad_creative" in JSON.
 */
@Serializable
internal data class Campaign(
    val source: String? = null,
    val name: String? = null,
    @SerialName("ad_group") val adGroup: String? = null,
    @SerialName("ad_creative") val adCreative: String? = null,
)

/**
 * Data class representing standard properties for a transaction.
 *
 * @property currency The currency code used for the transaction. Defaults to "USD".
 * @property products List of products included in the transaction. Defaults to empty list.
 */
@Serializable
internal data class StandardProperties(
    val currency: String = "USD",
    val products: List<Product> = emptyList(),
)

/**
 * Data class representing a product in a transaction.
 *
 * @property productId The unique identifier for the product. Maps to "product_id" in JSON.
 * @property price The price of the product.
 */
@Serializable
internal data class Product(
    @SerialName("product_id")val productId: String? = null,
    val price: Double? = null,
)

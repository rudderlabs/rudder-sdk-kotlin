package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    internal fun isHybridMode(): Boolean {
        return when (connectionMode) {
            ConnectionMode.HYBRID -> {
                LoggerAnalytics.verbose("BrazeIntegration: As connection mode is set to hybrid, dropping event request.")
                true
            }
            ConnectionMode.DEVICE -> return false
        }
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
        throwUnsupportedOperationException()
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
    val products: List<Product> = listOf(Product()),
) {

    companion object {

        internal fun getKeysAsList(): List<String> = listOf("currency", "products")
    }
}

/**
 * Data class representing a product in a transaction.
 *
 * @property productId The unique identifier for the product. Maps to "product_id" in JSON.
 * @property price The price of the product.
 */
@Serializable
internal data class Product(
    @SerialName("product_id") val productId: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal? = null,
) {

    companion object {

        internal fun getKeysAsList(): List<String> = listOf("product_id", "price")
    }

    internal fun isNotEmpty(): Boolean {
        return productId != null || price != null
    }
}

/**
 * Custom serializer for BigDecimal values in JSON serialization.
 */
private object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        throwUnsupportedOperationException()
    }
}

/**
 * Data class representing the traits of a user for identification.
 *
 * @property userId The unique identifier for the user.
 * @property context The context associated with the user.
 */
internal data class IdentifyTraits(
    val userId: String? = null,
    val context: Context = Context(),
)

private const val BRAZE_EXTERNAL_ID = "brazeExternalId"

/**
 * Data class representing the context associated with a user.
 *
 * @property traits The traits associated with the user.
 * @property externalId The external identifiers associated with the user.
 */
@Serializable
internal data class Context(
    val traits: Traits = Traits(),
    val externalId: List<ExternalId>? = null,
) {

    /**
     * Returns the Braze external identifier for the user.
     */
    internal val brazeExternalId: String?
        get() = externalId?.firstOrNull { it.type == BRAZE_EXTERNAL_ID }?.id
}

/**
 * Data class representing the traits associated with a user.
 *
 * @property email The email address of the user.
 * @property firstName The first name of the user.
 * @property lastName The last name of the
 * @property gender The gender of the user.
 * @property phone The phone number of the user.
 * @property address The address of the user.
 * @property birthday The birthday of the user.
 */
@Serializable
internal data class Traits(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val address: Address? = null,
    @Serializable(with = CalendarSerializer::class)
    val birthday: Calendar? = null,
) {

    companion object {

        internal fun getKeysAsList(): List<String> = listOf(
            "email",
            "firstName",
            "lastName",
            "gender",
            "phone",
            "address",
            "birthday",
        )
    }
}

/**
 * Data class representing the address of a user.
 *
 * @property city The city of the user.
 * @property country The country of the user.
 */
@Serializable
internal data class Address(
    val city: String? = null,
    val country: String? = null,
)

/**
 * Custom serializer for Calendar values in JSON serialization.
 */
private object CalendarSerializer : KSerializer<Calendar?> {

    private val iso8601CalendarDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override val descriptor = PrimitiveSerialDescriptor("Calendar", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Calendar? {
        return runCatching {
            decoder.decodeString()
                .let(iso8601CalendarDateFormatter::parse)
                ?.let { date ->
                    Calendar.getInstance(Locale.US).apply {
                        time = date
                    }
                }
        }.getOrNull()
    }

    override fun serialize(encoder: Encoder, value: Calendar?) {
        throwUnsupportedOperationException()
    }
}

private fun throwUnsupportedOperationException() {
    throw UnsupportedOperationException("Serialization is not supported")
}

package com.rudderstack.integration.kotlin.braze

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
import kotlinx.serialization.json.Json
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

// @Serializable
// internal data class OrderCompleted(
//    val products: List<Products>? = null,
//    @Transient val additionalFields: JsonObject = JsonObject(emptyMap()) // Stores unknown fields
// ) {
//    @OptIn(ExperimentalSerializationApi::class)
//    @Serializer(forClass = OrderCompleted::class)
//    companion object : KSerializer<OrderCompleted> {
//        override fun deserialize(decoder: Decoder): OrderCompleted {
//            val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
//            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
//
//            // Extract known fields
//            val products = jsonObject["products"]?.let {
//                Json.decodeFromJsonElement<List<Products>>(it)
//            }
//
//            // Capture unknown fields
//            val knownKeys = setOf("products")
//            val additionalFields = jsonObject.filterKeys { it !in knownKeys }
//
//            return OrderCompleted(products, JsonObject(additionalFields))
//        }
//
//        override fun serialize(encoder: Encoder, value: OrderCompleted) {
//            val jsonEncoder = encoder as? JsonEncoder ?: error("Expected JsonEncoder")
//            val baseJson = buildJsonObject {
//                value.products?.let { put("products", Json.encodeToJsonElement(it)) }
//            }
//
//            val mergedJson = JsonObject(baseJson + value.additionalFields)
//            jsonEncoder.encodeJsonElement(mergedJson)
//        }
//    }
// }

// 2a
/*
@Serializable(with = OrderCompletedSerializer::class)
internal data class OrderCompleted(
    val products: List<Products>? = null,
    @Transient val additionalFields: JsonObject = JsonObject(emptyMap())
)

internal object OrderCompletedSerializer : KSerializer<OrderCompleted> {
    override val descriptor: SerialDescriptor = OrderCompleted.serializer().descriptor

    override fun deserialize(decoder: Decoder): OrderCompleted {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val products = jsonObject["products"]?.let {
            Json.decodeFromJsonElement<List<Products>>(it)
        }

        val knownKeys = setOf("products")
        val additionalFields = jsonObject.filterKeys { it !in knownKeys }

        return OrderCompleted(products, JsonObject(additionalFields))
    }

    override fun serialize(encoder: Encoder, value: OrderCompleted) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("Expected JsonEncoder")
        val baseJson = buildJsonObject {
            value.products?.let { put("products", Json.encodeToJsonElement(it)) }
        }

        val mergedJson = JsonObject(baseJson + value.additionalFields)
        jsonEncoder.encodeJsonElement(mergedJson)
    }
}


@Serializable
internal data class Products(
    @SerialName("product_id") val productId: String,
    val quantity: Int,
    val price: Double
)
*/

// 2b
/*
@Serializable(with = OrderCompletedSerializer::class)
internal data class OrderCompleted(
    val products: List<Products>? = null,
    val additionalFields: JsonObject = JsonObject(emptyMap())
)

@Serializable(with = ProductsSerializer::class)
internal data class Products(
    @SerialName("product_id") val productId: String? = null,
    val quantity: Int? = null,
    val price: Double? = null,
    val additionalFields: JsonObject? = null,
)

internal object OrderCompletedSerializer : KSerializer<OrderCompleted> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("OrderCompleted") {
        element("products", Products.serializer().descriptor, isOptional = true)
    }

    @OptIn(InternalRudderApi::class)
    override fun deserialize(decoder: Decoder): OrderCompleted {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        // Extract known fields
        val products = jsonObject["products"]?.let {
            LenientJson.decodeFromJsonElement<List<Products>>(it)
        }

        // Capture unknown fields
        val knownKeys = setOf("products")
        val additionalFields = jsonObject.filterKeys { it !in knownKeys }

        return OrderCompleted(products, JsonObject(additionalFields))
    }

    override fun serialize(encoder: Encoder, value: OrderCompleted) {
        throw UnsupportedOperationException("Serialization is not supported for OrderCompleted")
    }
}

internal object ProductsSerializer : KSerializer<Products> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Products") {
        element("product_id", PrimitiveSerialDescriptor("productId", PrimitiveKind.STRING))
        element("quantity", PrimitiveSerialDescriptor("quantity", PrimitiveKind.INT))
        element("price", PrimitiveSerialDescriptor("price", PrimitiveKind.DOUBLE))
    }

    override fun deserialize(decoder: Decoder): Products {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        // Extract known fields
        val productId = jsonObject["product_id"]?.jsonPrimitive?.content
        val quantity = jsonObject["quantity"]?.jsonPrimitive?.intOrNull ?: 0
        val price = jsonObject["price"]?.jsonPrimitive?.doubleOrNull ?: 0.0

        // Capture unknown fields
        val knownKeys = setOf("product_id", "quantity", "price")
        val additionalFields = jsonObject.filterKeys { it !in knownKeys }

        return Products(productId, quantity, price, JsonObject(additionalFields))
    }

    override fun serialize(encoder: Encoder, value: Products) {
        throw UnsupportedOperationException("Serialization is not supported for Products")
    }
}
*/

// 2c
/*
@Serializable(with = ProductOrderCompletedSerializer::class)
internal data class OrderCompleted<T>(
    val products: List<T>? = null,
    val additionalFields: JsonObject = JsonObject(emptyMap())
)

@Serializable(with = ProductsSerializer::class)
internal data class Products(
    @SerialName("product_id") val productId: String? = null,
    val quantity: Int? = null,
    val price: Double? = null,
    val additionalFields: JsonObject? = null,
)

internal object ProductOrderCompletedSerializer : OrderCompletedSerializer<Products>(
    Products.serializer(),
    setOf("products")
)

internal open class OrderCompletedSerializer<T>(
    private val dataSerializer: KSerializer<T>,
    private val knownKeys: Set<String> = setOf("products"),
) : KSerializer<OrderCompleted<T>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("OrderCompleted") {
        element("products", dataSerializer.descriptor, isOptional = true)
    }

    override fun deserialize(decoder: Decoder): OrderCompleted<T> {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val products = jsonObject["products"]?.let {
            Json.decodeFromJsonElement(ListSerializer(dataSerializer), it)
        }

//        val knownKeys = setOf("products")
        val additionalFields = jsonObject.filterKeys { it !in knownKeys }

        return OrderCompleted(products, JsonObject(additionalFields))
    }

    override fun serialize(encoder: Encoder, value: OrderCompleted<T>) {
//        val jsonEncoder = encoder as? JsonEncoder ?: error("Expected JsonEncoder")
//        val baseJson = buildJsonObject {
//            value.products?.let { put("products", Json.encodeToJsonElement(ListSerializer(dataSerializer), it)) }
//        }
//
//        val mergedJson = JsonObject(baseJson + value.additionalFields)
//        jsonEncoder.encodeJsonElement(mergedJson)
    }
}

internal object ProductsSerializer : KSerializer<Products> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Products") {
        element("product_id", PrimitiveSerialDescriptor("productId", PrimitiveKind.STRING))
        element("quantity", PrimitiveSerialDescriptor("quantity", PrimitiveKind.INT))
        element("price", PrimitiveSerialDescriptor("price", PrimitiveKind.DOUBLE))
    }

    override fun deserialize(decoder: Decoder): Products {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        // Extract known fields
        val productId = jsonObject["product_id"]?.jsonPrimitive?.content
        val quantity = jsonObject["quantity"]?.jsonPrimitive?.intOrNull ?: 0
        val price = jsonObject["price"]?.jsonPrimitive?.doubleOrNull ?: 0.0

        // Capture unknown fields
        val knownKeys = setOf("product_id", "quantity", "price")
        val additionalFields = jsonObject.filterKeys { it !in knownKeys }

        return Products(productId, quantity, price, JsonObject(additionalFields))
    }

    override fun serialize(encoder: Encoder, value: Products) {
        throw UnsupportedOperationException("Serialization is not supported for Products")
    }
}
*/

// 2d
/*
@Serializable(with = CustomPropertiesSerializer::class)
internal data class CustomProperties<T>(
    val products: List<T>? = null,
    val additionalFields: JsonObject = JsonObject(emptyMap())
) {

    companion object {

        internal var knownKeys: MutableSet<String> = mutableSetOf("products")
    }
}

private open class CustomPropertiesSerializer<T>(
    private val dataSerializer: KSerializer<T>
) : KSerializer<CustomProperties<T>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("OrderCompleted") {
        element("products", buildClassSerialDescriptor("T"), isOptional = true)
    }

    override fun deserialize(decoder: Decoder): CustomProperties<T> {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val products = jsonObject["products"]?.let {
            Json.decodeFromJsonElement(ListSerializer(dataSerializer), it)
        }

        val additionalFields = jsonObject.filterKeys { it !in CustomProperties.knownKeys }
        return CustomProperties(products, JsonObject(additionalFields))
    }

    override fun serialize(encoder: Encoder, value: CustomProperties<T>) {
        throw UnsupportedOperationException("Serialization is not supported")
    }
}

@Serializable(with = ProductsSerializer::class)
internal data class Products(
    @SerialName("product_id") val productId: String? = null,
    val quantity: Int? = null,
    val price: Double? = null,
    val additionalFields: JsonObject? = null,
)

private object ProductsSerializer : KSerializer<Products> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Products") {
        element("product_id", PrimitiveSerialDescriptor("productId", PrimitiveKind.STRING))
        element("quantity", PrimitiveSerialDescriptor("quantity", PrimitiveKind.INT))
        element("price", PrimitiveSerialDescriptor("price", PrimitiveKind.DOUBLE))
    }

    override fun deserialize(decoder: Decoder): Products {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        // Extract known fields
        val productId = jsonObject["product_id"]?.jsonPrimitive?.content
        val quantity = jsonObject["quantity"]?.jsonPrimitive?.intOrNull ?: 0
        val price = jsonObject["price"]?.jsonPrimitive?.doubleOrNull ?: 0.0

        // Capture unknown fields
        val knownKeys = setOf("product_id", "quantity", "price")
        val additionalFields = jsonObject.filterKeys { it !in knownKeys }

        return Products(productId, quantity, price, JsonObject(additionalFields))
    }

    override fun serialize(encoder: Encoder, value: Products) {
        throw UnsupportedOperationException("Serialization is not supported for Products")
    }
}
*/

// 2e

/*
@Serializable(with = CustomPropertiesSerializer::class)
internal data class CustomProperties<T>(
    val products: List<T>? = null,
    val customProperties: JsonObject = JsonObject(emptyMap())
) {

    companion object {

        internal var knownKeys: MutableSet<String> = mutableSetOf()
    }
}

private open class CustomPropertiesSerializer<T>(
    private val dataSerializer: KSerializer<T>
) : KSerializer<CustomProperties<T>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CustomPropertiesSerializer")

    override fun deserialize(decoder: Decoder): CustomProperties<T> {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val products = jsonObject["products"]?.let {
            Json.decodeFromJsonElement(ListSerializer(dataSerializer), it)
        }

        val customProperties = jsonObject.filterKeys { it !in CustomProperties.knownKeys }
        return CustomProperties(products, JsonObject(customProperties))
    }

    override fun serialize(encoder: Encoder, value: CustomProperties<T>) {
        throw UnsupportedOperationException("Serialization is not supported")
    }
}

@Serializable(with = CustomProductsPropertiesSerializer::class)
internal data class CustomProductsProperties(
    val customProperties: JsonObject? = null,
) {

    companion object {

        internal var knownKeys: MutableSet<String> = mutableSetOf()
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
*/

// 2f
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

    override fun deserialize(decoder: Decoder): CustomProperties {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val products = jsonObject["products"]?.let {
            Json.decodeFromJsonElement(ListSerializer(CustomProductsProperties.serializer()), it)
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

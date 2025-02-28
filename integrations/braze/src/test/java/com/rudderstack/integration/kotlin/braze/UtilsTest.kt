package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.Calendar

private const val EXTERNAL_ID = "<externalId>"

class UtilsTest {

    @Test
    fun `given identify event with traits, when traits are accessed, then it should return some not null value`() {
        val identifyEvent = provideIdentifyEventWithTraits()

        val traits = identifyEvent.traits

        assertNotNull(traits)
    }

    @Test
    fun `given both standard and custom properties is present, when standard properties is requested, then return only standard properties`() {
        val properties = getStandardAndCustomProperties()

        val standardProperties = properties.getStandardProperties()

        val expectedProperties = provideStandardProperties()
        assertEquals(expectedProperties, standardProperties)
    }

    @Test
    fun `given only custom properties is present, when standard properties is requested, then return empty standard properties`() {
        val properties = getOnlyCustomProperties()

        val standardProperties = properties.getStandardProperties()

        val emptyStandardProperties = StandardProperties()
        assertEquals(emptyStandardProperties, standardProperties)
    }

    @Test
    fun `given both standard and custom properties is present, when custom properties is requested, then return only custom properties`() {
        val properties = getStandardAndCustomProperties()

        val customProperties = properties.filter(
            rootKeys = StandardProperties.getKeysAsList(),
            productKeys = Product.getKeysAsList(),
        )

        val expectedProperties = getCustomProperties()
        assertEquals(expectedProperties, customProperties)
    }

    @Test
    fun `given properties contains only standard properties, when custom properties is requested, then return empty object`() {
        val properties = getOnlyStandardProperties()

        val customProperties = properties.filter(
            rootKeys = StandardProperties.getKeysAsList(),
            productKeys = Product.getKeysAsList(),
        )

        val emptyCustomProperties = JsonObject(emptyMap())
        assertEquals(emptyCustomProperties, customProperties)
    }

    @Test
    fun `given IdentityTraits with both externalId and userId, when preferred Id is requested, then return externalId`() {
        val identifyEvent = IdentifyTraits(
            userId = USER_ID,
            context = Context(externalId = provideListOfExternalId())
        )

        val externalId = identifyEvent.getExternalIdOrUserId()

        assertEquals(EXTERNAL_ID, externalId)
    }

    @Test
    fun `given IdentityTraits with only userId, when preferred Id is requested, then return userId`() {
        val identifyEvent = IdentifyTraits(
            userId = USER_ID,
        )

        val externalId = identifyEvent.getExternalIdOrUserId()

        assertEquals(USER_ID, externalId)
    }

    @Test
    fun `given IdentifyEvent contains userId, traits and externalId, when it is converted into IdentityTraits, then return IdentityTraits object`() {
        val identifyEvent = provideIdentifyEvent()

        val identityTraits = identifyEvent.toIdentifyTraits()

        val expectedIdentityTraits = IdentifyTraits(
            userId = USER_ID,
            context = Context(
                traits = provideStandardTraits(),
                externalId = provideListOfExternalId()
            ),
            // This needs to be removed in future. Replace it with "customTraits = JsonObject(emptyMap())"
            customTraits = buildJsonObject {
                put("anonymousId", "<anonymousId>")
            }
        )
        assertEquals(expectedIdentityTraits, identityTraits)
    }

    @Test
    fun `given two IdentifyTraits with the same values, when deDupe, then return IdentityTraits object with all the values set as null`() {
        val identifyTraits1 = provideIdentifyTraits()
        val identifyTraits2 = provideIdentifyTraits()

        val deDupedIdentityTraits = identifyTraits1 deDupe identifyTraits2

        deDupedIdentityTraits.apply {
            assertEquals(null, userId)
            assertEquals(Context(), context)
        }
    }

    @Test
    fun `given two IdentifyTraits with different values, when deDupe, then return updated value`() {
        val identifyTraits = provideIdentifyTraits()
        val newIdentifyTraits = getNewIdentifyTraits()

        val deDupedIdentityTraits = newIdentifyTraits deDupe identifyTraits

        assertEquals(newIdentifyTraits, deDupedIdentityTraits)
    }

    @Test
    fun `given value is not date, when string is converted into date, then return null`() {
        val value = "value"

        val date = tryDateConversion(value)

        assertEquals(null, date)
    }

    @Test
    fun `given value is valid date, when string is converted into date, then return date in milliseconds`() {
        val value = "2021-09-01T00:00:00.000Z"

        val date = tryDateConversion(value)

        assertEquals(1630434600L, date)
    }

    @Test
    fun `given two JsonObject with same custom traits, when deDupe is enabled, then return an empty Json object`() {
        val customTraits = buildJsonObject {
            put("key-1", "value-1")
            put("key-2", buildJsonArray {
                add(1)
                add(2)
            })
        }

        val deDupedJsonObject = getDeDupedCustomTraits(
            deDupeEnabled = true,
            newCustomTraits = customTraits,
            oldCustomTraits = customTraits,
        )

        assertEquals(JsonObject(emptyMap()), deDupedJsonObject)
    }

    @Test
    fun `given two JsonObject with different custom traits, when deDupe is enabled, then return only new custom traits`() {
        // Here new and old custom traits are different. So, the new custom traits should be returned.
        val newCustomTraits = buildJsonObject {
            put("key-1", "value-1")
        }
        val oldCustomTraits = buildJsonObject {
            put("key-2", "value-2")
            put("key-3", 5678)
        }

        val deDupedJsonObject = getDeDupedCustomTraits(
            deDupeEnabled = true,
            newCustomTraits = newCustomTraits,
            oldCustomTraits = oldCustomTraits
        )

        assertEquals(newCustomTraits, deDupedJsonObject)
    }

    @Test
    fun `given two JsonObject with same and different traits, when deDupe is enabled, then return only new custom traits which are different`() {
        val newCustomTraits = buildJsonObject {
            put("key-1", "value-1") // This should be deDuped
            put("key-2", "value-2") // This should be returned
        }
        val oldCustomTraits = buildJsonObject {
            put("key-1", "value-1")
            put("key-3", 5678)
        }

        val deDupedJsonObject = getDeDupedCustomTraits(
            deDupeEnabled = true,
            newCustomTraits = newCustomTraits,
            oldCustomTraits = oldCustomTraits
        )

        assertEquals(buildJsonObject {
            put("key-2", "value-2")
        }, deDupedJsonObject)
    }

    @Test
    fun `given two JsonObject with same traits, when deDupe is disabled, then return new custom traits`() {
        val customTraits = buildJsonObject {
            put("key-1", "value-1")
            put("key-2", buildJsonArray {
                add(1)
                add(2)
            })
        }

        val deDupedJsonObject = getDeDupedCustomTraits(
            deDupeEnabled = false,
            newCustomTraits = customTraits,
            oldCustomTraits = customTraits,
        )

        assertEquals(customTraits, deDupedJsonObject)
    }
}

private fun getStandardAndCustomProperties() = buildJsonObject {
    put("currency", "USD")
    put("products", buildJsonArray {
        add(
            buildJsonObject {
                put("product_id", "product1")
                put("price", 10.0)
                put("key1", "value1")
            }
        )
        add(
            buildJsonObject {
                put("product_id", "product2")
                put("price", 20.0)
                put("key2", "value2")
            }
        )
    })
    put("key3", "value3")
    put("key4", 24)
}

private fun getOnlyStandardProperties() = buildJsonObject {
    put("currency", "USD")
    put("products", buildJsonArray {
        add(
            buildJsonObject {
                put("product_id", "product1")
                put("price", 10.0)
            }
        )
        add(
            buildJsonObject {
                put("product_id", "product2")
                put("price", 20.0)
            }
        )
    })
}

private fun getOnlyCustomProperties() = buildJsonObject {
    put("products", buildJsonArray {
        add(getCustomTraits())
    })
    put("key3", "value3")
    put("key4", 24)
}

private fun provideStandardProperties() = StandardProperties(
    currency = "USD",
    products = listOf(
        Product(productId = "product1", price = 10.0.toBigDecimal()),
        Product(productId = "product2", price = 20.0.toBigDecimal())
    )
)

private fun getCustomProperties() = buildJsonObject {
    put("key1", "value1")
    put("key2", "value2")
    put("key3", "value3")
    put("key4", 24)
}

@OptIn(InternalRudderApi::class)
private fun provideIdentifyEventWithTraits() = IdentifyEvent(
    userIdentityState = UserIdentity(
        anonymousId = "<anonymousId>",
        userId = USER_ID,
        traits = getCustomTraits(),
    )
).also {
    it.updateData(PlatformType.Mobile)
}

private fun getCustomTraits() = buildJsonObject {
    put("key-1", "value-1")
}

@OptIn(InternalRudderApi::class)
private fun provideIdentifyEvent(
    anonymousId: String = "<anonymousId>",
    userId: String = USER_ID,
    traits: JsonObject = provideStandardTraitsInJsonFormat(),
) = IdentifyEvent(
    options = RudderOption(
        externalIds = provideListOfExternalId()
    ),
    userIdentityState = UserIdentity(
        anonymousId = anonymousId,
        userId = userId,
        traits = traits,
    )
).also { it.updateData(PlatformType.Mobile) }

private fun provideListOfExternalId() = listOf(
    ExternalId(
        type = "brazeExternalId",
        id = EXTERNAL_ID
    )
)

private fun provideIdentifyTraits() = IdentifyTraits(
    userId = USER_ID,
    context = provideContext()
)

private fun provideContext(
    email: String = "<email>",
    firstName: String = "<firstName>",
    lastName: String = "<lastName>",
    gender: String = "<gender>",
    phone: String = "<phone>",
    address: Address = provideAddress(),
    birthday: Calendar? = null,
    externalId: List<ExternalId> = provideListOfExternalId()
) = Context(
    traits = Traits(
        email = email,
        firstName = firstName,
        lastName = lastName,
        gender = gender,
        phone = phone,
        address = address,
        birthday = birthday
    ),
    externalId = externalId,
)

private fun provideAddress(
    city: String = "<city>",
    country: String = "<country>"
) = Address(
    city = city,
    country = country,
)

private fun getNewIdentifyTraits() = IdentifyTraits(
    userId = "<newUserId>",
    context = provideContext(
        email = "<newEmail>",
        firstName = "<newFirstName>",
        lastName = "<newLastName>",
        gender = "<newGender>",
        phone = "<newPhone>",
        address = provideAddress(
            city = "<newCity>",
            country = "<newCountry>"
        ),
        birthday = null,
        externalId = listOf(
            ExternalId(
                type = "brazeExternalId",
                id = "<newExternalId>"
            )
        )
    )
)

private fun provideStandardTraits(
    email: String = "<email>",
    firstName: String = "<firstName>",
    lastName: String = "<lastName>",
    gender: String = "<gender>",
    phone: String = "<phone>",
    address: Address = provideAddress(),
    birthday: Calendar? = null,
): Traits = Traits(
    email = email,
    firstName = firstName,
    lastName = lastName,
    gender = gender,
    phone = phone,
    address = address,
    birthday = birthday,
)

private fun provideStandardTraitsInJsonFormat(): JsonObject = buildJsonObject {
    put("email", "<email>")
    put("firstName", "<firstName>")
    put("lastName", "<lastName>")
    put("gender", "<gender>")
    put("phone", "<phone>")
    put("address", buildJsonObject {
        put("city", "<city>")
        put("country", "<country>")
    })
    put("birthday", "")
}

package com.rudderstack.integration.kotlin.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.android.Analytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class FirebaseIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockFirebaseAnalytics: FirebaseAnalytics

    @MockK
    private lateinit var mockBundle: Bundle

    private lateinit var firebaseIntegration: FirebaseIntegration

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        mockAnalytics = mockAnalytics(testScope, testDispatcher)
        firebaseIntegration = spyk(FirebaseIntegration())
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)

        // mocking the bundle
        mockkStatic(::getBundle)
        every { getBundle() } returns mockBundle

        firebaseIntegration.setup(mockAnalytics)

        every { firebaseIntegration.provideFirebaseAnalyticsInstance() } returns mockFirebaseAnalytics
        firebaseIntegration.create(emptyJsonObject)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given screen event, when screen is called, then logEvent is called with correct screen name`() {
        val screenName = "HomeScreen"
        val properties = buildJsonObject { put("category", "home") }

        firebaseIntegration.screen(ScreenEvent(screenName, properties))

        verify(exactly = 1) {
            mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mockBundle)
        }
        verifyParamsInBundle(FirebaseAnalytics.Param.SCREEN_NAME to screenName)
    }

    @Test
    fun `given screen event with reserved keyword properties, when screen is called, then properties should be included in bundle`() {
        val screenName = "ProductScreen"
        val properties = buildJsonObject {
            put("product_id", "product456") // Reserved keyword
            put("name", "Product Name") // Reserved keyword
            put("category", "shoes") // Reserved keyword
            put("value", 149.99) // Reserved keyword
            put("currency", "EUR") // Reserved keyword
            put("custom_screen_prop", "screen_value") // Non-reserved property
        }

        firebaseIntegration.screen(ScreenEvent(screenName, properties))

        verify(exactly = 1) {
            mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mockBundle)
        }
        // All properties should be included since screen events don't filter reserved keywords
        verifyParamsInBundle(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName,
            "product_id" to "product456",
            "name" to "Product Name",
            "category" to "shoes",
            "value" to 149.99,
            "currency" to "EUR",
            "custom_screen_prop" to "screen_value"
        )
    }

    @Test
    fun `given identify event, when identify is called, then setUserId and setUserProperty are called with correct values`() {
        val userId = "user123"
        val email = "test@example.com"
        val age = 25

        val traits = buildJsonObject {
            put("email", email)
            put("age", age)
        }
        val identifyEvent = IdentifyEvent()
        identifyEvent.userId = userId
        identifyEvent.context = identifyEvent.context mergeWithHigherPriorityTo buildJsonObject {
            put("traits", traits)
        }

        firebaseIntegration.identify(identifyEvent)

        verify { mockFirebaseAnalytics.setUserId(userId) }
        verify { mockFirebaseAnalytics.setUserProperty("email", email) }
        verify(exactly = 0) { mockFirebaseAnalytics.setUserProperty("age", age.toString()) }
    }

    @Test
    fun `given identify event with different traits, when identify is called, then setUserProperty is called with stringify values`() {
        val traits = provideTraits()
        val identifyEvent = IdentifyEvent()
        identifyEvent.context = identifyEvent.context mergeWithHigherPriorityTo buildJsonObject {
            put("traits", traits)
        }

        firebaseIntegration.identify(identifyEvent)

        mockFirebaseAnalytics.apply {
            verify(exactly = 1) {
                setUserProperty("birthday", "Mon May 19 15:40:31 IST 2025")
                setUserProperty("email", "test@example.com")
                setUserProperty("firstName", "First")
                setUserProperty("lastName", "Last")
                setUserProperty("phone", "1234567890")
                setUserProperty("myByte", "100")
                setUserProperty("myShort", "5000")
                setUserProperty("myInt", "100000")
                setUserProperty("myLong", "15000000000")
                setUserProperty("myFloat", "5.75")
                setUserProperty("myDouble", "19.99")
                setUserProperty("f1", "35000.0")
                setUserProperty("d1", "120000.0")
                setUserProperty("isJavaFun", "true")
                setUserProperty("greeting", "Hello World")
                setUserProperty("intArr", "[1,2,3]")
                setUserProperty("justArr", "[1,2,3,4]")
                setUserProperty("key_with_hyphen", "value with hyphen")
                val expectedAddress = "[{\"city\":\"Hyderabad\",\"state\":\"Telangana\",\"country\":\"India\",\"street\":\"Mig\"},{\"city\":\"Hyderabad\",\"state\":\"Telangana\",\"country\":\"India\",\"street\":\"Mig\"}]"
                setUserProperty("address", expectedAddress.take(36))
            }
        }
    }

    @Test
    fun `when reset is called, then setUserId is called with null`() {
        firebaseIntegration.reset()

        verify { mockFirebaseAnalytics.setUserId(null) }
    }

    @Test
    fun `given custom event without properties, when track is called with it, then logEvent method is called with proper event name`() {
        val event = "Test Event"
        val properties = emptyJsonObject

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent("Test_Event", mockBundle) }
        verifyBundleIsEmpty()
    }

    @Test
    fun `given custom event with some properties, when track is called with it, then logEvent method is called with proper fields`() {
        val event = "Test Event"
        val properties = buildJsonObject {
            put("key1", "value1")
            put("key2", 2)
            put("key3", 3.0)
            put("key4", true)
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) {
            mockFirebaseAnalytics.logEvent("Test_Event", mockBundle)
        }
        verifyParamsInBundle(
            "key1" to "value1",
            "key2" to 2,
            "key3" to 3.0,
            "key4" to true
        )
    }

    @Test
    fun `given custom event with reserved keyword properties, when track is called, then properties should be included in bundle`() {
        val event = "Custom Event"
        val properties = buildJsonObject {
            put("product_id", "product123") // Reserved keyword
            put("name", "Test Product") // Reserved keyword
            put("category", "electronics") // Reserved keyword
            put("price", 99.99) // Reserved keyword
            put("currency", "USD") // Reserved keyword
            put("custom_property", "custom_value") // Non-reserved property
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) {
            mockFirebaseAnalytics.logEvent("Custom_Event", mockBundle)
        }
        // All properties should be included since custom events don't filter reserved keywords
        verifyParamsInBundle(
            "product_id" to "product123",
            "name" to "Test Product",
            "category" to "electronics",
            "price" to 99.99,
            "currency" to "USD",
            "custom_property" to "custom_value"
        )
    }

    @Test
    fun `given Application Opened event, when track is called with it, then logEvent method is called with proper fields`() {
        val event = "Application Opened"
        val properties = emptyJsonObject

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, mockBundle) }
        verifyBundleIsEmpty()
    }

    @Test
    fun `given Application Opened event with reserved keyword properties, when track is called, then reserved keywords should be included`() {
        val event = "Application Opened"
        val properties = buildJsonObject {
            put("product_id", "product789") // Reserved keyword - should now be included
            put("name", "App Name") // Reserved keyword - should now be included
            put("category", "mobile") // Reserved keyword - should now be included
            put("value", 0.0) // Reserved keyword - should now be included
            put("custom_app_property", "app_value") // Non-reserved property - should be included
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, mockBundle) }
        
        // ALL properties should be included (including reserved keywords)
        verifyParamsInBundle(
            "product_id" to "product789",
            "name" to "App Name", 
            "category" to "mobile",
            "value" to 0.0,
            "custom_app_property" to "app_value"
        )
    }

    @Test
    fun `given event name contains hyphen, when track is called, then logEvent is called with proper event name`() {
        val event = "Event-Name-With-Hyphen"
        val properties = emptyJsonObject

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent("Event_Name_With_Hyphen", mockBundle) }
        verifyBundleIsEmpty()
    }

    @Test
    fun `given event name contains spaces, when track is called, then logEvent is called with proper event name`() {
        val event = "Event Name With Spaces"
        val properties = emptyJsonObject

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent("Event_Name_With_Spaces", mockBundle) }
        verifyBundleIsEmpty()
    }

    @ParameterizedTest
    @MethodSource("provideECommerceEvents")
    fun `given ecommerce event, when track is called with it, then logEvent is called with correct params`(
        event: String,
        properties: JsonObject,
        expectedEvent: String,
        expectedParams: List<Pair<String, Any>>,
    ) {
        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(expectedEvent, mockBundle) }
        verifyParamsInBundle(*expectedParams.toTypedArray())
    }

    @ParameterizedTest
    @MethodSource("provideSingleProductECommerceEvents")
    fun `given single product type ecommerce event, when track is called with it, then logEvent is called with correct params`(
        event: String,
        properties: JsonObject,
        expectedEvent: String,
        expectedParams: List<Pair<String, Any>>
    ) {
        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(expectedEvent, mockBundle) }
        verifyParamsInBundle(*expectedParams.toTypedArray())
        verify(exactly = 1) {
            mockBundle.putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(mockBundle))
        }
    }

    @ParameterizedTest
    @MethodSource("provideProductArrayECommerceEvents")
    fun `given product array type ecommerce event, when track is called with it, then logEvent is called with correct params`(
        event: String,
        properties: JsonObject,
        numberOfProducts: Int,
        expectedEvent: String,
        expectedParams: List<Pair<String, Any>>
    ) {
        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(expectedEvent, mockBundle) }
        verifyParamsInBundle(*expectedParams.toTypedArray())
        verify(exactly = 1) {
            mockBundle.putParcelableArrayList(
                FirebaseAnalytics.Param.ITEMS,
                ArrayList(List(numberOfProducts) { mockBundle })
            )
        }
    }

    @Test
    fun `given ecommerce event with mixed properties, when track is called, then it should use standard property validation`() {
        val event = ECommerceEvents.PRODUCTS_SEARCHED // Use an e-commerce event that doesn't involve product arrays
        val properties = buildJsonObject {
            put("query", "search term") // This should be mapped to search_term
            put("custom_search_prop", "search_value") // Non-reserved property - should be included
            put("products", "should_be_filtered") // Reserved keyword that should be filtered
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, mockBundle) }
        
        // E-commerce events should still filter reserved keywords through standard validation
        // "query" gets mapped to SEARCH_TERM via ECOMMERCE_PROPERTY_MAPPING
        verifyParamsInBundle(
            FirebaseAnalytics.Param.SEARCH_TERM to "search term",
            "custom_search_prop" to "search_value"
        )
        
        // Reserved keyword "products" should be filtered out
        verifyParamsNotInBundle("products")
    }

    private fun verifyParamsInBundle(
        vararg values: Pair<String, Any>
    ) {
        values.forEach { (key, value) ->
            when (value) {
                is String -> verify(exactly = 1) { mockBundle.putString(key, value) }
                is Int -> verify(exactly = 1) { mockBundle.putInt(key, value) }
                is Double -> verify(exactly = 1) { mockBundle.putDouble(key, value) }
                is Boolean -> verify(exactly = 1) { mockBundle.putBoolean(key, value) }
            }
        }
    }

    private fun verifyBundleIsEmpty() {
        verify(exactly = 0) {
            mockBundle.putString(any(), any())
            mockBundle.putInt(any(), any())
            mockBundle.putDouble(any(), any())
            mockBundle.putBoolean(any(), any())
            mockBundle.putParcelableArray(any(), any())
            mockBundle.putParcelableArrayList(any(), any())
        }
    }

    private fun verifyParamsNotInBundle(vararg keys: String) {
        keys.forEach { key ->
            verify(exactly = 0) { 
                mockBundle.putString(key, any())
                mockBundle.putInt(key, any())
                mockBundle.putDouble(key, any())
                mockBundle.putBoolean(key, any())
            }
        }
    }

    companion object {

        @JvmStatic
        fun provideECommerceEvents() = listOf(
            Arguments.of(
                ECommerceEvents.PAYMENT_INFO_ENTERED,
                buildJsonObject { put("payment_method", "credit_card") },
                FirebaseAnalytics.Event.ADD_PAYMENT_INFO,
                listOf(FirebaseAnalytics.Param.PAYMENT_TYPE to "credit_card"),
            ),
            Arguments.of(
                ECommerceEvents.PRODUCTS_SEARCHED,
                buildJsonObject { put("query", "some query") },
                FirebaseAnalytics.Event.SEARCH,
                listOf(FirebaseAnalytics.Param.SEARCH_TERM to "some query"),
            ),
            Arguments.of(
                ECommerceEvents.CART_SHARED,
                buildJsonObject {
                    put("cart_id", "123")
                    put("share_via", "sms")
                },
                FirebaseAnalytics.Event.SHARE,
                listOf(FirebaseAnalytics.Param.ITEM_ID to "123", FirebaseAnalytics.Param.METHOD to "sms"),
            ),
            Arguments.of(
                ECommerceEvents.CART_SHARED,
                buildJsonObject { put("product_id", "123") },
                FirebaseAnalytics.Event.SHARE,
                listOf(FirebaseAnalytics.Param.ITEM_ID to "123"),
            ),
            Arguments.of(
                ECommerceEvents.PROMOTION_VIEWED,
                buildJsonObject {
                    put("name", "someName")
                    put("coupon", "someCoupon")
                },
                FirebaseAnalytics.Event.VIEW_PROMOTION,
                listOf(
                    FirebaseAnalytics.Param.PROMOTION_NAME to "someName",
                    FirebaseAnalytics.Param.COUPON to "someCoupon"
                ),
            ),
            Arguments.of(
                ECommerceEvents.PRODUCT_CLICKED,
                buildJsonObject { put("product_id", "123") },
                FirebaseAnalytics.Event.SELECT_CONTENT,
                listOf(
                    FirebaseAnalytics.Param.ITEM_ID to "123",
                    FirebaseAnalytics.Param.CONTENT_TYPE to "product"
                ),
            ),
            Arguments.of(
                ECommerceEvents.ORDER_COMPLETED,
                buildJsonObject {
                    put("order_id", "order456")
                    put("revenue", 200.00)
                    put("currency", "USD")
                    put("tax", 10.00)
                    put("shipping", 5.00)
                },
                FirebaseAnalytics.Event.PURCHASE,
                listOf(
                    FirebaseAnalytics.Param.TRANSACTION_ID to "order456",
                    FirebaseAnalytics.Param.VALUE to 200.00,
                    FirebaseAnalytics.Param.CURRENCY to "USD",
                    FirebaseAnalytics.Param.TAX to 10.00,
                    FirebaseAnalytics.Param.SHIPPING to 5.00
                ),
            ),
            Arguments.of(
                ECommerceEvents.PRODUCT_SHARED,
                buildJsonObject {
                    put("product_id", "prod_123")
                    put("name", "Test Product")
                    put("price", 49.99)
                    put("currency", "USD")
                },
                FirebaseAnalytics.Event.SHARE,
                listOf(
                    FirebaseAnalytics.Param.CONTENT_TYPE to "product",
                    FirebaseAnalytics.Param.CURRENCY to "USD",
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                ),
            ),

            Arguments.of(
                ECommerceEvents.PROMOTION_CLICKED,
                buildJsonObject {
                    put("promotion_id", "promo_123")
                    put("creative", "creative_123")
                    put("name", "Promotion Name")
                },
                FirebaseAnalytics.Event.SELECT_PROMOTION,
                listOf(
                    FirebaseAnalytics.Param.PROMOTION_ID to "promo_123",
                    FirebaseAnalytics.Param.CREATIVE_NAME to "creative_123",
                    FirebaseAnalytics.Param.PROMOTION_NAME to "Promotion Name"
                ),
            ),
        )

        @JvmStatic
        fun provideSingleProductECommerceEvents() = listOf(
            Arguments.of(
                ECommerceEvents.PRODUCT_ADDED,
                buildJsonObject {
                    put("product_id", "prod_123")
                    put("name", "Test Product")
                    put("price", 49.99)
                },
                FirebaseAnalytics.Event.ADD_TO_CART,
                listOf(
                    FirebaseAnalytics.Param.CURRENCY to "USD",
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
                    FirebaseAnalytics.Param.PRICE to 49.99
                )
            ),
            Arguments.of(
                ECommerceEvents.PRODUCT_ADDED_TO_WISH_LIST,
                buildJsonObject {
                    put("product_id", "prod_123")
                    put("name", "Test Product")
                    put("price", 49.99)
                    put("currency", "INR")
                },
                FirebaseAnalytics.Event.ADD_TO_WISHLIST,
                listOf(
                    FirebaseAnalytics.Param.CURRENCY to "INR",
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
                    FirebaseAnalytics.Param.PRICE to 49.99
                )
            ),
            Arguments.of(
                ECommerceEvents.PRODUCT_VIEWED,
                buildJsonObject {
                    put("product_id", "prod_123")
                    put("name", "Test Product")
                    put("price", 49.99)
                    put("currency", "USD")
                    put("category", "Electronics")
                },
                FirebaseAnalytics.Event.VIEW_ITEM,
                listOf(
                    FirebaseAnalytics.Param.CURRENCY to "USD",
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
                    FirebaseAnalytics.Param.PRICE to 49.99,
                    FirebaseAnalytics.Param.ITEM_CATEGORY to "Electronics"
                )
            ),
            Arguments.of(
                ECommerceEvents.PRODUCT_REMOVED,
                buildJsonObject {
                    put("product_id", "prod_123")
                },
                FirebaseAnalytics.Event.REMOVE_FROM_CART,
                listOf(
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123"
                )
            )
        )

        @JvmStatic
        fun provideProductArrayECommerceEvents() = listOf(
            Arguments.of(
                ECommerceEvents.CHECKOUT_STARTED,
                buildJsonObject {
                    put("order_id", "order123")
                    put("total", 150.50)
                    put("currency", "USD")
                    put("products", buildJsonArray {
                        add(
                            buildJsonObject {
                                put("product_id", "prod_123")
                                put("name", "Test Product")
                                put("price", 49.00)
                                put("currency", "USD")
                            }
                        )
                        add(
                            buildJsonObject {
                                put("product_id", "prod_456")
                                put("name", "Test Product 2")
                                put("price", 101.50)
                                put("currency", "USD")
                            }
                        )
                    })
                },
                2,
                FirebaseAnalytics.Event.BEGIN_CHECKOUT,
                listOf(
                    FirebaseAnalytics.Param.TRANSACTION_ID to "order123",
                    FirebaseAnalytics.Param.VALUE to 150.50,
                    FirebaseAnalytics.Param.CURRENCY to "USD",
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
                    FirebaseAnalytics.Param.PRICE to 49.00,
                    FirebaseAnalytics.Param.ITEM_ID to "prod_456",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product 2",
                    FirebaseAnalytics.Param.PRICE to 101.50
                )
            ),
            Arguments.of(
                ECommerceEvents.ORDER_COMPLETED,
                buildJsonObject {
                    put("order_id", "order_123")
                    put("value", 99.99)
                    put("currency", "USD")
                    put("products", buildJsonArray {
                        add(
                            buildJsonObject {
                                put("product_id", "prod_123")
                                put("name", "Test Product")
                                put("price", 49.00)
                                put("currency", "USD")
                            }
                        )
                        add(
                            buildJsonObject {
                                put("product_id", "prod_456")
                                put("name", "Test Product 2")
                                put("price", 50.99)
                                put("currency", "USD")
                            }
                        )
                    })
                },
                2,
                FirebaseAnalytics.Event.PURCHASE,
                listOf(
                    FirebaseAnalytics.Param.TRANSACTION_ID to "order_123",
                    FirebaseAnalytics.Param.VALUE to 99.99,
                    FirebaseAnalytics.Param.CURRENCY to "USD",
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
                    FirebaseAnalytics.Param.PRICE to 49.00,
                    FirebaseAnalytics.Param.ITEM_ID to "prod_456",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product 2",
                    FirebaseAnalytics.Param.PRICE to 50.99
                ),
            ),
            Arguments.of(
                ECommerceEvents.ORDER_REFUNDED,
                buildJsonObject {
                    put("order_id", "order_123")
                    put("products", buildJsonArray {
                        add(
                            buildJsonObject {
                                put("product_id", "prod_123")
                                put("name", "Test Product")
                                put("price", 49.00)
                                put("currency", "USD")
                            }
                        )
                        add(
                            buildJsonObject {
                                put("product_id", "prod_456")
                                put("name", "Test Product 2")
                                put("price", 50.99)
                                put("currency", "USD")
                            }
                        )
                    })
                },
                2,
                FirebaseAnalytics.Event.REFUND,
                listOf(
                    FirebaseAnalytics.Param.TRANSACTION_ID to "order_123",
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
                    FirebaseAnalytics.Param.PRICE to 49.00,
                    FirebaseAnalytics.Param.ITEM_ID to "prod_456",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product 2",
                    FirebaseAnalytics.Param.PRICE to 50.99
                ),
            ),
            Arguments.of(
                ECommerceEvents.PRODUCT_LIST_VIEWED,
                buildJsonObject {
                    put("list_id", "list_123")
                    put("products", buildJsonArray {
                        add(
                            buildJsonObject {
                                put("product_id", "prod_123")
                                put("name", "Test Product")
                                put("price", 49.99)
                                put("currency", "USD")
                            }
                        )
                    })
                },
                1,
                FirebaseAnalytics.Event.VIEW_ITEM_LIST,
                listOf(
                    FirebaseAnalytics.Param.ITEM_LIST_ID to "list_123",
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
                    FirebaseAnalytics.Param.PRICE to 49.99,
                    FirebaseAnalytics.Param.CURRENCY to "USD"
                ),
            ),
            Arguments.of(
                ECommerceEvents.CART_VIEWED,
                buildJsonObject {
                    put("cart_id", "cart_123")
                    put("products", buildJsonArray {
                        add(
                            buildJsonObject {
                                put("product_id", "prod_123")
                                put("name", "Test Product")
                                put("price", 49.99)
                                put("currency", "USD")
                            }
                        )
                    })
                },
                1,
                FirebaseAnalytics.Event.VIEW_CART,
                listOf(
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                    FirebaseAnalytics.Param.CURRENCY to "USD",
                    FirebaseAnalytics.Param.ITEM_ID to "prod_123",
                    FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
                    FirebaseAnalytics.Param.PRICE to 49.99
                ),
            )
        )
    }
}

private fun provideTraits(): JsonObject {
    // Creating Different Primitive types
    val myByte: Byte = 100
    val myShort: Short = 5000
    val myInt = 100000
    val myLong = 15000000000L
    val myFloat = 5.75f
    val myDouble = 19.99
    val f1 = 35e3f
    val d1 = 12E4
    val isJavaFun = true
    val greeting = "Hello World"

    // Creating Different Array types in Kotlin
    val address1 = buildJsonObject {
        put("city", "Hyderabad")
        put("state", "Telangana")
        put("country", "India")
        put("street", "Mig")
    }
    val address2 = buildJsonObject {
        put("city", "Hyderabad")
        put("state", "Telangana")
        put("country", "India")
        put("street", "Mig")
    }
    val addressesArray = buildJsonArray {
        add(address1)
        add(address2)
    }
    val arr = buildJsonArray {
        add(1)
        add(2)
        add(3)
    }
    val justArr = buildJsonArray {
        add(1)
        add(2)
        add(3)
        add(4)
    }

    val traits = buildJsonObject {
        put("birthday", "Mon May 19 15:40:31 IST 2025")
        put("email", "test@example.com")
        put("firstName", "First")
        put("lastName", "Last")
        put("phone", "1234567890")
        put("myByte", myByte)
        put("myShort", myShort)
        put("myInt", myInt)
        put("myLong", myLong)
        put("myFloat", myFloat)
        put("myDouble", myDouble)
        put("f1", f1)
        put("d1", d1)
        put("isJavaFun", isJavaFun)
        put("greeting", greeting)
        // Inserting Array types into Traits
        put("intArr", arr)
        put("justArr", justArr)
        put("key-with-hyphen", "value with hyphen")
        put("address", addressesArray)
    }

    return traits
}

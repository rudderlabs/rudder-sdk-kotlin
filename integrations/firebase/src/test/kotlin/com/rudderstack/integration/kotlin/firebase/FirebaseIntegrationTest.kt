package com.rudderstack.integration.kotlin.firebase

import android.os.Bundle
import android.os.Parcelable
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
import kotlinx.serialization.json.buildJsonObject
import org.junit.Before
import org.junit.Test
import kotlinx.serialization.json.put
import org.junit.After

class FirebaseIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    @MockK
    private lateinit var mockFirebaseAnalytics: FirebaseAnalytics

    @MockK
    private lateinit var mockBundle: Bundle

    private lateinit var firebaseIntegration: FirebaseIntegration

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        firebaseIntegration = spyk(FirebaseIntegration())
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)

        // mocking the bundle
        mockkStatic(::getBundle)
        every { getBundle() } returns mockBundle

        firebaseIntegration.setup(mockAnalytics)

        every { firebaseIntegration.provideFirebaseAnalyticsInstance() } returns mockFirebaseAnalytics
        firebaseIntegration.create(emptyJsonObject)
    }

    @After
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
        verifyParamsInBundle(mockBundle, FirebaseAnalytics.Param.SCREEN_NAME to screenName)
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
        every { mockAnalytics.traits } returns traits

        firebaseIntegration.identify(identifyEvent)

        verify { mockFirebaseAnalytics.setUserId(userId) }
        verify { mockFirebaseAnalytics.setUserProperty("email", email) }
        verify(exactly = 0) { mockFirebaseAnalytics.setUserProperty("age", age.toString()) }
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

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent("test_event", mockBundle) }
        verifyBundleIsEmpty(mockBundle)
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
            mockFirebaseAnalytics.logEvent("test_event", mockBundle)
        }
        verifyParamsInBundle(
            mockBundle,
            "key1" to "value1",
            "key2" to 2,
            "key3" to 3.0,
            "key4" to true
        )
    }

    @Test
    fun `given Application Opened event, when track is called with it, then logEvent method is called with proper fields`() {
        val event = "Application Opened"
        val properties = emptyJsonObject

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, mockBundle) }
        verifyBundleIsEmpty(mockBundle)
    }

    @Test
    fun `given ecommerce payment info entered event, when track called with it, then logEvent method is called with proper fields`() {
        val event = ECommerceEvents.PAYMENT_INFO_ENTERED
        val properties = buildJsonObject {
            put("payment_method", "credit_card")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) {
            mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_PAYMENT_INFO, mockBundle)
        }
        verifyParamsInBundle(mockBundle, FirebaseAnalytics.Param.PAYMENT_TYPE to "credit_card")
    }

    @Test
    fun `given ecommerce product added to wishlist event, when track is called with it, then logEvent method is called with proper fields`() {
        val event = ECommerceEvents.PRODUCT_ADDED_TO_WISH_LIST
        val properties = buildJsonObject {
            put("product_id", "123")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) {
            mockFirebaseAnalytics.logEvent(
                FirebaseAnalytics.Event.ADD_TO_WISHLIST,
                mockBundle
            )
        }
        verifyParamsInBundle(
            mockBundle,
            FirebaseAnalytics.Param.CURRENCY to "USD",
            FirebaseAnalytics.Param.ITEMS to arrayOf(mockBundle),
            FirebaseAnalytics.Param.ITEM_ID to "123"
        )
    }

    @Test
    fun `given ecommerce products searched event, when track is called with it, then logEvent method is called with proper fields`() {
        val event = ECommerceEvents.PRODUCTS_SEARCHED
        val properties = buildJsonObject {
            put("query", "some query")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, mockBundle) }
        verifyParamsInBundle(mockBundle, FirebaseAnalytics.Param.SEARCH_TERM to "some query")
    }

    @Test
    fun `given ecommerce share event with cart id, when track is called with it, then logEvent method is called with proper fields`() {
        val event = ECommerceEvents.CART_SHARED
        val properties = buildJsonObject {
            put("cart_id", "123")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, mockBundle) }
        verifyParamsInBundle(mockBundle, FirebaseAnalytics.Param.ITEM_ID to "123")
    }

    @Test
    fun `given ecommerce share event with product id, when track is called with it, then logEvent method is called with proper fields`() {
        val event = ECommerceEvents.CART_SHARED
        val properties = buildJsonObject {
            put("product_id", "123")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) {
            mockFirebaseAnalytics.logEvent(
                FirebaseAnalytics.Event.SHARE,
                mockBundle
            )
        }
        verifyParamsInBundle(mockBundle, FirebaseAnalytics.Param.ITEM_ID to "123")
    }

    @Test
    fun `given ecommerce promotion viewed event with name, when track is called with it, then logEvent method is called with proper fields`() {
        val event = ECommerceEvents.PROMOTION_VIEWED
        val properties = buildJsonObject {
            put("name", "someName")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_PROMOTION, mockBundle) }
        verifyParamsInBundle(mockBundle, FirebaseAnalytics.Param.PROMOTION_NAME to "someName")
    }

    @Test
    fun `given product clicked event with product id, when track is called with it, then logEvent method is called with proper fields`() {
        val event = ECommerceEvents.PRODUCT_CLICKED
        val properties = buildJsonObject {
            put("product_id", "123")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, mockBundle) }
        verifyParamsInBundle(
            mockBundle,
            FirebaseAnalytics.Param.ITEM_ID to "123",
            FirebaseAnalytics.Param.CONTENT_TYPE to "product"
        )
    }

    @Test
    fun `given ecommerce purchase event, when track is called with it, then logEvent method is called with proper fields`() {
        val event = ECommerceEvents.ORDER_COMPLETED
        val properties = buildJsonObject {
            put("order_id", "order_123")
            put("value", 99.99)
            put("currency", "USD")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, mockBundle) }
        verifyParamsInBundle(
            mockBundle,
            FirebaseAnalytics.Param.TRANSACTION_ID to "order_123",
            FirebaseAnalytics.Param.VALUE to 99.99,
            FirebaseAnalytics.Param.CURRENCY to "USD"
        )
    }

    @Test
    fun `given ecommerce checkout event, when track is called, then logEvent is called with correct params`() {
        val event = ECommerceEvents.CHECKOUT_STARTED
        val properties = buildJsonObject {
            put("order_id", "order123")
            put("total", 150.50)
            put("currency", "USD")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, mockBundle) }
        verifyParamsInBundle(
            mockBundle,
            FirebaseAnalytics.Param.TRANSACTION_ID to "order123",
            FirebaseAnalytics.Param.VALUE to 150.50,
            FirebaseAnalytics.Param.CURRENCY to "USD"
        )
    }

    @Test
    fun `given ecommerce order completed event, when track is called, then logEvent is called with correct params`() {
        val event = ECommerceEvents.ORDER_COMPLETED
        val properties = buildJsonObject {
            put("order_id", "order456")
            put("revenue", 200.00)
            put("currency", "USD")
            put("tax", 10.00)
            put("shipping", 5.00)
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, mockBundle) }
        verifyParamsInBundle(
            mockBundle,
            FirebaseAnalytics.Param.TRANSACTION_ID to "order456",
            FirebaseAnalytics.Param.VALUE to 200.00,
            FirebaseAnalytics.Param.CURRENCY to "USD",
            FirebaseAnalytics.Param.TAX to 10.00,
            FirebaseAnalytics.Param.SHIPPING to 5.00
        )
    }

    @Test
    fun `given refund event, when track is called with it, then logEvent method is called with proper fields`() {
        val event = ECommerceEvents.ORDER_REFUNDED
        val properties = buildJsonObject {
            put("order_id", "order_123")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.REFUND, mockBundle) }
        verifyParamsInBundle(
            mockBundle,
            FirebaseAnalytics.Param.TRANSACTION_ID to "order_123"
        )
    }

    @Test
    fun `given ecommerce product added event, when track is called, then logEvent is called with correct params`() {
        val event = ECommerceEvents.PRODUCT_ADDED
        val properties = buildJsonObject {
            put("product_id", "prod_123")
            put("name", "Test Product")
            put("price", 49.99)
            put("currency", "USD")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, mockBundle) }
        verifyParamsInBundle(
            mockBundle,
            FirebaseAnalytics.Param.CURRENCY to "USD",
            FirebaseAnalytics.Param.ITEMS to arrayOf(mockBundle),
            FirebaseAnalytics.Param.ITEM_ID to "prod_123",
            FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
            FirebaseAnalytics.Param.PRICE to 49.99
        )
    }

    @Test
    fun `given ecommerce product removed event, when track is called, then logEvent is called with correct params`() {
        val event = ECommerceEvents.PRODUCT_REMOVED
        val properties = buildJsonObject {
            put("product_id", "prod_123")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.REMOVE_FROM_CART, mockBundle) }
        verifyParamsInBundle(
            mockBundle,
            FirebaseAnalytics.Param.ITEMS to arrayOf(mockBundle),
            FirebaseAnalytics.Param.ITEM_ID to "prod_123"
        )
    }

    @Test
    fun `given ecommerce product viewed event, when track is called, then logEvent is called with correct params`() {
        val event = ECommerceEvents.PRODUCT_VIEWED
        val properties = buildJsonObject {
            put("product_id", "prod_123")
            put("name", "Test Product")
            put("price", 49.99)
            put("currency", "USD")
        }

        firebaseIntegration.track(TrackEvent(event, properties))

        verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, mockBundle) }
        verifyParamsInBundle(
            mockBundle,
            FirebaseAnalytics.Param.CURRENCY to "USD",
            FirebaseAnalytics.Param.ITEMS to arrayOf(mockBundle),
            FirebaseAnalytics.Param.ITEM_ID to "prod_123",
            FirebaseAnalytics.Param.ITEM_NAME to "Test Product",
            FirebaseAnalytics.Param.PRICE to 49.99
        )
    }

    private fun verifyParamsInBundle(bundle: Bundle, vararg values: Pair<String, Any>) {
        values.forEach { (key, value) ->
            when (value) {
                is String -> verify(exactly = 1) { bundle.putString(key, value) }
                is Int -> verify(exactly = 1) { bundle.putInt(key, value) }
                is Double -> verify(exactly = 1) { bundle.putDouble(key, value) }
                is Boolean -> verify(exactly = 1) { bundle.putBoolean(key, value) }

                is Array<*> -> {
                    verify(exactly = 1) {
                        bundle.putParcelableArray(key, value as Array<Parcelable>)
                    }
                }

                is ArrayList<*> -> {
                    verify(exactly = 1) {
                        bundle.putParcelableArrayList(key, value as ArrayList<Parcelable>)
                    }
                }
            }
        }
    }

    private fun verifyBundleIsEmpty(bundle: Bundle) {
        verify(exactly = 0) {
            bundle.putString(any(), any())
            bundle.putInt(any(), any())
            bundle.putDouble(any(), any())
            bundle.putBoolean(any(), any())
            bundle.putParcelableArray(any(), any())
            bundle.putParcelableArrayList(any(), any())
        }
    }
}

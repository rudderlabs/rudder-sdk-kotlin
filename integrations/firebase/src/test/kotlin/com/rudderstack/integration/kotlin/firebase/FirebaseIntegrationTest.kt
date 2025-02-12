package com.rudderstack.integration.kotlin.firebase

import com.google.firebase.analytics.FirebaseAnalytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.android.Analytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.serialization.json.put
import org.junit.After

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirebaseIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)
    private val mockFirebaseAnalytics = mockk<FirebaseAnalytics>(relaxed = true)

    private lateinit var firebaseIntegration: FirebaseIntegration

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        firebaseIntegration = spyk(FirebaseIntegration())
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)

        firebaseIntegration.setup(mockAnalytics)

        every { firebaseIntegration.provideFirebaseAnalyticsInstance() } returns mockFirebaseAnalytics
        firebaseIntegration.create(emptyJsonObject)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given screen event, when screen is called, then logEvent is called with correct screen name`() = runTest {
        val screenName = "HomeScreen"
        val properties = buildJsonObject { put("category", "home") }

        firebaseIntegration.screen(ScreenEvent(screenName, properties))

        verify(exactly = 1) {
            mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, match {
                it.getString(FirebaseAnalytics.Param.SCREEN_NAME) == screenName
            })
        }
    }

    @Test
    fun `given identify event, when identify is called, then setUserId and setUserProperty are called with correct values`() =
        runTest {
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
    fun `when reset is called, then setUserId is called with null`() = runTest {
        firebaseIntegration.reset()

        verify { mockFirebaseAnalytics.setUserId(null) }
    }

    @Test
    fun `given custom event without properties, when track is called with it, then logEvent method is called with proper event name`() =
        runTest {
            val event = "Test Event"
            val properties = emptyJsonObject
            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) { mockFirebaseAnalytics.logEvent("test_event", match { it.isEmpty }) }
        }

    @Test
    fun `given custom event with some properties, when track is called with it, then logEvent method is called with proper fields`() =
        runTest {
            val event = "Test Event"
            val properties = buildJsonObject {
                put("key1", "value1")
                put("key2", 2)
                put("key3", 3.0)
                put("key4", true)
            }

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) {
                mockFirebaseAnalytics.logEvent("test_event", match {
                    it.getString("key1") == "value1" &&
                            it.getInt("key2") == 2 &&
                            it.getDouble("key3") == 3.0 &&
                            it.getBoolean("key4")
                })
            }
        }

    @Test
    fun `given Application Opened event, when track is called with it, then logEvent method is called with proper fields`() =
        runTest {
            val event = "Application Opened"
            val properties = emptyJsonObject

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) { mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, match { it.isEmpty }) }
        }

    @Test
    fun `given ecommerce share event with cart id, when track is called with it, then logEvent method is called with proper fields`() =
        runTest {
            val event = ECommerceEvents.CART_SHARED
            val properties = buildJsonObject {
                put("cart_id", "123")
            }

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) {
                mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, match {
                    it.getString(FirebaseAnalytics.Param.ITEM_ID) == "123"
                })
            }
        }

    @Test
    fun `given ecommerce share event with product id, when track is called with it, then logEvent method is called with proper fields`() =
        runTest {
            val event = ECommerceEvents.CART_SHARED
            val properties = buildJsonObject {
                put("product_id", "123")
            }

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) {
                mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, match {
                    it.getString(FirebaseAnalytics.Param.ITEM_ID) == "123"
                })
            }
        }

    @Test
    fun `given ecommerce promotion viewed event with name, when track is called with it, then logEvent method is called with proper fields`() =
        runTest {
            val event = ECommerceEvents.PROMOTION_VIEWED
            val properties = buildJsonObject {
                put("name", "someName")
            }

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) {
                mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_PROMOTION, match {
                    it.getString(FirebaseAnalytics.Param.PROMOTION_NAME) == "someName"
                })
            }
        }

    @Test
    fun `given product clicked event with product id, when track is called with it, then logEvent method is called with proper fields`() =
        runTest {
            val event = ECommerceEvents.PRODUCT_CLICKED
            val properties = buildJsonObject {
                put("product_id", "123")
            }

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) {
                mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, match {
                    it.getString(FirebaseAnalytics.Param.ITEM_ID) == "123" &&
                            it.getString(FirebaseAnalytics.Param.CONTENT_TYPE) == "product"
                })
            }
        }

    @Test
    fun `given ecommerce purchase event, when track is called with it, then logEvent method is called with proper fields`() =
        runTest {
            val event = ECommerceEvents.ORDER_COMPLETED
            val properties = buildJsonObject {
                put("order_id", "order_123")
                put("value", 99.99)
                put("currency", "USD")
            }

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) {
                mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, match {
                    it.getString(FirebaseAnalytics.Param.TRANSACTION_ID) == "order_123" &&
                            it.getDouble(FirebaseAnalytics.Param.VALUE) == 99.99 &&
                            it.getString(FirebaseAnalytics.Param.CURRENCY) == "USD"
                })
            }
        }

    @Test
    fun `given ecommerce checkout event, when track is called, then logEvent is called with correct params`() =
        runTest {
            val event = ECommerceEvents.CHECKOUT_STARTED
            val properties = buildJsonObject {
                put("order_id", "order123")
                put("total", 150.50)
                put("currency", "USD")
            }

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) {
                mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, match {
                    it.getString(FirebaseAnalytics.Param.TRANSACTION_ID) == "order123" &&
                            it.getDouble(FirebaseAnalytics.Param.VALUE) == 150.50 &&
                            it.getString(FirebaseAnalytics.Param.CURRENCY) == "USD"
                })
            }
        }

    @Test
    fun `given ecommerce order completed event, when track is called, then logEvent is called with correct params`() =
        runTest {
            val event = ECommerceEvents.ORDER_COMPLETED
            val properties = buildJsonObject {
                put("order_id", "order456")
                put("revenue", 200.00)
                put("currency", "USD")
                put("tax", 10.00)
                put("shipping", 5.00)
            }

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) {
                mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, match {
                    it.getString(FirebaseAnalytics.Param.TRANSACTION_ID) == "order456" &&
                            it.getDouble(FirebaseAnalytics.Param.VALUE) == 200.00 &&
                            it.getString(FirebaseAnalytics.Param.CURRENCY) == "USD" &&
                            it.getDouble(FirebaseAnalytics.Param.TAX) == 10.00 &&
                            it.getDouble(FirebaseAnalytics.Param.SHIPPING) == 5.00
                })
            }
        }

    @Test
    fun `given refund event, when track is called with it, then logEvent method is called with proper fields`() =
        runTest {
            val event = ECommerceEvents.ORDER_REFUNDED
            val properties = buildJsonObject {
                put("order_id", "order_123")
            }

            firebaseIntegration.track(TrackEvent(event, properties))

            verify(exactly = 1) {
                mockFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.REFUND, match {
                    it.getString(FirebaseAnalytics.Param.TRANSACTION_ID) == "order_123"
                })
            }
        }

}

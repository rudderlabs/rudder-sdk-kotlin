package com.rudderstack.integration.kotlin.facebook

import android.app.Application
import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Currency

private const val pathToSourceConfigWithLimitedDataUseDisabled =
    "facebookconfig/facebook_config_with_limited_data_use_disabled.json"
private const val pathToSourceConfigWithLimitedDataUseEnabled =
    "facebookconfig/facebook_config_with_limited_data_use_enabled.json"

class FacebookIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockFacebookEventsLogger: AppEventsLogger

    @MockK
    private lateinit var mockBundle: Bundle

    private lateinit var facebookIntegration: FacebookIntegration

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        mockAnalytics = mockAnalytics(testScope, testDispatcher)
        facebookIntegration = spyk(FacebookIntegration())
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)

        // mocking the bundle
        mockkStatic(::getBundle)
        every { getBundle() } returns mockBundle

        mockkStatic(FacebookSdk::class)
        mockkObject(AppEventsLogger.Companion)

        every { facebookIntegration.provideAppEventsLogger() } returns mockFacebookEventsLogger
        every { AppEventsLogger.activateApp(any<Application>(), any<String>()) } just Runs
        every { AppEventsLogger.activateApp(any()) } just Runs

        every { FacebookSdk.setDataProcessingOptions(any<Array<String>>()) } just Runs
        every { FacebookSdk.setDataProcessingOptions(any<Array<String>>(), any(), any()) } just Runs

        every {
            FacebookSdk.setIsDebugEnabled(any())
            FacebookSdk.addLoggingBehavior(any())
        } just Runs

        every { AppEventsLogger.setUserID(any()) } just Runs
        every { AppEventsLogger.setUserData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        facebookIntegration.setup(mockAnalytics)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `given config with limited data use disabled, when facebook integration is created with it, then FacebookSdk setDataProcessingOptions called with appropriate params`() {
        val sourceConfigWithLimitedDataUseEnabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseEnabled)

        facebookIntegration.create(sourceConfigWithLimitedDataUseEnabled)

        verify(exactly = 1) { FacebookSdk.setDataProcessingOptions(arrayOf("LDU"), 0, 0) }
    }

    @Test
    fun `given config with limited data use enabled, when facebook integration is created with it, then FacebookSdk setDataProcessingOptions called with appropriate params`() {
        val sourceConfigWithLimitedDataUseDisabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseDisabled)

        facebookIntegration.create(sourceConfigWithLimitedDataUseDisabled)

        verify(exactly = 1) { FacebookSdk.setDataProcessingOptions(arrayOf()) }
    }

    @Test
    fun `when facebook integration is created, the getDestinationInstance returns non null value`() {
        val sourceConfigWithLimitedDataUseDisabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseDisabled)

        facebookIntegration.create(sourceConfigWithLimitedDataUseDisabled)

        assert(facebookIntegration.getDestinationInstance() != null)
        assertEquals(facebookIntegration.getDestinationInstance(), mockFacebookEventsLogger)
    }

    @Test
    fun `when facebook integration is updated, then data processing options gets updated for facebook sdk`() {
        val sourceConfigWithLimitedDataUseDisabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseDisabled)

        facebookIntegration.create(sourceConfigWithLimitedDataUseDisabled)

        verify(exactly = 1) { FacebookSdk.setDataProcessingOptions(arrayOf()) }
        val sourceConfigWithDataUseEnabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseEnabled)

        facebookIntegration.update(sourceConfigWithDataUseEnabled)
        verify(exactly = 1) { FacebookSdk.setDataProcessingOptions(arrayOf("LDU"), 0, 0) }
    }

    @Test
    fun `given screen event with some screen name and empty properties, when screen is called, then logEvent is called with appropriate params`() {
        createFacebookIntegration()
        val screenEvent = ScreenEvent("Home", emptyJsonObject)

        facebookIntegration.screen(screenEvent)

        verify(exactly = 1) {
            mockFacebookEventsLogger.logEvent("Viewed Home Screen")
        }
    }

    @Test
    fun `given screen event with some screen name and properties, when screen is called, then logEvent is called with appropriate params`() {
        createFacebookIntegration()
        val screenEvent = ScreenEvent("Home", buildJsonObject {
            put("key", "value")
            put("key2", 2)
            put("key3", 3.0)
            put("key4", true)
        })

        facebookIntegration.screen(screenEvent)

        verify(exactly = 1) {
            mockFacebookEventsLogger.logEvent(
                eventName = "Viewed Home Screen",
                parameters = mockBundle
            )
        }
        verifyParamsInBundle(mockBundle, "key" to "value", "key2" to 2, "key3" to 3.0, "key4" to "true")
    }

    @Test
    fun `given screen event with screen name exceeding 26 characters, when screen is called, then logEvent is called with screen name truncated to 26 characters`() {
        createFacebookIntegration()
        val completeScreenName = "HomeScreenNameExceeding26Characters"
        val truncatedScreenName = completeScreenName.take(26)
        val screenEvent = ScreenEvent(completeScreenName, emptyJsonObject)

        facebookIntegration.screen(screenEvent)

        verify {
            mockFacebookEventsLogger.logEvent("Viewed $truncatedScreenName Screen")
        }
    }

    @Test
    fun `given identify event with userId and traits, when identify called, then appropriate methods of AppEventsLogger are called`() {
        createFacebookIntegration()
        val identifyEvent = IdentifyEvent()
        identifyEvent.userId = "userId"
        every { mockAnalytics.traits } returns buildJsonObject {
            put("email", "email")
            put("firstName", "firstName")
            put("lastName", "lastName")
            put("phone", "phone")
            put("dateOfBirth", "dateOfBirth")
            put("gender", "male")
            put("address", buildJsonObject {
                put("city", "city")
                put("state", "state")
                put("postalcode", "12345")
                put("country", "country")
            })
        }

        facebookIntegration.identify(identifyEvent)

        verify(exactly = 1) {
            AppEventsLogger.setUserID("userId")
        }
        verify(exactly = 1) {
            AppEventsLogger.setUserData(
                email = "email",
                firstName = "firstName",
                lastName = "lastName",
                phone = "phone",
                dateOfBirth = "dateOfBirth",
                gender = "male",
                city = "city",
                state = "state",
                zip = "12345",
                country = "country",
            )
        }
    }

    @Test
    fun `given order completed e-commerce event, when track called, then logPurchase gets called with appropriate params`() {
        createFacebookIntegration()

        val trackEvent = TrackEvent(ECommerceEvents.ORDER_COMPLETED, buildJsonObject {
            put("revenue", 100)
            put("currency", "INR")
        })

        facebookIntegration.track(trackEvent)

        verify(exactly = 1) {
            mockFacebookEventsLogger.logPurchase(
                purchaseAmount = BigDecimal.valueOf(100.0),
                currency = Currency.getInstance("INR"),
                parameters = mockBundle
            )
        }
    }

    @ParameterizedTest
    @MethodSource("trackEventProvider")
    fun `given e-commerce event, when track called, then logEvent gets called with appropriate params`(
        trackEvent: TrackEvent,
        expectedEventName: String,
        expectedValueToSum: Double?,
        expectedParams: List<Pair<String, Any>>
    ) {
        createFacebookIntegration()

        facebookIntegration.track(trackEvent)

        verify(exactly = 1) {
            if (expectedValueToSum != null) {
                mockFacebookEventsLogger.logEvent(
                    eventName = expectedEventName,
                    valueToSum = expectedValueToSum,
                    parameters = mockBundle
                )
            } else {
                mockFacebookEventsLogger.logEvent(
                    eventName = expectedEventName,
                    parameters = mockBundle
                )
            }
        }
        verifyParamsInBundle(mockBundle, *expectedParams.toTypedArray())
    }

    @Test
    fun `when reset called, then it calls appropriate methods for facebook sdk`() {
        every { AppEventsLogger.clearUserID() } just Runs
        every { AppEventsLogger.clearUserData() } just Runs
        createFacebookIntegration()

        facebookIntegration.reset()

        verify {
            AppEventsLogger.clearUserID()
            AppEventsLogger.clearUserData()
        }
    }

    @Test
    fun `when logLevel in configuration is debug, then logging is enabled for facebook sdk`() {
        mockkObject(LoggerAnalytics)
        every { LoggerAnalytics.logLevel } returns Logger.LogLevel.DEBUG
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)

        createFacebookIntegration()

        verify {
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        }
    }

    @Test
    fun `when logLevel in configuration is verbose, then logging is enabled for facebook sdk`() {
        mockkObject(LoggerAnalytics)
        every { LoggerAnalytics.logLevel } returns Logger.LogLevel.VERBOSE
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)

        createFacebookIntegration()

        verify {
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        }
    }

    @Test
    fun `when logLevel in configuration is info, then logging is not enabled for facebook sdk`() {
        mockkObject(LoggerAnalytics)
        every { LoggerAnalytics.logLevel } returns Logger.LogLevel.INFO
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)

        createFacebookIntegration()

        verify(exactly = 0) {
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        }
    }

    private fun createFacebookIntegration() {
        val sourceConfigWithLimitedDataUseDisabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseDisabled)
        facebookIntegration.create(sourceConfigWithLimitedDataUseDisabled)
    }

    private fun verifyParamsInBundle(bundle: Bundle, vararg values: Pair<String, Any>) {
        values.forEach { (key, value) ->
            when (value) {
                is String -> verify(exactly = 1) { bundle.putString(key, value) }
                is Int -> verify(exactly = 1) { bundle.putInt(key, value) }
                is Double -> verify(exactly = 1) { bundle.putDouble(key, value) }
            }
        }
    }

    companion object {

        @JvmStatic
        fun trackEventProvider() = listOf(
            Arguments.of(
                TrackEvent(ECommerceEvents.PRODUCTS_SEARCHED, buildJsonObject {
                    put("query", "shoes")
                }),
                AppEventsConstants.EVENT_NAME_SEARCHED,
                null,
                listOf(
                    AppEventsConstants.EVENT_PARAM_SEARCH_STRING to "shoes",
                    AppEventsConstants.EVENT_PARAM_CURRENCY to "USD"
                )
            ),
            Arguments.of(
                TrackEvent(ECommerceEvents.PRODUCT_VIEWED, buildJsonObject {
                    put("product_id", "123")
                    put("price", 100)
                    put("currency", "INR")
                }),
                AppEventsConstants.EVENT_NAME_VIEWED_CONTENT,
                100.0,
                listOf(AppEventsConstants.EVENT_PARAM_CONTENT_ID to "123", AppEventsConstants.EVENT_PARAM_CURRENCY to "INR")
            ),
            Arguments.of(
                TrackEvent(ECommerceEvents.PRODUCT_ADDED, buildJsonObject {
                    put("product_id", "123")
                    put("price", 100)
                    put("currency", "INR")
                }),
                AppEventsConstants.EVENT_NAME_ADDED_TO_CART,
                100.0,
                listOf(AppEventsConstants.EVENT_PARAM_CONTENT_ID to "123", AppEventsConstants.EVENT_PARAM_CURRENCY to "INR")
            ),
            Arguments.of(
                TrackEvent(ECommerceEvents.PRODUCT_ADDED_TO_WISH_LIST, buildJsonObject {
                    put("product_id", "123")
                    put("price", 100)
                    put("currency", "INR")
                }),
                AppEventsConstants.EVENT_NAME_ADDED_TO_WISHLIST,
                100.0,
                listOf(AppEventsConstants.EVENT_PARAM_CONTENT_ID to "123", AppEventsConstants.EVENT_PARAM_CURRENCY to "INR")
            ),
            Arguments.of(
                TrackEvent(ECommerceEvents.PAYMENT_INFO_ENTERED, buildJsonObject {
                    put("value", 100)
                    put("currency", "INR")
                }),
                AppEventsConstants.EVENT_NAME_ADDED_PAYMENT_INFO,
                null,
                listOf(AppEventsConstants.EVENT_PARAM_CURRENCY to "INR")
            ),
            Arguments.of(
                TrackEvent(ECommerceEvents.CHECKOUT_STARTED, buildJsonObject {
                    put("value", 100)
                    put("currency", "INR")
                }),
                AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT,
                100.0,
                listOf(AppEventsConstants.EVENT_PARAM_CURRENCY to "INR")
            ),
            Arguments.of(
                TrackEvent(COMPLETE_REGISTRATION, buildJsonObject {
                    put("value", 100)
                    put("currency", "INR")
                }),
                AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
                null,
                listOf(AppEventsConstants.EVENT_PARAM_CURRENCY to "INR")
            ),
            Arguments.of(
                TrackEvent(ACHIEVE_LEVEL, buildJsonObject {
                    put("level", 10)
                }),
                AppEventsConstants.EVENT_NAME_ACHIEVED_LEVEL,
                null,
                listOf("level" to 10)
            ),
            Arguments.of(
                TrackEvent(COMPLETE_TUTORIAL, buildJsonObject {
                    put("value", 100)
                    put("currency", "INR")
                }),
                AppEventsConstants.EVENT_NAME_COMPLETED_TUTORIAL,
                null,
                listOf(AppEventsConstants.EVENT_PARAM_CURRENCY to "INR")
            ),
            Arguments.of(
                TrackEvent(UNLOCK_ACHIEVEMENT, buildJsonObject {
                    put(AppEventsConstants.EVENT_PARAM_DESCRIPTION, "123")
                }),
                AppEventsConstants.EVENT_NAME_UNLOCKED_ACHIEVEMENT,
                null,
                listOf(AppEventsConstants.EVENT_PARAM_DESCRIPTION to "123")
            ),
            Arguments.of(
                TrackEvent(SUBSCRIBE, buildJsonObject {
                    put("channel_id", 100)
                }),
                AppEventsConstants.EVENT_NAME_SUBSCRIBE,
                null,
                listOf("channel_id" to 100)
            ),
            Arguments.of(
                TrackEvent(START_TRIAL, buildJsonObject {
                    put("trial_duration", 10)
                }),
                AppEventsConstants.EVENT_NAME_START_TRIAL,
                null,
                listOf("trial_duration" to 10)
            ),
            Arguments.of(
                TrackEvent(ECommerceEvents.PROMOTION_CLICKED, buildJsonObject {
                    put("promotion_id", "123")
                }),
                AppEventsConstants.EVENT_NAME_AD_CLICK,
                null,
                listOf("promotion_id" to "123")
            ),
            Arguments.of(
                TrackEvent(ECommerceEvents.PROMOTION_VIEWED, buildJsonObject {
                    put("promotion_id", "123")
                }),
                AppEventsConstants.EVENT_NAME_AD_IMPRESSION,
                null,
                listOf("promotion_id" to "123", AppEventsConstants.EVENT_PARAM_CURRENCY to "USD")
            ),
            Arguments.of(
                TrackEvent(SPEND_CREDITS, buildJsonObject {
                    put("value", 100)
                    put("currency", "INR")
                }),
                AppEventsConstants.EVENT_NAME_SPENT_CREDITS,
                100.0,
                listOf(AppEventsConstants.EVENT_PARAM_CURRENCY to "INR")
            ),
            Arguments.of(
                TrackEvent(ECommerceEvents.PRODUCT_REVIEWED, buildJsonObject {
                    put("product_id", "123")
                    put("rating", 5)
                }),
                AppEventsConstants.EVENT_NAME_RATED,
                null,
                listOf(
                    AppEventsConstants.EVENT_PARAM_CONTENT_ID to "123",
                    AppEventsConstants.EVENT_PARAM_MAX_RATING_VALUE to 5
                )
            )
        )
    }
}

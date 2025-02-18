package com.rudderstack.integration.kotlin.facebook

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.serialization.json.put
import org.robolectric.annotation.Config

private const val pathToSourceConfigWithLimitedDataUseDisabled =
    "facebookconfig/facebook_config_with_limited_data_use_disabled.json"
private const val pathToSourceConfigWithLimitedDataUseEnabled =
    "facebookconfig/facebook_config_with_limited_data_use_enabled.json"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FacebookIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)
    private val mockFacebookEventsLogger = mockk<AppEventsLogger>(relaxed = true)

    private lateinit var facebookIntegration: FacebookIntegration

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        facebookIntegration = spyk(FacebookIntegration())
        every { mockAnalytics.configuration } returns mockk<Configuration>(relaxed = true)

        mockkStatic(FacebookSdk::class)
        mockkObject(AppEventsLogger.Companion)

        every { facebookIntegration.provideAppEventsLogger() } returns mockFacebookEventsLogger
        every { AppEventsLogger.activateApp(any<Application>(), any<String>()) } just Runs
        every { AppEventsLogger.activateApp(any()) } just Runs

        every { FacebookSdk.setDataProcessingOptions(any<Array<String>>()) } just Runs
        every { FacebookSdk.setDataProcessingOptions(any<Array<String>>(), any(), any()) } just Runs

        every { AppEventsLogger.setUserID(any()) } just Runs
        every { AppEventsLogger.setUserData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        facebookIntegration.setup(mockAnalytics)
    }

    @Test
    fun `given config with limited data use disabled, when facebook integration is created with it, then FacebookSdk setDataProcessingOptions called with appropriate params`() =
        runTest {
            val sourceConfigWithLimitedDataUseEnabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseEnabled)

            facebookIntegration.create(sourceConfigWithLimitedDataUseEnabled)

            verify {
                FacebookSdk.setDataProcessingOptions(
                    arrayOf("LDU"),
                    0,
                    0
                )
            }
        }

    @Test
    fun `given config with limited data use enabled, when facebook integration is created with it, then FacebookSdk setDataProcessingOptions called with appropriate params`() =
        runTest {
            val sourceConfigWithLimitedDataUseDisabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseDisabled)

            facebookIntegration.create(sourceConfigWithLimitedDataUseDisabled)

            verify {
                FacebookSdk.setDataProcessingOptions(arrayOf())
            }
        }

    @Test
    fun `when facebook integration is created, the getDestinationInstance returns non null value`() = runTest {
        val sourceConfigWithLimitedDataUseDisabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseDisabled)

        facebookIntegration.create(sourceConfigWithLimitedDataUseDisabled)

        assert(facebookIntegration.getDestinationInstance() != null)
        assertEquals(facebookIntegration.getDestinationInstance(), mockFacebookEventsLogger)
    }

    @Test
    fun `given screen event with some screen name and empty properties, when screen is called, then logEvent is called with appropriate params`() =
        runTest {
            createFacebookIntegration()
            val screenEvent = ScreenEvent("Home", emptyJsonObject)

            facebookIntegration.screen(screenEvent)

            verify {
                mockFacebookEventsLogger.logEvent("Viewed Home Screen")
            }
        }

    @Test
    fun `given screen event with some screen name and properties, when screen is called, then logEvent is called with appropriate params`() =
        runTest {
            createFacebookIntegration()
            val screenEvent = ScreenEvent("Home", buildJsonObject {
                put("key", "value")
                put("key2", 2)
                put("key3", 3.0)
                put("key4", true)
            })

            facebookIntegration.screen(screenEvent)

            verify {
                mockFacebookEventsLogger.logEvent(
                    eventName = "Viewed Home Screen",
                    parameters = match {
                        (it.getString("key") == "value"
                                && it.getInt("key2") == 2
                                && it.getDouble("key3") == 3.0) && it.getString("key4") == "true"
                    }
                )
            }
        }

    @Test
    fun `given screen event with screen name exceeding 26 characters, when screen is called, then logEvent is called with screen name truncated to 26 characters`() =
        runTest {
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
    fun `given identify event with userId and traits, when identify called, then appropriate methods of AppEventsLogger are called`() =
        runTest {
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

            verify {
                AppEventsLogger.setUserID("userId")
            }
            verify {
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

    private fun createFacebookIntegration() {
        val sourceConfigWithLimitedDataUseDisabled = readJsonObjectFromFile(pathToSourceConfigWithLimitedDataUseDisabled)
        facebookIntegration.create(sourceConfigWithLimitedDataUseDisabled)
    }
}

package com.rudderstack.kotlin.sdk.plugins

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.storage.LibraryVersion
import com.rudderstack.kotlin.sdk.internals.utils.empty
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val LIBRARY_KEY = "library"
private const val LIBRARY_NAME_KEY = "name"
private const val LIBRARY_VERSION_KEY = "version"

private const val KOTLIN_LIBRARY_NAME = "com.rudderstack.kotlin.sdk"
private const val KOTLIN_LIBRARY_VERSION = "1.0.1"

private const val ANDROID_LIBRARY_NAME = "com.rudderstack.android.sdk"
private const val ANDROID_LIBRARY_VERSION = "1.0.2"

private const val EVENT_NAME = "Sample Event"

class LibraryInfoPluginTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun `given mobile platform, when library info plugin is executed, then library info is attached to the context`() {
        every { mockAnalytics.configuration.storage.getLibraryVersion() } returns provideLibraryVersion(PlatformType.Mobile)
        val message = provideEvent()
        val libraryInfoPlugin = LibraryInfoPlugin()

        libraryInfoPlugin.setup(mockAnalytics)
        libraryInfoPlugin.execute(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideLibraryContextPayload(PlatformType.Mobile).toString(),
            actual.toString(),
            true
        )
    }

    @Test
    fun `given server platform, when library info plugin is executed, then library info is attached to the context`() {
        every { mockAnalytics.configuration.storage.getLibraryVersion() } returns provideLibraryVersion(PlatformType.Server)
        val message = provideEvent()
        val libraryInfoPlugin = LibraryInfoPlugin()

        libraryInfoPlugin.setup(mockAnalytics)
        libraryInfoPlugin.execute(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideLibraryContextPayload(PlatformType.Server).toString(),
            actual.toString(),
            true
        )
    }

    @Test
    fun `given some context is present, when library info is merged with other context, then it is given higher priority`() {
        every { mockAnalytics.configuration.storage.getLibraryVersion() } returns provideLibraryVersion(PlatformType.Mobile)
        val message = provideEvent()
        val libraryInfoPlugin = LibraryInfoPlugin()

        libraryInfoPlugin.setup(mockAnalytics)
        message.context = buildJsonObject {
            put(LIBRARY_KEY, String.empty())
        }
        libraryInfoPlugin.execute(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideLibraryContextPayload(PlatformType.Mobile).toString(),
            actual.toString(),
            true
        )
    }
}

private fun provideEvent(): Message = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
)

private fun provideLibraryVersion(platform: PlatformType): LibraryVersion = when (platform) {
    PlatformType.Mobile -> object : LibraryVersion {
        override fun getPackageName(): String = ANDROID_LIBRARY_NAME
        override fun getVersionName(): String = ANDROID_LIBRARY_VERSION
    }

    PlatformType.Server -> object : LibraryVersion {
        override fun getPackageName(): String = KOTLIN_LIBRARY_NAME
        override fun getVersionName(): String = KOTLIN_LIBRARY_VERSION
    }
}

private fun provideLibraryContextPayload(platform: PlatformType): JsonObject = when (platform) {
    PlatformType.Mobile -> {
        buildJsonObject {
            put(LIBRARY_KEY, buildJsonObject {
                put(LIBRARY_NAME_KEY, ANDROID_LIBRARY_NAME)
                put(LIBRARY_VERSION_KEY, ANDROID_LIBRARY_VERSION)
            })
        }
    }

    PlatformType.Server -> {
        buildJsonObject {
            put(LIBRARY_KEY, buildJsonObject {
                put(LIBRARY_NAME_KEY, KOTLIN_LIBRARY_NAME)
                put(LIBRARY_VERSION_KEY, KOTLIN_LIBRARY_VERSION)
            })
        }
    }
}

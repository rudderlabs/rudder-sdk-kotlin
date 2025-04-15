package com.rudderstack.sdk.kotlin.android.storage

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SharedPrefsStoreTest {

    private val prefsName = "test_prefs"

    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var sharedPrefsStore: SharedPrefsStore
    private lateinit var mockCheckBuildVersionUseCase: CheckBuildVersionUseCase

    @BeforeEach
    fun setUp() {
        mockContext = mockk<Context>(relaxed = true)
        mockSharedPreferences = mockk()
        mockCheckBuildVersionUseCase = mockk()
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor

        sharedPrefsStore = SharedPrefsStore(mockContext, prefsName)

        mockkObject(CheckBuildVersionUseCase)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CheckBuildVersionUseCase)
    }

    @Test
    fun `given a key value pair, when a getInt is called, then the expected value is returned and verified that interaction happened`() {
        every { mockSharedPreferences.getInt("key", 0) } returns 42

        assertEquals(42, sharedPrefsStore.getInt("key", 0))

        verify { mockSharedPreferences.getInt("key", 0) }
    }

    @Test
    fun `given a key value pair, when a getBoolean is called, then the expected value is returned and verified that interaction happened`() {
        every { mockSharedPreferences.getBoolean("key", false) } returns true

        assertEquals(true, sharedPrefsStore.getBoolean("key", false))

        verify { mockSharedPreferences.getBoolean("key", false) }
    }

    @Test
    fun `given a key value pair, when a getString is called, then the expected value is returned and verified that interaction happened`() {
        every { mockSharedPreferences.getString("key", "default") } returns "value"

        assertEquals("value", sharedPrefsStore.getString("key", "default"))

        verify { mockSharedPreferences.getString("key", "default") }
    }

    @Test
    fun `given a key value pair, when a getLong is called, then the expected value is returned and verified that interaction happened`() {
        every { mockSharedPreferences.getLong("key", 0L) } returns 123456789L

        assertEquals(123456789L, sharedPrefsStore.getLong("key", 0L))

        verify { mockSharedPreferences.getLong("key", 0L) }
    }

    @Test
    fun `given a key value pair with an int value, when a save is called, then the expected value is saved `() {
        sharedPrefsStore.save("key", 42)

        verify {
            mockEditor.putInt("key", 42)
            mockEditor.commit()
        }
    }

    @Test
    fun `given a key value pair with a boolean value, when a save is called, then the expected value is saved `() {
        sharedPrefsStore.save("key", true)

        verify {
            mockEditor.putBoolean("key", true)
            mockEditor.commit()
        }
    }

    @Test
    fun `given a key value pair with a string value, when a save is called, then the expected value is saved `() {
        sharedPrefsStore.save("key", "value")

        verify {
            mockEditor.putString("key", "value")
            mockEditor.commit()
        }
    }

    @Test
    fun `given a key value pair with a long value, when a save is called, then the expected value is saved `() {
        sharedPrefsStore.save("key", 123456789L)

        verify {
            mockEditor.putLong("key", 123456789L)
            mockEditor.commit()
        }
    }

    @Test
    fun `given a key value pair exists in shared preferences, when a clear is called, then verify that value is removed`() {
        sharedPrefsStore.clear("key")

        verify {
            mockEditor.remove("key")
            mockEditor.commit()
        }
    }

    @Test
    fun `given android version is N and above, when deletePrefs is called, then verify that shared preference is deleted`() {
        every { CheckBuildVersionUseCase.isAndroidVersionNAndAbove() } returns true

        sharedPrefsStore.deletePrefs()

        verify { mockContext.deleteSharedPreferences(prefsName) }
    }

    @Test
    fun `given android version is below N, when deletePrefs is called, then verify that shared preferences is deleted`() {
        every { CheckBuildVersionUseCase.isAndroidVersionNAndAbove() } returns false

        sharedPrefsStore.deletePrefs()

        verify { mockContext.deleteFile("null/shared_prefs/${prefsName}.xml") }
    }
}

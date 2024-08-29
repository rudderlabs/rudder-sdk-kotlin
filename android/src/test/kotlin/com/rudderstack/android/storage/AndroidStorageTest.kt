package com.rudderstack.android.storage

import android.content.Context
import com.rudderstack.core.internals.storage.MAX_EVENT_SIZE
import com.rudderstack.core.internals.storage.StorageKeys
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class AndroidStorageTest {

    private val writeKey = "testWriteKey"

    private lateinit var keyValueStorage: SharedPrefsStore
    private lateinit var directory: File
    private lateinit var mockContext: Context
    private lateinit var storage: AndroidStorage

    @Before
    fun setUp() {
        mockContext = mockk<Context>(relaxed = true)
        keyValueStorage = mockk<SharedPrefsStore>(relaxed = true)
        directory = mockk<File>(relaxed = true)
        storage = AndroidStorage(mockContext, writeKey, keyValueStorage)
    }

    @Test
    fun `given Android storage, when write is called, then the expected value is saved`() = runBlocking {
        val key = StorageKeys.RUDDER_OPTIONAL

        storage.write(key, true)

        verify { keyValueStorage.save(key.key, true) }
    }

    @Test(expected = Exception::class)
    fun `given Android storage with large event, when write is called, then test write string value exceeding max size`() =
        runTest {
            val key = StorageKeys.RUDDER_EVENT
            val largeEvent = "x".repeat(MAX_EVENT_SIZE + 1)

            storage.write(key, largeEvent)
        }

    @Test
    fun `given Android Storage, when storage remove is called, for a key, then key is cleared from storage`() = runTest {
        val key = StorageKeys.RUDDER_OPTIONAL

        storage.remove(key)

        verify { keyValueStorage.clear(key.key) }
    }

    @Test
    fun `given Android Storage, when get int is called, then the expected value is returned`() {
        val key = StorageKeys.RUDDER_OPTIONAL
        val defaultVal = 42
        every { keyValueStorage.getInt(key.key, defaultVal) } returns defaultVal

        val result = storage.readInt(key, defaultVal)

        assertEquals(defaultVal, result)
    }
}
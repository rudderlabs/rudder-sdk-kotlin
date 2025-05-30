package com.rudderstack.sdk.kotlin.android.storage

import android.content.Context
import com.rudderstack.sdk.kotlin.core.internals.storage.MAX_PAYLOAD_SIZE
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class AndroidStorageTest {

    private val writeKey = "testWriteKey"

    private lateinit var keyValueStorage: SharedPrefsStore
    private lateinit var directory: File
    private lateinit var mockContext: Context
    private lateinit var storage: AndroidStorage

    @BeforeEach
    fun setUp() {
        mockContext = mockk<Context>(relaxed = true)
        keyValueStorage = mockk<SharedPrefsStore>(relaxed = true)
        directory = mockk<File>(relaxed = true)
        storage = AndroidStorage(mockContext, writeKey, keyValueStorage)
    }

    @Test
    fun `given Android storage with large message, when write is called, then test write string value exceeding max size`() =
        runTest {
            val key = StorageKeys.EVENT
            val largeMessagePayload = "x".repeat(MAX_PAYLOAD_SIZE + 1)

            assertThrows<Exception> { storage.write(key, largeMessagePayload) }
        }
}

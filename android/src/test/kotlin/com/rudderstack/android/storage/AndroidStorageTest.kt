package com.rudderstack.android.storage

import android.content.Context
import com.rudderstack.android.sdk.storage.AndroidStorage
import com.rudderstack.android.sdk.storage.SharedPrefsStore
import com.rudderstack.kotlin.sdk.internals.storage.MAX_PAYLOAD_SIZE
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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

    @Test(expected = Exception::class)
    fun `given Android storage with large message, when write is called, then test write string value exceeding max size`() =
        runTest {
            val key = StorageKeys.MESSAGE
            val largeMessagePayload = "x".repeat(MAX_PAYLOAD_SIZE + 1)

            storage.write(key, largeMessagePayload)
        }

}

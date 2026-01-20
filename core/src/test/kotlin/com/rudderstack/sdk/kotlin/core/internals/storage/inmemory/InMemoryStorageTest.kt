package com.rudderstack.sdk.kotlin.core.internals.storage.inmemory

import com.rudderstack.sdk.kotlin.core.internals.storage.MAX_PAYLOAD_SIZE
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.storage.exception.PayloadTooLargeException
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_WRITE_KEY = "test-write-key"

class InMemoryStorageTest {

    private lateinit var storage: InMemoryStorage

    @BeforeEach
    fun setup() {
        storage = InMemoryStorage(writeKey = TEST_WRITE_KEY)
    }

    @Test
    fun `given a boolean value, when write is called with non-EVENT key, then value is stored`() = runBlocking {
        storage.write(StorageKeys.IS_SESSION_START, true)

        val result = storage.readBoolean(StorageKeys.IS_SESSION_START, false)
        assertTrue(result)
    }

    @Test
    fun `given a boolean value, when write is called with EVENT key, then value is ignored`() = runBlocking {
        storage.write(StorageKeys.EVENT, true)

        val fileList = storage.readFileList()
        assertTrue(fileList.isEmpty())
    }

    @Test
    fun `given a string value, when write is called with non-EVENT key, then value is stored`() = runBlocking {
        val expectedValue = "test-user-id"

        storage.write(StorageKeys.USER_ID, expectedValue)

        val result = storage.readString(StorageKeys.USER_ID, String.empty())
        assertEquals(expectedValue, result)
    }

    @Test
    fun `given a string value, when write is called with EVENT key, then event is stored in batch manager`() = runBlocking {
        val eventPayload = provideEventPayload()

        storage.write(StorageKeys.EVENT, eventPayload)
        storage.rollover()

        val fileList = storage.readFileList()
        assertEquals(1, fileList.size)
    }

    @Test
    fun `given a payload exceeding MAX_PAYLOAD_SIZE, when write is called with EVENT key, then PayloadTooLargeException is thrown`() {
        val oversizedPayload = createOversizedPayload()

        assertThrows(PayloadTooLargeException::class.java) {
            runBlocking {
                storage.write(StorageKeys.EVENT, oversizedPayload)
            }
        }
    }

    @Test
    fun `given an int value, when write is called with non-EVENT key, then value is stored`() = runBlocking {
        val expectedValue = 42

        storage.write(StorageKeys.APP_BUILD, expectedValue)

        val result = storage.readInt(StorageKeys.APP_BUILD, 0)
        assertEquals(expectedValue, result)
    }

    @Test
    fun `given an int value, when write is called with EVENT key, then value is ignored`() = runBlocking {
        storage.write(StorageKeys.EVENT, 123)

        val fileList = storage.readFileList()
        assertTrue(fileList.isEmpty())
    }

    @Test
    fun `given a long value, when write is called with non-EVENT key, then value is stored`() = runBlocking {
        val expectedValue = 1234567890123L

        storage.write(StorageKeys.SESSION_ID, expectedValue)

        val result = storage.readLong(StorageKeys.SESSION_ID, 0L)
        assertEquals(expectedValue, result)
    }

    @Test
    fun `given a long value, when write is called with EVENT key, then value is ignored`() = runBlocking {
        storage.write(StorageKeys.EVENT, 123L)

        val fileList = storage.readFileList()
        assertTrue(fileList.isEmpty())
    }

    @Test
    fun `given an int value stored, when readInt is called, then the stored value is returned`() = runBlocking {
        val expectedValue = 99
        storage.write(StorageKeys.APP_BUILD, expectedValue)

        val result = storage.readInt(StorageKeys.APP_BUILD, 0)

        assertEquals(expectedValue, result)
    }

    @Test
    fun `given no int value stored, when readInt is called, then default value is returned`() {
        val defaultValue = -1

        val result = storage.readInt(StorageKeys.APP_BUILD, defaultValue)

        assertEquals(defaultValue, result)
    }

    @Test
    fun `given a boolean value stored, when readBoolean is called, then the stored value is returned`() = runBlocking {
        storage.write(StorageKeys.IS_SESSION_MANUAL, true)

        val result = storage.readBoolean(StorageKeys.IS_SESSION_MANUAL, false)

        assertEquals(true, result)
    }

    @Test
    fun `given a long value stored, when readLong is called, then the stored value is returned`() = runBlocking {
        val expectedValue = 9876543210L
        storage.write(StorageKeys.LAST_ACTIVITY_TIME, expectedValue)

        val result = storage.readLong(StorageKeys.LAST_ACTIVITY_TIME, 0L)

        assertEquals(expectedValue, result)
    }

    @Test
    fun `given a string value stored with non-EVENT key, when readString is called, then the stored value is returned`() = runBlocking {
        val expectedValue = "anonymous-123"
        storage.write(StorageKeys.ANONYMOUS_ID, expectedValue)

        val result = storage.readString(StorageKeys.ANONYMOUS_ID, String.empty())

        assertEquals(expectedValue, result)
    }

    @Test
    fun `given events stored, when readString is called with EVENT key, then batch file list is returned as string`() = runBlocking {
        val eventPayload = provideEventPayload()
        storage.write(StorageKeys.EVENT, eventPayload)
        storage.rollover()

        val result = storage.readString(StorageKeys.EVENT, String.empty())

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `given no events stored, when readFileList is called, then empty list is returned`() {
        val fileList = storage.readFileList()

        assertTrue(fileList.isEmpty())
    }

    @Test
    fun `given events stored and rollover called, when readFileList is called, then list contains batch file`() = runBlocking {
        val eventPayload = provideEventPayload()
        storage.write(StorageKeys.EVENT, eventPayload)
        storage.rollover()

        val fileList = storage.readFileList()

        assertEquals(1, fileList.size)
    }

    @Test
    fun `given a batch file exists, when readBatchContent is called, then content is returned`() = runBlocking {
        val eventPayload = provideEventPayload()

        storage.write(StorageKeys.EVENT, eventPayload)
        storage.rollover()

        val fileList = storage.readFileList()
        val batchContent = storage.readBatchContent(fileList.first())

        assertNotNull(batchContent)
        assertTrue(batchContent!!.contains(eventPayload))
    }

    @Test
    fun `given a batch file does not exist, when readBatchContent is called, then null is returned`() {
        val result = storage.readBatchContent("non-existent-file")

        assertNull(result)
    }

    @Test
    fun `given a value stored, when remove with StorageKeys is called, then value is cleared`() = runBlocking {
        val expectedValue = "user-123"
        storage.write(StorageKeys.USER_ID, expectedValue)

        storage.remove(StorageKeys.USER_ID)

        val result = storage.readString(StorageKeys.USER_ID, "default")
        assertEquals("default", result)
    }

    @Test
    fun `given a batch file exists, when remove with file path is called, then file is removed`() = runBlocking {
        val eventPayload = provideEventPayload()
        storage.write(StorageKeys.EVENT, eventPayload)
        storage.rollover()
        val fileList = storage.readFileList()
        assertEquals(1, fileList.size)

        storage.remove(fileList.first())

        val updatedFileList = storage.readFileList()
        assertTrue(updatedFileList.isEmpty())
    }

    @Test
    fun `given events stored, when rollover is called, then batch is finalized`() = runBlocking {
        val eventPayload = provideEventPayload()
        storage.write(StorageKeys.EVENT, eventPayload)
        val fileListBeforeRollover = storage.readFileList()
        assertTrue(fileListBeforeRollover.isEmpty())

        storage.rollover()

        val fileListAfterRollover = storage.readFileList()
        assertEquals(1, fileListAfterRollover.size)
    }

    @Test
    fun `given storage is in use, when close is called, then no error occurs`() = runBlocking {
        val eventPayload = provideEventPayload()
        storage.write(StorageKeys.EVENT, eventPayload)

        storage.close()

        // Verify no exception is thrown and storage can still be used for reads
        val fileList = storage.readFileList()
        assertNotNull(fileList)
    }

    @OptIn(UseWithCaution::class)
    @Test
    fun `given values stored, when delete is called, then all data is removed`() = runBlocking {
        storage.write(StorageKeys.USER_ID, "user-123")
        storage.write(StorageKeys.ANONYMOUS_ID, "anonymous-123")
        storage.write(StorageKeys.EVENT, provideEventPayload())
        storage.rollover()

        storage.delete()

        assertEquals(String.empty(), storage.readString(StorageKeys.USER_ID, String.empty()))
        assertEquals(String.empty(), storage.readString(StorageKeys.ANONYMOUS_ID, String.empty()))
        assertTrue(storage.readFileList().isEmpty())
    }

    @Test
    fun `when getLibraryVersion is called, then it returns expected library name and version`() {
        val libraryVersion = storage.getLibraryVersion()

        assertEquals("com.rudderstack.sdk.kotlin.core", libraryVersion.getLibraryName())
        assertTrue(libraryVersion.getVersionName().isNotEmpty())
    }

    private fun provideEventPayload() = """{"id":"123","message":"test"}"""

    private fun createOversizedPayload(): String {
        return buildString { repeat(MAX_PAYLOAD_SIZE + 1) { append("x") } }
    }
}

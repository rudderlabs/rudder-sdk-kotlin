package com.rudderstack.sdk.kotlin.android.storage

import android.content.Context
import com.rudderstack.sdk.kotlin.android.storage.exceptions.QueuedPayloadTooLargeException
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.storage.EventBatchFileManager
import com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage
import com.rudderstack.sdk.kotlin.core.internals.storage.MAX_PAYLOAD_SIZE
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys.APP_BUILD
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys.EVENT
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys.IS_SESSION_START
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys.SESSION_ID
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys.USER_ID
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

private const val TEST_WRITE_KEY = "testWriteKey"

class AndroidStorageTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockKeyValueStorage: KeyValueStorage

    @MockK
    private lateinit var mockEventBatchFile: EventBatchFileManager

    @MockK
    private lateinit var mockStorageDirectory: File

    private lateinit var storage: AndroidStorage

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        storage = AndroidStorage(
            context = mockContext,
            writeKey = TEST_WRITE_KEY,
            platformType = PlatformType.Mobile,
            rudderPrefsRepo = mockKeyValueStorage,
            storageDirectory = mockStorageDirectory,
            eventBatchFile = mockEventBatchFile,
        )
    }

    @Nested
    inner class WriteBooleanTest {

        @Test
        fun `given a boolean value, when write is called with non-EVENT key, then value is saved to key-value storage`() = runTest {
            storage.write(IS_SESSION_START, true)

            verify(exactly = 1) { mockKeyValueStorage.save(IS_SESSION_START.key, true) }
        }

        @Test
        fun `given a boolean value, when write is called with EVENT key, then value is not saved to key-value storage`() = runTest {
            storage.write(EVENT, true)

            verify(exactly = 0) { mockKeyValueStorage.save(any<String>(), any<Boolean>()) }
        }
    }

    @Nested
    inner class WriteIntTest {

        @Test
        fun `given an int value, when write is called with non-EVENT key, then value is saved to key-value storage`() = runTest {
            val appBuild = 42
            storage.write(APP_BUILD, appBuild)

            verify(exactly = 1) { mockKeyValueStorage.save(APP_BUILD.key, appBuild) }
        }

        @Test
        fun `given an int value, when write is called with EVENT key, then value is not saved to key-value storage`() = runTest {
            val appBuild = 42
            storage.write(EVENT, appBuild)

            verify(exactly = 0) { mockKeyValueStorage.save(any<String>(), any<Int>()) }
        }
    }

    @Nested
    inner class WriteLongTest {

        @Test
        fun `given a long value, when write is called with non-EVENT key, then value is saved to key-value storage`() = runTest {
            val sessionId = 1234567890L
            storage.write(SESSION_ID, sessionId)

            verify(exactly = 1) { mockKeyValueStorage.save(SESSION_ID.key, sessionId) }
        }

        @Test
        fun `given a long value, when write is called with EVENT key, then value is not saved to key-value storage`() = runTest {
            val sessionId = 1234567890L
            storage.write(EVENT, sessionId)

            verify(exactly = 0) { mockKeyValueStorage.save(any<String>(), any<Long>()) }
        }
    }

    @Nested
    inner class WriteStringTest {

        @Test
        fun `given a string value, when write is called with non-EVENT key, then value is saved to key-value storage`() = runTest {
            val userId = "user-123"
            storage.write(USER_ID, userId)

            verify { mockKeyValueStorage.save(USER_ID.key, userId) }
        }

        @Test
        fun `given a valid event payload, when write is called with EVENT key, then event is stored via eventBatchFile`() = runTest {
            val payload = """{"event":"test"}"""

            storage.write(EVENT, payload)

            coVerify { mockEventBatchFile.storeEvent(payload) }
        }

        @Test
        fun `given a payload exceeding MAX_PAYLOAD_SIZE, when write is called with EVENT key, then QueuedPayloadTooLargeException is thrown`() = runTest {
            val oversizedPayload = "x".repeat(MAX_PAYLOAD_SIZE + 1)

            assertThrows<QueuedPayloadTooLargeException> { storage.write(EVENT, oversizedPayload) }
            coVerify(exactly = 0) { mockEventBatchFile.storeEvent(any()) }
        }
    }

    @Nested
    inner class ReadTest {

        @Test
        fun `given key-value storage returns an int, when readInt is called, then the value from repo is returned`() {
            val appBuild = 99
            every { mockKeyValueStorage.getInt(APP_BUILD.key, 0) } returns appBuild

            val actual = storage.readInt(APP_BUILD, 0)

            assertEquals(appBuild, actual)
        }

        @Test
        fun `given key-value storage returns a boolean, when readBoolean is called, then the value from repo is returned`() {
            val isSessionStart = true
            every { mockKeyValueStorage.getBoolean(IS_SESSION_START.key, false) } returns isSessionStart

            val actual = storage.readBoolean(IS_SESSION_START, false)

            assertEquals(isSessionStart, actual)
        }

        @Test
        fun `given key-value storage returns a long, when readLong is called, then the value from repo is returned`() {
            val sessionId = 9876543210L
            every { mockKeyValueStorage.getLong(SESSION_ID.key, 0L) } returns sessionId

            val actual = storage.readLong(SESSION_ID, 0L)

            assertEquals(sessionId, actual)
        }

        @Test
        fun `given key-value storage returns a string, when readString is called with non-EVENT key, then the value from repo is returned`() {
            val userId = "user-456"
            every { mockKeyValueStorage.getString(USER_ID.key, String.empty()) } returns userId

            val actual = storage.readString(USER_ID, String.empty())

            assertEquals(userId, actual)
        }

        @Test
        fun `given eventBatchFile returns file list, when readString is called with EVENT key, then joined file list is returned`() {
            val fileList = listOf("path/file1", "path/file2")
            val joinedFileList = "path/file1, path/file2"
            every { mockEventBatchFile.read() } returns fileList

            val actual = storage.readString(EVENT, String.empty())

            assertEquals(joinedFileList, actual)
        }
    }

    @Nested
    inner class FileOperationsTest {

        @Test
        fun `given eventBatchFile returns file list, when readFileList is called, then the list from eventBatchFile is returned`() {
            val fileList = listOf("file1")
            every { mockEventBatchFile.read() } returns fileList

            val actual = storage.readFileList()

            assertEquals(fileList, actual)
        }

        @Test
        fun `given eventBatchFile returns content, when readBatchContent is called, then the content is returned`() {
            val batchRef = "file1"
            val batchContent = "batch-content"
            every { mockEventBatchFile.readContent(batchRef) } returns batchContent

            val actual = storage.readBatchContent(batchRef)

            assertEquals(batchContent, actual)
        }

        @Test
        fun `given a batch file path with numeric name, when getBatchId is called, then the numeric id is returned`() {
            val batchFilePath = "/some/path/42"

            val actual = storage.getBatchId(batchFilePath)

            assertEquals(42, actual)
        }

        @Test
        fun `given a batch file path with non-numeric name, when getBatchId is called, then 0 is returned`() {
            val batchFilePath = "/some/path/not-a-number"

            val actual = storage.getBatchId(batchFilePath)

            assertEquals(0, actual)
        }
    }

    @Nested
    inner class LifecycleTest {

        @Test
        fun `given a StorageKey, when remove is called, then key-value storage clear is called`() = runTest {
            storage.remove(USER_ID)

            verify { mockKeyValueStorage.clear(USER_ID.key) }
        }

        @Test
        fun `given a file path, when remove is called, then eventBatchFile remove is called`() {
            val filePath = "file1"
            storage.remove(filePath)

            verify { mockEventBatchFile.remove(filePath) }
        }

        @Test
        fun `when rollover is called, then eventBatchFile rollover is called`() = runTest {
            storage.rollover()

            coVerify { mockEventBatchFile.rollover() }
        }

        @Test
        fun `when close is called, then eventBatchFile closeAndReset is called`() {
            storage.close()

            verify { mockEventBatchFile.closeAndReset() }
        }

        @OptIn(UseWithCaution::class)
        @Test
        fun `when delete is called, then key-value storage is deleted and storage directory is deleted`() {
            mockkStatic(File::deleteRecursively)
            try {
                every { mockStorageDirectory.deleteRecursively() } returns true

                storage.delete()

                verify { mockKeyValueStorage.delete() }
                verify { mockStorageDirectory.deleteRecursively() }
            } finally {
                unmockkStatic(File::deleteRecursively)
            }
        }
    }
}

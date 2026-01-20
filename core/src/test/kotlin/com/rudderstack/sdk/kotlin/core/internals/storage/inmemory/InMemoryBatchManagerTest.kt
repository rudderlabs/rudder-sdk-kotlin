package com.rudderstack.sdk.kotlin.core.internals.storage.inmemory

import com.rudderstack.sdk.kotlin.core.internals.models.DEFAULT_SENT_AT_TIMESTAMP
import com.rudderstack.sdk.kotlin.core.internals.storage.TMP_SUFFIX
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_WRITE_KEY = "testWriteKey"
private const val EVENT_PAYLOAD_1 = """{"id":"123","message":"test"}"""
private const val EVENT_PAYLOAD_2 = """{"id":"124","message":"test"}"""
private const val EVENT_PAYLOAD_3 = """{"id":"125","message":"test"}"""

class InMemoryBatchManagerTest {

    private lateinit var keyValueStorage: InMemoryPrefsStore
    private lateinit var inMemoryBatchManager: InMemoryBatchManager

    @BeforeEach
    fun setup() {
        keyValueStorage = InMemoryPrefsStore()
        inMemoryBatchManager = InMemoryBatchManager(TEST_WRITE_KEY, keyValueStorage)
    }

    @Test
    fun `given no batch file exists, when storeEvent is called, then a batch is created`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)

        rolloverAndAssertBatchContains(EVENT_PAYLOAD_1)
    }

    @Test
    fun `given a batch file exists, when storeEvent is called, then event is appended to existing batch`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)

        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_2)

        rolloverAndAssertBatchContains(",$EVENT_PAYLOAD_2")
    }

    @Test
    fun `given multiple events stored, when readContent is called, then events are comma-separated`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_2)
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_3)

        rolloverAndAssertBatchContains("$EVENT_PAYLOAD_1,$EVENT_PAYLOAD_2,$EVENT_PAYLOAD_3")
    }

    @Test
    fun `given a batch file size exceeds MAX_BATCH_SIZE, when storeEvent is called, then a new batch is created`() = runBlocking {
        val largePayload = createLargePayload()
        inMemoryBatchManager.storeEvent(largePayload)

        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)

        inMemoryBatchManager.rollover()
        val files = inMemoryBatchManager.read()
        assertEquals(2, files.size)
    }

    @Test
    fun `given no batch files exist, when read is called, then empty list is returned`() {
        val files = inMemoryBatchManager.read()

        assertTrue(files.isEmpty())
    }

    @Test
    fun `given a tmp file exists, when read is called, then the tmp file is not included`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)

        val files = inMemoryBatchManager.read()

        assertTrue(files.isEmpty())
    }

    @Test
    fun `given a batch file exists, when remove is called, then true is returned`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)
        inMemoryBatchManager.rollover()

        val files = inMemoryBatchManager.read()
        val result = inMemoryBatchManager.remove(files.first())

        assertTrue(result)
    }

    @Test
    fun `given a batch file does not exist, when remove is called, then false is returned`() {
        val result = inMemoryBatchManager.remove("nonexistent-file")

        assertFalse(result)
    }

    @Test
    fun `given a batch file is removed, when readContent is called, then null is returned`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)
        inMemoryBatchManager.rollover()
        val files = inMemoryBatchManager.read()
        val fileName = files.first()
        inMemoryBatchManager.remove(fileName)

        val content = inMemoryBatchManager.readContent(fileName)

        assertNull(content)
    }

    @Test
    fun `given a batch file exists, when readContent is called, then the content is returned`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)

        rolloverAndAssertBatchContains(EVENT_PAYLOAD_1)
    }

    @Test
    fun `given a batch file does not exist, when readContent is called, then null is returned`() {
        val content = inMemoryBatchManager.readContent("nonexistent-file")

        assertNull(content)
    }

    @Test
    fun `given a batch file exists, when rollover is called, then file is finalized with sentAt suffix`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)

        rolloverAndAssertBatchContains("\"sentAt\":\"$DEFAULT_SENT_AT_TIMESTAMP\"")
    }

    @Test
    fun `given a batch file exists, when rollover is called, then tmp suffix is removed from filename`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)

        inMemoryBatchManager.rollover()

        val files = inMemoryBatchManager.read()
        assertEquals(1, files.size)
        assertFalse(files.first().endsWith(TMP_SUFFIX))
        assertEquals("0", files.first())
    }

    @Test
    fun `given no batch file exists, when rollover is called, then no error occurs`() = runBlocking {
        inMemoryBatchManager.rollover()

        val files = inMemoryBatchManager.read()
        assertTrue(files.isEmpty())
    }

    @Test
    fun `given a batch file exists, when rollover is called, then file index is incremented`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)
        inMemoryBatchManager.rollover()
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_2)
        inMemoryBatchManager.rollover()

        val files = inMemoryBatchManager.read()
        assertEquals(2, files.size)
        assertTrue(files.contains("0"))
        assertTrue(files.contains("1"))
    }

    @Test
    fun `given events stored, when closeAndReset is called, then in-progress batch is abandoned`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)

        inMemoryBatchManager.closeAndReset()

        val files = inMemoryBatchManager.read()
        assertTrue(files.isEmpty())
    }

    @Test
    fun `given multiple batch files exist, when delete is called, then all files are removed`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)
        inMemoryBatchManager.rollover()
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_2)
        inMemoryBatchManager.rollover()
        assertEquals(2, inMemoryBatchManager.read().size)

        inMemoryBatchManager.delete()

        assertTrue(inMemoryBatchManager.read().isEmpty())
    }

    @Test
    fun `given single event, when rollover is called, then batch JSON structure is correct`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)

        val expectedContent = """{"batch":[$EVENT_PAYLOAD_1],"sentAt":"$DEFAULT_SENT_AT_TIMESTAMP"}"""
        rolloverAndAssertBatchEquals(expectedContent)
    }

    @Test
    fun `given multiple events, when rollover is called, then batch JSON structure is correct`() = runBlocking {
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_1)
        inMemoryBatchManager.storeEvent(EVENT_PAYLOAD_2)

        val expectedContent = """{"batch":[$EVENT_PAYLOAD_1,$EVENT_PAYLOAD_2],"sentAt":"$DEFAULT_SENT_AT_TIMESTAMP"}"""
        rolloverAndAssertBatchEquals(expectedContent)
    }

    private suspend fun rolloverAndAssertBatchContains(expected: String) {
        inMemoryBatchManager.rollover()
        val files = inMemoryBatchManager.read()
        val content = inMemoryBatchManager.readContent(files.first())
        assertNotNull(content)
        assertTrue(content!!.contains(expected))
    }

    private suspend fun rolloverAndAssertBatchEquals(expected: String) {
        inMemoryBatchManager.rollover()
        val files = inMemoryBatchManager.read()
        val content = inMemoryBatchManager.readContent(files.first())
        assertNotNull(content)
        assertEquals(expected, content)
    }
}

private fun createLargePayload(): String {
    val baseString = "This is a line of text to make the string large.\n"
    val targetSizeKB = 600
    val repetitions = (targetSizeKB * 1024) / baseString.length + 1
    return buildString { repeat(repetitions) { append(baseString) } }
}

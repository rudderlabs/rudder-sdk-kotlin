package com.rudderstack.sdk.kotlin.core.internals.storage

import com.rudderstack.sdk.kotlin.core.internals.models.DEFAULT_SENT_AT_TIMESTAMP
import com.rudderstack.sdk.kotlin.core.internals.utils.appendWriteKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

private const val TEST_WRITE_KEY = "writeKey"

class EventBatchFileManagerTest {

    private val writeKey = TEST_WRITE_KEY
    private val fileName = "0"
    private val epochTimestamp = DEFAULT_SENT_AT_TIMESTAMP
    private val directory = File(FILE_DIRECTORY.appendWriteKey(writeKey))
    private val keyValueStorage = PropertiesFile(directory.parentFile, writeKey)

    private lateinit var eventBatchFileManager: EventBatchFileManager

    @BeforeEach
    fun setup() {
        eventBatchFileManager = EventBatchFileManager(directory, writeKey, keyValueStorage)
    }

    @AfterEach
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `when storeEvent is called, then a file is created`() = runBlocking {
        eventBatchFileManager.storeEvent(provideMessagePayload())

        val files = directory.listFiles()

        assertEquals(1, files?.size)
    }

    @Test
    fun `given no batch file exists, when storeEvent is called, then a file is created`() = runBlocking {
        eventBatchFileManager.rollover()
        eventBatchFileManager.storeEvent(provideMessagePayload())

        val files = directory.listFiles()

        assertEquals(1, files?.size)
    }

    @Test
    fun `given the batch file exists, when remove is called, then the specified file is deleted`() {
        val file = provideFile(directory, fileName)
        file.writeText("content")

        val result = eventBatchFileManager.remove(file.absolutePath)
        assertTrue(result)
        assertFalse(file.exists())
    }

    @Test
    fun `given the file exists, when the file size is less than MAX_BATCH_SIZE, then the previous file is used`() =
        runBlocking {
            val expectedPayload = provideMessagePayload()

            eventBatchFileManager.storeEvent(expectedPayload)

            val files = directory.listFiles()
            assertNotNull(files)
            assertEquals(1, files?.size)

            eventBatchFileManager.storeEvent(expectedPayload)
            eventBatchFileManager.storeEvent(expectedPayload)

            val file = files?.first()
            val contents = file?.readText()
            assertTrue(contents!!.contains(",$expectedPayload"))
        }

    @Test
    fun `given the file exists, when the file size is more than MAX_BATCH_SIZE, then a new file is created`() = runBlocking {
        eventBatchFileManager.storeEvent(createLargeString(800))
        eventBatchFileManager.storeEvent(provideMessagePayload())

        assertFalse(File(directory, "0.tmp").exists())
        assertTrue(File(directory, "0").exists())

        val expectedContents = """{"batch":[${provideMessagePayload()}"""
        val newFile = File(directory, "1.tmp")
        assertTrue(newFile.exists())

        val actualContents = newFile.readText()
        assertEquals(expectedContents, actualContents)
    }

    @Test
    fun `given the file exists, when read is called, then the file path is inside the list of file paths`() {
        val file = provideFile(directory, fileName)
        file.writeText("content")

        val files = eventBatchFileManager.read()

        assertEquals(1, files.size)
        assertTrue(files.first().contains(fileName))
    }


    @Test
    fun `given a batch file, when rollover is called, then current file is finalized and a new one is created`() =
        runBlocking {
            val expectedPayload = provideMessagePayload()
            eventBatchFileManager.storeEvent(expectedPayload)

            val filesBeforeRollover = directory.listFiles()
            assertNotNull(filesBeforeRollover)
            assertEquals(1, filesBeforeRollover?.size)

            eventBatchFileManager.rollover()

            val filesAfterRollover = directory.listFiles()
            assertNotNull(filesAfterRollover)
            assertEquals(1, filesAfterRollover?.size)
        }

    @Test
    fun `given a batch file exists, when  read finishes open file and lists it`() = runBlocking {
        val expectedPayload = provideMessagePayload()
        eventBatchFileManager.storeEvent(expectedPayload)
        eventBatchFileManager.rollover()

        val fileUrls = eventBatchFileManager.read()
        assertEquals(1, fileUrls.size)

        val expectedContents = """ {"batch":[${expectedPayload}],"sentAt":"$epochTimestamp"}""".trim()
        val newFile = File(directory, fileName)
        assertTrue(newFile.exists())

        val actualContents = newFile.readText()
        assertEquals(expectedContents, actualContents)
    }

    @Test
    fun `given a batch file exists, when finish is called, then current file is finalised`() = runBlocking {
        val file = File(directory, fileName + TMP_SUFFIX)
        file.writeText("content")
        eventBatchFileManager.storeEvent(provideMessagePayload())

        eventBatchFileManager.finish()

        assertFalse(File(directory, fileName + TMP_SUFFIX).exists())
        assertTrue(File(directory, fileName).exists())
    }

    @Test
    fun `given a batch file exists, when rollover is called, then filename includes subject`() = runBlocking {
        eventBatchFileManager.storeEvent(provideMessagePayload())

        eventBatchFileManager.rollover()

        assertEquals(1, keyValueStorage.getInt(FILE_INDEX + writeKey, -1))
    }


    @Test
    fun `given an empty list, when read is executed then no messages are stored`() {
        val file = EventBatchFileManager(directory, writeKey, keyValueStorage)
        assertTrue(file.read().isEmpty())
    }

    @Test
    fun `given a batch file exists, when multiple reads happen, multiple reads do not create extra files`() = runBlocking {
        eventBatchFileManager.storeEvent(provideMessagePayload())
        eventBatchFileManager.rollover()

        eventBatchFileManager.read().let {
            assertEquals(1, it.size)
            val expectedContents =
                """ {"batch":[${provideMessagePayload()}],"sentAt":"$epochTimestamp"}""".trim()
            val newFile = File(directory, fileName)
            assertTrue(newFile.exists())
            val actualContents = newFile.readText()
            assertEquals(expectedContents, actualContents)
        }

        eventBatchFileManager.read().let {
            assertEquals(1, it.size)
            assertEquals(1, directory.list()!!.size)
        }
    }

    @Test
    fun `given different write keys, when an message is stored, then the filename is the expected one`() = runBlocking {
        val writeKey1 = "123"
        val writeKey2 = "qwerty"

        val file1 = EventBatchFileManager(directory, writeKey1, keyValueStorage)
        val file2 = EventBatchFileManager(directory, writeKey2, keyValueStorage)

        file1.storeEvent(provideMessagePayload())
        file2.storeEvent(provideMessagePayload())

        file1.rollover()
        file2.rollover()

        assertEquals(listOf("${FILE_DIRECTORY.appendWriteKey(writeKey)}/0"), file1.read())
        assertEquals(listOf("${FILE_DIRECTORY.appendWriteKey(writeKey)}/0"), file2.read())
    }

    @Test
    fun `given a batch file exists, when rollover, then file is deleted`() = runBlocking {
        eventBatchFileManager.storeEvent(provideMessagePayload())
        eventBatchFileManager.rollover()

        val list = eventBatchFileManager.read()
        eventBatchFileManager.remove(list[0])

        assertFalse(File(list[0]).exists())
    }
}


private fun provideMessagePayload() = "{\"id\":\"123\",\"message\":\"test\"}"

private fun provideFile(directory: File, fileName: String) = File(directory, fileName)

private fun createLargeString(targetSizeKB: Int): String {
    val stringBuilder = StringBuilder()
    val smallString = "This is a line of text to make the string large.\n"
    val smallStringSize = smallString.toByteArray(Charsets.UTF_8).size

    val repetitions = (targetSizeKB * 1024) / smallStringSize + 1

    repeat(repetitions) {
        stringBuilder.append(smallString)
    }
    return stringBuilder.toString()
}

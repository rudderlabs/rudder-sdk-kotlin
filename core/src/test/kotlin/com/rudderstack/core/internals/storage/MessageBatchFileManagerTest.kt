package com.rudderstack.core.internals.storage

import com.rudderstack.core.internals.models.DEFAULT_SENT_AT_TIMESTAMP
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

private const val TEST_WRITE_KEY = "asdf"

class MessageBatchFileManagerTest {

    private val writeKey = TEST_WRITE_KEY
    private val fileName = "$writeKey-0"
    private val epochTimestamp = DEFAULT_SENT_AT_TIMESTAMP
    private val directory = File(FILE_DIRECTORY)
    private val keyValueStorage = PropertiesFile(directory.parentFile, writeKey)

    private lateinit var messageBatchFileManager: MessageBatchFileManager

    @Before
    fun setup() {
        messageBatchFileManager = MessageBatchFileManager(directory, writeKey, keyValueStorage)
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `when storeMessage is called, then a file is created`() = runBlocking {
        messageBatchFileManager.storeMessage(provideMessagePayload())

        val files = directory.listFiles()

        assertEquals(1, files?.size)
    }

    @Test
    fun `given no batch file exists, when storeMessage is called, then a file is created`() = runBlocking {
        messageBatchFileManager.rollover()
        messageBatchFileManager.storeMessage(provideMessagePayload())

        val files = directory.listFiles()

        assertEquals(1, files?.size)
    }

    @Test
    fun `given the batch file exists, when remove is called, then the specified file is deleted`() {
        val file = provideFile(directory, fileName)
        file.writeText("content")

        val result = messageBatchFileManager.remove(file.absolutePath)
        assertTrue(result)
        assertFalse(file.exists())
    }

    @Test
    fun `given the file exists, when the file size is less than MAX_BATCH_SIZE, then the previous file is used`() =
        runBlocking {
            val expectedPayload = provideMessagePayload()

            messageBatchFileManager.storeMessage(expectedPayload)

            val files = directory.listFiles()
            assertNotNull(files)
            assertEquals(1, files?.size)

            messageBatchFileManager.storeMessage(expectedPayload)
            messageBatchFileManager.storeMessage(expectedPayload)

            val file = files?.first()
            val contents = file?.readText()
            assertTrue(contents!!.contains(",$expectedPayload"))
        }

    @Test
    fun `given the file exists, when the file size is more than MAX_BATCH_SIZE, then a new file is created`() = runBlocking {
        messageBatchFileManager.storeMessage(createLargeString(800))
        messageBatchFileManager.storeMessage(provideMessagePayload())

        assertFalse(File(directory, "$writeKey-0.tmp").exists())
        assertTrue(File(directory, "$writeKey-0").exists())

        val expectedContents = """{"batch":[${provideMessagePayload()}"""
        val newFile = File(directory, "$writeKey-1.tmp")
        assertTrue(newFile.exists())

        val actualContents = newFile.readText()
        assertEquals(expectedContents, actualContents)
    }

    @Test
    fun `given the file exists, when read is called, then the file path is inside the list of file paths`() {
        val file = provideFile(directory, fileName)
        file.writeText("content")

        val files = messageBatchFileManager.read()

        assertEquals(1, files.size)
        assertTrue(files.first().contains(fileName))
    }


    @Test
    fun `given a batch file, when rollover is called, then current file is finalized and a new one is created`() =
        runBlocking {
            val expectedPayload = provideMessagePayload()
            messageBatchFileManager.storeMessage(expectedPayload)

            val filesBeforeRollover = directory.listFiles()
            assertNotNull(filesBeforeRollover)
            assertEquals(1, filesBeforeRollover?.size)

            messageBatchFileManager.rollover()

            val filesAfterRollover = directory.listFiles()
            assertNotNull(filesAfterRollover)
            assertEquals(1, filesAfterRollover?.size)
        }

    @Test
    fun `given a batch file exists, when  read finishes open file and lists it`() = runBlocking {
        val expectedPayload = provideMessagePayload()
        messageBatchFileManager.storeMessage(expectedPayload)
        messageBatchFileManager.rollover()

        val fileUrls = messageBatchFileManager.read()
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
        messageBatchFileManager.storeMessage(provideMessagePayload())

        messageBatchFileManager.finish()

        assertFalse(File(directory, fileName + TMP_SUFFIX).exists())
        assertTrue(File(directory, fileName).exists())
    }

    @Test
    fun `given a batch file, when shutdown happens, hook closes output stream`() = runBlocking {
        val file = File(directory, fileName + TMP_SUFFIX)
        messageBatchFileManager.start(file)

        messageBatchFileManager.registerShutdownHook()

        Runtime.getRuntime().addShutdownHook(Thread {
            assertTrue(file.exists())
            val outputStream = FileOutputStream(file, true)
            assertFalse(outputStream.fd.valid())
        })
    }

    @Test
    fun `given a batch file exists, when rollover is called, then filename includes subject`() = runBlocking {
        messageBatchFileManager.storeMessage(provideMessagePayload())

        messageBatchFileManager.rollover()

        assertEquals(1, keyValueStorage.getInt(FILE_INDEX + writeKey, -1))
    }


    @Test
    fun `given an empty list, when read is executed then no messages are stored`() {
        val file = MessageBatchFileManager(directory, writeKey, keyValueStorage)
        assertTrue(file.read().isEmpty())
    }

    @Test
    fun `given a batch file exists, when multiple reads happen, multiple reads do not create extra files`() = runBlocking {
        messageBatchFileManager.storeMessage(provideMessagePayload())
        messageBatchFileManager.rollover()

        messageBatchFileManager.read().let {
            assertEquals(1, it.size)
            val expectedContents =
                """ {"batch":[${provideMessagePayload()}],"sentAt":"$epochTimestamp"}""".trim()
            val newFile = File(directory, fileName)
            assertTrue(newFile.exists())
            val actualContents = newFile.readText()
            assertEquals(expectedContents, actualContents)
        }

        messageBatchFileManager.read().let {
            assertEquals(1, it.size)
            assertEquals(1, directory.list()!!.size)
        }
    }

    @Test
    fun `given different write keys, when an message is stored, then the filename is the expected one`() = runBlocking {
        val writeKey1 = "123"
        val writeKey2 = "qwerty"

        val file1 = MessageBatchFileManager(directory, writeKey1, keyValueStorage)
        val file2 = MessageBatchFileManager(directory, writeKey2, keyValueStorage)

        file1.storeMessage(provideMessagePayload())
        file2.storeMessage(provideMessagePayload())

        file1.rollover()
        file2.rollover()

        assertEquals(listOf("$FILE_DIRECTORY$writeKey1-0"), file1.read())
        assertEquals(listOf("$FILE_DIRECTORY$writeKey2-0"), file2.read())
    }

    @Test
    fun `given a batch file exists, when rollover, then file is deleted`() = runBlocking {
        messageBatchFileManager.storeMessage(provideMessagePayload())
        messageBatchFileManager.rollover()

        val list = messageBatchFileManager.read()
        messageBatchFileManager.remove(list[0])

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

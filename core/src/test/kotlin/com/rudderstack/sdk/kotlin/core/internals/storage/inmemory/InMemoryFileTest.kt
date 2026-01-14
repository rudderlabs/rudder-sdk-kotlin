package com.rudderstack.sdk.kotlin.core.internals.storage.inmemory

import com.rudderstack.sdk.kotlin.core.internals.storage.TMP_SUFFIX
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InMemoryFileTest {

    private lateinit var inMemoryFile: InMemoryFile

    @BeforeEach
    fun setUp() {
        inMemoryFile = InMemoryFile("test-file.tmp")
    }

    @Test
    fun `given a new file, when exists is called, then it returns false`() {
        val newFile = InMemoryFile("new-file")

        assertFalse(newFile.exists())
    }

    @Test
    fun `given a new file, when createNewFile is called, then it returns true`() {
        val newFile = InMemoryFile("new-file")

        assertTrue(newFile.createNewFile())
    }

    @Test
    fun `given a file that was created, when createNewFile is called again, then it returns false`() {
        inMemoryFile.createNewFile()

        assertFalse(inMemoryFile.createNewFile())
    }

    @Test
    fun `given a file that was created, when exists is called, then it returns true`() {
        inMemoryFile.createNewFile()

        assertTrue(inMemoryFile.exists())
    }

    @Test
    fun `given a new file, when name is accessed, then it returns the constructor value`() {
        val fileName = "my-custom-file.txt"
        val file = InMemoryFile(fileName)

        assertEquals(fileName, file.name)
    }

    @Test
    fun `given a new file, when length is accessed, then it returns zero`() {
        val newFile = InMemoryFile("new-file")

        assertEquals(0, newFile.length)
    }

    @Test
    fun `given content appended to file, when length is accessed, then it returns the content length`() {
        val content = "Hello, World!"
        inMemoryFile.append(content)

        assertEquals(content.length, inMemoryFile.length)
    }

    @Test
    fun `given content appended to file, when readText is called, then it returns the appended content`() {
        val content = "Hello, World!"
        inMemoryFile.append(content)

        assertEquals(content, inMemoryFile.readText())
    }

    @Test
    fun `given multiple content appended to file, when readText is called, then it returns all content concatenated`() {
        val content1 = "Hello, "
        val content2 = "World!"
        inMemoryFile.append(content1)
        inMemoryFile.append(content2)

        assertEquals(content1 + content2, inMemoryFile.readText())
    }

    @Test
    fun `given a file with tmp suffix, when nameWithoutExtension is accessed, then it returns name without suffix`() {
        val baseName = "test-file"
        val file = InMemoryFile(baseName + TMP_SUFFIX)

        assertEquals(baseName, file.nameWithoutExtension)
    }

    @Test
    fun `given a file without tmp suffix, when nameWithoutExtension is accessed, then it returns the original name`() {
        val fileName = "test-file.json"
        val file = InMemoryFile(fileName)

        assertEquals(fileName, file.nameWithoutExtension)
    }

    @Test
    fun `given an empty file, when readText is called, then it returns empty string`() {
        val newFile = InMemoryFile("empty-file")

        assertEquals("", newFile.readText())
    }
}

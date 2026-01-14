package com.rudderstack.sdk.kotlin.core.internals.storage.inmemory

import com.rudderstack.sdk.kotlin.core.internals.models.DEFAULT_SENT_AT_TIMESTAMP
import com.rudderstack.sdk.kotlin.core.internals.storage.BATCH_SENT_AT_SUFFIX
import com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage
import com.rudderstack.sdk.kotlin.core.internals.storage.MAX_BATCH_SIZE
import com.rudderstack.sdk.kotlin.core.internals.storage.TMP_SUFFIX
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.toFileDirectory
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentHashMap

internal const val BATCH_INDEX = "rudderstack.event.batch.index."
private const val BATCH_PREFIX = "{\"batch\":["

/**
 * Manages event batches in memory, providing a similar interface to [com.rudderstack.sdk.kotlin.core.internals.storage.EventBatchFileManager]
 * but without file system dependencies.
 *
 * This class is useful for in-memory storage implementations where events need to be
 * batched and managed without persisting to disk.
 *
 * @property writeKey A unique key used to name and identify batches.
 * @property keyValueStorage A [KeyValueStorage] instance for storing and retrieving batch index information.
 */
@InternalRudderApi
internal class InMemoryBatchManager(
    private val writeKey: String,
    private val keyValueStorage: KeyValueStorage
) {

    /**
     * The key used to store the index of the current batch file.
     */
    private val fileIndexKey = writeKey.toFileDirectory(BATCH_INDEX)

    /**
     * Storage for event batches. Keys are batch identifiers (with or without .tmp suffix).
     */
    private val files = ConcurrentHashMap<String, InMemoryFile>()

    /**
     * The current batch file being written to.
     */
    private var curFile: InMemoryFile? = null

    /**
     * A semaphore to control concurrent access to batch operations.
     */
    private val semaphore = Semaphore(1)

    /**
     * Stores an event payload in the current batch file. If the current file exceeds the maximum
     * batch size, it is finalized and a new file is started.
     *
     * @param eventPayload The event payload to be stored.
     */
    internal suspend fun storeEvent(eventPayload: String) = withLock {
        var newFile = false
        var file = currentFile()

        if (!file.exists()) {
            file.createNewFile()
            start(file)
            newFile = true
        }

        if (file.length > MAX_BATCH_SIZE) {
            finish()
            file = currentFile()
            file.createNewFile()
            start(file)
            newFile = true
        }

        val contents = if (newFile) eventPayload else ",$eventPayload"
        writeToFile(contents, file)
    }

    /**
     * Reads the list of completed batch file names (without .tmp suffix).
     * Files are sorted by their numeric batch index to ensure correct upload order.
     *
     * @return A list of file names for completed batches, sorted by batch index.
     */
    internal fun read(): List<String> {
        return files.keys()
            .toList()
            .filter { !it.endsWith(TMP_SUFFIX) }
            .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
    }

    /**
     * Removes a specific batch file from storage.
     *
     * @param filePath The file name to remove.
     * @return `true` if the file existed and was removed, `false` otherwise.
     */
    internal fun remove(filePath: String): Boolean {
        return files.remove(filePath) != null
    }

    /**
     * Reads the content of a batch file.
     *
     * @param filePath The file name to read.
     * @return The batch content as a String, or null if the file does not exist.
     */
    internal fun readContent(filePath: String): String? {
        return files[filePath]?.readText()
    }

    /**
     * Completes the current batch file and prepares for the next batch.
     * Appends the closing suffix with sentAt timestamp and renames the file.
     */
    internal suspend fun rollover() = withLock {
        finish()
    }

    /**
     * Increments the index used to name batch files.
     */
    private fun incrementFileIndex() {
        val index = keyValueStorage.getInt(fileIndexKey, 0)
        keyValueStorage.save(fileIndexKey, index + 1)
    }

    /**
     * Starts a new batch file with the batch prefix.
     *
     * @param file The file to start writing to.
     */
    private fun start(file: InMemoryFile) {
        files[file.name] = file
        writeToFile(BATCH_PREFIX, file)
    }

    /**
     * Completes the current batch file with a timestamp and renames it.
     */
    private fun finish() {
        val file = currentFile()
        if (!file.exists()) return
        val contents = "$BATCH_SENT_AT_SUFFIX$DEFAULT_SENT_AT_TIMESTAMP\"}"
        writeToFile(contents, file)
        files.remove(file.name)
        files[file.nameWithoutExtension] = file
        incrementFileIndex()
        reset()
    }

    /**
     * Returns the current batch file, creating it if necessary.
     *
     * @return The current batch file.
     */
    private fun currentFile(): InMemoryFile {
        return curFile ?: run {
            val index = keyValueStorage.getInt(fileIndexKey, 0)
            InMemoryFile("$index$TMP_SUFFIX").also { curFile = it }
        }
    }

    /**
     * Writes the given content to the specified file, appending to the existing content.
     *
     * @param content The content to write.
     * @param file The file to write to.
     */
    private fun writeToFile(content: String, file: InMemoryFile) {
        file.append(content)
    }

    /**
     * Resets the current file reference.
     */
    private fun reset() {
        curFile = null
    }

    /**
     * Closes the current file reference without finalizing it.
     */
    internal fun closeAndReset() {
        reset()
    }

    /**
     * Deletes all batch files and resets the current file reference.
     */
    internal fun delete() {
        files.clear()
        reset()
    }

    /**
     * Acquires a lock, executes the provided block, and releases the lock.
     *
     * @param block The block of code to execute within the lock.
     */
    private suspend fun withLock(block: () -> Unit) {
        semaphore.acquire()
        try {
            block()
        } finally {
            semaphore.release()
        }
    }
}

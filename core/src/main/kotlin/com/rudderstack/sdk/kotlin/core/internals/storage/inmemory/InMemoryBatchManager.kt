package com.rudderstack.sdk.kotlin.core.internals.storage.inmemory

import com.rudderstack.sdk.kotlin.core.internals.models.DEFAULT_SENT_AT_TIMESTAMP
import com.rudderstack.sdk.kotlin.core.internals.storage.BATCH_SENT_AT_SUFFIX
import com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage
import com.rudderstack.sdk.kotlin.core.internals.storage.MAX_BATCH_SIZE
import com.rudderstack.sdk.kotlin.core.internals.storage.TMP_SUFFIX
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.toFileDirectory
import kotlinx.coroutines.sync.Semaphore
import org.jetbrains.annotations.VisibleForTesting
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
    private val batches = ConcurrentHashMap<String, StringBuilder>()

    /**
     * The current batch file being written to.
     */
    private var curFile: String? = null

    /**
     * A semaphore to control concurrent access to batch operations.
     */
    private val semaphore = Semaphore(1)

    /**
     * Stores an event payload in the current batch. If the current batch exceeds the maximum
     * batch size, it is finalized and a new batch is started.
     *
     * @param eventPayload The event payload to be stored.
     */
    suspend fun storeEvent(eventPayload: String) = withLock {
        var newFile = false
        var file = currentFile()

        if (!batches.containsKey(file)) {
            start(file)
            newFile = true
        }

        val batchLength = batches[file]?.length ?: 0
        if (batchLength > MAX_BATCH_SIZE) {
            finish()
            file = currentFile()
            start(file)
            newFile = true
        }

        val contents = if (newFile) eventPayload else ",$eventPayload"
        writeToFile(contents, file)
    }

    /**
     * Reads the list of completed batch keys (without .tmp suffix).
     *
     * @return A list of batch keys for completed batches.
     */
    fun read(): List<String> {
        return batches.keys().toList().filter { !it.endsWith(TMP_SUFFIX) }
    }

    /**
     * Removes a specific batch from storage.
     *
     * @param batchRef The batch key to remove.
     * @return `true` if the batch existed and was removed, `false` otherwise.
     */
    fun remove(batchRef: String): Boolean {
        return batches.remove(batchRef) != null
    }

    /**
     * Reads the content of a batch.
     *
     * @param batchRef The batch key to read.
     * @return The batch content as a String, or null if the batch does not exist.
     */
    fun readContent(batchRef: String): String? {
        return batches[batchRef]?.toString()
    }

    /**
     * Completes the current batch and prepares for the next batch.
     * Appends the closing suffix with sentAt timestamp and renames the batch key.
     */
    suspend fun rollover() = withLock {
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
    @VisibleForTesting
    internal fun start(file: String) {
        batches[file] = StringBuilder()
        writeToFile(BATCH_PREFIX, file)
    }

    /**
     * Completes the current batch file with a timestamp and renames it.
     */
    @VisibleForTesting
    internal fun finish() {
        val file = currentFile()
        val batch = batches[file] ?: return
        val contents = "$BATCH_SENT_AT_SUFFIX$DEFAULT_SENT_AT_TIMESTAMP\"}"
        writeToFile(contents, file)
        batches.remove(file)
        batches[file.removeSuffix(TMP_SUFFIX)] = batch
        incrementFileIndex()
        reset()
    }

    /**
     * Returns the current batch file, creating it if necessary.
     *
     * @return The current batch file.
     */
    private fun currentFile(): String {
        if (curFile == null) {
            val index = keyValueStorage.getInt(fileIndexKey, 0)
            curFile = "$index$TMP_SUFFIX"
        }
        return curFile ?: ""
    }

    /**
     * Writes the given content to the specified file, appending to the existing content if the file is already open.
     *
     * @param content The content to write.
     * @param file The file to write to.
     */
    private fun writeToFile(content: String, file: String) {
        batches[file]?.append(content)
    }

    /**
     * Resets the state of the current batch key reference.
     */
    private fun reset() {
        curFile = null
    }

    /**
     * Closes the current batch reference without finalizing it.
     */
    fun closeAndReset() {
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

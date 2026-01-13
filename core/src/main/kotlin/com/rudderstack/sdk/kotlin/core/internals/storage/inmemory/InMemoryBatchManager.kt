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
     * The key used to store the index of the current batch.
     */
    private val batchIndexKey = writeKey.toFileDirectory(BATCH_INDEX)

    /**
     * Storage for event batches. Keys are batch identifiers (with or without .tmp suffix).
     */
    private val batches = ConcurrentHashMap<String, StringBuilder>()

    /**
     * Current batch key reference (with .tmp suffix).
     */
    private var curBatchKey: String? = null

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
        var batchKey = currentBatchKey()

        if (!batches.containsKey(batchKey)) {
            start(batchKey)
            newFile = true
        }

        val batchLength = batches[batchKey]?.length ?: 0
        if (batchLength > MAX_BATCH_SIZE) {
            finish()
            batchKey = currentBatchKey()
            start(batchKey)
            newFile = true
        }

        val contents = if (newFile) eventPayload else ",$eventPayload"
        writeToBatch(contents, batchKey)
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
     * Increments the index used to name batches.
     */
    private fun incrementBatchIndex() {
        val index = keyValueStorage.getInt(batchIndexKey, 0)
        keyValueStorage.save(batchIndexKey, index + 1)
    }

    /**
     * Starts a new batch with the batch prefix.
     *
     * @param batchKey The batch key to start writing to.
     */
    @VisibleForTesting
    internal fun start(batchKey: String) {
        batches[batchKey] = StringBuilder()
        writeToBatch(BATCH_PREFIX, batchKey)
    }

    /**
     * Completes the current batch with a timestamp and renames it.
     */
    @VisibleForTesting
    internal fun finish() {
        val batchKey = currentBatchKey()
        val batch = batches[batchKey] ?: return
        val contents = "$BATCH_SENT_AT_SUFFIX$DEFAULT_SENT_AT_TIMESTAMP\"}"
        writeToBatch(contents, batchKey)
        batches.remove(batchKey)
        batches[batchKey.removeSuffix(TMP_SUFFIX)] = batch
        incrementBatchIndex()
        reset()
    }

    /**
     * Returns the current batch key, creating it if necessary.
     *
     * @return The current batch key.
     */
    private fun currentBatchKey(): String {
        if (curBatchKey == null) {
            val index = keyValueStorage.getInt(batchIndexKey, 0)
            curBatchKey = "$index$TMP_SUFFIX"
        }
        return curBatchKey ?: ""
    }

    /**
     * Writes the given content to the specified batch.
     *
     * @param content The content to write.
     * @param batchKey The batch key to write to.
     */
    private fun writeToBatch(content: String, batchKey: String) {
        batches[batchKey]?.append(content)
    }

    /**
     * Resets the state of the current batch key reference.
     */
    private fun reset() {
        curBatchKey = null
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

package com.rudderstack.kotlin.core.internals.storage

import com.rudderstack.kotlin.core.internals.models.DEFAULT_SENT_AT_TIMESTAMP
import com.rudderstack.kotlin.core.internals.utils.toFileDirectory
import kotlinx.coroutines.sync.Semaphore
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.FileOutputStream

internal const val FILE_INDEX = "rudderstack.message.file.index."
private const val BATCH_PREFIX = "{\"batch\":["
internal const val BATCH_SENT_AT_SUFFIX = "],\"sentAt\":\""
internal const val TMP_SUFFIX = ".tmp"

/**
 * Manages the creation, storage, and management of message batch files within a specified directory.
 *
 * The [MessageBatchFileManager] handles batching messages into files, ensuring that individual files do not exceed
 * a predefined size. It supports operations such as storing messages, reading batch files, and rolling over to new files.
 *
 * @property directory The directory where batch files are stored.
 * @property writeKey A unique key used to name and identify batch files.
 * @property keyValueStorage A [KeyValueStorage] instance for storing and retrieving file index information.
 */
@Suppress("Detekt.TooManyFunctions")
class MessageBatchFileManager(
    private val directory: File,
    private val writeKey: String,
    private val keyValueStorage: KeyValueStorage
) {

    /**
     * The key used to store the index of the current batch file.
     */
    private val fileIndexKey = writeKey.toFileDirectory(FILE_INDEX)

    /**
     * The current file output stream used for writing to the active batch file.
     */
    private var os: FileOutputStream? = null

    /**
     * The current batch file being written to.
     */
    private var curFile: File? = null

    /**
     * A semaphore to control concurrent access to file operations.
     */
    private val semaphore = Semaphore(1)

    init {
        // Create the directory if it does not exist and register a shutdown hook to close the file output stream.
        createDirectory(directory)
    }

    /**
     * Stores a message payload in the current batch file. If the current file exceeds the maximum batch size,
     * a new file is created.
     *
     * @param messagePayload The message payload to be stored.
     * @throws Exception If there is an issue with file operations.
     */
    suspend fun storeMessage(messagePayload: String) = withLock {
        var newFile = false
        var file = currentFile()

        if (!file.exists()) {
            file.createNewFile()
            start(file)
            newFile = true
        }

        if (file.length() > MAX_BATCH_SIZE) {
            finish()
            file = currentFile()
            file.createNewFile()
            start(file)
            newFile = true
        }

        val contents = if (newFile) messagePayload else ",$messagePayload"
        writeToFile(contents.toByteArray(), file)
    }

    /**
     * Reads the list of batch files in the directory that are associated with the given write key.
     *
     * @return A list of file paths for the batch files.
     */
    fun read(): List<String> {
        val files = directory.listFiles { _, name ->
            name.contains(writeKey) && !name.endsWith(TMP_SUFFIX)
        } ?: emptyArray()
        return files.map { it.absolutePath }
    }

    /**
     * Removes a specific batch file from the directory.
     *
     * @param filePath The path of the file to be removed.
     * @return `true` if the file was successfully deleted, `false` otherwise.
     */
    fun remove(filePath: String): Boolean {
        return File(filePath).delete()
    }

    /**
     * Completes the current batch file and prepares for the next batch. Renames the file and increments the file index.
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
    fun start(file: File) {
        writeToFile(BATCH_PREFIX.toByteArray(), file)
    }

    /**
     * Completes the current batch file with a timestamp and renames it.
     */
    @VisibleForTesting
    fun finish() {
        val file = currentFile()
        if (!file.exists()) return
        val contents = "$BATCH_SENT_AT_SUFFIX$DEFAULT_SENT_AT_TIMESTAMP\"}"
        writeToFile(contents.toByteArray(), file)
        file.renameTo(File(directory, file.nameWithoutExtension))
        os?.close()
        incrementFileIndex()
        reset()
    }

    /**
     * Returns the current batch file, creating it if necessary.
     *
     * @return The current batch file.
     */
    private fun currentFile(): File {
        if (curFile == null) {
            val index = keyValueStorage.getInt(fileIndexKey, 0)
            curFile = File(directory, "$writeKey-$index$TMP_SUFFIX")
        }
        return curFile!!
    }

    /**
     * Writes the given content to the specified file, appending to the existing content if the file is already open.
     *
     * @param content The content to write.
     * @param file The file to write to.
     */
    private fun writeToFile(content: ByteArray, file: File) {
        if (os == null) {
            os = FileOutputStream(file, true)
        }
        os?.apply {
            write(content)
            flush()
        }
    }

    /**
     * Resets the state of the file output stream and the current file reference.
     */
    private fun reset() {
        os = null
        curFile = null
    }

    /**
     * Closes the current file output stream and resets the state of the file output stream and the current file reference.
     */
    fun closeAndReset() {
        os?.close()
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

    /**
     * Creates the specified directory if it does not already exist.
     *
     * @param directory The directory to create.
     */
    private fun createDirectory(directory: File) {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }
}

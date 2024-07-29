package com.rudderstack.core.internals.storage

import com.rudderstack.core.internals.utils.DateTimeInstant
import com.rudderstack.core.internals.utils.toFileDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Semaphore

private const val FILE_INDEX = "rudder.events.file.index."

class EventsFileManager(
    private val directory: File,
    private val writeKey: String,
    private val keyValueStorage: KeyValueStorage
) {

    init {
        createDirectory(directory)
        registerShutdownHook()
    }

    private val fileIndexKey = writeKey.toFileDirectory(FILE_INDEX)
    private var os: FileOutputStream? = null
    private var curFile: File? = null
    private val semaphore = Semaphore(1)

    suspend fun storeEvent(event: String) = withLock {
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

        val contents = if (newFile) event else ",$event"
        writeToFile(contents.toByteArray(), file)
    }

    fun read(): List<String> {
        val files = directory.listFiles { _, name ->
            name.contains(writeKey) && !name.endsWith(".tmp")
        } ?: emptyArray()
        return files.map { it.absolutePath }
    }

    fun remove(filePath: String): Boolean {
        return File(filePath).delete()
    }

    suspend fun rollover() = withLock {
        finish()
    }

    private fun incrementFileIndex() {
        val index = keyValueStorage.getInt(fileIndexKey, 0)
        keyValueStorage.save(fileIndexKey, index + 1)
    }

    private fun start(file: File) {
        val contents = """{"batch":["""
        writeToFile(contents.toByteArray(), file)
    }

    private fun finish() {
        val file = currentFile()
        if (!file.exists()) return

        val contents = """],"sentAt":"${DateTimeInstant.now()}","writeKey":"$writeKey"}"""
        writeToFile(contents.toByteArray(), file)
        file.renameTo(File(directory, file.nameWithoutExtension))
        os?.close()
        incrementFileIndex()
        reset()
    }

    private fun currentFile(): File {
        if (curFile == null) {
            val index = keyValueStorage.getInt(fileIndexKey, 0)
            curFile = File(directory, "$writeKey-$index.tmp")
        }
        return curFile!!
    }

    private fun writeToFile(content: ByteArray, file: File) {
        if (os == null) {
            os = FileOutputStream(file, true)
        }
        os?.apply {
            write(content)
            flush()
        }
    }

    private fun reset() {
        os = null
        curFile = null
    }

    private fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            os?.close()
        })
    }

    private suspend fun withLock(block: () -> Unit) {
        withContext(Dispatchers.IO) {
            semaphore.acquire()
        }
        try {
            block()
        } finally {
            semaphore.release()
        }
    }

    private fun createDirectory(directory: File) {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }
}

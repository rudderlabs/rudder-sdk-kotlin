package com.rudderstack.kotlin.internals.storage

import com.rudderstack.kotlin.internals.storage.exception.PayloadTooLargeException
import com.rudderstack.kotlin.internals.utils.toFileDirectory
import source.version.VersionConstants
import java.io.File

/**
 * The directory where the message files are stored.
 * */
const val FILE_DIRECTORY = "/tmp/rudderstack-analytics-kotlin/"
private const val FILE_NAME = "messages"

/**
 * Implementation of the [Storage] interface that provides a basic file-based storage mechanism.
 *
 * This class handles storing, retrieving, and managing key-value pairs in files. It supports various data types for storage
 * and manages message files separately from properties files.
 *
 * @property writeKey The key used to create a unique storage directory.
 */
@Suppress("Detekt.TooManyFunctions")
internal class BasicStorage(writeKey: String) : Storage {

    /**
     * The directory where the storage files are kept, determined by the provided [writeKey].
     */
    private val storageDirectory = File(writeKey.toFileDirectory(FILE_DIRECTORY))

    /**
     * The subdirectory within [storageDirectory] where message files are stored.
     */
    private val messageStorageDirectory = File(storageDirectory, FILE_NAME)

    /**
     * Manages properties files, including loading and saving properties.
     */
    private val propertiesFile = PropertiesFile(storageDirectory, writeKey)

    /**
     * Manages message batch files, including storing and reading messages.
     */
    private val messagesFile = MessageBatchFileManager(messageStorageDirectory, writeKey, propertiesFile)

    init {
        // Load properties from the properties file during initialization.
        propertiesFile.load()
    }

    override suspend fun write(key: StorageKeys, value: Boolean) {
        if (key != StorageKeys.MESSAGE) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: String) {
        if (key == StorageKeys.MESSAGE) {
            if (value.length < MAX_PAYLOAD_SIZE) {
                messagesFile.storeMessage(value)
            } else {
                throw PayloadTooLargeException()
            }
        } else {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Int) {
        if (key != StorageKeys.MESSAGE) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Long) {
        if (key != StorageKeys.MESSAGE) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun remove(key: StorageKeys) {
        propertiesFile.clear(key.key)
    }

    override fun remove(filePath: String) {
        messagesFile.remove(filePath)
    }

    override suspend fun rollover() {
        messagesFile.rollover()
    }

    override fun readInt(key: StorageKeys, defaultVal: Int): Int {
        return propertiesFile.getInt(key.key, defaultVal)
    }

    override fun readBoolean(key: StorageKeys, defaultVal: Boolean): Boolean {
        return propertiesFile.getBoolean(key.key, defaultVal)
    }

    override fun readLong(key: StorageKeys, defaultVal: Long): Long {
        return propertiesFile.getLong(key.key, defaultVal)
    }

    override fun readString(key: StorageKeys, defaultVal: String): String {
        return if (key == StorageKeys.MESSAGE) {
            messagesFile.read().joinToString()
        } else {
            propertiesFile.getString(key.key, defaultVal)
        }
    }

    override fun readFileList(): List<String> {
        return messagesFile.read()
    }

    override fun getLibraryVersion(): LibraryVersion {
        return object : LibraryVersion {
            override fun getVersionName(): String = VersionConstants.VERSION_NAME
        }
    }
}

/**
 * Implementation of [StorageProvider] that provides instances of [BasicStorage].
 */
object BasicStorageProvider : StorageProvider {

    /**
     * Provides an instance of [BasicStorage] for the given write key and application.
     *
     * @param writeKey The key used to create a unique storage instance.
     * @param application The application object, which may be used for additional context or configuration.
     * @return A new instance of [BasicStorage] configured with the provided write key.
     */
    override fun getStorage(writeKey: String, application: Any): Storage {
        return BasicStorage(writeKey = writeKey)
    }
}

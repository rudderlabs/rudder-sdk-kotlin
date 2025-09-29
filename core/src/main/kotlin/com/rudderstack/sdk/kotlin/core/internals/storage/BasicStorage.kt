package com.rudderstack.sdk.kotlin.core.internals.storage

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.storage.exception.PayloadTooLargeException
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.appendWriteKey
import source.version.VersionConstants
import java.io.File

/**
 * The directory where the event files are stored.
 * */
internal const val FILE_DIRECTORY = "/tmp/rudderstack-analytics-kotlin"
private const val FILE_NAME = "events"

/**
 * Implementation of the [Storage] interface that provides a basic file-based storage mechanism.
 *
 * This class handles storing, retrieving, and managing key-value pairs in files. It supports various data types for storage
 * and manages event files separately from properties files.
 *
 * @param writeKey The key used to create a unique storage directory.
 */
@Suppress("Detekt.TooManyFunctions")
internal class BasicStorage(writeKey: String) : Storage {

    /**
     * The directory where the storage files are kept, determined by the provided `writeKey`.
     */
    private val storageDirectory = File(FILE_DIRECTORY.appendWriteKey(writeKey))

    /**
     * The subdirectory within [storageDirectory] where event files are stored.
     */
    private val eventStorageDirectory = File(storageDirectory, FILE_NAME)

    /**
     * Manages properties files, including loading and saving properties.
     */
    private val propertiesFile = PropertiesFile(storageDirectory, writeKey)

    /**
     * Manages event batch files, including storing and reading events.
     */
    private val eventsFile = EventBatchFileManager(
        eventStorageDirectory,
        writeKey,
        propertiesFile
    )

    init {
        // Load properties from the properties file during initialization.
        propertiesFile.load()
    }

    override suspend fun write(key: StorageKeys, value: Boolean) {
        if (key != StorageKeys.EVENT) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: String) {
        if (key == StorageKeys.EVENT) {
            if (value.length < MAX_PAYLOAD_SIZE) {
                eventsFile.storeEvent(value)
            } else {
                throw PayloadTooLargeException()
            }
        } else {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Int) {
        if (key != StorageKeys.EVENT) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Long) {
        if (key != StorageKeys.EVENT) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun remove(key: StorageKeys) {
        propertiesFile.clear(key.key)
    }

    override fun remove(filePath: String) {
        eventsFile.remove(filePath)
    }

    override suspend fun rollover() {
        eventsFile.rollover()
    }

    override fun close() {
        eventsFile.closeAndReset()
        LoggerAnalytics.info("Storage closed")
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
        return if (key == StorageKeys.EVENT) {
            eventsFile.read().joinToString()
        } else {
            propertiesFile.getString(key.key, defaultVal)
        }
    }

    override fun readFileList(): List<String> {
        return eventsFile.read()
    }

    override fun getLibraryVersion(): LibraryVersion {
        return object : LibraryVersion {
            override fun getLibraryName(): String = VersionConstants.LIBRARY_NAME

            override fun getVersionName(): String = VersionConstants.VERSION_NAME
        }
    }

    @UseWithCaution
    override fun delete() {
        propertiesFile.delete()
        storageDirectory.deleteRecursively().let { isDeleted ->
            LoggerAnalytics.info("Storage directory deleted: $isDeleted")
        }
    }
}

/**
 * Provides an instance of [BasicStorage] with the given [writeKey].
 *
 * @param writeKey The key used to create a unique storage directory.
 * @return An instance of [BasicStorage] with the provided [writeKey].
 */
internal fun provideBasicStorage(writeKey: String): Storage {
    return BasicStorage(writeKey = writeKey)
}

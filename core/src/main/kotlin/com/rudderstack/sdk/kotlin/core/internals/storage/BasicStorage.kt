package com.rudderstack.sdk.kotlin.core.internals.storage

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
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
 * @param platformType The platform type used for event file ordering behaviour.
 * @param storageDirectory The directory where the storage files are kept, determined by the provided `writeKey`.
 * @param eventStorageDirectory The subdirectory within [storageDirectory] where event files are stored.
 * @param propertiesFile The key-value storage implementation.
 * @param eventsFile The event batch file manager.
 */
@Suppress("Detekt.TooManyFunctions")
internal class BasicStorage(
    writeKey: String,
    platformType: PlatformType,
    private val logger: Logger,
    private val storageDirectory: File = File(FILE_DIRECTORY.appendWriteKey(writeKey)),
    eventStorageDirectory: File = File(storageDirectory, FILE_NAME),
    private val propertiesFile: KeyValueStorage = PropertiesFile(storageDirectory, writeKey, logger)
        .also {
            // Load properties from the properties file
            it.load()
        },
    private val eventsFile: EventBatchFileManager = EventBatchFileManager(
        directory = eventStorageDirectory,
        writeKey = writeKey,
        keyValueStorage = propertiesFile,
        platformType = platformType,
        logger = logger,
    ),
) : Storage {

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
                logger.warn("BasicStorage: Event payload exceeds MAX_PAYLOAD_SIZE, dropping event")
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
        logger.debug("BasicStorage: Storage closed")
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

    override fun readBatchContent(batchRef: String): String? {
        return eventsFile.readContent(batchRef)
    }

    override fun getBatchId(batchRef: String): Int = File(batchRef).name.toIntOrNull() ?: 0

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
            logger.debug("BasicStorage: Storage directory deleted: $isDeleted")
        }
    }
}

/**
 * Provides an instance of [BasicStorage] with the given [writeKey] and [platformType].
 *
 * @param writeKey The key used to create a unique storage directory.
 * @param platformType The platform type used for event file ordering behaviour.
 * @return An instance of [BasicStorage] with the provided [writeKey] and [platformType].
 */
internal fun provideBasicStorage(writeKey: String, platformType: PlatformType, logger: Logger): Storage {
    return BasicStorage(writeKey = writeKey, platformType = platformType, logger = logger)
}

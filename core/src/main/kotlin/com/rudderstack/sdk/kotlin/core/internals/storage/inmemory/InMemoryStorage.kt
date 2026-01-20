package com.rudderstack.sdk.kotlin.core.internals.storage.inmemory

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage
import com.rudderstack.sdk.kotlin.core.internals.storage.LibraryVersion
import com.rudderstack.sdk.kotlin.core.internals.storage.MAX_PAYLOAD_SIZE
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.storage.exception.PayloadTooLargeException
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import source.version.VersionConstants

/**
 * Implementation of the [com.rudderstack.sdk.kotlin.core.internals.storage.Storage] interface that provides an in-memory storage mechanism.
 *
 * This class handles storing, retrieving, and managing key-value pairs and event batches
 * entirely in memory without any file system interactions. All data is ephemeral and will
 * be lost when the process terminates.
 *
 * This storage is ideal for server-side SDK deployments where persistence is not required.
 *
 * @param writeKey The key used to identify this storage instance.
 * @param prefsStore The key-value storage for non-event data.
 */
@Suppress("Detekt.TooManyFunctions")
@InternalRudderApi
internal class InMemoryStorage(
    writeKey: String,
    private val prefsStore: KeyValueStorage = InMemoryPrefsStore()
) : Storage {

    private val eventBatchFile = InMemoryBatchManager(writeKey, prefsStore)

    override suspend fun write(key: StorageKeys, value: Boolean) {
        if (key != StorageKeys.EVENT) {
            prefsStore.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: String) {
        if (key == StorageKeys.EVENT) {
            if (value.length < MAX_PAYLOAD_SIZE) {
                eventBatchFile.storeEvent(value)
            } else {
                throw PayloadTooLargeException()
            }
        } else {
            prefsStore.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Int) {
        if (key != StorageKeys.EVENT) {
            prefsStore.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Long) {
        if (key != StorageKeys.EVENT) {
            prefsStore.save(key.key, value)
        }
    }

    override suspend fun remove(key: StorageKeys) {
        prefsStore.clear(key.key)
    }

    override fun remove(filePath: String) {
        eventBatchFile.remove(filePath)
    }

    override suspend fun rollover() {
        eventBatchFile.rollover()
    }

    override fun close() {
        eventBatchFile.closeAndReset()
        LoggerAnalytics.info("InMemoryStorage closed")
    }

    override fun readInt(key: StorageKeys, defaultVal: Int): Int {
        return prefsStore.getInt(key.key, defaultVal)
    }

    override fun readBoolean(key: StorageKeys, defaultVal: Boolean): Boolean {
        return prefsStore.getBoolean(key.key, defaultVal)
    }

    override fun readLong(key: StorageKeys, defaultVal: Long): Long {
        return prefsStore.getLong(key.key, defaultVal)
    }

    override fun readString(key: StorageKeys, defaultVal: String): String {
        return if (key == StorageKeys.EVENT) {
            eventBatchFile.read().joinToString()
        } else {
            prefsStore.getString(key.key, defaultVal)
        }
    }

    override fun readFileList(): List<String> {
        return eventBatchFile.read()
    }

    override fun readBatchContent(batchRef: String): String? {
        return eventBatchFile.readContent(batchRef)
    }

    override fun getLibraryVersion(): LibraryVersion {
        return object : LibraryVersion {
            override fun getLibraryName(): String = VersionConstants.LIBRARY_NAME

            override fun getVersionName(): String = VersionConstants.VERSION_NAME
        }
    }

    @UseWithCaution
    override fun delete() {
        prefsStore.delete()
        eventBatchFile.delete()
        LoggerAnalytics.info("InMemoryStorage deleted")
    }
}

/**
 * Provides an instance of [InMemoryStorage] with the given [writeKey].
 *
 * @param writeKey The key used to identify the storage instance.
 * @return An instance of [InMemoryStorage] with the provided [writeKey].
 */
internal fun provideInMemoryStorage(writeKey: String): Storage {
    return InMemoryStorage(writeKey = writeKey)
}

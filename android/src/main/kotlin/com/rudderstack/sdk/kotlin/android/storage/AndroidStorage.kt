package com.rudderstack.sdk.kotlin.android.storage

import android.content.Context
import com.rudderstack.sdk.kotlin.BuildConfig
import com.rudderstack.sdk.kotlin.android.storage.exceptions.QueuedPayloadTooLargeException
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.storage.EventBatchFileManager
import com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage
import com.rudderstack.sdk.kotlin.core.internals.storage.LibraryVersion
import com.rudderstack.sdk.kotlin.core.internals.storage.MAX_PAYLOAD_SIZE
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.appendWriteKey
import com.rudderstack.sdk.kotlin.core.internals.utils.toAndroidPrefsKey
import java.io.File

private const val RUDDER_PREFS = "rl_prefs"
private const val DIRECTORY_NAME = "rudder-android-store"

internal class AndroidStorage(
    private val context: Context,
    private val writeKey: String,
    private val rudderPrefsRepo: KeyValueStorage = SharedPrefsStore(context, RUDDER_PREFS.toAndroidPrefsKey(writeKey))
) : Storage {

    private val storageDirectory: File =
        context.getDir(DIRECTORY_NAME.appendWriteKey(writeKey), Context.MODE_PRIVATE)
    private val eventBatchFile = EventBatchFileManager(storageDirectory, writeKey, rudderPrefsRepo)

    override suspend fun write(key: StorageKeys, value: Boolean) {
        if (key != StorageKeys.EVENT) {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: String) {
        if (key == StorageKeys.EVENT) {
            if (value.length < MAX_PAYLOAD_SIZE) {
                eventBatchFile.storeEvent(value)
            } else {
                throw QueuedPayloadTooLargeException("queued payload is too large")
            }
        } else {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Int) {
        if (key != StorageKeys.EVENT) {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Long) {
        if (key != StorageKeys.EVENT) {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun remove(key: StorageKeys) {
        rudderPrefsRepo.clear(key.key)
    }

    override fun remove(filePath: String) {
        eventBatchFile.remove(filePath)
    }

    override suspend fun rollover() {
        eventBatchFile.rollover()
    }

    override fun close() {
        eventBatchFile.closeAndReset()
    }

    override fun readInt(key: StorageKeys, defaultVal: Int): Int {
        return rudderPrefsRepo.getInt(key.key, defaultVal)
    }

    override fun readBoolean(key: StorageKeys, defaultVal: Boolean): Boolean {
        return rudderPrefsRepo.getBoolean(key.key, defaultVal)
    }

    override fun readLong(key: StorageKeys, defaultVal: Long): Long {
        return rudderPrefsRepo.getLong(key.key, defaultVal)
    }

    override fun readString(key: StorageKeys, defaultVal: String): String {
        return if (key == StorageKeys.EVENT) {
            eventBatchFile.read().joinToString()
        } else {
            rudderPrefsRepo.getString(key.key, defaultVal)
        }
    }

    override fun readFileList(): List<String> {
        return eventBatchFile.read()
    }

    override fun getLibraryVersion(): LibraryVersion {
        return object : LibraryVersion {
            override fun getPackageName(): String = BuildConfig.LIBRARY_PACKAGE_NAME

            override fun getVersionName(): String = BuildConfig.VERSION_NAME

            override fun getBuildVersion(): String = android.os.Build.VERSION.SDK_INT.toString()
        }
    }

    @UseWithCaution
    override fun delete() {
        rudderPrefsRepo.delete()
        storageDirectory.deleteRecursively().let { isDeleted ->
            LoggerAnalytics.info("Storage directory deleted: $isDeleted")
        }
    }
}

/**
 * Provides an instance of [AndroidStorage] for use in the SDK.
 *
 *  @param writeKey The write key used to identify the storage location.
 *  @param application The application context.
 *  @return An instance of [AndroidStorage].
 */
internal fun provideAndroidStorage(writeKey: String, application: Context): Storage {
    return AndroidStorage(
        context = application,
        writeKey = writeKey,
    )
}

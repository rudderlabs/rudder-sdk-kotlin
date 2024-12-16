package com.rudderstack.sdk.kotlin.android.storage

import android.content.Context
import com.rudderstack.sdk.kotlin.BuildConfig
import com.rudderstack.sdk.kotlin.android.storage.exceptions.QueuedPayloadTooLargeException
import com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage
import com.rudderstack.sdk.kotlin.core.internals.storage.LibraryVersion
import com.rudderstack.sdk.kotlin.core.internals.storage.MAX_PAYLOAD_SIZE
import com.rudderstack.sdk.kotlin.core.internals.storage.MessageBatchFileManager
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.toAndroidPrefsKey
import java.io.File

private const val RUDDER_PREFS = "rl_prefs"
internal const val DIRECTORY_NAME = "rudder-android-store"

internal class AndroidStorage(
    private val context: Context,
    private val writeKey: String,
    private val rudderPrefsRepo: KeyValueStorage = SharedPrefsStore(context, RUDDER_PREFS.toAndroidPrefsKey(writeKey))
) : Storage {

    private val storageDirectory: File = context.getDir(DIRECTORY_NAME, Context.MODE_PRIVATE)
    private val messageBatchFile = MessageBatchFileManager(storageDirectory, writeKey, rudderPrefsRepo)

    override suspend fun write(key: StorageKeys, value: Boolean) {
        if (key != StorageKeys.MESSAGE) {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: String) {
        if (key == StorageKeys.MESSAGE) {
            if (value.length < MAX_PAYLOAD_SIZE) {
                messageBatchFile.storeMessage(value)
            } else {
                throw QueuedPayloadTooLargeException("queued payload is too large")
            }
        } else {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Int) {
        if (key != StorageKeys.MESSAGE) {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Long) {
        if (key != StorageKeys.MESSAGE) {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun remove(key: StorageKeys) {
        rudderPrefsRepo.clear(key.key)
    }

    override fun remove(filePath: String) {
        messageBatchFile.remove(filePath)
    }

    override suspend fun rollover() {
        messageBatchFile.rollover()
    }

    override fun close() {
        messageBatchFile.closeAndReset()
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
        return if (key == StorageKeys.MESSAGE) {
            messageBatchFile.read().joinToString()
        } else {
            rudderPrefsRepo.getString(key.key, defaultVal)
        }
    }

    override fun readFileList(): List<String> {
        return messageBatchFile.read()
    }

    override fun getLibraryVersion(): LibraryVersion {
        return object : LibraryVersion {
            override fun getPackageName(): String = BuildConfig.LIBRARY_PACKAGE_NAME

            override fun getVersionName(): String = BuildConfig.VERSION_NAME

            override fun getBuildVersion(): String = android.os.Build.VERSION.SDK_INT.toString()
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
fun provideAndroidStorage(writeKey: String, application: Context): Storage {
    return AndroidStorage(
        context = application,
        writeKey = writeKey,
    )
}

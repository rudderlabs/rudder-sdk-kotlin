package com.rudderstack.android.storage

import android.content.Context
import com.rudderstack.android.storage.exceptions.QueuedPayloadTooLargeException
import com.rudderstack.android2.BuildConfig
import com.rudderstack.core.internals.storage.KeyValueStorage
import com.rudderstack.core.internals.storage.LibraryVersion
import com.rudderstack.core.internals.storage.MAX_PAYLOAD_SIZE
import com.rudderstack.core.internals.storage.MessageBatchFileManager
import com.rudderstack.core.internals.storage.Storage
import com.rudderstack.core.internals.storage.StorageKeys
import com.rudderstack.core.internals.storage.StorageProvider
import com.rudderstack.core.internals.utils.toAndroidPrefsKey
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
            override fun getVersionName(): String = BuildConfig.VERSION_NAME

            override fun getBuildVersion(): String = android.os.Build.VERSION.SDK_INT.toString()

            override fun getVersionCode(): String = BuildConfig.VERSION_CODE
        }
    }
}

/**
 * `AndroidStorageProvider` is an object that implements the `StorageProvider` interface for providing
 * storage solutions on Android devices.
 *
 * This object is responsible for creating instances of `Storage` specifically tailored for the Android platform.
 * It provides a method to obtain a storage instance using a provided write key and Android application context.
 *
 * ## Description
 * `AndroidStorageProvider` acts as a factory for creating `Storage` instances that manage data storage on Android devices.
 * It utilizes the Android-specific `Context` to initialize the storage solution, allowing the RudderStack SDK to store and
 * retrieve data on the device efficiently.
 *
 * The object implements the `StorageProvider` interface, ensuring compatibility with the RudderStack core library, which relies on
 * standardized storage operations across different platforms.
 *
 * ## Method
 * - `getStorage(writeKey: String, application: Any): Storage`:
 *   - Creates and returns an instance of `AndroidStorage` by casting the provided `application` parameter to an Android `Context`.
 *   - The `writeKey` is used to uniquely identify the storage instance for a particular RudderStack setup.
 *   - **Parameters**:
 *     - `writeKey`: A `String` representing the write key used to identify the RudderStack workspace.
 *     - `application`: An `Any` type that is expected to be an Android `Context`. This is the context of the Android application.
 *   - **Returns**: An instance of `Storage` tailored for the Android platform.
 *
 * ## Example Usage
 * ```kotlin
 * val storageProvider: StorageProvider = AndroidStorageProvider
 * val storage: Storage = storageProvider.getStorage("your_write_key", applicationContext)
 * ```
 *
 * This `storage` instance can then be used by the RudderStack SDK to persist data on an Android device.
 *
 * @see StorageProvider
 * @see AndroidStorage
 */
object AndroidStorageProvider : StorageProvider {

    override fun getStorage(writeKey: String, application: Any): Storage {
        return AndroidStorage(
            context = application as Context,
            writeKey = writeKey,
        )
    }
}

package com.rudderstack.android.storage

import android.content.Context
import com.rudderstack.core.internals.storage.KeyValueStorage
import com.rudderstack.core.internals.storage.MAX_EVENT_SIZE
import com.rudderstack.core.internals.storage.MessageBatchFileManager
import com.rudderstack.core.internals.storage.Storage
import com.rudderstack.core.internals.storage.StorageKeys
import com.rudderstack.core.internals.storage.StorageProvider
import com.rudderstack.core.internals.utils.toAndroidPrefsKey
import java.io.File

private const val RUDDER_PREFS = "rl_prefs"
internal const val DIRECTORY_NAME = "rudder-android-store"

class AndroidStorage(
    private val context: Context,
    private val writeKey: String,
    private val rudderPrefsRepo: KeyValueStorage = SharedPrefsStore(context, RUDDER_PREFS.toAndroidPrefsKey(writeKey))
) : Storage {

    private val storageDirectory: File = context.getDir(DIRECTORY_NAME, Context.MODE_PRIVATE)
    private val eventsFile = MessageBatchFileManager(storageDirectory, writeKey, rudderPrefsRepo)

    override suspend fun write(key: StorageKeys, value: Boolean) {
        if (key != StorageKeys.RUDDER_EVENT) {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: String) {
        if (key == StorageKeys.RUDDER_EVENT) {
            if (value.length < MAX_EVENT_SIZE) {
                eventsFile.storeEvent(value)
            } else {
                throw Exception("queued payload is too large")
            }
        } else {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Int) {
        if (key != StorageKeys.RUDDER_EVENT) {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Long) {
        if (key != StorageKeys.RUDDER_EVENT) {
            rudderPrefsRepo.save(key.key, value)
        }
    }

    override suspend fun remove(key: StorageKeys) {
        rudderPrefsRepo.clear(key.key)
    }

    override fun remove(filePath: String) {
        eventsFile.remove(filePath)
    }

    override suspend fun rollover() {
        eventsFile.rollover()
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
        return if (key == StorageKeys.RUDDER_EVENT) {
            eventsFile.read().joinToString()
        } else {
            rudderPrefsRepo.getString(key.key, defaultVal)
        }
    }

    override fun readEventsContent(): List<String> {
        return eventsFile.read()
    }
}

object AndroidStorageProvider : StorageProvider {

    override fun getStorage(writeKey: String, application: Any): Storage {
        return AndroidStorage(
            context = application as Context,
            writeKey = writeKey,
        )
    }
}

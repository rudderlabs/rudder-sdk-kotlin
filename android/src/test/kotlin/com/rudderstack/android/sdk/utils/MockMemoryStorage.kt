package com.rudderstack.android.sdk.utils

import com.rudderstack.sdk.kotlin.core.internals.storage.LibraryVersion
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys

internal class MockMemoryStorage : Storage {

    private val propertiesMap: MutableMap<String, Any> = mutableMapOf()
    private val messageBatchMap: MutableList<String> = mutableListOf()

    override suspend fun write(key: StorageKeys, value: Boolean) {
        if (key != StorageKeys.MESSAGE) {
            propertiesMap[key.key] = value
        }
    }

    override suspend fun write(key: StorageKeys, value: Int) {
        if (key != StorageKeys.MESSAGE) {
            propertiesMap[key.key] = value
        }
    }

    override suspend fun write(key: StorageKeys, value: Long) {
        if (key != StorageKeys.MESSAGE) {
            propertiesMap[key.key] = value
        }
    }

    override suspend fun write(key: StorageKeys, value: String) {
        if (key == StorageKeys.MESSAGE) {
            messageBatchMap.add(value)
        } else {
            propertiesMap[key.key] = value
        }
    }

    override suspend fun remove(key: StorageKeys) {
        propertiesMap.remove(key.key)
    }

    override fun remove(filePath: String) {
        messageBatchMap.remove(filePath)
    }

    override suspend fun rollover() {
        messageBatchMap.clear()
    }

    override fun close() {
        messageBatchMap.clear()
    }

    override fun readInt(key: StorageKeys, defaultVal: Int): Int {
        return (propertiesMap[key.key] as? Int) ?: defaultVal
    }

    override fun readBoolean(key: StorageKeys, defaultVal: Boolean): Boolean {
        return (propertiesMap[key.key] as? Boolean) ?: defaultVal
    }

    override fun readLong(key: StorageKeys, defaultVal: Long): Long {
        return (propertiesMap[key.key] as? Long) ?: defaultVal
    }

    override fun readString(key: StorageKeys, defaultVal: String): String {
        return if (key == StorageKeys.MESSAGE) {
            messageBatchMap.joinToString()
        } else {
            (propertiesMap[key.key] as? String) ?: defaultVal
        }
    }

    override fun readFileList(): List<String> {
        return messageBatchMap
    }

    override fun getLibraryVersion(): LibraryVersion {
        return object : LibraryVersion {
            override fun getPackageName(): String = "com.rudderstack.android.sdk"

            override fun getVersionName() = "1.0.0"
        }
    }
}

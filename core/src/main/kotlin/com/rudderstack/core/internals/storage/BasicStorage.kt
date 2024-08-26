package com.rudderstack.core.internals.storage

import com.rudderstack.core.internals.utils.toFileDirectory
import java.io.File

private const val FILE_DIRECTORY = "/tmp/rudderstack-analytics-kotlin/"
private const val FILE_NAME = "messages"

class BasicStorage(writeKey: String) : Storage {

    private val storageDirectory = File(writeKey.toFileDirectory(FILE_DIRECTORY))
    private val storageDirectoryEvents = File(storageDirectory, FILE_NAME)

    private val propertiesFile = PropertiesFile(storageDirectory, writeKey)
    private val eventsFile = MessageBatchFileManager(storageDirectoryEvents, writeKey, propertiesFile)

    init {
        propertiesFile.load()
    }

    override suspend fun write(key: StorageKeys, value: Boolean) {
        if (key != StorageKeys.RUDDER_EVENT) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: String) {
        if (key == StorageKeys.RUDDER_EVENT) {
            if (value.length < MAX_EVENT_SIZE) {
                eventsFile.storeEvent(value)
            } else {
                throw Exception("enqueued payload is too large")
            }
        } else {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Int) {
        if (key != StorageKeys.RUDDER_EVENT) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Long) {
        if (key != StorageKeys.RUDDER_EVENT) {
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
        return if (key == StorageKeys.RUDDER_EVENT) {
            eventsFile.read().joinToString()
        } else {
            propertiesFile.getString(key.key, defaultVal)
        }
    }
}

object BasicStorageProvider : StorageProvider {

    override fun getStorage(writeKey: String, application: Any): Storage {
        return BasicStorage(writeKey = writeKey)
    }
}
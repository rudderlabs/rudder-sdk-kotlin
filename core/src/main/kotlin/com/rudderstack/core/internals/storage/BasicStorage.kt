package com.rudderstack.core.internals.storage

import com.rudderstack.core.internals.utils.toFileDirectory
import java.io.File

const val FILE_DIRECTORY = "/tmp/rudderstack-analytics-kotlin/"
private const val FILE_NAME = "messages"

class BasicStorage(writeKey: String) : Storage {

    private val storageDirectory = File(writeKey.toFileDirectory(FILE_DIRECTORY))
    private val messageStorageDirectory = File(storageDirectory, FILE_NAME)

    private val propertiesFile = PropertiesFile(storageDirectory, writeKey)
    private val messagesFile = MessageBatchFileManager(messageStorageDirectory, writeKey, propertiesFile)

    init {
        propertiesFile.load()
    }

    override suspend fun write(key: StorageKeys, value: Boolean) {
        if (key != StorageKeys.RUDDER_MESSAGE) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: String) {
        if (key == StorageKeys.RUDDER_MESSAGE) {
            if (value.length < MAX_PAYLOAD_SIZE) {
                messagesFile.storeMessage(value)
            } else {
                throw Exception("enqueued payload is too large")
            }
        } else {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Int) {
        if (key != StorageKeys.RUDDER_MESSAGE) {
            propertiesFile.save(key.key, value)
        }
    }

    override suspend fun write(key: StorageKeys, value: Long) {
        if (key != StorageKeys.RUDDER_MESSAGE) {
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
        return if (key == StorageKeys.RUDDER_MESSAGE) {
            messagesFile.read().joinToString()
        } else {
            propertiesFile.getString(key.key, defaultVal)
        }
    }

    override fun readFileList(): List<String> {
        return messagesFile.read()
    }
}

object BasicStorageProvider : StorageProvider {

    override fun getStorage(writeKey: String, application: Any): Storage {
        return BasicStorage(writeKey = writeKey)
    }
}
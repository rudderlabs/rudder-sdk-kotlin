package com.rudderstack.core.internals.storage

const val MAX_PAYLOAD_SIZE = 32 * 1024 // 32 KB
const val MAX_BATCH_SIZE = 500 * 1024 // 500 KB

interface Storage {

    suspend fun write(key: StorageKeys, value: Boolean)
    suspend fun write(key: StorageKeys, value: Int)
    suspend fun write(key: StorageKeys, value: Long)
    suspend fun write(key: StorageKeys, value: String)
    suspend fun remove(key: StorageKeys)
    suspend fun rollover() {}
    fun readInt(key: StorageKeys, defaultVal: Int): Int
    fun readBoolean(key: StorageKeys, defaultVal: Boolean): Boolean
    fun readLong(key: StorageKeys, defaultVal: Long): Long
    fun readString(key: StorageKeys, defaultVal: String): String
    fun remove(filePath: String) {}
    fun readFileList(): List<String>
    fun getLibraryVersion(): LibraryVersion
}

enum class StorageKeys(val key: String) {
    RUDDER_MESSAGE("rl_message"),
    RUDDER_OPTIONAL("")
}

interface LibraryVersion {

    fun getPlatform(): String
    fun getVersionName(): String
    fun getVersionCode(): String

    fun toMap(): Map<String, String> {
        // This is needed to ensure that URL encoding is done properly
        if (getPlatform().isEmpty() || getVersionName().isEmpty() || getVersionCode().isEmpty()) {
            return emptyMap()
        }
        return mapOf(
            "p" to getPlatform(),
            "v" to getVersionName(),
            "bv" to getVersionCode()
        )
    }
}

interface StorageProvider {

    fun getStorage(writeKey: String, application: Any): Storage
}

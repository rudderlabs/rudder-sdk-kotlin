package com.rudderstack.core.internals.storage

const val MAX_EVENT_SIZE = 32 * 1024 // 32 KB
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
}

enum class StorageKeys(val key: String) {
    RUDDER_TRAITS_KEY("rl_traits"),
    RUDDER_TRACK_AUTO_SESSION_KEY("rl_track_auto_session_key"),
    RUDDER_EXTERNAL_ID_KEY("rl_external_id"),
    RUDDER_OPT_STATUS_KEY("rl_opt_status"),
    RUDDER_ANONYMOUS_ID_KEY("rl_anonymous_id_key"),
    RUDDER_USER_ID_KEY("rl_user_id_key"),
    RUDDER_SESSION_ID_KEY("rl_session_id_key"),
    RUDDER_SESSION_LAST_ACTIVE_TIMESTAMP_KEY("rl_last_event_timestamp_key"),
    RUDDER_ADVERTISING_ID_KEY("rl_advertising_id_key"),
    RUDDER_APPLICATION_VERSION_KEY("rl_application_version_key"),
    RUDDER_APPLICATION_BUILD_KEY("rl_application_build_key"),
    RUDDER_EVENT("rl_event"),
}

interface StorageProvider {

    fun getStorage(writeKey: String, application: Any): Storage
}

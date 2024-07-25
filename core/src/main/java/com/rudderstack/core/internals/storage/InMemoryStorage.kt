package com.rudderstack.core.internals.storage

import com.rudderstack.core.internals.models.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class InMemoryStorage : Storage {

    private val messageList: MutableList<Message> = mutableListOf()
    private val messageLock = Any()
    private val valuesMap = ConcurrentHashMap<String, Any>()

    fun writeEvent(event: Message) {
        synchronized(messageLock) {
            messageList.add(event)
        }
    }

    override suspend fun write(storageKey: StorageKeys, value: Boolean) {
        valuesMap[storageKey.key] = value
    }

    override suspend fun write(storageKey: StorageKeys, value: Int) {
        valuesMap[storageKey.key] = value
    }

    override suspend fun write(storageKey: StorageKeys, value: Long) {
        valuesMap[storageKey.key] = value
    }

    override suspend fun write(storageKey: StorageKeys, value: String) {
        valuesMap[storageKey.key] = value
    }

    override suspend fun remove(storageKey: StorageKeys) {
        valuesMap.remove(storageKey.key)
    }

    override fun readInt(storageKey: StorageKeys, defaultVal: Int): Int {
        return valuesMap[storageKey.key] as Int
    }

    override fun readBoolean(storageKey: StorageKeys, defaultVal: Boolean): Boolean {
        return valuesMap[storageKey.key] as Boolean
    }

    override fun readLong(storageKey: StorageKeys, defaultVal: Long): Long {
        return valuesMap[storageKey.key] as Long
    }

    override fun readString(storageKey: StorageKeys, defaultVal: String): String {
        return valuesMap[storageKey.key] as String
    }

    fun readEventsContent(): List<Any> {
        val eventsToSend: List<Message>
        synchronized(messageLock) {
            eventsToSend = messageList.toList()
            messageList.clear()
        }
        return listOf(eventsToSend)
    }

    fun getEventsString(content: Any): String {
        return Json.encodeToString(content as List<*>)
    }

    fun removeEvents() {
        synchronized(messageLock) {
            messageList.clear()
        }
    }
}

class InMemoryStorageProvider : StorageProvider {

    override fun getStorage(writeKey: String, application: Any): Storage {
        return InMemoryStorage()
    }
}

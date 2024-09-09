package com.rudderstack.core.internals.storage

interface KeyValueStorage {
    fun getInt(key: String, defaultVal: Int): Int
    fun getBoolean(key: String, defaultVal: Boolean): Boolean
    fun getString(key: String, defaultVal: String): String
    fun getLong(key: String, defaultVal: Long): Long
    fun save(key: String, value: Int)
    fun save(key: String, value: Boolean)
    fun save(key: String, value: String)
    fun save(key: String, value: Long)
    fun clear(key: String)
}
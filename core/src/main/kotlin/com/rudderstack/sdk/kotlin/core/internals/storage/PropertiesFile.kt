package com.rudderstack.sdk.kotlin.core.internals.storage

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.utils.toPropertiesFileName
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

private const val PROPERTIES_PREFIX = "rudder"
private const val PROPERTIES_SUFFIX = ".properties"

internal class PropertiesFile(
    directory: File,
    writeKey: String,
) : KeyValueStorage {

    private var properties: Properties = Properties()
    private val propsFile = File(directory, writeKey.toPropertiesFileName(PROPERTIES_PREFIX, PROPERTIES_SUFFIX))

    /**
     * Loads properties from the file. If the file does not exist or fails to load, it creates a new file.
     */
    @Suppress("TooGenericExceptionCaught")
    internal fun load() {
        if (propsFile.exists()) {
            try {
                FileInputStream(propsFile).use {
                    properties.load(it)
                }
            } catch (e: Throwable) {
                propsFile.delete()
                LoggerAnalytics.error(
                    "Failed to load property file with path " +
                        "${propsFile.absolutePath}, error stacktrace: ${e.stackTraceToString()}"
                )
            }
        } else {
            propsFile.parentFile.mkdirs()
            propsFile.createNewFile()
        }
    }

    /**
     * Saves the current properties to the file.
     */
    private fun save() {
        FileOutputStream(propsFile).use {
            properties.store(it, null)
        }
    }

    /**
     * Retrieves an integer value from properties.
     * @param key The property key.
     * @param defaultVal The default value if the key does not exist or cannot be converted.
     * @return The integer value associated with the key.
     */
    override fun getInt(key: String, defaultVal: Int): Int {
        return properties.getProperty(key, defaultVal.toString()).toInt()
    }

    /**
     * Retrieves a boolean value from properties.
     * @param key The property key.
     * @param defaultVal The default value if the key does not exist or cannot be converted.
     * @return The boolean value associated with the key.
     */
    override fun getBoolean(key: String, defaultVal: Boolean): Boolean {
        return properties.getProperty(key, defaultVal.toString()).toBoolean()
    }

    override fun getString(key: String, defaultVal: String): String {
        return properties.getProperty(key, defaultVal).toString()
    }

    override fun getLong(key: String, defaultVal: Long): Long {
        return properties.getProperty(key, defaultVal.toString()).toLong()
    }

    override fun save(key: String, value: Int) {
        properties.setProperty(key, value.toString())
        save()
    }

    override fun save(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
        save()
    }

    override fun save(key: String, value: String) {
        properties.setProperty(key, value)
        save()
    }

    override fun save(key: String, value: Long) {
        properties.setProperty(key, value.toString())
        save()
    }

    override fun clear(key: String) {
        properties.clear()
    }

    internal fun remove(key: String): Boolean {
        properties.remove(key)
        save()
        return true
    }
}

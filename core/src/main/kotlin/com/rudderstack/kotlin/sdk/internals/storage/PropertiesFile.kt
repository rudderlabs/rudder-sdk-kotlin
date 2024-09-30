package com.rudderstack.kotlin.sdk.internals.storage

import com.rudderstack.kotlin.sdk.internals.logger.KotlinLogger
import com.rudderstack.kotlin.sdk.internals.logger.Logger
import com.rudderstack.kotlin.sdk.internals.logger.TAG
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.internals.utils.toPropertiesFileName
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

private const val PROPERTIES_PREFIX = "rudder"
private const val PROPERTIES_SUFFIX = ".properties"

internal class PropertiesFile(
    directory: File,
    writeKey: String,
    private val logger: Logger? = KotlinLogger()
) : KeyValueStorage {

    private var properties: Properties = Properties()
    private val propsFile = File(directory, writeKey.toPropertiesFileName(PROPERTIES_PREFIX, PROPERTIES_SUFFIX))

    /**
     * Loads properties from the file. If the file does not exist or fails to load, it creates a new file.
     */
    @Suppress("TooGenericExceptionCaught")
    fun load() {
        if (propsFile.exists()) {
            try {
                FileInputStream(propsFile).use {
                    properties.load(it)
                }
            } catch (e: Throwable) {
                propsFile.delete()
                logger?.error(
                    TAG,
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
        return properties.getProperty(key, String.empty()).toIntOrNull() ?: defaultVal
    }

    /**
     * Retrieves a boolean value from properties.
     * @param key The property key.
     * @param defaultVal The default value if the key does not exist or cannot be converted.
     * @return The boolean value associated with the key.
     */
    override fun getBoolean(key: String, defaultVal: Boolean): Boolean {
        return properties.getProperty(key, String.empty()).toBoolean()
    }

    override fun getString(key: String, defaultVal: String): String {
        return properties.getProperty(key, String.empty()).toString()
    }

    override fun getLong(key: String, defaultVal: Long): Long {
        return properties.getProperty(key, String.empty()).toLong()
    }

    override fun save(key: String, value: Int) {
        properties.setProperty(key, value.toString())
    }

    override fun save(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
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

    fun remove(key: String): Boolean {
        properties.remove(key)
        save()
        return true
    }
}

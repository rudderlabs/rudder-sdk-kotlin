package com.rudderstack.core.internals.storage

import com.rudderstack.core.internals.logger.TAG
import com.rudderstack.core.internals.logger.KotlinLogger
import com.rudderstack.core.internals.logger.Logger
import com.rudderstack.core.internals.utils.empty
import com.rudderstack.core.internals.utils.toPropertiesFileName
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

private const val PROPERTIES_PREFIX = "rudder"
private const val PROPERTIES_SUFFIX = ".properties"

class PropertiesFile(
    directory: File,
    writeKey: String,
    private val logger: Logger? = KotlinLogger()
) : KeyValueStorage {

    private var properties: Properties = Properties()
    private val propertiesFile = File(directory, writeKey.toPropertiesFileName(PROPERTIES_PREFIX, PROPERTIES_SUFFIX))

    fun load() {
        if (propertiesFile.exists()) {
            try {
                FileInputStream(propertiesFile).use {
                    properties.load(it)
                }
                return
            } catch (e: Throwable) {
                propertiesFile.delete()
                logger?.error(
                    TAG,
                    "Failed to load property file with path ${propertiesFile.absolutePath}, error stacktrace: ${e.stackTraceToString()}"
                )
            }
        }
        propertiesFile.parentFile.mkdirs()
        propertiesFile.createNewFile()
    }

    private fun save() {
        FileOutputStream(propertiesFile).use {
            properties.store(it, null)
        }
    }

    override fun getInt(key: String, defaultVal: Int): Int {
        return properties.getProperty(key, String.empty()).toIntOrNull() ?: defaultVal
    }

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
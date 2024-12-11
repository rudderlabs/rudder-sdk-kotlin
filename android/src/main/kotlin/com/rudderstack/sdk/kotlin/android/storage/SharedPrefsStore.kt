package com.rudderstack.sdk.kotlin.android.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage
import java.io.File

internal class SharedPrefsStore(
    private val context: Context,
    prefsName: String,
) : KeyValueStorage {

    private val preferences: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val useCase: CheckBuildVersionUseCase = CheckBuildVersionUseCase()

    override fun getInt(key: String, defaultVal: Int): Int {
        return preferences.getInt(key, defaultVal)
    }

    override fun getBoolean(key: String, defaultVal: Boolean): Boolean {
        return preferences.getBoolean(key, defaultVal)
    }

    override fun getString(key: String, defaultVal: String): String {
        return preferences.getString(key, defaultVal) ?: defaultVal
    }

    override fun getLong(key: String, defaultVal: Long): Long {
        return preferences.getLong(key, defaultVal)
    }

    override fun save(key: String, value: Int) {
        put(key, value)
    }

    override fun save(key: String, value: Boolean) {
        put(key, value)
    }

    override fun save(key: String, value: String) {
        put(key, value)
    }

    override fun save(key: String, value: Long) {
        put(key, value)
    }

    override fun clear(key: String) {
        with(preferences.edit()) {
            remove(key)
            commit()
        }
    }

    private fun delete(fileName: String) {
        if (useCase.isAndroidVersionNougatAndAbove()) {
            context.deleteSharedPreferences(fileName)
        } else {
            context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit().clear().apply()
            val dir = File(context.applicationInfo.dataDir, "shared_prefs")
            File(dir, "$fileName.xml").delete()
        }
    }

    private fun <T> put(key: String, value: T) {
        put(key, value, preferences)
    }

    private fun <T> put(key: String, value: T, prefs: SharedPreferences) {
        prefs.edit(commit = true) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
                else -> {
                }
            }
        }
    }
}

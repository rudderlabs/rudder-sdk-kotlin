package com.rudderstack.sdk.kotlin.android.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.storage.KeyValueStorage

internal class SharedPrefsStore(
    private val context: Context,
    private val prefsName: String,
) : KeyValueStorage {

    private val preferences: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

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

    override fun deletePrefs() {
        if (CheckBuildVersionUseCase.isAndroidVersionNAndAbove()) {
            context.deleteSharedPreferences(prefsName)
        } else {
            // TODO: Test this
            context.deleteFile(context.applicationInfo.dataDir + "/shared_prefs/$prefsName.xml")
        }
        LoggerAnalytics.info("SharedPrefsStore: Preference cleared.")
    }

    override fun clear(key: String) {
        with(preferences.edit()) {
            remove(key)
            commit()
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
                is String -> putString(key, value)

                else -> {
                    LoggerAnalytics.error("SharedPrefsStore: ($key and $value) type is not supported.")
                }
            }
        }
    }
}

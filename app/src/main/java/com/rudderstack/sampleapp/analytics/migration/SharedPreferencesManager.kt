package com.rudderstack.sampleapp.analytics.migration

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.io.File

/**
 * Manages SharedPreferences operations for migration.
 *
 * Provides a clean abstraction over Android's SharedPreferences API with:
 * - File existence checks
 * - Read operations (getString, getLong, getInt, contains, isEmpty)
 * - Write operations with chainable builder pattern
 * - Cleanup/delete operations
 *
 * @param context Android context
 * @param prefsName SharedPreferences file name (without .xml extension)
 */
internal class SharedPreferencesManager(
    private val context: Context,
    private val prefsName: String
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    // ==================== File Operations ====================

    /**
     * Checks if the SharedPreferences file physically exists on disk.
     *
     * @return true if the file exists, false otherwise
     */
    internal fun fileExists(): Boolean {
        val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/$prefsName.xml")
        return prefsFile.exists()
    }

    /**
     * Deletes the SharedPreferences file from disk.
     *
     * Uses the platform's deleteSharedPreferences on API 24+,
     * falls back to manual clear + file deletion on older versions.
     *
     * @return true if deletion was successful, false otherwise
     */
    internal fun delete(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences(prefsName)
        } else {
            val clearSuccess = prefs.edit().clear().commit()
            if (clearSuccess) {
                val prefsFile = File(
                    context.applicationInfo.dataDir,
                    "shared_prefs/$prefsName.xml"
                )
                prefsFile.delete()
            } else {
                false
            }
        }
    }

    // ==================== Read Operations ====================

    /**
     * Retrieves a string value from SharedPreferences.
     *
     * @param key The preference key
     * @return The string value, or null if not found
     */
    internal fun getString(key: String): String? {
        return prefs.getString(key, null)
    }

    /**
     * Retrieves a long value from SharedPreferences.
     *
     * @param key The preference key
     * @param default Default value if key not found
     * @return The long value, or default if not found
     */
    internal fun getLong(key: String, default: Long = -1L): Long {
        return prefs.getLong(key, default)
    }

    /**
     * Retrieves an int value from SharedPreferences.
     *
     * @param key The preference key
     * @param default Default value if key not found
     * @return The int value, or default if not found
     */
    internal fun getInt(key: String, default: Int = -1): Int {
        return prefs.getInt(key, default)
    }

    /**
     * Retrieves a boolean value from SharedPreferences.
     *
     * @param key The preference key
     * @param default Default value if key not found
     * @return The boolean value, or default if not found
     */
    internal fun getBoolean(key: String, default: Boolean): Boolean {
        return prefs.getBoolean(key, default)
    }

    /**
     * Checks if a key exists in SharedPreferences.
     *
     * @param key The preference key
     * @return true if the key exists, false otherwise
     */
    internal fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    /**
     * Checks if SharedPreferences is empty.
     *
     * @return true if no keys exist, false otherwise
     */
    internal fun isEmpty(): Boolean {
        return prefs.all.isEmpty()
    }

    /**
     * Gets the number of keys in SharedPreferences.
     *
     * @return The count of stored keys
     */
    internal fun getKeyCount(): Int {
        return prefs.all.size
    }

    // ==================== Write Operations ====================

    /**
     * Creates an editor for writing to SharedPreferences.
     *
     * Uses a chainable builder pattern for writes:
     * ```kotlin
     * manager.edit()
     *     .putString("key1", "value1")
     *     .putLong("key2", 123L)
     *     .commit()
     * ```
     *
     * @return A new Editor instance
     */
    internal fun edit(): Editor {
        return Editor(prefs.edit())
    }

    /**
     * Chainable editor for SharedPreferences writes.
     *
     * All put methods return the editor for chaining.
     * Call [commit] to persist changes synchronously.
     */
    internal inner class Editor(private val editor: SharedPreferences.Editor) {

        /**
         * Puts a string value.
         *
         * @param key The preference key
         * @param value The string value
         * @return This editor for chaining
         */
        internal fun putString(key: String, value: String): Editor {
            editor.putString(key, value)
            return this
        }

        /**
         * Puts a long value.
         *
         * @param key The preference key
         * @param value The long value
         * @return This editor for chaining
         */
        internal fun putLong(key: String, value: Long): Editor {
            editor.putLong(key, value)
            return this
        }

        /**
         * Puts a boolean value.
         *
         * @param key The preference key
         * @param value The boolean value
         * @return This editor for chaining
         */
        internal fun putBoolean(key: String, value: Boolean): Editor {
            editor.putBoolean(key, value)
            return this
        }

        /**
         * Clears all values from SharedPreferences.
         *
         * @return This editor for chaining
         */
        internal fun clear(): Editor {
            editor.clear()
            return this
        }

        /**
         * Commits changes synchronously.
         *
         * @return true if commit was successful, false otherwise
         */
        internal fun commit(): Boolean {
            return editor.commit()
        }
    }

    companion object {
        /**
         * Generates the new SDK SharedPreferences file name from a write key.
         *
         * @param writeKey The SDK write key
         * @return SharedPreferences file name in format "rl_prefs-{writeKey}"
         */
        internal fun newPrefsName(writeKey: String): String {
            return "${MigrationConstants.New.PREFS_PREFIX}-$writeKey"
        }
    }
}

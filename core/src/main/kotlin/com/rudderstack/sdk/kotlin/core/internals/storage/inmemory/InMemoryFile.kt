package com.rudderstack.sdk.kotlin.core.internals.storage.inmemory

import com.rudderstack.sdk.kotlin.core.internals.storage.TMP_SUFFIX
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Represents an in-memory file that mimics file-like behaviour for batch storage.
 *
 * This class provides a lightweight abstraction over [StringBuilder] to store event data
 * in memory, offering an API similar to [java.io.File] operations for consistency with
 * file-based storage.
 *
 * @property name The name of this in-memory file.
 */
@InternalRudderApi
internal class InMemoryFile(val name: String) {

    private val buffer = StringBuilder()

    private var created = false

    /**
     * The current length of the content stored in this file.
     */
    internal val length: Int
        get() = buffer.length

    /**
     * Returns the name without the temporary suffix.
     */
    internal val nameWithoutExtension: String
        get() = name.removeSuffix(TMP_SUFFIX)

    /**
     * Checks whether this file has been created.
     *
     * @return `true` if the file exists, `false` otherwise.
     */
    internal fun exists(): Boolean = created

    /**
     * Creates a new file if it does not already exist.
     *
     * @return `true` if the file was created, `false` if it already existed.
     */
    internal fun createNewFile(): Boolean {
        if (created) return false
        created = true
        return true
    }

    /**
     * Appends the given content to this file.
     *
     * @param content The content to append.
     */
    internal fun append(content: String) {
        buffer.append(content)
    }

    /**
     * Returns the content of this file as a String.
     *
     * @return The complete content stored in this file.
     */
    internal fun readText(): String = buffer.toString()
}

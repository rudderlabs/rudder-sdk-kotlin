package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.utils.Base64.encodeToBase64
import java.util.Locale
import java.util.UUID

private const val EMPTY_STRING = ""
private const val UNDERSCORE_SEPARATOR = "_"

/**
 * Encodes the string to a Base64 encoded string.
 *
 * This extension function formats the string by appending a colon (`:`) to it and then
 * converts the formatted string to a Base64 encoded string.
 *
 * @return The Base64 encoded representation of the string.
 */
internal fun String.encodeToBase64(): String {
    val formattedString = String.format(Locale.US, "%s:", this)
    val bytes = formattedString.toByteArray(Charsets.UTF_8)
    return bytes.encodeToBase64()
}

/**
 * Converts the string to an Android SharedPreferences key format by appending the given write key.
 *
 * This extension function formats the string by appending a hyphen (`-`) and the provided
 * write key to the original string.
 *
 * @param writeKey The write key to be appended to the string.
 * @return The formatted string suitable for use as an Android SharedPreferences key.
 */
@InternalRudderApi
fun String.toAndroidPrefsKey(writeKey: String): String {
    return "$this-$writeKey"
}

/**
 * Converts the string to a properties file name by appending the provided prefix and suffix.
 *
 * This extension function formats the string by prepending the given prefix and appending
 * the provided suffix to the original string.
 *
 * @param prefix The prefix to prepend to the string.
 * @param suffix The suffix to append to the string.
 * @return The formatted properties file name.
 */
internal fun String.toPropertiesFileName(prefix: String, suffix: String): String {
    return "$prefix-$this$suffix"
}

/**
 * Converts the string to a file directory path by prepending the provided directory path.
 *
 * This extension function formats the string by prepending the given directory path to the original string.
 *
 * @param directory The directory path to prepend to the string.
 * @return The resulting file directory path.
 */
internal fun String.toFileDirectory(directory: String): String {
    return "$directory$this"
}

/**
 * Parses a comma-separated string into a list of file paths.
 *
 * This extension function splits the string by commas, trims each resulting segment, and
 * returns a list of non-empty file paths. If the string is null or empty, an empty list is returned.
 *
 * @return A list of parsed file paths.
 */
internal fun String?.parseFilePaths(): List<String> {
    return if (this.isNullOrEmpty()) {
        emptyList()
    } else {
        this.split(",").map { it.trim() }
    }
}

/**
 * Provides an empty string.
 *
 * This companion object extension function returns an empty string (`""`).
 *
 * @return An empty string.
 */
@InternalRudderApi
fun String.Companion.empty(): String = EMPTY_STRING

/**
 * Provides an underscore separator constant.
 *
 * This companion object extension function returns an underscore separator (`"_"`).
 *
 * @return An underscore separator as a string.
 */
@InternalRudderApi
fun String.Companion.underscoreSeparator(): String = UNDERSCORE_SEPARATOR

/**
 * Validates and formats a base URL by ensuring it ends with a slash (`/`).
 *
 * This property checks if the string ends with a slash. If not, it appends a slash to the end.
 *
 * @return The validated base URL with a trailing slash.
 */
internal val String.validatedBaseUrl
    get() = if (this.endsWith('/')) this.removeSuffix("/") else this

/**
 * Generates a random UUID.
 */
@InternalRudderApi
fun generateUUID(): String {
    return UUID.randomUUID().toString()
}

/**
 * Appends the provided write key to the directory name using an underscore separator (`_`).
 *
 * This extension function formats the directory name by appending the given
 * write key using an underscore separator.
 *
 * @param writeKey The write key to be appended to the directory name.
 * @return The formatted directory name.
 */
@InternalRudderApi
fun String.appendWriteKey(writeKey: String): String {
    return "$this${String.underscoreSeparator()}$writeKey"
}

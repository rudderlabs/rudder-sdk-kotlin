package com.rudderstack.core.internals.utils

import java.util.Base64
import java.util.Locale

private const val EMPTY_STRING = ""

/**
 * Encodes the string to a Base64 encoded string.
 *
 * This extension function formats the string by appending a colon (`:`) to it and then
 * converts the formatted string to a Base64 encoded string.
 *
 * @return The Base64 encoded representation of the string.
 */
fun String.encodeToBase64(): String {
    val formattedString = String.format(Locale.US, "%s:", this)
    val bytes = formattedString.toByteArray(Charsets.UTF_8)
    return Base64.getEncoder().encodeToString(bytes)
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
fun String.toPropertiesFileName(prefix: String, suffix: String): String {
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
fun String.toFileDirectory(directory: String): String {
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
fun String?.parseFilePaths(): List<String> {
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
fun String.Companion.empty(): String = EMPTY_STRING

/**
 * Validates and formats a base URL by ensuring it ends with a slash (`/`).
 *
 * This property checks if the string ends with a slash. If not, it appends a slash to the end.
 *
 * @return The validated base URL with a trailing slash.
 */
val String.validatedBaseUrl
    get() = if (this.endsWith('/')) this.removeSuffix("/") else this

package com.rudderstack.core

import java.io.BufferedReader

fun Any.readFileAsString(fileName: String): String {
    val inputStream = this::class.java.classLoader.getResourceAsStream(fileName)
    return inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
}

fun Any.readFileTrimmed(fileName: String) =
    this.readFileAsString(fileName).cleanJsonString()

private fun String.cleanJsonString(): String {
    val regex = Regex("""(?<!\\)"[^"]*?"|[\s\n\r]+""")
    return this.replace(regex) { matchResult ->
        val matchedText = matchResult.value
        if (matchedText.startsWith("\"") && matchedText.endsWith("\"")) {
            // Keep the quoted text as is
            matchedText
        } else {
            // Remove spaces and newlines
            ""
        }
    }
}

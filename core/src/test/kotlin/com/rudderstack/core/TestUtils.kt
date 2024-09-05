package com.rudderstack.core

import java.io.BufferedReader

fun Any.readFileAsString(fileName: String): String {
    val inputStream = this::class.java.classLoader.getResourceAsStream(fileName)
    return inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
}
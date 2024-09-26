package com.rudderstack.core.internals.utils

private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

@Suppress("MagicNumber")
internal object Base64Encoder {

    fun encode(input: ByteArray): String {
        val output = StringBuilder()
        var paddingCount = 0

        // Process every 3 bytes into 4 Base64 characters
        for (i in input.indices step 3) {
            val chunk = (input[i].toInt() and 0xFF) shl 16 or
                (if (i + 1 < input.size) (input[i + 1].toInt() and 0xFF) shl 8 else 0) or
                (if (i + 2 < input.size) (input[i + 2].toInt() and 0xFF) else 0)

            output.append(BASE64_ALPHABET[(chunk shr 18) and 0x3F])
            output.append(BASE64_ALPHABET[(chunk shr 12) and 0x3F])

            if (i + 1 < input.size) {
                output.append(BASE64_ALPHABET[(chunk shr 6) and 0x3F])
            } else {
                output.append('=')
                paddingCount++
            }

            if (i + 2 < input.size) {
                output.append(BASE64_ALPHABET[chunk and 0x3F])
            } else {
                output.append('=')
                paddingCount++
            }
        }

        return output.toString()
    }
}

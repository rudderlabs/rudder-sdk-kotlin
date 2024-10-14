package com.rudderstack.android.utils

import android.app.Application
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

private const val SHA_256 = "SHA-256"

/**
 * `UniqueIdProvider` is a utility object that provides unique identifiers for the device.
 *
 * This object includes methods for obtaining a unique Widevine ID and the Android device ID,
 * which can be used to uniquely identify the device for tracking or analytical purposes.
 * The object also handles exceptions gracefully and ensures that resources are properly released.
 */
internal object UniqueIdProvider {

    /**
     * Retrieves the Widevine-protected unique device ID.
     *
     * This method uses the `MediaDrm` API to access a secure, hardware-backed ID specific to the device.
     * The ID is then hashed using the SHA-256 algorithm and returned as a hexadecimal string.
     * If the device does not support Widevine or if an error occurs during retrieval, the method returns `null`.
     *
     * @return A hashed string representation of the Widevine-protected device ID, or `null` if the ID cannot be obtained.
     */
    internal fun getUniqueID(): String? {
        val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
        var wvDrm: MediaDrm? = null
        try {
            wvDrm = MediaDrm(WIDEVINE_UUID)
            val wideVineId = wvDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            val md = MessageDigest.getInstance(SHA_256)
            md.update(wideVineId)
            return md.digest().toHexString()
        } catch (ignored: Exception) {
            return null
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                wvDrm?.close()
            } else {
                wvDrm?.release()
            }
        }
    }

    /**
     * Retrieves the Android device ID.
     *
     * This method accesses the Android ID, a unique identifier maintained by the Android OS.
     * The value is read from the `Settings.Secure.ANDROID_ID` property. If the Android ID is invalid
     * (e.g., empty, "unknown", or a known default value), the method returns `null`.
     *
     * @param application The application context used to access the Android system settings.
     * @return A string representing the Android ID, or `null` if the ID is invalid.
     */
    internal fun getDeviceId(application: Application): String? {
        val androidId = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
        val isInvalidId = androidId.isEmpty() ||
            androidId == "9774d56d682e549c" ||
            androidId == "unknown" ||
            androidId == "000000000000000"
        return if (!isInvalidId) androidId else null
    }

    /**
     * Converts a `ByteArray` to a hexadecimal string representation.
     *
     * This extension function is used internally by the class to convert a byte array (e.g., a hashed ID)
     * into a human-readable hexadecimal string format.
     *
     * @return The hexadecimal string representation of the byte array.
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}

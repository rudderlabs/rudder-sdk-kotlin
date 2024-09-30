package com.rudderstack.android.utils

import android.app.Application
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import java.lang.Exception
import java.security.MessageDigest
import java.util.UUID

private const val SHA_256 = "SHA-256"

object UniqueIdProvider {

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

    internal fun getDeviceId(application: Application): String? {
        val androidId = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
        val isInvalidId = androidId.isNullOrEmpty() ||
                androidId == "9774d56d682e549c" ||
                androidId == "unknown" ||
                androidId == "000000000000000"
        return if (!isInvalidId) androidId else null
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}

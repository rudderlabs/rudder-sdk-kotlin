package com.rudderstack.testapp.ipc

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.rudderstack.testapp.TestApp

/**
 * Read-only, single-column ContentProvider exposing the live SDK's identity state.
 *
 * Authority: `com.rudderstack.testapp.state`. Each path is a state field; the cursor has
 * one row with one column named `value`, holding either the field's current value or null
 * (when the SDK is shut down or the field is unset). Synchronous — chosen to survive
 * `am broadcast` ordering, where the driver issues a TRACK then immediately wants to read
 * `anonymousId` before any background coroutines finish.
 *
 * Supported paths:
 *  - `anonymousId` → [com.rudderstack.sdk.kotlin.android.Analytics.anonymousId]
 *  - `userId` → [com.rudderstack.sdk.kotlin.android.Analytics.userId]
 *  - `session` → [com.rudderstack.sdk.kotlin.android.Analytics.sessionId] as a string
 *
 * Mutating CRUD methods are no-ops; the provider is read-only by design.
 */
class StateProvider : ContentProvider() {

    private val app: TestApp
        get() = context!!.applicationContext as TestApp

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val analytics = app.analytics
        val value: String? = when (uri.lastPathSegment) {
            FIELD_ANONYMOUS_ID -> analytics?.anonymousId
            FIELD_USER_ID -> analytics?.userId
            FIELD_SESSION -> analytics?.sessionId?.toString()
            else -> null
        }
        return MatrixCursor(arrayOf(COLUMN_VALUE)).apply {
            addRow(arrayOf<Any?>(value))
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    internal companion object {
        /** Provider authority. Must match `android:authorities` in the SUT's `AndroidManifest.xml`. */
        const val AUTHORITY = "com.rudderstack.testapp.state"
        const val COLUMN_VALUE = "value"
        const val FIELD_ANONYMOUS_ID = "anonymousId"
        const val FIELD_USER_ID = "userId"
        const val FIELD_SESSION = "session"
    }
}

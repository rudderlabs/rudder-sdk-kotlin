package com.rudderstack.scenarioengine.infrastructure.state

import android.content.Context
import android.net.Uri
import com.rudderstack.scenarioengine.domain.helper.StateProbe
import com.rudderstack.testapp.ipc.StateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [StateProbe] that reads the SUT's identity state via the
 * [StateProvider] ContentProvider.
 *
 * Each method dispatches to [Dispatchers.IO] because [android.content.ContentResolver.query]
 * blocks (cross-process binder). Returning null is the contract for "field unset" or
 * "SDK not initialized" — see [StateProvider]'s query implementation.
 *
 * Has no internal state; safe to construct once per test and reuse across all reads.
 *
 * @param context the **driver** context — the [android.content.ContentResolver] reaches the
 *                SUT's exported provider over binder.
 */
class ContentProviderStateProbe(
    private val context: Context,
    private val authority: String = StateProvider.AUTHORITY,
) : StateProbe {

    override suspend fun anonymousId(): String? = read(StateProvider.FIELD_ANONYMOUS_ID)

    override suspend fun userId(): String? = read(StateProvider.FIELD_USER_ID)

    override suspend fun sessionId(): String? = read(StateProvider.FIELD_SESSION)

    private suspend fun read(field: String): String? = withContext(Dispatchers.IO) {
        val uri = Uri.parse("content://$authority/$field")
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}

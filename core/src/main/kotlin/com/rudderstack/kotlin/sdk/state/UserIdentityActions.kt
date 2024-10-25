package com.rudderstack.kotlin.sdk.state

import com.rudderstack.kotlin.sdk.internals.models.UserIdentity
import com.rudderstack.kotlin.sdk.internals.statemanagement.FlowAction
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.empty
import java.util.UUID

internal sealed interface UserIdentityAction : FlowAction<UserIdentity>

internal class SetAnonymousIdAction(
    private val storage: Storage,
    private val anonymousID: String = String.empty()
) : UserIdentityAction {

    override suspend fun reduce(currentState: UserIdentity): UserIdentity {
        val updatedAnonymousID = anonymousID.ifEmpty {
            currentState.anonymousID.ifEmpty {
                UUID.randomUUID().toString()
            }
        }
        val isAnonymousByClient = anonymousID.isNotEmpty()

        storage.write(StorageKeys.ANONYMOUS_ID, updatedAnonymousID)
        // not sure if we need to know if anonymous id was set from the client, thought it might be helpful in the future.
        storage.write(StorageKeys.IS_ANONYMOUS_ID_BY_CLIENT, isAnonymousByClient)

        return currentState.copy(anonymousID = updatedAnonymousID)
    }
}

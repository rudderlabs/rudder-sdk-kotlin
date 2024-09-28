package com.rudderstack.core.state

import com.rudderstack.core.internals.models.UserIdentity
import com.rudderstack.core.internals.statemanagement.Action
import com.rudderstack.core.internals.statemanagement.Reducer
import com.rudderstack.core.internals.statemanagement.State
import com.rudderstack.core.internals.storage.Storage
import com.rudderstack.core.internals.storage.StorageKeys.ANONYMOUS_ID
import com.rudderstack.core.internals.storage.StorageKeys.IS_ANONYMOUS_ID_BY_CLIENT
import com.rudderstack.core.internals.utils.empty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

internal data class UserIdentityState(
    val userIdentity: UserIdentity
) : State {

    companion object {

        fun currentState(storage: Storage): UserIdentityState {
            return UserIdentityState(
                userIdentity = UserIdentity(
                    anonymousID = storage.readString(ANONYMOUS_ID, defaultVal = String.empty()),
                    userId = String.empty()
                )
            )
        }
    }

    internal class SetIdentityAction(val storage: Storage, val anonymousID: String = String.empty()) : Action

    internal class GenerateUserAnonymousID(
        private val stateScope: CoroutineScope
    ) : Reducer<UserIdentityState, SetIdentityAction> {

        override fun invoke(currentState: UserIdentityState, action: SetIdentityAction): UserIdentityState {
            val updatedAnonymousID = action.anonymousID.ifEmpty {
                currentState.userIdentity.anonymousID.ifEmpty {
                    UUID.randomUUID().toString()
                }
            }
            val isAnonymousByClient = action.anonymousID.isNotEmpty()
            stateScope.launch {
                action.storage.write(ANONYMOUS_ID, updatedAnonymousID)
                // not sure if we need to know if anonymous id was set from the client, thought it might be helpful in the future.
                action.storage.write(IS_ANONYMOUS_ID_BY_CLIENT, isAnonymousByClient)
            }

            return currentState.copy(
                userIdentity = currentState.userIdentity.copy(anonymousID = updatedAnonymousID)
            )
        }
    }
}

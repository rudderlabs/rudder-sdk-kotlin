package com.rudderstack.kotlin.sdk.state

import com.rudderstack.kotlin.sdk.internals.models.UserIdentity
import com.rudderstack.kotlin.sdk.internals.statemanagement.Action
import com.rudderstack.kotlin.sdk.internals.statemanagement.Reducer
import com.rudderstack.kotlin.sdk.internals.statemanagement.State
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.empty

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

internal data class UserIdentityState(val userIdentity: UserIdentity) : State {

    companion object {

        fun currentState(storage: Storage): UserIdentityState {
            return UserIdentityState(
                userIdentity = UserIdentity(
                    anonymousID = storage.readString(StorageKeys.ANONYMOUS_ID, defaultVal = String.empty()),
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
                action.storage.write(StorageKeys.ANONYMOUS_ID, updatedAnonymousID)
                // not sure if we need to know if anonymous id was set from the client, thought it might be helpful in the future.
                action.storage.write(StorageKeys.IS_ANONYMOUS_ID_BY_CLIENT, isAnonymousByClient)
            }

            return currentState.copy(
                userIdentity = currentState.userIdentity.copy(anonymousID = updatedAnonymousID)
            )
        }
    }
}

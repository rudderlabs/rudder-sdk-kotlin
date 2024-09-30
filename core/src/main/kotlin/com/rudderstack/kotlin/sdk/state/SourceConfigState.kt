package com.rudderstack.kotlin.sdk.state

import com.rudderstack.kotlin.sdk.internals.models.RudderServerConfigSource
import com.rudderstack.kotlin.sdk.internals.models.SourceConfig
import com.rudderstack.kotlin.sdk.internals.statemanagement.Action
import com.rudderstack.kotlin.sdk.internals.statemanagement.Reducer
import com.rudderstack.kotlin.sdk.internals.statemanagement.State
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys.SOURCE_CONFIG_PAYLOAD
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys.SOURCE_IS_ENABLED
import com.rudderstack.kotlin.sdk.internals.utils.empty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal data class SourceConfigState(
    val sourceConfig: SourceConfig
) : State {

    companion object {
        fun initialState(): SourceConfigState = SourceConfigState(
            sourceConfig = SourceConfig(
                source = RudderServerConfigSource(
                    sourceId = String.empty(),
                    sourceName = String.empty(),
                    writeKey = String.empty(),
                    isSourceEnabled = false,
                    workspaceId = String.empty(),
                    updatedAt = String.empty()
                )
            )
        )
    }

    internal class UpdateAction(val updatedSourceConfig: SourceConfig) : Action

    internal class SaveSourceConfigValuesReducer(val storage: Storage, private val stateScope: CoroutineScope) :
        Reducer<SourceConfigState, UpdateAction> {

        override fun invoke(currentState: SourceConfigState, action: UpdateAction): SourceConfigState {
            stateScope.launch {
                storage.apply {
                    write(SOURCE_CONFIG_PAYLOAD, Json.encodeToString(SourceConfig.serializer(), action.updatedSourceConfig))
                    write(SOURCE_IS_ENABLED, action.updatedSourceConfig.source.isSourceEnabled)
                }
            }
            return SourceConfigState(action.updatedSourceConfig)
        }
    }
}

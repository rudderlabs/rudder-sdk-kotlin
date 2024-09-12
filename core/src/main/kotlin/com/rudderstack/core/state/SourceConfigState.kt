package com.rudderstack.core.state

import com.rudderstack.core.internals.models.RudderServerConfigSource
import com.rudderstack.core.internals.models.SourceConfig
import com.rudderstack.core.internals.statemanagement.Action
import com.rudderstack.core.internals.statemanagement.Reducer
import com.rudderstack.core.internals.statemanagement.State
import com.rudderstack.core.internals.storage.Storage
import com.rudderstack.core.internals.storage.StorageKeys.SOURCE_CONFIG_PAYLOAD
import com.rudderstack.core.internals.storage.StorageKeys.SOURCE_IS_ENABLED
import com.rudderstack.core.internals.utils.empty
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

    internal class Update(val updatedSourceConfig: SourceConfig) : Action

    internal class SaveSourceConfigValues(val storage: Storage, private val stateScope: CoroutineScope) :
        Reducer<SourceConfigState, Update> {

        override fun invoke(currentState: SourceConfigState, action: Update): SourceConfigState {
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

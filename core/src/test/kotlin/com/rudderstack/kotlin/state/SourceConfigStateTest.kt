package com.rudderstack.kotlin.state

import com.rudderstack.kotlin.sdk.internals.models.RudderServerConfigSource
import com.rudderstack.kotlin.sdk.internals.models.SourceConfig
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys.SOURCE_CONFIG_PAYLOAD
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys.SOURCE_IS_ENABLED
import com.rudderstack.kotlin.sdk.state.SourceConfigState
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourceConfigStateTest {

    private val storage: Storage = mockk(relaxed = true)
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val initialState = SourceConfigState.initialState()
    private val reducer = SourceConfigState.SaveSourceConfigValuesReducer(storage, testScope)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given an initial SourceConfigState when state is updated then update subscribers with updated SourceConfig`() {
        val newSourceConfig = SourceConfig(
            source = RudderServerConfigSource(
                sourceId = "newId",
                sourceName = "newName",
                writeKey = "newKey",
                isSourceEnabled = true,
                workspaceId = "newWorkspaceId",
                updatedAt = "2023-09-12"
            )
        )

        val updateAction = SourceConfigState.UpdateAction(newSourceConfig)
        val newState = reducer.invoke(initialState, updateAction)

        assertEquals(newSourceConfig, newState.sourceConfig)
    }

    @Test
    fun `given an initial SourceConfigState, when state is updated then save the result in the storage`() = runTest {
        val newSourceConfig = SourceConfig(
            source = RudderServerConfigSource(
                sourceId = "newId",
                sourceName = "newName",
                writeKey = "newKey",
                isSourceEnabled = true,
                workspaceId = "newWorkspaceId",
                updatedAt = "2023-09-12"
            )
        )

        val updateAction = SourceConfigState.UpdateAction(newSourceConfig)
        reducer.invoke(initialState, updateAction)
        advanceUntilIdle()

        coVerify {
            storage.write(SOURCE_CONFIG_PAYLOAD, Json.encodeToString(SourceConfig.serializer(), newSourceConfig))
            storage.write(SOURCE_IS_ENABLED, newSourceConfig.source.isSourceEnabled)
        }
    }
}

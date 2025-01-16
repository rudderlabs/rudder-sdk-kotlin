package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

import io.mockk.spyk

private const val API_KEY = "test-api-key"

class MockDestinationSdk private constructor(private val apiKey: String) {

    fun trackEvent(event: String) {
        println("MockDestinationSdk: Tracking event: $event")
    }

    fun screenEvent(screenName: String) {
        println("MockDestinationSdk: Screen event: $screenName")
    }

    fun groupEvent(groupId: String) {
        println("MockDestinationSdk: Group event: $groupId")
    }

    fun identifyUser(userId: String) {
        println("MockDestinationSdk: Identifying user: $userId")
    }

    fun aliasUser(userId: String, previousId: String) {
        println("MockDestinationSdk: Aliasing user: $userId from $previousId")
    }

    fun reset() {
        println("MockDestinationSdk: Resetting")
    }

    fun flush() {
        println("MockDestinationSdk: Flushing")
    }

    fun update() {
        println("MockDestinationSdk: Updating")
    }

    companion object {

        fun initialise(apiKey: String): MockDestinationSdk {
            return if (apiKey == API_KEY) {
                spyk(MockDestinationSdk(apiKey))
            } else {
                throw IllegalArgumentException("MockDestinationSdk: Invalid API Key")
            }
        }
    }
}

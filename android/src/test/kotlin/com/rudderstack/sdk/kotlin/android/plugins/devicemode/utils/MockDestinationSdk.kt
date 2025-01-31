package com.rudderstack.sdk.kotlin.android.plugins.devicemode.utils

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

    companion object {

        fun initialise(apiKey: String): MockDestinationSdk {
            // if the API key has any special characters other than hyphen, throw an exception
            if (apiKey.contains(Regex("[^a-zA-Z0-9-]"))) {
                throw IllegalArgumentException("Invalid API key")
            }
            return MockDestinationSdk(apiKey)
        }
    }
}

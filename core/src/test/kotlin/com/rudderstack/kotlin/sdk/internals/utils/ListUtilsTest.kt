package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.internals.models.ExternalId
import org.junit.Test

class ListUtilsTest {

    @Test
    fun `given two lists with different external ids, when merged, then return a list with all external ids`() {
        val list1 = listOf(ExternalId(type = "brazeExternalId", id = "braze id 1"))
        val list2 = listOf(ExternalId(type = "ga4", id = "ga4 id 1"))

        val mergedList = list1 mergeWithHigherPriorityTo list2

        assert(mergedList.size == 2) { "Expected merged list to contain 2 items" }
        assert(
            mergedList[0] == ExternalId(
                type = "brazeExternalId",
                id = "braze id 1"
            )
        ) { "First item did not match expected brazeExternalId" }
        assert(mergedList[1] == ExternalId(type = "ga4", id = "ga4 id 1")) { "Second item did not match expected ga4" }
    }

    @Test
    fun `given two lists with same external ids, when merged, then return a list with only one external id with higher priority to the last one`() {
        val list1 = listOf(ExternalId(type = "brazeExternalId", id = "braze id 1"))
        val list2 = listOf(ExternalId(type = "brazeExternalId", id = "braze id 2"))

        val mergedList = list1 mergeWithHigherPriorityTo list2

        assert(mergedList.size == 1) { "Expected merged list to contain 1 item" }
        assert(
            mergedList[0] == ExternalId(
                type = "brazeExternalId",
                id = "braze id 2"
            )
        ) { "Item did not match expected brazeExternalId" }
    }

    @Test
    fun `given two lists with multiple external ids without any overlap, when merged, then return a list with all external ids`() {
        val list1 = listOf(
            ExternalId(type = "brazeExternalId", id = "braze id 1"),
            ExternalId(type = "amplitudeExternalId", id = "Amplitude id 1")
        )
        val list2 = listOf(
            ExternalId(type = "ga4", id = "ga4 id 1"),
            ExternalId(type = "clevertapExternalId", id = "Clevertap id 1")
        )

        val mergedList = list1 mergeWithHigherPriorityTo list2

        assert(mergedList.size == 4) { "Expected merged list to contain 2 items" }
        assert(
            mergedList[0] == ExternalId(
                type = "brazeExternalId",
                id = "braze id 1"
            )
        ) { "First item did not match expected brazeExternalId" }
        assert(
            mergedList[1] == ExternalId(
                type = "amplitudeExternalId",
                id = "Amplitude id 1"
            )
        ) { "Second item did not match expected amplitudeExternalId" }
        assert(mergedList[2] == ExternalId(type = "ga4", id = "ga4 id 1")) { "Third item did not match expected ga4" }
        assert(
            mergedList[3] == ExternalId(
                type = "clevertapExternalId",
                id = "Clevertap id 1"
            )
        ) { "Fourth item did not match expected clevertapExternalId" }
    }

    @Test
    fun `given two lists with multiple external ids with overlap, when merged, then return a list with only one external id with higher priority to the last one`() {
        val list1 = listOf(
            ExternalId(type = "brazeExternalId", id = "braze id 1"),
            ExternalId(type = "amplitudeExternalId", id = "Amplitude id 1")
        )
        val list2 = listOf(
            ExternalId(type = "brazeExternalId", id = "braze id 2"),
            ExternalId(type = "clevertapExternalId", id = "Clevertap id 1")
        )

        val mergedList = list1 mergeWithHigherPriorityTo list2

        assert(mergedList.size == 3) { "Expected merged list to contain 3 items" }
        assert(
            mergedList[0] == ExternalId(
                type = "brazeExternalId",
                id = "braze id 2"
            )
        ) { "First item did not match expected brazeExternalId" }
        assert(
            mergedList[1] == ExternalId(
                type = "amplitudeExternalId",
                id = "Amplitude id 1"
            )
        ) { "Second item did not match expected amplitudeExternalId" }
        assert(
            mergedList[2] == ExternalId(
                type = "clevertapExternalId",
                id = "Clevertap id 1"
            )
        ) { "Third item did not match expected clevertapExternalId" }
    }
}

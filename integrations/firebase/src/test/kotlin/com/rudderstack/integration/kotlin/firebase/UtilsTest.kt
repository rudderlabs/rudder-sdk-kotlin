import com.rudderstack.integration.kotlin.firebase.getString
import com.rudderstack.integration.kotlin.firebase.attachAllCustomProperties
import android.os.Bundle
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class UtilsTest {

    private lateinit var mockBundle: Bundle

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        mockBundle = mockk(relaxed = true)
        every { mockBundle.putString(any(), any()) } returns Unit
        every { mockBundle.putInt(any(), any()) } returns Unit
        every { mockBundle.putDouble(any(), any()) } returns Unit
        every { mockBundle.putBoolean(any(), any()) } returns Unit
    }

    @ParameterizedTest
    @MethodSource("provideJsonElementsForGetString")
    fun `when getString is called with different JsonElement types, it should return the expected string`(
        jsonElement: Any?,
        expected: String,
    ) {
        val result = getString(jsonElement as? JsonElement, maxLength = 100)

        assertEquals(expected, result)
    }

    @Test
    fun `when attachAllCustomProperties is called with reserved keywords for ecommerce events, then reserved keywords should be filtered out`() {
        val properties = buildJsonObject {
            put("product_id", "product123") // Reserved keyword
            put("name", "Test Product") // Reserved keyword
            put("custom_property", "custom_value") // Non-reserved property
        }

        attachAllCustomProperties(mockBundle, properties, isEcommerceEvent = true)

        // Verify reserved keywords are NOT added to bundle
        verify(exactly = 0) { mockBundle.putString("product_id", any()) }
        verify(exactly = 0) { mockBundle.putString("name", any()) }
        
        // Verify non-reserved property IS added to bundle
        verify(exactly = 1) { mockBundle.putString("custom_property", "custom_value") }
    }

    @Test
    fun `when attachAllCustomProperties is called with reserved keywords for non-ecommerce events, then all keywords should be included`() {
        val properties = buildJsonObject {
            put("product_id", "product456") // Reserved keyword
            put("name", "Test Product") // Reserved keyword
            put("custom_property", "custom_value") // Non-reserved property
        }

        attachAllCustomProperties(mockBundle, properties, isEcommerceEvent = false)

        // Verify ALL properties are added to bundle (including reserved keywords)
        verify(exactly = 1) { mockBundle.putString("product_id", "product456") }
        verify(exactly = 1) { mockBundle.putString("name", "Test Product") }
        verify(exactly = 1) { mockBundle.putString("custom_property", "custom_value") }
    }

    @Test
    fun `when attachAllCustomProperties is called with empty properties, then no properties should be added`() {
        val properties = buildJsonObject { }

        attachAllCustomProperties(mockBundle, properties, isEcommerceEvent = true)

        verify(exactly = 0) { mockBundle.putString(any(), any()) }
        verify(exactly = 0) { mockBundle.putInt(any(), any()) }
        verify(exactly = 0) { mockBundle.putDouble(any(), any()) }
        verify(exactly = 0) { mockBundle.putBoolean(any(), any()) }
    }

    @Test
    fun `when attachAllCustomProperties is called with empty properties for non-ecommerce events, then no properties should be added`() {
        val properties = buildJsonObject { }

        attachAllCustomProperties(mockBundle, properties, isEcommerceEvent = false)

        verify(exactly = 0) { mockBundle.putString(any(), any()) }
        verify(exactly = 0) { mockBundle.putInt(any(), any()) }
        verify(exactly = 0) { mockBundle.putDouble(any(), any()) }
        verify(exactly = 0) { mockBundle.putBoolean(any(), any()) }
    }

    @Test
    fun `when attachAllCustomProperties is called with null properties, then no properties should be added`() {
        attachAllCustomProperties(mockBundle, null, isEcommerceEvent = true)

        verify(exactly = 0) { mockBundle.putString(any(), any()) }
        verify(exactly = 0) { mockBundle.putInt(any(), any()) }
        verify(exactly = 0) { mockBundle.putDouble(any(), any()) }
        verify(exactly = 0) { mockBundle.putBoolean(any(), any()) }
    }

    @Test
    fun `when attachAllCustomProperties is called with null properties for non-ecommerce events, then no properties should be added`() {
        attachAllCustomProperties(mockBundle, null, isEcommerceEvent = false)

        verify(exactly = 0) { mockBundle.putString(any(), any()) }
        verify(exactly = 0) { mockBundle.putInt(any(), any()) }
        verify(exactly = 0) { mockBundle.putDouble(any(), any()) }
        verify(exactly = 0) { mockBundle.putBoolean(any(), any()) }
    }

    companion object {

        @JvmStatic
        fun provideJsonElementsForGetString(): Stream<Arguments> {
            return Stream.of(
                // JsonPrimitive cases
                Arguments.of(JsonPrimitive("string value"), "string value"),
                Arguments.of(JsonPrimitive(123), "123"),
                Arguments.of(JsonPrimitive(3.14), "3.14"),
                Arguments.of(JsonPrimitive(true), "true"),

                // JsonArray cases
                Arguments.of(
                    buildJsonArray {
                        add("item1")
                        add(2)
                    },
                    "[\"item1\",2]"
                ),
                Arguments.of(
                    buildJsonArray { },
                    "[]"
                ),

                // JsonObject cases
                Arguments.of(
                    buildJsonObject { put("key", "value") },
                    "{\"key\":\"value\"}"
                ),

                // Null case
                Arguments.of(null, "null"),

                // JsonNull case
                Arguments.of(JsonNull, "null"),

                // Nested JsonObject case
                Arguments.of(
                    buildJsonObject {
                        put("outer", buildJsonObject {
                            put("inner", "value")
                        })
                        put("outer2", buildJsonArray {
                            add("item1")
                            add(2)
                        })
                    },
                    "{\"outer\":{\"inner\":\"value\"},\"outer2\":[\"item1\",2]}"
                )
            )
        }
    }
}

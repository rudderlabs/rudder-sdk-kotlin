import com.rudderstack.integration.kotlin.firebase.getString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class UtilsTest {

    @ParameterizedTest
    @MethodSource("provideJsonElementsForGetString")
    fun `when getString is called with different JsonElement types, it should return the expected string`(
        jsonElement: Any?,
        expected: String,
    ) {
        val result = getString(jsonElement as? JsonElement, maxLength = 100)

        assertEquals(expected, result)
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

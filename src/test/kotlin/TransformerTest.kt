import dev.kikugie.j52j.J52JConverterProperties
import dev.kikugie.j52j.transform
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.quiltmc.parsers.json.MalformedSyntaxException
import kotlin.reflect.KClass

object TransformerTest {
    private val SAMPLES = buildList {
        test("standard conversion") {
            input = """
                {
                    // hey, I'm a comment!
                    key: "value"
                }
            """.trimIndent()
            output = """
                {
                  "key": "value"
                }
            """.trimIndent()
        }
        test("conversion error") {
            input = """
                {
                    // hey, I'm a comment!
                    key: "value
                }
            """.trimIndent()
            output = ""
            exception = MalformedSyntaxException::class
        }
    }

    @TestFactory
    fun `test transformer`() = SAMPLES.map {
        DynamicTest.dynamicTest(it.name) { check(it) }
    }

    private fun check(data: TestEntry) {
        fun convert() = transform(data.input.reader(), J52JConverterProperties().apply { prettyPrinting = true })
        val result = data.exception?.let { Assertions.assertThrows(it.java) { convert() }; "" } ?: convert()
        Assertions.assertEquals(data.output, result)
    }

    private inline fun MutableList<TestEntry>.test(name: String, block: TestEntry.() -> Unit) {
        add(TestEntry().apply { this.name = name }.apply(block))
    }

    private class TestEntry {
        lateinit var name: String
        lateinit var input: String
        lateinit var output: String
        var exception: KClass<out Throwable>? = null
    }
}
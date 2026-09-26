package tr.ovayuva.ovayuvam

import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ResourceCoverageTest {
    private fun entries(folder: String): Map<String, String> {
        val builder = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder()
        return File("src/main/res/$folder").listFiles()!!.filter { it.extension == "xml" }.flatMap { file ->
            val nodes = builder.parse(file).getElementsByTagName("string")
            (0 until nodes.length).map { (nodes.item(it) as Element).let { node -> node.getAttribute("name") to node.textContent } }
        }.toMap()
    }

    @Test fun allSixLanguagesHaveEveryInterfaceStringAndFormatArgument() {
        val english = entries("values")
        val placeholders = Regex("%[0-9]+\\$[sd]")
        for (code in listOf("de", "tr", "ru", "es", "fr")) {
            val translated = entries("values-$code")
            assertEquals("Missing or extra strings in $code", english.keys, translated.keys)
            english.forEach { (key, value) ->
                assertTrue("Empty $code/$key", translated.getValue(key).isNotBlank())
                assertEquals("Format mismatch $code/$key", placeholders.findAll(value).map { it.value }.toList(),
                    placeholders.findAll(translated.getValue(key)).map { it.value }.toList())
            }
        }
    }
}

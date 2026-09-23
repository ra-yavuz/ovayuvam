package tr.ovayuva.ovayuvam

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherIconTest {
    private val android = "http://schemas.android.com/apk/res/android"

    private fun resource(path: String) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(File("src/main/res/$path")).documentElement

    @Test fun normalForegroundUsesBlackWithoutReplacingTheDrawing() {
        val icon = resource("drawable/ic_launcher_foreground.xml")
        assertEquals("bitmap", icon.tagName)
        assertEquals("@drawable/launcher_map_foreground", icon.getAttributeNS(android, "src"))
        assertEquals("#000000", icon.getAttributeNS(android, "tint"))
    }

    @Test fun bothLauncherShapesUseTheSameReadableForeground() {
        for (name in listOf("ic_launcher", "ic_launcher_round")) {
            val icon = resource("mipmap-anydpi-v26/$name.xml")
            val foreground = icon.getElementsByTagName("foreground").item(0) as org.w3c.dom.Element
            val background = icon.getElementsByTagName("background").item(0) as org.w3c.dom.Element
            assertEquals("@drawable/ic_launcher_foreground", foreground.getAttributeNS(android, "drawable"))
            assertEquals("@color/seed", background.getAttributeNS(android, "drawable"))
        }
    }

    @Test fun themedIconStillLetsAndroidChooseItsColor() {
        val icon = resource("drawable/ic_launcher_monochrome.xml")
        assertEquals("@drawable/launcher_map_monochrome", icon.getAttributeNS(android, "src"))
        assertEquals("", icon.getAttributeNS(android, "tint"))
    }
}

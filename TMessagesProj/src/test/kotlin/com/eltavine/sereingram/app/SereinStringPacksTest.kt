package com.eltavine.sereingram.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Telegram shows strings from the packs its build makes of the string files, not from the
 * app's resources: SereinGram's own strings must reach it that way, in each language they are
 * written in, and the brand overlay's names must take the place of Nagram's.
 */
@RunWith(RobolectricTestRunner::class)
// Switching Telegram's locale tells its network code, whose native methods Robolectric stubs once it instruments them.
@Config(application = Application::class, sdk = [34], instrumentedPackages = ["org.telegram.tgnet"])
class SereinStringPacksTest {
    @Test
    fun telegramShowsSereinGramsStringsInEnglish() = assertShown("values")

    @Test
    @Config(qualifiers = "zh-rCN")
    fun telegramShowsSereinGramsStringsInChinese() = assertShown("values-zh-rCN")

    private fun assertShown(folder: String) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        ApplicationLoader.applicationContext = context
        showSereinStrings(context).use {
            val written = strings(File("src/main/res/$folder/strings_serein.xml")) + strings(File("src/serein/res/$folder/strings_brand.xml"))
            val shown = written.keys.associateWith { name -> LocaleController.getString(R.string::class.java.getField(name).getInt(null)) }
            assertEquals(written, shown)
        }
    }

    // Unescaped as Telegram's build unescapes them for its packs.
    private fun strings(file: File): Map<String, String> {
        val elements = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
        return (0 until elements.length).map { elements.item(it) as Element }.associate { element ->
            element.getAttribute("name") to element.textContent.replace("\\n", "\n").replace("\\", "")
        }
    }
}

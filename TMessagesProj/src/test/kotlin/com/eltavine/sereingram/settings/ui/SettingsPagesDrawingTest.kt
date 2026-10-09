package com.eltavine.sereingram.settings.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.app.SereinApp
import com.eltavine.sereingram.app.categoryTitle
import com.eltavine.sereingram.app.rootPage
import com.eltavine.sereingram.app.showSereinStrings
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.KeyValueStore
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.features.backup.BackupSettings
import com.eltavine.sereingram.features.dns.DnsMode
import com.eltavine.sereingram.features.dns.DnsOptions
import com.eltavine.sereingram.features.history.HistoryOptions
import com.eltavine.sereingram.features.transcription.TranscriptionOptions
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsState
import com.eltavine.sereingram.settings.featureSections
import com.eltavine.sereingram.ui.drawAsSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.UniversalRecyclerView
import java.io.File

/**
 * Draws the SereinGram page and every feature's page with Telegram's own rows, as a
 * phone would, once with the defaults and once with some features on. Each page must
 * draw without a fault and with every text found in Telegram's string packs; the pictures
 * go to build/reports/serein-settings to be looked at.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Telegram's theme reaches its network code, whose native methods Robolectric stubs once it instruments them. Its
// database is left as it is: stubbed, a cursor never ends and the storage thread fills the heap; as it is, that thread stops.
@Config(
    application = Application::class,
    sdk = [34],
    qualifiers = "w411dp-h914dp-xxhdpi",
    instrumentedPackages = ["org.telegram.tgnet"],
)
class SettingsPagesDrawingTest {
    private val stores = HashMap<Pair<OptionScope, Int>, KeyValueStore>()
    private val options = Options { scope, account -> stores.getOrPut(scope to account) { MemoryKeyValueStore() } }
    private val state = SettingsState(options, account = 0)

    @Test
    fun everyPageDrawsWithTelegramsRows() = drawEveryPage(File("build/reports/serein-settings"))

    @Test
    @Config(qualifiers = "zh-rCN-w411dp-h914dp-xxhdpi")
    fun everyPageDrawsInChinese() = drawEveryPage(File("build/reports/serein-settings/zh-rCN"))

    private fun drawEveryPage(pictures: File) {
        pictures.mkdirs()
        val context = ApplicationProvider.getApplicationContext<Application>()
        ApplicationLoader.applicationContext = context
        AndroidUtilities.checkDisplaySize(context, context.resources.configuration)
        Theme.createCommonResources(context)
        val modules = SereinApp.modules(context)
        val contributors = modules.modules.filterIsInstance<SettingsContributor>()
        val pages = listOf("serein" to rootPage(featureSections(contributors, ::categoryTitle) + BackupSettings.section(options, modules.options))) +
            contributors.map { it::class.java.simpleName.removeSuffix("Feature").lowercase() to it.settingsPage }

        val faults = ArrayList<String>()
        Faults.reporters.install { message, error -> faults += "$message: $error" }.use {
            showSereinStrings(context).use {
                pages.forEach { (name, page) -> assertTrue("$name drew nothing", draw(context, page, File(pictures, "$name.png")) > 0) }
                state[HistoryOptions.saveDeleted] = true
                state[DnsOptions.mode] = DnsMode.CUSTOM.code
                state[DnsOptions.customServer] = "dns.example/dns-query"
                state[TranscriptionOptions.enabled] = true
                pages.forEach { (name, page) -> draw(context, page, File(pictures, "$name-on.png")) }
            }
        }
        assertEquals(emptyList<String>(), faults)
        assertEquals("texts Telegram could not find", emptyList<String>(), unresolved)
    }

    private val unresolved = ArrayList<String>()

    private fun draw(context: Context, page: SettingsPage, picture: File): Int {
        var drawn = 0
        val host = object : RowHost {
            override val fragment: BaseFragment get() = error("drawing needs no screen")
            override val state: SettingsState get() = this@SettingsPagesDrawingTest.state

            override fun refresh() = Unit

            override fun <T : Any> commit(row: SettingsRow.Editor<T>, value: T) = Unit

            override fun open(page: SettingsPage) = Unit

            override fun pick(mimeTypes: List<String>, picked: (uri: String) -> Unit) = Unit
        }
        val list = UniversalRecyclerView(context, 0, 0, { items, _ ->
            drawn = drawPage(page, host, items).sections.sumOf { it.rows.size }
            unresolved += items.flatMap { listOfNotNull(it.text, it.subtext, it.textValue) }.map { it.toString() }.filter { "LOC_ERR" in it }
        }, null, null, null)
        list.drawAsSettings()
        val width = context.resources.displayMetrics.widthPixels
        list.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(MAX_HEIGHT, View.MeasureSpec.AT_MOST))
        list.layout(0, 0, width, list.measuredHeight)
        val bitmap = Bitmap.createBitmap(width, list.measuredHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Theme.getColor(Theme.key_windowBackgroundGray))
            list.draw(this)
        }
        picture.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return drawn
    }

    private companion object {
        const val MAX_HEIGHT = 1 shl 14
    }
}

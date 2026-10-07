package com.eltavine.sereingram.app

import android.app.Application
import com.eltavine.sereingram.adapters.prefs.PreferencesLocalNameStore
import com.eltavine.sereingram.adapters.prefs.PreferencesStores
import com.eltavine.sereingram.adapters.room.RoomBookmarkStore
import com.eltavine.sereingram.adapters.room.RoomHistoryStore
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.ModuleRegistry
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.features.bookmarks.BookmarksFeature
import com.eltavine.sereingram.features.declutter.DeclutterFeature
import com.eltavine.sereingram.features.ghost.GhostFeature
import com.eltavine.sereingram.features.history.HistoryFeature
import com.eltavine.sereingram.features.input.InputFeature
import com.eltavine.sereingram.features.links.LinksFeature
import com.eltavine.sereingram.features.localnames.LocalNamesFeature
import com.eltavine.sereingram.features.messagemenu.MessageMenuFeature
import com.eltavine.sereingram.features.notifications.NotificationsFeature
import com.eltavine.sereingram.features.search.SearchFeature
import com.eltavine.sereingram.features.services.ServicesModule
import com.eltavine.sereingram.features.timestamps.TimestampsFeature
import com.eltavine.sereingram.ports.BookmarkStore
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.ports.LocalNameStore
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.FileLog
import org.telegram.messenger.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Composition root, started once per process from `ApplicationLoader.onCreate`.
 * The module list is the only place that knows every feature.
 */
object SereinApp {
    @JvmStatic
    lateinit var options: Options
        private set

    @JvmStatic
    fun onCreate(application: Application) {
        if (::options.isInitialized) {
            return
        }
        Faults.reporters.install { message, error -> FileLog.e("SereinGram: $message", error) }
        options = Options(PreferencesStores(application))
        val historyStores = ConcurrentHashMap<Int, HistoryStore>()
        val bookmarkStores = ConcurrentHashMap<Int, BookmarkStore>()
        val localNameStores = ConcurrentHashMap<Int, LocalNameStore>()
        val modules = ModuleRegistry(
            listOf(
                ServicesModule,
                DeclutterFeature,
                HistoryFeature { account -> historyStores.getOrPut(account) { RoomHistoryStore(application, account) } },
                BookmarksFeature { account -> bookmarkStores.getOrPut(account) { RoomBookmarkStore(application, account) } },
                GhostFeature,
                InputFeature,
                LinksFeature,
                LocalNamesFeature { account -> localNameStores.getOrPut(account) { PreferencesLocalNameStore(application, account) } },
                MessageMenuFeature,
                NotificationsFeature,
                SearchFeature,
                TimestampsFeature,
            ),
        )
        modules.start(ModuleContext(options, Faults::report))
        installSettingsEntry(options, listOf(featuresSection(modules)))
    }

    private fun featuresSection(modules: ModuleRegistry) = SettingsSection(
        header = R.string.serein_settings_features,
        rows = modules.modules.filterIsInstance<SettingsContributor>().map {
            SettingsRow.Subpage(it.settingsPage, it.settingsIcon)
        },
    )
}

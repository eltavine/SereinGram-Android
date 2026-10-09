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
import com.eltavine.sereingram.features.backup.BackupSettings
import com.eltavine.sereingram.features.bookmarks.BookmarksFeature
import com.eltavine.sereingram.features.chatlock.ChatLockFeature
import com.eltavine.sereingram.features.chatshortcuts.ChatShortcutsFeature
import com.eltavine.sereingram.features.declutter.DeclutterFeature
import com.eltavine.sereingram.features.dns.DnsFeature
import com.eltavine.sereingram.features.ghost.GhostFeature
import com.eltavine.sereingram.features.history.HistoryFeature
import com.eltavine.sereingram.features.input.InputFeature
import com.eltavine.sereingram.features.links.LinksFeature
import com.eltavine.sereingram.features.localnames.LocalNamesFeature
import com.eltavine.sereingram.features.messagemenu.MessageMenuFeature
import com.eltavine.sereingram.features.notifications.NotificationsFeature
import com.eltavine.sereingram.features.playback.PlaybackFeature
import com.eltavine.sereingram.features.qrcode.QrCodeFeature
import com.eltavine.sereingram.features.reactions.ReactionsFeature
import com.eltavine.sereingram.features.search.SearchFeature
import com.eltavine.sereingram.features.sending.SendingFeature
import com.eltavine.sereingram.features.services.ServicesModule
import com.eltavine.sereingram.features.timestamps.TimestampsFeature
import com.eltavine.sereingram.features.transcription.TranscriptionFeature
import com.eltavine.sereingram.hooks.LocaleHooks
import com.eltavine.sereingram.ports.BookmarkStore
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.ports.LocalNameStore
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.editors
import com.eltavine.sereingram.settings.featureSections
import com.eltavine.sereingram.support.Logouts
import org.telegram.messenger.FileLog
import java.io.File
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
        LocaleHooks.stringPacks.install(SereinStringPacks)
        options = Options(PreferencesStores(application))
        val modules = modules(application)
        val context = ModuleContext(options, Faults::report)
        modules.start(context)
        Logouts.observe { account -> modules.forgetAccount(account, context) }
        val contributors = modules.modules.filterIsInstance<SettingsContributor>()
        val restarting = contributors.flatMap { it.settingsPage.editors() }.filter { it.restarts }.map { it.option }
        installSettingsEntry(options, featureSections(contributors, ::categoryTitle) + BackupSettings.section(options, modules.options, restarting))
    }

    /** Every module of the app, in start order; building the registry checks their ids and option keys. */
    internal fun modules(application: Application): ModuleRegistry {
        val historyStores = ConcurrentHashMap<Int, HistoryStore>()
        val bookmarkStores = ConcurrentHashMap<Int, BookmarkStore>()
        val localNameStores = ConcurrentHashMap<Int, LocalNameStore>()
        return ModuleRegistry(
            listOf(
                ServicesModule,
                ChatLockFeature,
                ChatShortcutsFeature,
                DeclutterFeature,
                DnsFeature,
                HistoryFeature(File(application.filesDir, "serein_media")) { account ->
                    historyStores.computeIfAbsent(account) { RoomHistoryStore(application, account) }
                },
                BookmarksFeature { account -> bookmarkStores.computeIfAbsent(account) { RoomBookmarkStore(application, account) } },
                GhostFeature,
                InputFeature,
                LinksFeature,
                LocalNamesFeature { account -> localNameStores.computeIfAbsent(account) { PreferencesLocalNameStore(application, account) } },
                MessageMenuFeature,
                NotificationsFeature,
                PlaybackFeature,
                QrCodeFeature,
                ReactionsFeature,
                SearchFeature,
                SendingFeature,
                TimestampsFeature,
                TranscriptionFeature,
            ),
        )
    }
}

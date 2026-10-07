package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.HistoryHooks
import com.eltavine.sereingram.hooks.MessageHooks
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.R
import java.util.concurrent.Executors

/** Saves deleted messages and earlier versions of edited ones, after AyuGram's message history. */
class HistoryFeature(private val stores: (account: Int) -> HistoryStore) : SereinModule, SettingsContributor {
    override val id: String = "history"

    override val options: List<Option<*>> = HistoryOptions.all

    override fun start(context: ModuleContext) {
        val writer = Executors.newSingleThreadExecutor { Thread(it, "serein-history") }
        val recorder = HistoryRecorder(context.options, stores, writer)
        HistoryHooks.deletionListeners.install(recorder::beforeDeleted)
        HistoryHooks.editListeners.install(recorder::beforeEdited)
        val restorer = HistoryRestorer(context.options, stores, KeptMessages())
        HistoryHooks.loadListeners.install(restorer::afterLoaded)
        MessageHooks.timeDecorators.install(restorer::decorateTime)
    }

    override val settingsIcon: Int = R.drawable.msg_recent

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_history_title,
        listOf(
            SettingsSection(
                header = R.string.serein_history_save,
                rows = listOf(
                    SettingsRow.Toggle(HistoryOptions.saveDeleted, R.string.serein_history_save_deleted),
                    SettingsRow.Toggle(HistoryOptions.saveEdits, R.string.serein_history_save_edits),
                    SettingsRow.Toggle(HistoryOptions.saveInBotChats, R.string.serein_history_save_in_bots),
                ),
                note = R.string.serein_history_save_note,
            ),
        ),
    )
}

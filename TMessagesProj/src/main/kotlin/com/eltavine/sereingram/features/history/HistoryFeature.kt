package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.HistoryHooks
import com.eltavine.sereingram.hooks.MessageHooks
import com.eltavine.sereingram.hooks.MessageMenuHooks
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
        val kept = MessageMap<Long>()
        val revised = MessageSet()
        val deletedByUser = MessageSet()
        val recorder = HistoryRecorder(context.options, stores, writer, revised, deletedByUser)
        HistoryHooks.userDeletionListeners.install(recorder::beforeUserDeletes)
        HistoryHooks.deletionListeners.install(recorder::beforeDeleted)
        HistoryHooks.editListeners.install(recorder::beforeEdited)
        val restorer = HistoryRestorer(context.options, stores, kept, revised)
        HistoryHooks.loadListeners.install(restorer::afterLoaded)
        HistoryHooks.chatKeepers.install(KeptInChat(context.options, kept, deletedByUser))
        MessageHooks.timeDecorators.install(restorer::decorateTime)
        MessageMenuHooks.entries.install(EditHistoryEntry(stores, revised))
        MessageMenuHooks.entries.install(DeletedAtEntry(kept))
        ChatMenuHooks.entries.install(DeletedMessagesEntry(context.options, stores))
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
            SettingsSection(
                header = R.string.serein_history_appearance,
                rows = listOf(
                    SettingsRow.Text(HistoryOptions.deletedMark, R.string.serein_history_deleted_mark_title, R.string.serein_history_deleted_mark),
                ),
                note = R.string.serein_history_deleted_mark_note,
            ),
        ),
    )
}

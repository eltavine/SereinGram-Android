package com.eltavine.sereingram.features.history

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.hooks.MessageMenuHooks
import com.eltavine.sereingram.ports.HistoryRecord
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.ui.SettingsListFragment
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.LocaleController
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessageObject
import org.telegram.messenger.R
import org.telegram.messenger.Utilities
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter

/** "Edit history" in the message menu, shown on messages that have earlier versions kept. */
internal class EditHistoryEntry(
    private val stores: (account: Int) -> HistoryStore,
    private val revised: MessageSet,
) : MessageMenuHooks.Entry {
    override val option: Int = MessageMenuHooks.FIRST_OPTION + 1

    override val icon: Int = R.drawable.msg_recent

    override fun title(account: Int, message: Any): CharSequence = getString(R.string.serein_history_edit_history)

    override fun isShown(account: Int, message: Any): Boolean {
        val shown = message as MessageObject
        return isInHistory(shown) && revised.contains(account, shown.dialogId, shown.id)
    }

    override fun onSelected(account: Int, message: Any, host: Any) {
        val shown = message as MessageObject
        (host as BaseFragment).presentFragment(EditHistoryActivity(stores(account), shown.dialogId, shown.id))
    }
}

/** The kept earlier versions of one message, oldest first. */
internal class EditHistoryActivity(
    private val store: HistoryStore,
    private val dialogId: Long,
    private val messageId: Int,
) : SettingsListFragment() {
    private var revisions: List<HistoryRecord>? = null

    override fun onFragmentCreate(): Boolean {
        Utilities.globalQueue.postRunnable {
            val loaded = Faults.guard("edit history", fallback = emptyList()) { store.revisions(dialogId, messageId) }
            AndroidUtilities.runOnUIThread {
                revisions = loaded
                listView?.adapter?.update(true)
            }
        }
        return super.onFragmentCreate()
    }

    override fun getTitle(): CharSequence = getString(R.string.serein_history_versions_title)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        val revisions = revisions ?: return
        if (revisions.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.serein_history_versions_empty)))
            return
        }
        revisions.forEach { record ->
            items.add(UItem.asHeader(label(record)))
            items.add(UItem.asCustom(text(record)))
            items.add(UItem.asShadow(null))
        }
    }

    private fun label(record: HistoryRecord): String = if (record.revision == 0) {
        LocaleController.formatString(R.string.serein_history_version_original, LocaleController.formatDateTime(record.date.toLong(), true))
    } else {
        LocaleController.formatString(R.string.serein_history_version_edited, LocaleController.formatDateTime(record.revision.toLong(), true))
    }

    private fun text(record: HistoryRecord): View = TextView(context).apply {
        text = record.text.ifEmpty { getString(R.string.serein_history_version_no_text) }
        setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        setPadding(dp(21f), dp(4f), dp(21f), dp(14f))
        setTextIsSelectable(true)
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) = Unit

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean = false
}

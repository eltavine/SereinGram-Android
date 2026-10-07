package com.eltavine.sereingram.features.ghost

import android.widget.LinearLayout
import com.eltavine.sereingram.hooks.ChatMenuHooks
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Components.LayoutHelper

/** "Ghost mode in this chat" in a chat's menu: exceptions for the chat, and marking it read. */
internal class GhostChatEntry(private val gate: GhostGate) : ChatMenuHooks.Entry {
    override val id: Int = ChatMenuHooks.FIRST_ID + 1

    override val icon: Int = R.drawable.icon_ghost

    override fun title(account: Int, dialogId: Long): CharSequence = getString(R.string.serein_ghost_chat)

    override fun isShown(account: Int, dialogId: Long): Boolean =
        (NagramGhost.readsHidden || NagramGhost.typingHidden) && dialogId != UserConfig.getInstance(account).clientUserId

    override fun onSelected(account: Int, dialogId: Long, chat: Any) {
        val fragment = chat as BaseFragment
        val context = fragment.parentActivity ?: return
        val resources = fragment.resourceProvider
        val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        GhostAction.entries.forEach { action ->
            val cell = TextCheckCell(context, resources)
            cell.background = Theme.getSelectorDrawable(false)
            cell.setTextAndCheck(getString(label(action)), gate.isExcepted(account, dialogId, action), action == GhostAction.READ)
            cell.setOnClickListener {
                val excepted = !cell.isChecked
                gate.setExcepted(account, dialogId, action, excepted)
                cell.isChecked = excepted
            }
            rows.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        }
        val builder = AlertDialog.Builder(context, resources)
            .setTitle(getString(R.string.serein_ghost_chat))
            .setMessage(getString(R.string.serein_ghost_chat_note))
            .setView(rows)
            .setPositiveButton(getString(R.string.OK), null)
        if (NagramGhost.readsHidden) {
            builder.setNeutralButton(getString(R.string.serein_ghost_mark_read)) { _, _ -> gate.markRead(account, dialogId) }
        }
        fragment.showDialog(builder.create())
    }

    private fun label(action: GhostAction): Int = when (action) {
        GhostAction.READ -> R.string.serein_ghost_chat_reads
        GhostAction.TYPING -> R.string.serein_ghost_chat_typing
    }
}

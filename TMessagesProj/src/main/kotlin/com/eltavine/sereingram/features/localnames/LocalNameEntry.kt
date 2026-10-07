package com.eltavine.sereingram.features.localnames

import android.util.TypedValue
import android.widget.FrameLayout
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.support.Chats
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.DialogObject
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper

/** "Local name" in the menu of a chat with a person, a group or a channel. */
internal class LocalNameEntry(private val feature: LocalNamesFeature) : ChatMenuHooks.Entry {
    override val id: Int = ChatMenuHooks.FIRST_ID + 4

    override val icon: Int = R.drawable.msg_edit

    override fun title(): CharSequence = getString(R.string.serein_local_name)

    override fun isShown(account: Int, dialogId: Long): Boolean =
        !DialogObject.isEncryptedDialog(dialogId) && dialogId != UserConfig.getInstance(account).clientUserId

    override fun onSelected(account: Int, dialogId: Long, chat: Any) {
        val fragment = chat as BaseFragment
        val context = fragment.parentActivity ?: return
        val current = feature.names.of(account, dialogId)
        val field = EditTextBoldCursor(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setTextColor(Theme.getColor(Theme.key_dialogTextBlack, fragment.resourceProvider))
            setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, fragment.resourceProvider))
            background = Theme.createEditTextDrawable(context, true)
            setSingleLine(true)
            setPadding(0, dp(4f), 0, dp(4f))
            hint = feature.originals.of(account, dialogId) ?: Chats.name(account, dialogId)
            setText(current.orEmpty())
            setSelection(length())
        }
        val frame = FrameLayout(context)
        frame.addView(field, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat(), 0, 24f, 6f, 24f, 0f))
        val builder = AlertDialog.Builder(context, fragment.resourceProvider)
            .setTitle(getString(R.string.serein_local_name))
            .setMessage(getString(R.string.serein_local_name_note))
            .setView(frame)
            .setPositiveButton(getString(R.string.OK)) { _, _ -> feature.set(account, dialogId, field.text.toString()) }
            .setNegativeButton(getString(R.string.Cancel), null)
        if (current != null) {
            builder.setNeutralButton(getString(R.string.serein_local_name_remove)) { _, _ -> feature.set(account, dialogId, null) }
        }
        fragment.showDialog(builder.create())
        field.requestFocus()
        AndroidUtilities.runOnUIThread({ AndroidUtilities.showKeyboard(field) }, 200)
    }
}

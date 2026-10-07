package com.eltavine.sereingram.features.bookmarks

import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.MessageMenuHooks
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessageObject
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.BulletinFactory

/** "Bookmark" or "Remove bookmark" in the menu of a message. */
internal class BookmarkMessageEntry(private val feature: BookmarksFeature) : MessageMenuHooks.Entry {
    override val option: Int = MessageMenuHooks.FIRST_OPTION + 201

    override val icon: Int = R.drawable.msg_fave

    // Messages of secret chats and ones not yet sent have no lasting id.
    override fun isShown(account: Int, message: Any): Boolean = (message as MessageObject).id > 0 && !message.isSponsored

    override fun title(account: Int, message: Any): CharSequence {
        val shown = message as MessageObject
        val bookmarked = feature.isBookmarked(account, shown.dialogId, shown.id)
        return getString(if (bookmarked) R.string.serein_bookmarks_remove else R.string.serein_bookmarks_add)
    }

    override fun onSelected(account: Int, message: Any, host: Any) {
        val shown = message as MessageObject
        val chat = host as BaseFragment
        val bulletin = if (feature.toggle(account, shown)) {
            BulletinFactory.of(chat).createSimpleBulletin(
                R.raw.saved_messages,
                getString(R.string.serein_bookmarks_added),
                getString(R.string.serein_bookmarks_view),
            ) { chat.presentFragment(BookmarksActivity(feature, shown.dialogId, chat as? ChatActivity)) }
        } else {
            BulletinFactory.of(chat).createSimpleBulletin(R.raw.ic_delete, getString(R.string.serein_bookmarks_removed))
        }
        bulletin.show()
    }
}

/** "Bookmarks" in the menu of a chat that has some. */
internal class BookmarksChatEntry(private val feature: BookmarksFeature) : ChatMenuHooks.Entry {
    override val id: Int = ChatMenuHooks.FIRST_ID + 2

    override val icon: Int = R.drawable.msg_fave

    override fun title(account: Int, dialogId: Long): CharSequence = getString(R.string.serein_bookmarks_title)

    override fun isShown(account: Int, dialogId: Long): Boolean = feature.hasAny(account, dialogId)

    override fun onSelected(account: Int, dialogId: Long, chat: Any) {
        (chat as BaseFragment).presentFragment(BookmarksActivity(feature, dialogId, chat as? ChatActivity))
    }
}

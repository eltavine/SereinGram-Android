package com.eltavine.sereingram.features.join

import android.content.Intent
import android.net.Uri
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.JoinHooks
import org.telegram.messenger.*
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.LocaleController.formatString
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.AlertsCreator
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.LaunchActivity
import tw.nekomimi.nekogram.NekoXConfig

/** A server rejection is not evidence that the user's account has exactly 500 memberships. */
object JoinFeature : SereinModule {
    override val id: String = "join_errors"
    override fun start(context: ModuleContext) {
        JoinHooks.handlers.install { account, request, error, host ->
            if (!isJoinLimit(request, error)) return@install false
            val page = host as? BaseFragment ?: LaunchActivity.getLastFragmentIncludeMainTabs() ?: return@install false
            if (page.parentActivity == null) return@install false
            show(account, request as TLObject, page)
            true
        }
    }

    internal fun isJoinLimit(request: Any, error: String): Boolean = error == "CHANNELS_TOO_MUCH" &&
        (request is TLRPC.TL_channels_joinChannel || request is TLRPC.TL_messages_importChatInvite)

    private fun show(account: Int, request: TLObject, page: BaseFragment) {
        val activity = page.parentActivity ?: return
        val owner = UserConfig.getInstance(account).clientUserId
        val detail = formatString(R.string.serein_join_explanation, owner.toString(), NekoXConfig.currentAppId())
        page.showDialog(AlertDialog.Builder(activity, page.resourceProvider).setTitle(getString(R.string.serein_join_title))
            .setMessage(detail)
            .setItems(arrayOf(getString(R.string.serein_join_diagnose), getString(R.string.serein_join_retry), getString(R.string.serein_join_official))) { _, which ->
                if (UserConfig.getInstance(account).clientUserId != owner) return@setItems
                when (which) {
                    0 -> JoinDiagnostics(account, owner).run { count, complete, failure ->
                        if (page.isFinished || page.parentActivity == null || UserConfig.getInstance(account).clientUserId != owner) return@run
                        JoinSessionDiagnostics(account, owner).run session@ { apiId ->
                            if (page.isFinished || page.parentActivity == null || UserConfig.getInstance(account).clientUserId != owner) return@session
                            val report = detail + "\n\n" + formatString(R.string.serein_join_count, count,
                                getString(if (complete) R.string.serein_join_complete else R.string.serein_join_partial)) +
                                (failure?.let { "\n${getString(R.string.ErrorOccurred)}: $it" } ?: "") + "\n\n" +
                                (apiId?.let { formatString(R.string.serein_join_session_api, it) }
                                    ?: getString(R.string.serein_join_session_unknown))
                            page.showDialog(AlertDialog.Builder(page.parentActivity, page.resourceProvider)
                                .setTitle(getString(R.string.serein_join_diagnose)).setMessage(report)
                                .setPositiveButton(getString(R.string.Copy)) { _, _ -> AndroidUtilities.addToClipboard(report) }
                                .setNegativeButton(getString(R.string.Close), null).create())
                        }
                    }
                    1 -> retry(account, owner, request, page)
                    2 -> openOfficial(account, request, page)
                }
            }.setNegativeButton(getString(R.string.Cancel), null).create())
    }

    private fun retry(account: Int, owner: Long, request: TLObject, page: BaseFragment) {
        BulletinFactory.of(page).createSimpleBulletin(R.raw.done, getString(R.string.serein_join_retrying)).show()
        ConnectionsManager.getInstance(account).sendRequest(request, { response, error ->
            if (UserConfig.getInstance(account).clientUserId != owner) return@sendRequest
            val updates = when (response) {
                is TLRPC.TL_chatInviteJoinResultOk -> response.updates
                is TLRPC.Updates -> response
                else -> null
            }
            if (updates != null) MessagesController.getInstance(account).processUpdates(updates, false)
            AndroidUtilities.runOnUIThread {
                if (page.isFinished || page.parentActivity == null || UserConfig.getInstance(account).clientUserId != owner) return@runOnUIThread
                when {
                    error?.text == "INVITE_REQUEST_SENT" -> BulletinFactory.of(page).createSimpleBulletin(R.raw.done, getString(R.string.serein_join_requested)).show()
                    error != null -> AlertsCreator.processError(account, error, page, request)
                    response is TLRPC.TL_chatInviteJoinResultWebView -> {
                        MessagesController.getInstance(account).putUsers(response.users, false)
                        val dialog = (request as? TLRPC.TL_channels_joinChannel)?.channel?.channel_id?.let { -it } ?: 0L
                        BotGuardHelper.getInstance(account).openGuardBotWebApp(dialog, response.bot_id, response.query_id)
                    }
                    updates != null -> {
                        val controller = MessagesController.getInstance(account)
                        controller.putUsers(updates.users, false)
                        controller.putChats(updates.chats, false)
                        val chat = updates.chats.firstOrNull { !it.left && !it.kicked }
                        if (chat != null) page.presentFragment(ChatActivity.of(-chat.id).apply { setCurrentAccount(account) })
                        else BulletinFactory.of(page).createSimpleBulletin(R.raw.done, getString(R.string.serein_join_updated)).show()
                    }
                    else -> BulletinFactory.of(page).createSimpleBulletin(R.raw.error, getString(R.string.ErrorOccurred)).show()
                }
            }
        }, ConnectionsManager.RequestFlagFailOnServerErrors)
    }

    private fun openOfficial(account: Int, request: TLObject, page: BaseFragment) {
        val link = when (request) {
            is TLRPC.TL_messages_importChatInvite -> "https://t.me/+${request.hash}"
            is TLRPC.TL_channels_joinChannel -> MessagesController.getInstance(account).getChat(request.channel.channel_id)
                ?.username?.takeIf { it.isNotEmpty() }?.let { "https://t.me/$it" }
            else -> null
        }
        if (link != null) {
            for (packageName in listOf("org.telegram.messenger", "org.telegram.messenger.web")) {
                try { page.parentActivity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(packageName)); return }
                catch (_: android.content.ActivityNotFoundException) { }
            }
        }
        BulletinFactory.of(page).createSimpleBulletin(R.raw.error, getString(R.string.serein_join_official_missing)).show()
    }
}

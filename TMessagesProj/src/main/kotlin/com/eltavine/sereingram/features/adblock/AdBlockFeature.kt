package com.eltavine.sereingram.features.adblock

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.FrameLayout
import android.text.InputType
import androidx.core.content.FileProvider
import com.eltavine.sereingram.core.*
import com.eltavine.sereingram.hooks.AdBlockHooks
import com.eltavine.sereingram.hooks.MessageMenuHooks
import com.eltavine.sereingram.settings.*
import com.eltavine.sereingram.support.HttpDns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.source
import org.telegram.messenger.*
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.LocaleController.formatString
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.LaunchActivity
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class AdBlockFeature(private val application: Application) : SereinModule, SettingsContributor {
    override val id: String = "adblock"
    override val options: List<Option<*>> = AdBlockOptions.all
    private lateinit var preferences: Options
    internal val store = AdBlockStore(application, ::changed)
    private val client by lazy { OkHttpClient.Builder().dns(HttpDns).followRedirects(false).followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build() }

    override fun start(context: ModuleContext) {
        preferences = context.options
        AdBlockHooks.filters.install(::hides)
        AdBlockHooks.sponsoredPolicies.install { account -> preferences.get(AdBlockOptions.enabled, account) }
        MessageMenuHooks.entries.install(MarkAd())
        preferences.addListener { option, account ->
            if (option == AdBlockOptions.enabled || option == AdBlockOptions.bundled) {
                if (preferences.get(AdBlockOptions.enabled, account)) load(account)
                changed(account)
            }
        }
        Handler(Looper.getMainLooper()).post {
            for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                if (preferences.get(AdBlockOptions.enabled, account) && UserConfig.getInstance(account).isClientActivated) load(account)
            }
        }
    }

    override fun forgetAccount(account: Int) = store.forget(account)
    internal fun load(account: Int) = store.load(account, UserConfig.getInstance(account).clientUserId)

    private fun hides(account: Int, message: Any, album: Any?): Boolean {
        if (!preferences.get(AdBlockOptions.enabled, account)) return false
        val shown = message as? MessageObject ?: return false
        val ownerId = UserConfig.getInstance(account).clientUserId
        if (ownerId == 0L) return false
        if (store.peek(account)?.owner != ownerId && Looper.myLooper() != Looper.getMainLooper()) store.ensure(account, ownerId)
        val candidates = when (album) {
            is MessageObject.GroupedMessages -> album.messages
            is List<*> -> if (shown.groupId != 0L) album.filterIsInstance<MessageObject>()
                .filter { it.groupId == shown.groupId && it.dialogId == shown.dialogId } else listOf(shown)
            else -> listOf(shown)
        }
        return candidates.any { item ->
            if (!eligible(item)) return@any false
            val owner = item.messageOwner
            val urls = owner.entities.mapNotNull {
                when (it) {
                    is TLRPC.TL_messageEntityTextUrl -> it.url
                    is TLRPC.TL_messageEntityUrl -> owner.message?.let { text ->
                        if (it.offset >= 0 && it.length > 0 && it.offset <= text.length - it.length) text.substring(it.offset, it.offset + it.length) else null
                    }
                    else -> null
                }
            }.take(32)
            store.match(account, UserConfig.getInstance(account).clientUserId,
                AdMessage(item.dialogId, item.id, item.senderId, textOf(item).take(32_768), urls),
                preferences.get(AdBlockOptions.bundled, account)) != null
        }
    }

    private fun eligible(message: MessageObject): Boolean = message.id > 0 && message.dialogId < 0 &&
        !DialogObject.isEncryptedDialog(message.dialogId) && !message.isOutOwner && !message.scheduled &&
        message.messageOwner !is TLRPC.TL_messageService && !message.isSponsored

    private fun changed(account: Int) = AndroidUtilities.runOnUIThread {
        when (val page = LaunchActivity.getLastFragmentIncludeMainTabs()) {
            is AdBlockListActivity -> if (page.currentAccount == account) page.reload()
            is ChatActivity -> if (page.currentAccount == account) page.getChatListView()?.adapter?.notifyDataSetChanged()
        }
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload)
    }

    override val settingsIcon: Int = R.drawable.msg_block
    override val settingsCategory: SettingsCategory = SettingsCategory.MESSAGES
    override val settingsTint: SettingsTint = SettingsTint.MINT
    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_adblock_title,
        summary = R.string.serein_adblock_summary,
        status = { getString(if (it[AdBlockOptions.enabled]) R.string.serein_settings_on else R.string.serein_downloads_off) },
        sections = listOf(
            SettingsSection(rows = listOf(
                SettingsRow.Toggle(AdBlockOptions.enabled, R.string.serein_adblock_enabled, summary = R.string.serein_adblock_enabled_info),
                SettingsRow.Toggle(AdBlockOptions.bundled, R.string.serein_adblock_builtin, summary = R.string.serein_adblock_builtin_info),
                SettingsRow.Screen(R.string.serein_adblock_rules, { AdBlockListActivity(this, false) }, summary = R.string.serein_adblock_rules_info),
                SettingsRow.Screen(R.string.serein_adblock_history, { AdBlockListActivity(this, true) }, summary = R.string.serein_adblock_history_info),
                SettingsRow.Action(R.string.serein_adblock_add, { addKeyword(it as BaseFragment) }, summary = R.string.serein_adblock_add_info),
            ), note = R.string.serein_adblock_note),
            SettingsSection(rows = listOf(
                SettingsRow.Action(R.string.serein_adblock_subscription, { editSubscription(it as BaseFragment) },
                    summary = R.string.serein_adblock_subscription_info),
                SettingsRow.Action(R.string.serein_adblock_update, { update(it as BaseFragment) }, summary = R.string.serein_adblock_update_info),
                SettingsRow.Action(R.string.serein_adblock_recommended, { host ->
                    val page = host as BaseFragment
                    preferences.set(AdBlockOptions.subscription, AdRules.RECOMMENDED_URL, page.currentAccount)
                    update(page)
                }, summary = R.string.serein_adblock_recommended_info),
                SettingsRow.Action(R.string.serein_adblock_remove_subscription, { host -> task(host as BaseFragment) { account, owner ->
                    store.replaceRemote(account, AdRuleSet("Subscription"), owner)
                    AndroidUtilities.runOnUIThread { if (UserConfig.getInstance(account).clientUserId == owner) preferences.reset(AdBlockOptions.lastUpdate, account) }
                    getString(R.string.serein_adblock_removed)
                } }, summary = R.string.serein_adblock_remove_subscription_info),
                SettingsRow.Link(R.string.serein_adblock_source, "https://github.com/hagezi/dns-blocklists#popupads", summary = R.string.serein_adblock_source_info),
            ), note = R.string.serein_adblock_subscription_note),
            SettingsSection(rows = listOf(
                SettingsRow.PickFile(R.string.serein_adblock_import, listOf("application/json", "text/plain", "application/octet-stream"),
                    { host, uri -> import(host as BaseFragment, Uri.parse(uri)) }, summary = R.string.serein_adblock_import_info),
                SettingsRow.Action(R.string.serein_adblock_export, { export(it as BaseFragment) }, summary = R.string.serein_adblock_export_info),
                SettingsRow.Action(R.string.serein_adblock_format, { showFormat(it as BaseFragment) }, summary = R.string.serein_adblock_format_info),
            )),
        ),
    )

    private fun update(page: BaseFragment) {
        val url = preferences.get(AdBlockOptions.subscription, page.currentAccount)
        if (!validSubscription(url)) { bulletin(page, getString(R.string.serein_adblock_invalid_url), false); return }
        bulletin(page, getString(R.string.serein_adblock_updating), true)
        task(page) { account, owner ->
            val set = client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                require(response.isSuccessful) { "HTTP ${response.code}" }
                val body = requireNotNull(response.body) { "Empty response" }
                val source = body.source()
                require(!source.request(AdRules.MAX_BYTES + 1)) { "Rule file exceeds 4 MiB" }
                AdRules.parse(source.readUtf8())
            }
            require(preferences.get(AdBlockOptions.subscription, account) == url) { "Subscription changed; update discarded" }
            require(UserConfig.getInstance(account).clientUserId == owner) { "Account changed; update discarded" }
            store.replaceRemote(account, if (url == AdRules.RECOMMENDED_URL) set.copy(name = "HaGeZi Pop-Up Ads") else set, owner)
            AndroidUtilities.runOnUIThread { if (UserConfig.getInstance(account).clientUserId == owner) preferences.set(AdBlockOptions.lastUpdate, System.currentTimeMillis(), account) }
            formatString(R.string.serein_adblock_loaded, set.rules.size, set.domains.size)
        }
    }

    private fun editSubscription(page: BaseFragment) {
        val activity = page.parentActivity ?: return
        val account = page.currentAccount
        val field = EditTextBoldCursor(activity).apply {
            setTextSize(16f)
            setTextColor(Theme.getColor(Theme.key_dialogTextBlack, page.resourceProvider))
            background = Theme.createEditTextDrawable(activity, true)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(preferences.get(AdBlockOptions.subscription, account))
            setSelection(length())
        }
        val frame = FrameLayout(activity).apply {
            addView(field, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat(), 0, 24f, 0f, 24f, 0f))
        }
        lateinit var dialog: AlertDialog
        dialog = AlertDialog.Builder(activity, page.resourceProvider).setTitle(getString(R.string.serein_adblock_subscription))
            .setMessage(getString(R.string.serein_adblock_subscription_info)).setView(frame)
            .setPositiveButton(getString(R.string.Save)) { _, _ ->
                val value = field.text.toString().trim()
                if (value.isNotEmpty() && !validSubscription(value)) bulletin(page, getString(R.string.serein_adblock_invalid_url), false)
                else { preferences.set(AdBlockOptions.subscription, value, account); dialog.dismiss() }
            }.setNegativeButton(getString(R.string.Cancel)) { shown, _ -> shown.dismiss() }.create()
        dialog.setDismissDialogByButtons(false)
        page.showDialog(dialog)
    }

    private fun import(page: BaseFragment, uri: Uri) = task(page) { account, owner ->
        val set = requireNotNull(application.contentResolver.openInputStream(uri)).source().buffer().use { source ->
            require(!source.request(AdRules.MAX_BYTES + 1)) { "Rule file exceeds 4 MiB" }
            AdRules.parse(source.readUtf8())
        }
        store.replaceImported(account, set, owner)
        formatString(R.string.serein_adblock_loaded, set.rules.size, set.domains.size)
    }

    private fun export(page: BaseFragment) = task(page) { account, owner ->
        val file = File(application.cacheDir, "media/adblock-rules-${UUID.randomUUID()}.json").apply { parentFile!!.mkdirs(); writeText(store.export(account, owner)) }
        AndroidUtilities.runOnUIThread {
            if (page.isFinished || UserConfig.getInstance(account).clientUserId != owner) return@runOnUIThread
            val uri = FileProvider.getUriForFile(application, ApplicationLoader.getApplicationId() + ".provider", file)
            page.parentActivity?.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json")
                .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), getString(R.string.serein_adblock_export)))
        }
        getString(R.string.serein_adblock_export)
    }

    private fun addKeyword(page: BaseFragment) {
        val activity = page.parentActivity ?: return
        val field = EditText(activity).apply { hint = getString(R.string.serein_adblock_keyword_hint); setSingleLine(true) }
        page.showDialog(AlertDialog.Builder(activity, page.resourceProvider).setTitle(getString(R.string.serein_adblock_add))
            .setMessage(getString(R.string.serein_adblock_add_info)).setView(field)
            .setPositiveButton(getString(R.string.Add)) { _, _ ->
                val text = field.text.toString().trim()
                if (text.length !in 2..200) bulletin(page, getString(R.string.serein_adblock_bad_keyword), false)
                else task(page) { account, owner ->
                    store.add(account, AdRule(UUID.randomUUID().toString(), text, RuleKind.CONTAINS, text), owner)
                    getString(R.string.serein_adblock_added)
                }
            }.setNegativeButton(getString(R.string.Cancel), null).create())
    }

    private fun showFormat(page: BaseFragment) {
        val activity = page.parentActivity ?: return
        val sample = AdRules.encode(AdRuleSet("Example", listOf(AdRule("sample", "Example ad", RuleKind.PREFIX, "[AD]"))))
        page.showDialog(AlertDialog.Builder(activity, page.resourceProvider).setTitle(getString(R.string.serein_adblock_format))
            .setMessage(getString(R.string.serein_adblock_format_note) + "\n\n" + sample)
            .setPositiveButton(getString(R.string.Copy)) { _, _ -> AndroidUtilities.addToClipboard(sample) }
            .setNegativeButton(getString(R.string.Close), null).create())
    }

    internal fun task(page: BaseFragment, action: (Int, Long) -> String) {
        val account = page.currentAccount
        val owner = UserConfig.getInstance(account).clientUserId
        store.io.execute {
            val result = runCatching {
                require(owner != 0L && UserConfig.getInstance(account).clientUserId == owner) { "Account changed" }
                store.ensure(account, owner)
                action(account, owner)
            }
            AndroidUtilities.runOnUIThread {
                if (!page.isFinished && page.currentAccount == account && UserConfig.getInstance(account).clientUserId == owner) bulletin(page,
                    result.getOrElse { formatString(R.string.serein_adblock_failed, it.message.orEmpty().take(180)) }, result.isSuccess)
            }
            result.exceptionOrNull()?.let { Faults.report("adblock action", it) }
        }
    }

    private inner class MarkAd : MessageMenuHooks.Entry {
        override val option: Int = MessageMenuHooks.FIRST_OPTION + 701
        override val icon: Int = R.drawable.msg_block
        override fun title(account: Int, message: Any): CharSequence = getString(R.string.serein_adblock_mark)
        override fun isShown(account: Int, message: Any): Boolean = (message as? MessageObject)?.let(::eligible) == true
        override fun onSelected(account: Int, message: Any, host: Any) {
            val page = host as BaseFragment
            val shown = message as MessageObject
            val activity = page.parentActivity ?: return
            val text = textOf(shown)
            val choices = mutableListOf(getString(R.string.serein_adblock_mark_one) to RuleKind.MESSAGE)
            if (text.isNotBlank()) choices += getString(R.string.serein_adblock_mark_text) to RuleKind.TEXT_HASH
            if (shown.senderId > 0L) choices += getString(R.string.serein_adblock_mark_sender) to RuleKind.SENDER
            page.showDialog(AlertDialog.Builder(activity, page.resourceProvider).setTitle(getString(R.string.serein_adblock_mark))
                .setItems(choices.map { it.first }.toTypedArray()) { _, index ->
                    task(page) { current, owner ->
                        val kind = choices[index].second
                        val value = when (kind) {
                            RuleKind.MESSAGE -> shown.id.toString()
                            RuleKind.TEXT_HASH -> adTextHash(text)
                            else -> shown.senderId.toString()
                        }
                        store.add(current, AdRule("manual_${shown.dialogId}_${shown.id}_${kind.name}", choices[index].first,
                            kind, value, shown.dialogId), owner, shown.id)
                        AndroidUtilities.runOnUIThread { if (UserConfig.getInstance(current).clientUserId == owner) preferences.set(AdBlockOptions.enabled, true, current) }
                        getString(R.string.serein_adblock_added)
                    }
                }.setNegativeButton(getString(R.string.Cancel), null).create())
        }
    }

    private fun bulletin(page: BaseFragment, text: String, success: Boolean) =
        BulletinFactory.of(page).createSimpleBulletin(if (success) R.raw.done else R.raw.error, text).show()

    internal companion object {
        private fun textOf(message: MessageObject): String = message.messageOwner.message?.takeIf { it.isNotBlank() }
            ?: message.messageText?.toString().orEmpty()
        fun validSubscription(value: String): Boolean = value.length <= 2048 && value.toHttpUrlOrNull()?.let {
            it.isHttps && it.username.isEmpty() && it.password.isEmpty() && it.fragment == null
        } == true
    }
}

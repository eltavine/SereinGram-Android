package com.eltavine.sereingram.features.adblock

import android.view.View
import com.eltavine.sereingram.support.Chats
import com.eltavine.sereingram.ui.SettingsListFragment
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.LocaleController.formatString
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter

internal class AdBlockListActivity(private val feature: AdBlockFeature, private val history: Boolean) : SettingsListFragment() {
    private var rules = emptyList<Pair<AdRule, Boolean>>()
    private var records = emptyList<BlockRecord>()
    override fun getTitle(): CharSequence = getString(if (history) R.string.serein_adblock_history else R.string.serein_adblock_rules)

    override fun onResume() { super.onResume(); feature.load(currentAccount); reload() }
    fun reload() { listView?.adapter?.update(true) }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        val state = feature.store.peek(currentAccount)?.takeIf { it.owner == UserConfig.getInstance(currentAccount).clientUserId }
        if (state == null) { items += UItem.asShadow(getString(R.string.Loading)); return }
        synchronized(state) {
            if (history) {
                records = state.records.values.toList().asReversed()
                items += UItem.asShadow(getString(R.string.serein_adblock_history_info))
                records.forEachIndexed { i, row ->
                    items += UItem.asButton(i + 1, "${row.rule}: ${row.text.ifBlank { row.message.toString() }}",
                        LocaleController.formatShortDateTime(row.time / 1000))
                }
                if (records.isEmpty()) items += UItem.asShadow(getString(R.string.serein_adblock_no_records))
                items += UItem.asButton(-1, getString(R.string.serein_adblock_clear_history))
                items += UItem.asButton(-2, getString(R.string.serein_adblock_clear_allowed))
            } else {
                rules = state.manual.map { it to true } + state.imported.rules.map { it to false }
                items += UItem.asShadow(formatString(R.string.serein_adblock_counts, state.manual.size,
                    state.imported.rules.size + state.imported.domains.size, state.remote.rules.size + state.remote.domains.size))
                rules.forEachIndexed { i, (rule, manual) ->
                    items += UItem.asButton(i + 1, rule.label, getString(if (manual) R.string.serein_adblock_personal else R.string.serein_adblock_imported))
                }
                items += UItem.asShadow(getString(R.string.serein_adblock_manage_note))
                items += UItem.asButton(-3, getString(R.string.serein_adblock_clear_imported))
            }
        }
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        val context = parentActivity ?: return
        if (item.id < 0) {
            val title = when (item.id) {
                -1 -> R.string.serein_adblock_clear_history
                -2 -> R.string.serein_adblock_clear_allowed
                else -> R.string.serein_adblock_clear_imported
            }
            showDialog(AlertDialog.Builder(context, resourceProvider).setTitle(getString(title))
                .setMessage(getString(R.string.serein_adblock_confirm_clear))
                .setPositiveButton(getString(R.string.Clear)) { _, _ -> feature.task(this) { account, owner ->
                    when (item.id) {
                        -1 -> feature.store.clearHistory(account, owner)
                        -2 -> feature.store.clearAllowed(account, owner)
                        else -> feature.store.clearImported(account, owner)
                    }
                    getString(R.string.serein_adblock_removed)
                } }.setNegativeButton(getString(R.string.Cancel), null).create())
        } else if (history) {
            val row = records.getOrNull(item.id - 1) ?: return
            showDialog(AlertDialog.Builder(context, resourceProvider).setTitle(getString(R.string.serein_adblock_history))
                .setMessage(formatString(R.string.serein_adblock_record, row.source, row.rule, row.dialog.toString(), row.text))
                .setPositiveButton(getString(R.string.serein_adblock_restore)) { _, _ -> feature.task(this) { account, owner ->
                    feature.store.allow(account, row, owner)
                    AndroidUtilities.runOnUIThread {
                        if (!isFinished && UserConfig.getInstance(account).clientUserId == owner) presentFragment(Chats.screen(row.dialog, row.message).apply { setCurrentAccount(account) })
                    }
                    getString(R.string.serein_adblock_restored)
                } }.setNegativeButton(getString(R.string.Close), null).create())
        } else {
            val (rule, manual) = rules.getOrNull(item.id - 1) ?: return
            showDialog(AlertDialog.Builder(context, resourceProvider).setTitle(rule.label)
                .setMessage(AdRules.encode(AdRuleSet("Rule", listOf(rule))))
                .setPositiveButton(getString(R.string.Delete)) { _, _ -> feature.task(this) { account, owner ->
                    feature.store.remove(account, rule.id, manual, owner)
                    getString(R.string.serein_adblock_removed)
                } }.setNegativeButton(getString(R.string.Cancel), null).create())
        }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean {
        onClick(item, view, position, x, y)
        return true
    }
}

package com.eltavine.sereingram.features.declutter

import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.SettingsHooks
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.SettingsActivity

/** Removes the Premium and Help groups from Telegram's main settings list when asked to. */
internal class MainSettingsDeclutter(private val options: Options) : SettingsHooks.MainSettingsFilter {
    override fun filter(items: MutableList<Any?>) {
        val hidePremium = options.get(DeclutterOptions.hidePremiumSection)
        val hideHelp = options.get(DeclutterOptions.hideHelpSection)
        if (!hidePremium && !hideHelp) {
            return
        }
        val helpHeader = getString(R.string.SettingsHelp)
        items.removeAll { item ->
            item is UItem && (
                hidePremium && (item.isSettingRow(PREMIUM_ROWS) || item.`object` is TLRPC.TL_attachMenuBot) ||
                    hideHelp && (item.isSettingRow(HELP_ROWS) || item.isHeader(helpHeader))
                )
        }
        tidy(items)
    }

    private fun UItem.isSettingRow(ids: Set<Int>) = id in ids && instanceOf(SettingsActivity.SettingCell.Factory::class.java)

    private fun UItem.isHeader(text: String) = viewType == UniversalAdapter.VIEW_TYPE_HEADER && this.text?.toString() == text

    // The list ends right before Telegram appends its version footer, itself drawn as a shadow.
    private fun tidy(items: MutableList<Any?>) {
        var index = 0
        while (index < items.size) {
            val item = items[index] as? UItem
            val next = items.getOrNull(index + 1) as? UItem
            when {
                item.isShadow() && (next == null || next.isShadow()) -> items.removeAt(index)
                item?.viewType == UniversalAdapter.VIEW_TYPE_HEADER && (next == null || next.isShadow()) -> items.removeAt(index)
                else -> index++
            }
        }
    }

    private fun UItem?.isShadow() = this?.viewType == UniversalAdapter.VIEW_TYPE_SHADOW

    private companion object {
        val PREMIUM_ROWS = setOf(11, 12, 13, 15, 16)
        val HELP_ROWS = setOf(17, 18, 19, 23)
    }
}

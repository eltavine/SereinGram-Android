package com.eltavine.sereingram.settings.ui

import android.view.View
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsState
import com.eltavine.sereingram.ui.ButtonCell
import com.eltavine.sereingram.ui.FeatureCell
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.browser.Browser
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.UItem

internal object SubpageBinder : RowBinder<SettingsRow.Subpage> {
    override fun draw(row: SettingsRow.Subpage, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>) {
        val page = row.page
        val title = getString(page.title)
        val summary = page.summary?.let(::getString)
        val status = page.status?.let { valueOf(it, host.state) }
        val tint = row.tint
        items += if (tint == null) {
            ButtonCell.of(id, title, icon = row.icon, value = status, summary = summary, enabled = enabled)
        } else {
            val gradient = tint.gradient
            FeatureCell.of(id, gradient.top, gradient.bottom, row.icon, title, summary, status, enabled)
        }
    }

    override fun tap(row: SettingsRow.Subpage, index: Int, host: RowHost, view: View) = host.open(row.page)
}

internal object ScreenBinder : RowBinder<SettingsRow.Screen> {
    override fun draw(row: SettingsRow.Screen, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>) {
        items += ButtonCell.of(id, getString(row.title), row.icon, valueOf(row.value, host.state), row.summary?.let(::getString), enabled = enabled)
    }

    override fun tap(row: SettingsRow.Screen, index: Int, host: RowHost, view: View) {
        val screen = row.open(host.state) as BaseFragment
        val account = host.fragment.currentAccount
        if (screen.currentAccount != account) {
            screen.setCurrentAccount(account)
        }
        host.fragment.presentFragment(screen)
    }
}

internal object LinkBinder : RowBinder<SettingsRow.Link> {
    override fun draw(row: SettingsRow.Link, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>) {
        items += ButtonCell.of(id, getString(row.title), row.icon, valueOf(row.value, host.state), row.summary?.let(::getString), enabled = enabled)
    }

    override fun tap(row: SettingsRow.Link, index: Int, host: RowHost, view: View) {
        Browser.openUrl(host.fragment.parentActivity, row.url)
    }
}

internal object ActionBinder : RowBinder<SettingsRow.Action> {
    override fun draw(row: SettingsRow.Action, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>) {
        items += ButtonCell.of(id, getString(row.title), row.icon, summary = row.summary?.let(::getString), enabled = enabled)
    }

    override fun tap(row: SettingsRow.Action, index: Int, host: RowHost, view: View) = row.run(host.fragment)
}

internal object PickFileBinder : RowBinder<SettingsRow.PickFile> {
    override fun draw(row: SettingsRow.PickFile, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>) {
        items += ButtonCell.of(id, getString(row.title), row.icon, summary = row.summary?.let(::getString), enabled = enabled)
    }

    override fun tap(row: SettingsRow.PickFile, index: Int, host: RowHost, view: View) {
        host.pick(row.mimeTypes) { uri -> row.picked(host.fragment, uri) }
    }
}

private fun valueOf(value: (SettingsState) -> CharSequence?, state: SettingsState): CharSequence? =
    Faults.guard("settings value", fallback = null) { value(state) }

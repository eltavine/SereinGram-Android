package com.eltavine.sereingram.settings.ui

import android.view.View
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.ui.SwitchCell
import org.telegram.messenger.LocaleController.getString
import org.telegram.ui.Components.UItem

internal object ToggleBinder : RowBinder<SettingsRow.Toggle> {
    override fun draw(row: SettingsRow.Toggle, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>) {
        items += SwitchCell.of(id, getString(row.title), row.summary?.let(::getString), host.state[row.option], enabled)
    }

    override fun tap(row: SettingsRow.Toggle, index: Int, host: RowHost, view: View) {
        val on = !host.state[row.option]
        val change = { host.commit(row, on) }
        row.guard?.invoke(on, change) ?: change()
    }
}

internal object SwitchBinder : RowBinder<SettingsRow.Switch> {
    override fun draw(row: SettingsRow.Switch, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>) {
        items += SwitchCell.of(id, getString(row.title), row.summary?.let(::getString), row.isOn(), enabled)
    }

    override fun tap(row: SettingsRow.Switch, index: Int, host: RowHost, view: View) {
        row.toggle()
        host.refresh()
    }
}

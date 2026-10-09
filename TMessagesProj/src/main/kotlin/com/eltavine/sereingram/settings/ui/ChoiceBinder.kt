package com.eltavine.sereingram.settings.ui

import android.view.View
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.ui.ButtonCell
import com.eltavine.sereingram.ui.RadioCell
import com.eltavine.sereingram.ui.SliderCell
import org.telegram.messenger.LocaleController.getString
import org.telegram.ui.Components.UItem
import kotlin.math.abs

internal object ChoiceBinder : RowBinder<SettingsRow.Choice> {
    override fun draw(row: SettingsRow.Choice, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>) {
        val current = host.state[row.option]
        when (row.style) {
            SettingsRow.ChoiceStyle.DIALOG -> items += ButtonCell.of(
                id,
                getString(row.title),
                value = row.label(current),
                summary = row.summary?.let(::getString),
                enabled = enabled,
            )
            SettingsRow.ChoiceStyle.INLINE -> {
                items += UItem.asHeader(getString(row.title)).setEnabled(enabled)
                row.choices.forEachIndexed { index, choice ->
                    items += RadioCell.of(id + index, row.label(choice), row.describe?.invoke(choice), choice == current, enabled)
                }
            }
            SettingsRow.ChoiceStyle.SLIDER -> {
                items += UItem.asHeader(getString(row.title)).setEnabled(enabled)
                // A value the slider has no step for, such as one an older release offered, shows at the nearest step.
                val shown = row.choices.indices.minBy { abs(row.choices[it].toLong() - current) }
                items += SliderCell.of(id, row.choices.map { row.label(it).toString() }, shown, enabled) { index ->
                    row.choices.getOrNull(index)?.let { host.commit(row, it) }
                }
            }
        }
    }

    override fun tap(row: SettingsRow.Choice, index: Int, host: RowHost, view: View) {
        when (row.style) {
            SettingsRow.ChoiceStyle.DIALOG -> choose(row, host, view)
            SettingsRow.ChoiceStyle.INLINE -> row.choices.getOrNull(index)?.let { host.commit(row, it) }
            SettingsRow.ChoiceStyle.SLIDER -> Unit
        }
    }

    // As Telegram and Nagram offer the values of a setting: a menu over its row, with the current one checked.
    private fun choose(row: SettingsRow.Choice, host: RowHost, view: View) {
        val current = host.state[row.option]
        val menu = host.options(view)
        row.choices.forEach { choice -> menu.addChecked(choice == current, row.label(choice)) { host.commit(row, choice) } }
        menu.show()
    }
}

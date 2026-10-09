package com.eltavine.sereingram.settings.ui

import android.view.View
import com.eltavine.sereingram.settings.PageLayout
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsState
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.ItemOptions
import org.telegram.ui.Components.UItem

/** The page that shows a row, as the row's binder needs it. */
internal interface RowHost {
    val fragment: BaseFragment

    /** The settings as of now, for the account the page was opened in. */
    val state: SettingsState

    /** Draws the page again, since what changed may show, hide or change rows. */
    fun refresh()

    /** Saves [value] for the option of [row], draws the page again and, if the change waits for a restart, offers one. */
    fun <T : Any> commit(row: SettingsRow.Editor<T>, value: T)

    fun open(page: SettingsPage)

    /** Telegram's menu over [row], an item the page drew, to fill and show. */
    fun options(row: View): ItemOptions

    /** Lets the user pick a document of [mimeTypes], then hands [picked] its content URI. */
    fun pick(mimeTypes: List<String>, picked: (uri: String) -> Unit)
}

/** Draws one kind of row as list items, and handles taps on them. */
internal interface RowBinder<R : SettingsRow> {
    /** Adds the items [row] is drawn as; their ids are [id] and the ones after it, fewer than [PageLayout.ITEM_SPAN]. */
    fun draw(row: R, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>)

    /** Handles a tap on the [index]th item of the row, counted from its first. */
    fun tap(row: R, index: Int, host: RowHost, view: View)
}

/** The binder for [row]; a kind of row without one does not build. */
@Suppress("UNCHECKED_CAST")
internal fun binderOf(row: SettingsRow): RowBinder<SettingsRow> = when (row) {
    is SettingsRow.Toggle -> ToggleBinder
    is SettingsRow.Text -> TextBinder
    is SettingsRow.Choice -> ChoiceBinder
    is SettingsRow.Switch -> SwitchBinder
    is SettingsRow.Subpage -> SubpageBinder
    is SettingsRow.Screen -> ScreenBinder
    is SettingsRow.Link -> LinkBinder
    is SettingsRow.Action -> ActionBinder
    is SettingsRow.PickFile -> PickFileBinder
} as RowBinder<SettingsRow>

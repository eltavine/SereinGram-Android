package com.eltavine.sereingram.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import org.telegram.ui.ActionBar.BaseFragment

/** A page of SereinGram settings; features declare them and [SereinSettingsActivity] draws them. */
class SettingsPage(
    @StringRes val title: Int,
    val sections: List<SettingsSection>,
)

class SettingsSection(
    val rows: List<SettingsRow>,
    @StringRes val header: Int? = null,
    /** Explanation under the section, closing it. */
    @StringRes val note: Int? = null,
)

sealed interface SettingsRow {
    /** A switch for [option]; account options apply to the account the page was opened in. */
    class Toggle(val option: Option<Boolean>, @StringRes val title: Int) : SettingsRow

    /** A switch for state kept outside SereinGram's options, such as one of Nagram's settings. */
    class Switch(@StringRes val title: Int, val isOn: () -> Boolean, val toggle: () -> Unit) : SettingsRow

    class Subpage(val page: SettingsPage, @DrawableRes val icon: Int) : SettingsRow

    /** Opens a screen that is not a [SettingsPage], such as one of Nagram's. */
    class Screen(
        @StringRes val title: Int,
        val open: (Options) -> BaseFragment,
        val value: () -> CharSequence? = { null },
    ) : SettingsRow

    class Link(@StringRes val title: Int, val url: String, val value: () -> CharSequence? = { null }) : SettingsRow

    /** Does something once when tapped; [run] gets the page it was tapped on. */
    class Action(@StringRes val title: Int, val run: (BaseFragment) -> Unit) : SettingsRow
}

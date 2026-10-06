package com.eltavine.sereingram.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.eltavine.sereingram.core.Option

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

    class Subpage(val page: SettingsPage, @DrawableRes val icon: Int) : SettingsRow

    class Link(@StringRes val title: Int, val url: String, val value: () -> CharSequence? = { null }) : SettingsRow
}

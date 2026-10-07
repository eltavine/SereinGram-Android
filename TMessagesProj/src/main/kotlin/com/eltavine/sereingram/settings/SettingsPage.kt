package com.eltavine.sereingram.settings

import android.net.Uri
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
    /**
     * A switch for [option]; account options apply to the account the page was opened in.
     * A [guard] gets the value the switch would take and the [change] to make, which it makes or not.
     */
    class Toggle(
        val option: Option<Boolean>,
        @StringRes val title: Int,
        val guard: ((on: Boolean, change: () -> Unit) -> Unit)? = null,
    ) : SettingsRow

    /** Edits [option] in a dialog; [placeholder] stands in for it while it is empty. A [secret] shows only its end. */
    class Text(
        val option: Option<String>,
        @StringRes val title: Int,
        @StringRes val placeholder: Int,
        val secret: Boolean = option.secret,
    ) : SettingsRow

    /** Picks [option]'s value among [choices] in a dialog; [label] names each one. */
    class Choice(
        val option: Option<Int>,
        @StringRes val title: Int,
        val choices: List<Int>,
        val label: (Int) -> CharSequence,
    ) : SettingsRow

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

    /** Lets the user pick a document of [mimeTypes] with the system picker, then hands it to [picked]. */
    class PickFile(
        @StringRes val title: Int,
        val mimeTypes: List<String>,
        val picked: (BaseFragment, Uri) -> Unit,
    ) : SettingsRow
}

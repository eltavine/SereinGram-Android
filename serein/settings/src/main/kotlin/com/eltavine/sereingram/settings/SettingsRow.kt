package com.eltavine.sereingram.settings

import com.eltavine.sereingram.core.Option

/**
 * One row of a [SettingsPage]. Any row may say what it does in a line under its
 * title, and may show, or be usable, only while a condition holds, which the page
 * checks each time it draws. Optional arguments are meant to be passed by name, so
 * that declared rows keep building as rows learn more.
 */
public sealed interface SettingsRow {
    /** A line under the title that says what the row does. */
    public val summary: Int? get() = null

    /** The row shows only while this holds. */
    public val shownWhen: SettingsCondition get() = SettingsCondition.ALWAYS

    /** The row can be used only while this holds, and is drawn greyed out otherwise. */
    public val enabledWhen: SettingsCondition get() = SettingsCondition.ALWAYS

    /** A row that edits one of SereinGram's options; account options apply to the account the page was opened in. */
    public sealed interface Editor<T : Any> : SettingsRow {
        public val option: Option<T>

        /** A change takes effect the next time the app starts, which the page then offers to do. */
        public val restarts: Boolean
    }

    /** A switch for [option]. A [guard] gets the value the switch would take and the change to make, which it makes or not. */
    public class Toggle(
        override val option: Option<Boolean>,
        public val title: Int,
        override val summary: Int? = null,
        public val guard: ((on: Boolean, change: () -> Unit) -> Unit)? = null,
        override val restarts: Boolean = false,
        override val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
        override val enabledWhen: SettingsCondition = SettingsCondition.ALWAYS,
    ) : Editor<Boolean>

    /**
     * Edits [option] in a dialog, where emptying it restores its default; [placeholder]
     * stands in for it while it is empty, and [hint], an example, in the empty field, where
     * it is [placeholder] if null. A [secret] shows only its end. [check] says why a text
     * cannot be saved, as a string resource, or returns null when it can; the empty text
     * always can. While [requiredWhen] holds, an empty value is drawn as one that is missing.
     */
    public class Text(
        override val option: Option<String>,
        public val title: Int,
        public val placeholder: Int,
        public val hint: Int? = null,
        override val summary: Int? = null,
        public val secret: Boolean = option.secret,
        public val kind: TextKind = TextKind.PLAIN,
        public val check: ((text: String) -> Int?)? = null,
        public val requiredWhen: SettingsCondition? = null,
        override val restarts: Boolean = false,
        override val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
        override val enabledWhen: SettingsCondition = SettingsCondition.ALWAYS,
    ) : Editor<String>

    /** What a [Text] holds, which picks the keyboard for it. */
    public enum class TextKind { PLAIN, URL }

    /** Picks [option]'s value among [choices]; [label] names each one, and [describe] may say more about it. */
    public class Choice(
        override val option: Option<Int>,
        public val title: Int,
        public val choices: List<Int>,
        public val label: (Int) -> CharSequence,
        override val summary: Int? = null,
        public val describe: ((Int) -> CharSequence?)? = null,
        public val style: ChoiceStyle = ChoiceStyle.DIALOG,
        override val restarts: Boolean = false,
        override val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
        override val enabledWhen: SettingsCondition = SettingsCondition.ALWAYS,
    ) : Editor<Int> {
        init {
            require(choices.isNotEmpty() && choices.size <= PageLayout.ITEM_SPAN) { "$option offers ${choices.size} choices" }
        }
    }

    /** How a [Choice] is drawn; the styles that make its title a header leave out its summary, which a section note can carry. */
    public enum class ChoiceStyle {
        /** A row with the current choice that offers all of them in a menu over it. */
        DIALOG,

        /** Every choice as a row of its own, under the title as a header, with what [Choice.describe] says. */
        INLINE,

        /** A slider along the choices, under the title as a header, for steps of one quantity with short labels. */
        SLIDER,
    }

    /** A switch for state kept outside SereinGram's options, such as one of Nagram's settings. */
    public class Switch(
        public val title: Int,
        public val isOn: () -> Boolean,
        public val toggle: () -> Unit,
        override val summary: Int? = null,
        override val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
        override val enabledWhen: SettingsCondition = SettingsCondition.ALWAYS,
    ) : SettingsRow

    /**
     * Opens [page], saying what it holds with its summary and status. With a [tint], [icon]
     * sits on a tile of that colour, as features do on Telegram's own main settings.
     */
    public class Subpage(
        public val page: SettingsPage,
        public val icon: Int = 0,
        public val tint: SettingsTint? = null,
        override val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
        override val enabledWhen: SettingsCondition = SettingsCondition.ALWAYS,
    ) : SettingsRow {
        override val summary: Int? get() = page.summary
    }

    /** Opens a screen that is not a [SettingsPage], such as one of Nagram's; [open] makes it, a fragment of the app. */
    public class Screen(
        public val title: Int,
        public val open: (SettingsState) -> Any,
        override val summary: Int? = null,
        public val icon: Int = 0,
        public val value: (SettingsState) -> CharSequence? = { null },
        override val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
        override val enabledWhen: SettingsCondition = SettingsCondition.ALWAYS,
    ) : SettingsRow

    public class Link(
        public val title: Int,
        public val url: String,
        override val summary: Int? = null,
        public val icon: Int = 0,
        public val value: (SettingsState) -> CharSequence? = { null },
        override val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
        override val enabledWhen: SettingsCondition = SettingsCondition.ALWAYS,
    ) : SettingsRow

    /** Does something once when tapped; [run] gets the screen it was tapped on, a fragment of the app. */
    public class Action(
        public val title: Int,
        public val run: (host: Any) -> Unit,
        override val summary: Int? = null,
        public val icon: Int = 0,
        override val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
        override val enabledWhen: SettingsCondition = SettingsCondition.ALWAYS,
    ) : SettingsRow

    /**
     * Lets the user pick a document of [mimeTypes] with the system picker, then hands
     * [picked] the screen it was picked from and the document's content URI.
     */
    public class PickFile(
        public val title: Int,
        public val mimeTypes: List<String>,
        public val picked: (host: Any, uri: String) -> Unit,
        override val summary: Int? = null,
        public val icon: Int = 0,
        override val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
        override val enabledWhen: SettingsCondition = SettingsCondition.ALWAYS,
    ) : SettingsRow
}

package com.eltavine.sereingram.settings

/**
 * A page of SereinGram settings. Features declare their pages with these types,
 * which know nothing of how the app draws them; texts and icons are Android
 * resource ids.
 */
public class SettingsPage(
    public val title: Int,
    public val sections: List<SettingsSection>,
    /** What the page holds, in a few words, under the row that opens it. */
    public val summary: Int? = null,
    /** The page's state in a word or two, at the end of the row that opens it, such as whether its feature is on. */
    public val status: ((SettingsState) -> CharSequence?)? = null,
)

/** Every row of the page that edits an option, and those of the pages it opens. */
public fun SettingsPage.editors(): List<SettingsRow.Editor<*>> = sections.flatMap { it.rows }.flatMap { row ->
    when (row) {
        is SettingsRow.Editor<*> -> listOf(row)
        is SettingsRow.Subpage -> row.page.editors()
        else -> emptyList()
    }
}

/** Rows under an optional header, closed by an optional note that explains them. */
public class SettingsSection(
    public val rows: List<SettingsRow>,
    public val header: Int? = null,
    public val note: Int? = null,
    /** The section shows only while this holds; it also hides while none of its rows shows. */
    public val shownWhen: SettingsCondition = SettingsCondition.ALWAYS,
)

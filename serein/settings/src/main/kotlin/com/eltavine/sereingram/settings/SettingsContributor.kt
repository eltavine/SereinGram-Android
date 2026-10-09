package com.eltavine.sereingram.settings

import com.eltavine.sereingram.core.SereinModule

/** A module with its own page, opened from a row of the SereinGram settings page. */
public interface SettingsContributor {
    public val settingsPage: SettingsPage

    /** The icon of the module's row, a drawable resource. */
    public val settingsIcon: Int

    /** The group of the SereinGram page the module's row goes in. */
    public val settingsCategory: SettingsCategory get() = SettingsCategory.MORE

    /** The colour of the tile behind [settingsIcon]. */
    public val settingsTint: SettingsTint get() = SettingsTint.BLUE

    /** Where the row goes in its group, lowest first; rows of the same order keep the order of the modules. */
    public val settingsOrder: Int get() = 0

    /** The name the user's colour and picture for the row's tile are kept under; a module's own id unless given. */
    public val settingsKey: String? get() = (this as? SereinModule)?.id
}

/** The groups of the SereinGram page, in the order they show. */
public enum class SettingsCategory { PRIVACY, MESSAGES, CHATS, MEDIA, INTERFACE, MORE }

/**
 * Colours of the tile behind an icon, in the order a palette offers them: those of Telegram's
 * own settings, and more of SereinGram's, so that each feature can have a colour of its own.
 */
public enum class SettingsTint {
    RED,
    CORAL,
    ORANGE_DEEP,
    ORANGE,
    YELLOW,
    LIME,
    GREEN,
    TEAL,
    CYAN,
    SKY,
    BLUE,
    BLUE_DEEP,
    INDIGO,
    VIOLET,
    PURPLE,
    MAGENTA,
    PINK,
    BROWN,
    GRAY,
    SLATE,
}

/**
 * The sections of the SereinGram page that open the contributors' pages: one for each
 * [SettingsCategory] that has any, in the categories' order, under the [header] and above
 * the [note] each gets. Within a category the rows follow [SettingsContributor.settingsOrder],
 * and then the order of [contributors].
 */
public fun featureSections(
    contributors: List<SettingsContributor>,
    note: (SettingsCategory) -> Int? = { null },
    header: (SettingsCategory) -> Int?,
): List<SettingsSection> =
    contributors.withIndex()
        .sortedWith(compareBy({ it.value.settingsCategory }, { it.value.settingsOrder }, { it.index }))
        .groupBy({ it.value.settingsCategory }, { it.value })
        .map { (category, members) ->
            SettingsSection(
                header = header(category),
                note = note(category),
                rows = members.map { SettingsRow.Subpage(it.settingsPage, it.settingsIcon, it.settingsTint, key = it.settingsKey) },
            )
        }

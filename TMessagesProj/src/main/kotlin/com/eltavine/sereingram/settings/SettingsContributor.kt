package com.eltavine.sereingram.settings

import androidx.annotation.DrawableRes

/** A module with its own page on the SereinGram settings page. */
interface SettingsContributor {
    val settingsPage: SettingsPage

    @get:DrawableRes
    val settingsIcon: Int
}

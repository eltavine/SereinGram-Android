package com.eltavine.sereingram.settings.ui

import com.eltavine.sereingram.settings.SettingsTint
import org.telegram.ui.Components.IconBackgroundColors

/** The two colours, from top to bottom, of the gradient a tile is painted with. */
internal class TileGradient(val top: Int, val bottom: Int)

/** Telegram's own gradient where it has the colour, and SereinGram's, in the same manner, where it does not. */
internal val SettingsTint.gradient: TileGradient
    get() = when (this) {
        SettingsTint.RED -> IconBackgroundColors.RED.gradient
        SettingsTint.CORAL -> TileGradient(0xFFFF7E67.toInt(), 0xFFF0583F.toInt())
        SettingsTint.ORANGE_DEEP -> IconBackgroundColors.ORANGE_DEEP.gradient
        SettingsTint.ORANGE -> IconBackgroundColors.ORANGE.gradient
        SettingsTint.YELLOW -> TileGradient(0xFFF5C33B.toInt(), 0xFFEBA314.toInt())
        SettingsTint.LIME -> TileGradient(0xFF9BD045.toInt(), 0xFF74B42B.toInt())
        SettingsTint.GREEN -> IconBackgroundColors.GREEN.gradient
        SettingsTint.TEAL -> TileGradient(0xFF2DCBA8.toInt(), 0xFF14A68A.toInt())
        SettingsTint.CYAN -> IconBackgroundColors.CYAN.gradient
        SettingsTint.SKY -> TileGradient(0xFF62C9F7.toInt(), 0xFF39A9EC.toInt())
        SettingsTint.BLUE -> IconBackgroundColors.BLUE.gradient
        SettingsTint.BLUE_DEEP -> IconBackgroundColors.BLUE_DEEP.gradient
        SettingsTint.INDIGO -> TileGradient(0xFF7D73F2.toInt(), 0xFF5A4EDB.toInt())
        // Telegram Premium's.
        SettingsTint.VIOLET -> TileGradient(0xFFB659FF.toInt(), 0xFF617CFF.toInt())
        SettingsTint.PURPLE -> IconBackgroundColors.PURPLE.gradient
        SettingsTint.MAGENTA -> TileGradient(0xFFE45DD6.toInt(), 0xFFC23CBF.toInt())
        SettingsTint.PINK -> TileGradient(0xFFF7709E.toInt(), 0xFFE5487D.toInt())
        SettingsTint.BROWN -> TileGradient(0xFFC79466.toInt(), 0xFFA6713F.toInt())
        SettingsTint.GRAY -> IconBackgroundColors.GRAY.gradient
        SettingsTint.SLATE -> TileGradient(0xFF6B7888.toInt(), 0xFF4C5868.toInt())
    }

private val IconBackgroundColors.gradient: TileGradient get() = TileGradient(top, bottom)

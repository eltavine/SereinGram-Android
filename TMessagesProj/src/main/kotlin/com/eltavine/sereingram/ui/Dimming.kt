package com.eltavine.sereingram.ui

import android.view.ViewGroup

/**
 * Greys out a row that cannot be used, as Telegram's own cells do: its content fades and
 * its white background stays, which fading the row itself would turn grey. The list only
 * sets whether a row is enabled when it comes into view, so this sets it as well.
 */
internal fun dim(row: ViewGroup, enabled: Boolean) {
    val alpha = if (enabled) 1f else 0.5f
    for (index in 0 until row.childCount) {
        row.getChildAt(index).alpha = alpha
    }
    row.isEnabled = enabled
}

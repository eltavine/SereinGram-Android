package com.eltavine.sereingram.ui

import android.content.Context
import android.view.View
import org.telegram.ui.Components.UniversalFragment
import org.telegram.ui.Components.UniversalRecyclerView

/**
 * A list screen drawn as Telegram's own settings and Nagram's are: rows on rounded cards, and
 * a header in the list's colour until the list scrolls under it. The list reaches under the
 * navigation bar, leaving room below its last row; a screen that left the bar to Telegram would
 * keep its last rows under it, beyond reach, once edge-to-edge is forced in Nagram's settings.
 */
internal abstract class SettingsListFragment : UniversalFragment() {
    override fun createView(context: Context): View = super.createView(context).also {
        listView.drawAsSettings()
        actionBar.setAdaptiveBackground(listView)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun isSupportEdgeToEdge(): Boolean = true

    override fun onInsets(left: Int, top: Int, right: Int, bottom: Int) {
        val list = listView ?: return
        list.setPadding(0, 0, 0, bottom)
        list.clipToPadding = false
    }
}

/** Draws the list's sections as rounded cards, the cards drawing the rows' background. */
internal fun UniversalRecyclerView.drawAsSettings() {
    setSections(true)
    adapter.setApplyBackground(false)
}

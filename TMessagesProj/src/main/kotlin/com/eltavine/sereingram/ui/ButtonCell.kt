package com.eltavine.sereingram.ui

import android.content.Context
import android.view.View
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalRecyclerView

/**
 * Telegram's row that opens or does something: a grey icon, the title, a value at the
 * end and a short line under the title, each optional. The row keeps its place while
 * its value changes, which then animates instead of the row being drawn anew.
 */
internal class ButtonCell private constructor() : UItem.UItemFactory<TextCell>() {
    enum class Tone { NORMAL, ACCENT, DESTRUCTIVE }

    override fun createView(context: Context, listView: RecyclerListView?, currentAccount: Int, classGuid: Int, resourcesProvider: Theme.ResourcesProvider?) =
        TextCell(context, resourcesProvider)

    override fun bindView(view: View, item: UItem, divider: Boolean, adapter: UniversalAdapter?, listView: UniversalRecyclerView?) {
        val cell = view as TextCell
        val same = cell.tag == item.id
        val value = item.textValue
        val icon = item.iconResId
        when {
            icon != 0 && value != null -> cell.setTextAndValueAndIcon(item.text, value, same, icon, divider)
            icon != 0 -> cell.setTextAndIcon(item.text, icon, divider)
            value != null -> cell.setTextAndValue(item.text, value, same, divider)
            else -> cell.setText(item.text, divider)
        }
        cell.setSubtitle(item.subtext)
        val height = if (item.subtext.isNullOrEmpty()) 50 else 60
        if (cell.heightDp != height) {
            cell.heightDp = height
            cell.requestLayout()
        }
        when {
            item.red -> cell.setColors(Theme.key_text_RedBold, Theme.key_text_RedRegular)
            item.accent -> cell.setColors(Theme.key_windowBackgroundWhiteBlueText4, Theme.key_windowBackgroundWhiteBlueText4)
            else -> cell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText)
        }
        // After setColors, which gives the value its usual colour.
        if (item.flags and FLAG_UNUSABLE_VALUE != 0) {
            cell.valueTextView.setTextColor(Theme.getColor(Theme.key_text_RedRegular))
        }
        cell.setEnabled(item.enabled, same)
        // TextCell fades only its texts, which would leave a greyed-out row's icon looking usable.
        val iconAlpha = if (item.enabled) 1f else 0.5f
        if (same) cell.imageView.animate().alpha(iconAlpha).start() else cell.imageView.alpha = iconAlpha
        cell.tag = item.id
    }

    override fun equals(a: UItem, b: UItem): Boolean = a.id == b.id

    companion object {
        private const val FLAG_UNUSABLE_VALUE = 1

        init {
            setup(ButtonCell())
        }

        /** [unusable] draws the value in red, for one that the app cannot work with as it is. */
        fun of(
            id: Int,
            title: CharSequence,
            icon: Int = 0,
            value: CharSequence? = null,
            summary: CharSequence? = null,
            tone: Tone = Tone.NORMAL,
            unusable: Boolean = false,
            enabled: Boolean = true,
        ): UItem = UItem.ofFactory(ButtonCell::class.java).apply {
            this.id = id
            text = title
            iconResId = icon
            textValue = value
            subtext = summary
            accent = tone == Tone.ACCENT
            red = tone == Tone.DESTRUCTIVE
            flags = if (unusable) FLAG_UNUSABLE_VALUE else 0
            this.enabled = enabled
        }
    }
}

package com.eltavine.sereingram.ui

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.children
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalRecyclerView
import org.telegram.ui.SettingsActivity

/**
 * The row of Telegram's own main settings: an icon on a coloured tile, the title, a
 * grey line under it and a value at the end. Unlike Telegram's list of them, the row
 * keeps its place when its value changes, so a status that appears does not redraw it.
 * The tile may show a picture instead, or keep colours the user chose under a Monet theme,
 * which otherwise paints every tile in its accent colour.
 */
internal class FeatureCell private constructor() : UItem.UItemFactory<FeatureCell.Row>() {
    internal class Row(context: Context, resourcesProvider: Theme.ResourcesProvider?) : SettingsActivity.SettingCell(context, resourcesProvider) {
        // Telegram keeps the tile to itself: it is the child drawn on a tile background, holding the icon.
        private val tile: View? = children.firstOrNull { it.background is SettingsActivity.SettingCell.Background }
        private val tileBackground = tile?.background as? SettingsActivity.SettingCell.Background
        private val icon: View? = (tile as? ViewGroup)?.getChildAt(0)

        fun keepColours(top: Int, bottom: Int) {
            tileBackground?.setColor(top, bottom, false)
            tile?.invalidate()
        }

        fun showPicture(picture: Bitmap?) {
            val tile = tile ?: return
            if (picture == null) {
                tile.background = tileBackground
                icon?.visibility = VISIBLE
                return
            }
            if ((tile.background as? RoundedBitmapDrawable)?.bitmap !== picture) {
                tile.background = RoundedBitmapDrawableFactory.create(resources, picture).apply {
                    cornerRadius = dp(TILE_RADIUS).toFloat()
                    setAntiAlias(true)
                }
            }
            icon?.visibility = INVISIBLE
        }
    }

    override fun createView(context: Context, listView: RecyclerListView?, currentAccount: Int, classGuid: Int, resourcesProvider: Theme.ResourcesProvider?) =
        Row(context, resourcesProvider)

    override fun bindView(view: View, item: UItem, divider: Boolean, adapter: UniversalAdapter?, listView: UniversalRecyclerView?) {
        val cell = view as Row
        val top = item.intValue
        val bottom = item.longValue.toInt()
        cell.set(top, bottom, item.iconResId, item.text, item.subtext, item.textValue)
        if (item.flags and FLAG_OWN_COLOURS != 0) {
            cell.keepColours(top, bottom)
        }
        cell.showPicture(item.`object` as? Bitmap)
        dim(cell, item.enabled)
    }

    override fun equals(a: UItem, b: UItem): Boolean = a.id == b.id

    companion object {
        private const val FLAG_OWN_COLOURS = 1

        // As Telegram rounds its own tiles.
        private const val TILE_RADIUS = 10f

        init {
            setup(FeatureCell())
        }

        /** [ownColours] keeps the colours as given under any theme; a [picture] takes the place of the icon and its tile. */
        fun of(
            id: Int,
            colorTop: Int,
            colorBottom: Int,
            icon: Int,
            title: CharSequence,
            summary: CharSequence?,
            value: CharSequence?,
            enabled: Boolean = true,
            ownColours: Boolean = false,
            picture: Bitmap? = null,
        ): UItem = UItem.ofFactory(FeatureCell::class.java).apply {
            this.id = id
            intValue = colorTop
            longValue = colorBottom.toLong()
            iconResId = icon
            text = title
            subtext = summary
            textValue = value
            this.enabled = enabled
            flags = if (ownColours) FLAG_OWN_COLOURS else 0
            `object` = picture
        }
    }
}

package com.eltavine.sereingram.settings.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsState
import com.eltavine.sereingram.settings.SettingsTint
import com.eltavine.sereingram.settings.TileTints
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.LocaleController.formatString
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.SettingsActivity

/**
 * What a long press on a feature's tile offers, after the menu of Nagram's settings: a colour of
 * the user's own for the tile, a picture in its place, and the tile as the feature declares it.
 */
internal object TileMenu {
    /** The colour the user chose for [key]'s tile, if any. */
    fun chosenTint(state: SettingsState, key: String): SettingsTint? = TileTints.decode(state[TileTints.tints])[key]

    fun show(row: SettingsRow.Subpage, key: String, host: RowHost, view: View) {
        val chosen = chosenTint(host.state, key)
        val pictured = TilePictures.has(key)
        val menu = host.options(view)
        menu.add(R.drawable.msg_palette, getString(R.string.serein_tiles_colour)) { pickTint(row, key, host) }
        menu.add(R.drawable.msg_photos, getString(R.string.serein_tiles_picture)) { pickPicture(key, host) }
        if (pictured) {
            menu.add(R.drawable.msg_delete, getString(R.string.serein_tiles_remove_picture)) {
                TilePictures.remove(key)
                host.refresh()
            }
        }
        if (chosen != null || pictured) {
            menu.add(R.drawable.msg_reset, getString(R.string.serein_tiles_reset)) {
                TilePictures.remove(key)
                setTint(host.state, key, null)
                host.refresh()
            }
        }
        menu.show()
    }

    private fun setTint(state: SettingsState, key: String, tint: SettingsTint?) {
        state[TileTints.tints] = TileTints.with(state[TileTints.tints], key, tint)
    }

    private fun pickTint(row: SettingsRow.Subpage, key: String, host: RowHost) {
        val context = host.fragment.parentActivity ?: return
        val declared = row.tint ?: return
        val chosen = chosenTint(host.state, key)
        lateinit var dialog: AlertDialog
        val palette = palette(context, row.icon, chosen ?: declared) { tint ->
            // The feature's own colour leaves the tile as declared, for a Monet theme to paint again.
            setTint(host.state, key, tint.takeIf { it != declared })
            dialog.dismiss()
        }
        val builder = AlertDialog.Builder(context, host.fragment.resourceProvider)
            .setTitle(getString(R.string.serein_tiles_colour))
            .setView(palette)
            .setNegativeButton(getString(R.string.Cancel), null)
        if (chosen != null) {
            builder.setNeutralButton(getString(R.string.Default)) { _, _ -> setTint(host.state, key, null) }
        }
        dialog = builder.create()
        host.fragment.showDialog(dialog)
    }

    /** Every colour a tile can take, each drawn as a tile with [icon] in it, [current] ringed; a tap hands [picked] its colour. */
    fun palette(context: Context, icon: Int, current: SettingsTint, picked: (SettingsTint) -> Unit): View {
        val grid = GridLayout(context).apply { columnCount = COLUMNS }
        SettingsTint.entries.forEachIndexed { index, tint ->
            grid.addView(
                Swatch(context, tint.gradient, icon, tint == current).apply {
                    contentDescription = formatString(R.string.serein_tiles_colour_n, index + 1)
                    setOnClickListener { picked(tint) }
                },
            )
        }
        return FrameLayout(context).apply {
            addView(grid, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT.toFloat(), Gravity.CENTER_HORIZONTAL, 0f, 4f, 0f, 4f))
        }
    }

    private fun pickPicture(key: String, host: RowHost) {
        host.pick(IMAGES) { uri ->
            val page = host.fragment
            TilePictures.io.execute {
                val saved = runCatching { TilePictures.save(key, Uri.parse(uri)) }
                AndroidUtilities.runOnUIThread {
                    saved.onFailure { error ->
                        Faults.report("settings tile picture", error)
                        BulletinFactory.of(page).createSimpleBulletin(R.raw.error, getString(R.string.serein_tiles_picture_failed)).show()
                    }
                    host.refresh()
                }
            }
        }
    }

    /** One colour of the palette, drawn as the tile would look in it and ringed while it is the tile's colour. */
    @SuppressLint("ViewConstructor")
    private class Swatch(context: Context, gradient: TileGradient, icon: Int, private val current: Boolean) : View(context) {
        private val tile = SettingsActivity.SettingCell.Background().apply {
            setColor(gradient.top, gradient.bottom, false)
            setDrawBorder(Theme.isCurrentThemeDark())
        }
        private val glyph = ContextCompat.getDrawable(context, icon)?.mutate()
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2f).toFloat()
            color = Theme.getColor(Theme.key_featuredStickers_addButton)
        }
        private val bounds = RectF()

        init {
            isClickable = true
            background = Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_listSelector), dp(14f), dp(14f))
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) = setMeasuredDimension(dp(CELL), dp(CELL))

        override fun onDraw(canvas: Canvas) {
            val size = dp(TILE)
            val left = (width - size) / 2
            val top = (height - size) / 2
            if (current) {
                val gap = dp(4f) - ring.strokeWidth / 2
                bounds.set(left - gap, top - gap, left + size + gap, top + size + gap)
                canvas.drawRoundRect(bounds, dp(13f).toFloat(), dp(13f).toFloat(), ring)
            }
            tile.setBounds(left, top, left + size, top + size)
            tile.draw(canvas)
            glyph?.let {
                val side = dp(GLYPH)
                val start = (width - side) / 2
                val glyphTop = (height - side) / 2
                it.setBounds(start, glyphTop, start + side, glyphTop + side)
                it.draw(canvas)
            }
        }
    }

    private val IMAGES = listOf("image/*")
    private const val COLUMNS = 5
    private const val CELL = 52f
    private const val TILE = 34f
    private const val GLYPH = 26f
}

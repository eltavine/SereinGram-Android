package com.eltavine.sereingram.ui

import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import org.telegram.messenger.Utilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.SlideChooseView
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalRecyclerView

/**
 * Telegram's slider over a few labelled steps, as for the size of its cache. The row
 * keeps its place while the chosen step changes, so the slider is not drawn anew under
 * the finger, and a drag is handed on once, where it ends.
 */
internal class SliderCell private constructor() : UItem.UItemFactory<SliderCell.Slider>() {
    /**
     * Telegram's slider takes every touch and accessibility action whether or not it is enabled,
     * and the list keeps rows that cannot be tapped disabled; this one stays still while it may not be used.
     */
    internal class Slider(context: Context, resourcesProvider: Theme.ResourcesProvider?) : SlideChooseView(context, resourcesProvider) {
        var usable = true

        var touching = false
            private set

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!usable) {
                return true
            }
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touching = true
            }
            try {
                return super.onTouchEvent(event)
            } finally {
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    touching = false
                }
            }
        }

        override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean =
            usable && super.performAccessibilityAction(action, arguments)

        // Dims what the slider draws and not the row's background, which would turn grey.
        override fun onDraw(canvas: Canvas) {
            if (usable) {
                super.onDraw(canvas)
                return
            }
            val saved = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), DIMMED)
            super.onDraw(canvas)
            canvas.restoreToCount(saved)
        }
    }

    override fun createView(context: Context, listView: RecyclerListView?, currentAccount: Int, classGuid: Int, resourcesProvider: Theme.ResourcesProvider?) =
        Slider(context, resourcesProvider)

    override fun bindView(view: View, item: UItem, divider: Boolean, adapter: UniversalAdapter?, listView: UniversalRecyclerView?) {
        val slider = view as Slider
        slider.setCallback(null)
        slider.setOptions(item.intValue, *item.texts)
        slider.usable = item.enabled
        slider.invalidate()
        val chosen = item.intCallback
        slider.setCallback(object : SlideChooseView.Callback {
            private var dragged = -1

            override fun onOptionSelected(index: Int) {
                if (slider.touching) dragged = index else chosen?.run(index)
            }

            override fun onTouchEnd() {
                if (dragged >= 0) {
                    chosen?.run(dragged)
                }
                dragged = -1
            }
        })
    }

    override fun isClickable(): Boolean = false

    override fun equals(a: UItem, b: UItem): Boolean = a.id == b.id

    companion object {
        private const val DIMMED = 128

        init {
            setup(SliderCell())
        }

        fun of(id: Int, labels: List<String>, chosen: Int, enabled: Boolean, onChosen: (index: Int) -> Unit): UItem =
            UItem.ofFactory(SliderCell::class.java).apply {
                this.id = id
                texts = labels.toTypedArray()
                intValue = chosen
                this.enabled = enabled
                intCallback = Utilities.Callback { onChosen(it) }
            }
    }
}

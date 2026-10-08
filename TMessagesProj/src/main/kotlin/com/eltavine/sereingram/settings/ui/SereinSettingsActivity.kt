package com.eltavine.sereingram.settings.ui

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.View
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.settings.PageLayout
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsState
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment

/**
 * Draws one [SettingsPage] with Telegram's own list rows: each section under its
 * header, closed by its note, and each row by the binder of its kind.
 */
class SereinSettingsActivity(
    private val page: SettingsPage,
    private val options: Options,
) : UniversalFragment(), RowHost {
    private var layout: PageLayout? = null
    private var picked: ((uri: String) -> Unit)? = null
    private var watching: AutoCloseable? = null
    private val redraw = Runnable { refresh() }

    override val fragment: BaseFragment get() = this

    override val state: SettingsState get() = SettingsState(options, currentAccount)

    override fun getTitle(): CharSequence = getString(page.title)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        layout = drawPage(page, this, items)
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        val row = layout?.rowOf(item.id)?.takeIf { it.enabled } ?: return
        Faults.guard("settings row", fallback = Unit) { binderOf(row.row).tap(row.row, item.id - row.id, this, view) }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean = false

    // Options also change elsewhere while the page shows, such as when a backup is restored, often on
    // another thread and many at once; the page draws once after them.
    override fun onResume() {
        super.onResume()
        watching = watching ?: options.addListener { _, _ ->
            AndroidUtilities.cancelRunOnUIThread(redraw)
            AndroidUtilities.runOnUIThread(redraw)
        }
        refresh()
    }

    override fun onPause() {
        super.onPause()
        watching?.close()
        watching = null
        AndroidUtilities.cancelRunOnUIThread(redraw)
    }

    // A value is typed here while the user looks it up elsewhere, such as an API key.
    override fun dismissDialogOnPause(dialog: Dialog): Boolean = false

    override fun refresh() {
        listView?.adapter?.update(true)
    }

    // The page draws again as the options tell it of the change, or once it shows again if it is away;
    // this also runs after a dialog or an unlock, outside the guard around taps.
    override fun <T : Any> commit(row: SettingsRow.Editor<T>, value: T) = Faults.guard("settings change", fallback = Unit) {
        val state = state
        val option = row.option
        val before = state[option]
        // A default that is stored, as an older release could leave it, still goes, so the option follows its default.
        if (before == value && (value != option.default || !state.isModified(option))) {
            return@guard
        }
        if (row.restarts) {
            Restarts.changing(option, before)
        }
        state[option] = value
        if (row.restarts && !isFinished && Restarts.pending(option, value)) {
            Restarts.offer(this)
        }
    }

    override fun open(page: SettingsPage) {
        presentFragment(SereinSettingsActivity(page, options).apply { setCurrentAccount(this@SereinSettingsActivity.currentAccount) })
    }

    override fun pick(mimeTypes: List<String>, picked: (uri: String) -> Unit) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(mimeTypes.singleOrNull() ?: "*/*")
        if (mimeTypes.size > 1) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
        }
        try {
            startActivityForResult(intent, PICK_FILE)
            this.picked = picked
        } catch (_: ActivityNotFoundException) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, getString(R.string.serein_settings_no_file_picker)).show()
        }
    }

    override fun onActivityResultFragment(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != PICK_FILE) {
            return
        }
        val then = picked ?: return
        picked = null
        val uri = data?.data
        if (resultCode == Activity.RESULT_OK && uri != null) {
            Faults.guard("settings file", fallback = Unit) { then(uri.toString()) }
        }
    }

    private companion object {
        // Telegram's own request codes stay well below this.
        const val PICK_FILE = 0x5e17
    }
}

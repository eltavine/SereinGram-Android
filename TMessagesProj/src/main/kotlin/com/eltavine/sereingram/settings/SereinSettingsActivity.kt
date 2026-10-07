package com.eltavine.sereingram.settings

import android.app.Activity
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.messenger.browser.Browser
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment

/** Draws one [SettingsPage] with Telegram's list cells. */
class SereinSettingsActivity(
    private val page: SettingsPage,
    private val options: Options,
) : UniversalFragment() {
    private val rows = HashMap<Int, SettingsRow>()

    override fun getTitle(): CharSequence = getString(page.title)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        rows.clear()
        page.sections.forEach { section ->
            section.header?.let { items.add(UItem.asHeader(getString(it))) }
            section.rows.forEach { row ->
                val id = rows.size + 1
                rows[id] = row
                items.add(item(id, row))
            }
            items.add(UItem.asShadow(section.note?.let(::getString)))
        }
    }

    private fun item(id: Int, row: SettingsRow): UItem = when (row) {
        is SettingsRow.Toggle -> UItem.asCheck(id, getString(row.title)).setChecked(options.get(row.option, account(row.option)))
        is SettingsRow.Text -> UItem.asButton(id, getString(row.title), shown(row, options.get(row.option, account(row.option))))
        is SettingsRow.Choice -> UItem.asButton(id, getString(row.title), row.label(options.get(row.option, account(row.option))))
        is SettingsRow.Switch -> UItem.asCheck(id, getString(row.title)).setChecked(row.isOn())
        is SettingsRow.Subpage -> UItem.asButton(id, row.icon, getString(row.page.title))
        is SettingsRow.Screen -> UItem.asButton(id, getString(row.title), row.value())
        is SettingsRow.Link -> UItem.asButton(id, getString(row.title), row.value())
        is SettingsRow.Action -> UItem.asButton(id, getString(row.title))
        is SettingsRow.PickFile -> UItem.asButton(id, getString(row.title))
    }

    private fun shown(row: SettingsRow.Text, value: String): String = when {
        value.isEmpty() -> getString(row.placeholder)
        row.secret -> "••••" + value.takeLast(4)
        else -> value
    }

    override fun onResume() {
        super.onResume()
        listView?.adapter?.update(true)
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        when (val row = rows[item.id]) {
            is SettingsRow.Toggle -> {
                val account = account(row.option)
                val on = !options.get(row.option, account)
                val change: () -> Unit = {
                    options.set(row.option, on, account)
                    listView?.adapter?.update(true)
                }
                row.guard?.invoke(on, change) ?: change()
            }
            is SettingsRow.Text -> edit(row)
            is SettingsRow.Choice -> choose(row)
            is SettingsRow.Switch -> {
                row.toggle()
                listView.adapter.update(true)
            }
            is SettingsRow.Subpage -> presentFragment(SereinSettingsActivity(row.page, options))
            is SettingsRow.Screen -> presentFragment(row.open(options))
            is SettingsRow.Link -> Browser.openUrl(parentActivity, row.url)
            is SettingsRow.Action -> row.run(this)
            is SettingsRow.PickFile -> pick(row)
            null -> Unit
        }
    }

    private var picking: SettingsRow.PickFile? = null

    private fun pick(row: SettingsRow.PickFile) {
        picking = row
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(row.mimeTypes.singleOrNull() ?: "*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, row.mimeTypes.toTypedArray())
        startActivityForResult(intent, PICK_FILE)
    }

    override fun onActivityResultFragment(requestCode: Int, resultCode: Int, data: Intent?) {
        val row = picking.takeIf { requestCode == PICK_FILE } ?: return
        picking = null
        val uri = data?.data
        if (resultCode == Activity.RESULT_OK && uri != null) {
            row.picked(this, uri)
        }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean = false

    private fun choose(row: SettingsRow.Choice) {
        val context = parentActivity ?: return
        val account = account(row.option)
        val dialog = AlertDialog.Builder(context, resourceProvider)
            .setTitle(getString(row.title))
            .setItems(row.choices.map(row.label).toTypedArray()) { _, which ->
                options.set(row.option, row.choices[which], account)
                listView?.adapter?.update(true)
            }
            .setNegativeButton(getString(R.string.Cancel), null)
            .create()
        showDialog(dialog)
    }

    private fun edit(row: SettingsRow.Text) {
        val context = parentActivity ?: return
        val account = account(row.option)
        val field = EditTextBoldCursor(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourceProvider))
            setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, resourceProvider))
            background = Theme.createEditTextDrawable(context, true)
            setSingleLine(true)
            setPadding(0, dp(4f), 0, dp(4f))
            hint = getString(row.placeholder)
            setText(options.get(row.option, account))
            setSelection(length())
        }
        val frame = FrameLayout(context)
        frame.addView(field, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat(), 0, 24f, 6f, 24f, 0f))
        val dialog = AlertDialog.Builder(context, resourceProvider)
            .setTitle(getString(row.title))
            .setView(frame)
            .setPositiveButton(getString(R.string.OK)) { _, _ ->
                options.set(row.option, field.text.toString().trim(), account)
                listView.adapter.update(true)
            }
            .setNegativeButton(getString(R.string.Cancel), null)
            .create()
        showDialog(dialog)
        field.requestFocus()
        AndroidUtilities.runOnUIThread({ AndroidUtilities.showKeyboard(field) }, 200)
    }

    private fun account(option: Option<*>): Int =
        if (option.scope == OptionScope.ACCOUNT) currentAccount else Options.NO_ACCOUNT

    private companion object {
        // Telegram's own request codes stay well below this.
        const val PICK_FILE = 0x5e17
    }
}

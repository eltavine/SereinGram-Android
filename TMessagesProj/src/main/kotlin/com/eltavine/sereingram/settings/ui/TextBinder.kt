package com.eltavine.sereingram.settings.ui

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.ui.ButtonCell
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.UItem

internal object TextBinder : RowBinder<SettingsRow.Text> {
    override fun draw(row: SettingsRow.Text, id: Int, enabled: Boolean, host: RowHost, items: MutableList<UItem>) {
        val text = host.state[row.option]
        val unusable = Faults.guard("settings condition", fallback = false) {
            if (text.isEmpty()) row.requiredWhen?.holds(host.state) == true else row.check?.invoke(text) != null
        }
        items += ButtonCell.of(
            id,
            getString(row.title),
            value = shown(row, text),
            summary = row.summary?.let(::getString),
            unusable = unusable,
            enabled = enabled,
        )
    }

    // A short secret would show whole as its end.
    private fun shown(row: SettingsRow.Text, text: String): CharSequence = when {
        text.isEmpty() -> getString(row.placeholder)
        row.secret -> if (text.length > SECRET_SHOWN * 2) MASK + text.takeLast(SECRET_SHOWN) else MASK
        else -> text
    }

    override fun tap(row: SettingsRow.Text, index: Int, host: RowHost, view: View) = edit(row, host)

    private fun edit(row: SettingsRow.Text, host: RowHost) {
        val fragment = host.fragment
        val context = fragment.parentActivity ?: return
        val theme = fragment.resourceProvider
        val field = EditTextBoldCursor(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setTextColor(Theme.getColor(Theme.key_dialogTextBlack, theme))
            setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, theme))
            background = Theme.createEditTextDrawable(context, true)
            setSingleLine(true)
            // setSingleLine replaces the input type, masking included, so this comes after it.
            inputType = inputType(row)
            imeOptions = EditorInfo.IME_ACTION_DONE or if (row.secret) EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING else 0
            setPadding(0, dp(4f), 0, dp(4f))
            hint = getString(row.hint ?: row.placeholder)
            setText(host.state[row.option])
            setSelection(length())
        }
        val problem = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            setTextColor(Theme.getColor(Theme.key_text_RedRegular, theme))
            visibility = View.GONE
        }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(field, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
            addView(problem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 8f, 0f, 0f))
        }
        val frame = FrameLayout(context).apply {
            addView(column, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat(), 0, 24f, 6f, 24f, 0f))
        }
        val builder = AlertDialog.Builder(context, theme)
            .setTitle(getString(row.title))
            .setView(frame)
            .setNegativeButton(getString(R.string.Cancel)) { dialog, _ -> dialog.dismiss() }
        row.summary?.let { builder.setMessage(getString(it)) }
        if (host.state.isModified(row.option)) {
            builder.setNeutralButton(getString(R.string.Reset)) { dialog, _ ->
                host.commit(row, row.option.default)
                dialog.dismiss()
            }
        }
        lateinit var dialog: AlertDialog
        val save = {
            val text = field.text.toString().trim()
            val reason = if (text.isEmpty()) null else Faults.guard("settings check", fallback = null) { row.check?.invoke(text) }
            if (reason == null) {
                host.commit(row, text.ifEmpty { row.option.default })
                dialog.dismiss()
            } else {
                problem.text = getString(reason)
                problem.visibility = View.VISIBLE
                AndroidUtilities.shakeViewSpring(field, -6f)
            }
        }
        builder.setPositiveButton(getString(R.string.Save)) { _, _ -> save() }
        dialog = builder.create()
        // Buttons leave the dialog open, so that a text that cannot be saved can be corrected.
        dialog.setDismissDialogByButtons(false)
        // A hardware keyboard's Enter comes without an action of its own.
        field.setOnEditorActionListener { _, action, event ->
            val done = action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_NULL && event?.action == KeyEvent.ACTION_DOWN
            done.also { if (it) save() }
        }
        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(text: Editable?) {
                problem.visibility = View.GONE
            }
        })
        fragment.showDialog(dialog)
        field.requestFocus()
        AndroidUtilities.runOnUIThread({ AndroidUtilities.showKeyboard(field) }, 200)
    }

    private fun inputType(row: SettingsRow.Text): Int = when {
        row.secret -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        row.kind == SettingsRow.TextKind.URL -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        else -> InputType.TYPE_CLASS_TEXT
    }

    private const val MASK = "••••"
    private const val SECRET_SHOWN = 4
}

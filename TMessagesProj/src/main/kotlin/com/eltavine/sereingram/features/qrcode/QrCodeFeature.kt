package com.eltavine.sereingram.features.qrcode

import android.graphics.BitmapFactory
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.MessageMenuHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLoader
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessageObject
import org.telegram.messenger.R
import org.telegram.messenger.browser.Browser
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.BulletinFactory
import java.io.File

/** Reads the QR code in a photo from the photo's menu, after NagramX's item in the media viewer. */
object QrCodeFeature : SereinModule, SettingsContributor {
    override val id: String = "qr_code"

    override val options: List<Option<*>> = QrCodeOptions.all

    override fun start(context: ModuleContext) {
        MessageMenuHooks.entries.install(ScanEntry(context.options))
    }

    override val settingsIcon: Int = R.drawable.msg_qrcode

    override val settingsCategory: SettingsCategory = SettingsCategory.MEDIA

    override val settingsTint: SettingsTint = SettingsTint.GREEN

    override val settingsOrder: Int = 2

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_qr_title,
        summary = R.string.serein_qr_summary,
        sections = listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Toggle(QrCodeOptions.scanInMenu, R.string.serein_qr_scan_in_menu, summary = R.string.serein_qr_scan_in_menu_info),
                ),
                note = R.string.serein_qr_scan_in_menu_note,
            ),
        ),
    )
}

private class ScanEntry(private val options: Options) : MessageMenuHooks.Entry {
    // Telegram's global queue runs much of the app's work one task at a time; decoding would hold it up.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val option: Int = MessageMenuHooks.FIRST_OPTION + 301

    override val icon: Int = R.drawable.msg_qrcode

    override fun title(account: Int, message: Any): CharSequence = getString(R.string.serein_qr_scan)

    override fun isShown(account: Int, message: Any): Boolean {
        val shown = message as MessageObject
        return options.get(QrCodeOptions.scanInMenu) && isImage(shown) && file(account, shown) != null
    }

    override fun onSelected(account: Int, message: Any, host: Any) {
        val fragment = host as BaseFragment
        val image = file(account, message as MessageObject) ?: return
        scope.launch {
            val text = Faults.guard("qr code", fallback = null) { read(image) }
            AndroidUtilities.runOnUIThread {
                if (text == null) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, getString(R.string.serein_qr_none)).show()
                } else {
                    show(fragment, text)
                }
            }
        }
    }

    private fun show(fragment: BaseFragment, text: String) {
        val context = fragment.parentActivity ?: return
        val builder = AlertDialog.Builder(context, fragment.resourceProvider)
            .setTitle(getString(R.string.serein_qr_found))
            .setMessage(text)
            .setNegativeButton(getString(R.string.Copy)) { _, _ ->
                AndroidUtilities.addToClipboard(text)
                BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.TextCopied)).show()
            }
        if (isOpenable(text)) {
            builder.setPositiveButton(getString(R.string.Open)) { _, _ -> Browser.openUrl(context, text.trim()) }
        } else {
            builder.setPositiveButton(getString(R.string.Close), null)
        }
        fragment.showDialog(builder.create())
    }

    private fun isImage(message: MessageObject): Boolean =
        message.isPhoto ||
            message.document?.mime_type?.startsWith("image/") == true && !message.isAnyKindOfSticker

    // Where Telegram itself looks for a message's file when saving it to the gallery.
    private fun file(account: Int, message: MessageObject): File? {
        val loader = FileLoader.getInstance(account)
        return sequenceOf(
            { message.messageOwner?.attachPath?.takeIf(String::isNotEmpty)?.let(::File) },
            { loader.getPathToMessage(message.messageOwner) },
            { loader.getPathToMessage(message.messageOwner, true, true) },
        ).firstNotNullOfOrNull { candidate -> candidate()?.takeIf(File::isFile) }
    }

    private fun read(image: File): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(image.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) {
            sample *= 2
        }
        val bitmap = BitmapFactory.decodeFile(image.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return readQrCode(pixels, bitmap.width, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        // Codes stay readable at this size even in full-screen screenshots, and decoding stays quick.
        const val MAX_SIDE = 2048
    }
}

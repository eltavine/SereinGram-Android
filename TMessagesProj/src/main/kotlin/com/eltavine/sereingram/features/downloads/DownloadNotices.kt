package com.eltavine.sereingram.features.downloads

import android.app.Activity
import android.app.Application
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.eltavine.sereingram.core.Faults
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController.formatString
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.messenger.SharedConfig
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.LaunchActivity
import java.util.ArrayDeque

/** Show each result after the previous dialog closes, or after the app returns to the foreground. */
internal class DownloadNotices(application: Application) : Application.ActivityLifecycleCallbacks {
    private data class Notice(val title: Int, val message: Int, val directory: String?, val account: Int?)

    private val main = Handler(Looper.getMainLooper())
    private val pending = ArrayDeque<Notice>()
    private var showing = false
    private val drain = Runnable { showNext() }

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun enabled() = enqueue(Notice(R.string.serein_downloads_enable_title, R.string.serein_downloads_enable_message, null, null))

    fun completed(account: Int, tree: String) = enqueue(
        Notice(R.string.serein_downloads_complete_title, R.string.serein_downloads_complete_message,
            DownloadsFeature.directoryLabel(tree), account),
    )

    private fun enqueue(notice: Notice) {
        main.post {
            pending.addLast(notice)
            schedule()
        }
    }

    private fun schedule(delay: Long = 0) {
        main.removeCallbacks(drain)
        if (pending.isNotEmpty() && !showing) main.postDelayed(drain, delay)
    }

    private fun showNext() {
        if (showing || pending.isEmpty() || ApplicationLoader.mainInterfacePaused) return
        if (SharedConfig.appLocked || SharedConfig.isWaitingForPasscodeEnter) {
            schedule(500)
            return
        }
        val page = LaunchActivity.getLastFragmentIncludeMainTabs()
        val activity = page?.parentActivity
        if (page == null || activity == null || activity.isFinishing || activity.isDestroyed || page.isPaused) {
            schedule(500)
            return
        }
        val notice = pending.firstOrNull { it.account == null || it.account == page.currentAccount } ?: run {
            schedule(500)
            return
        }
        if (page.visibleDialog?.isShowing == true) {
            schedule(500)
            return
        }
        val dialog = AlertDialog.Builder(activity, page.resourceProvider)
            .setTitle(getString(notice.title))
            .setMessage(notice.directory?.let { formatString(notice.message, it) } ?: getString(notice.message))
            .setPositiveButton(getString(R.string.GotIt), null)
            .create()
        showing = true
        val dismissed = DialogInterface.OnDismissListener {
            // A lifecycle dismissal while the app leaves the screen can be shown after returning.
            if (!ApplicationLoader.mainInterfacePaused) pending.remove(notice)
            showing = false
            schedule()
        }
        // Some fragment sheets show the dialog themselves, bypassing BaseFragment's listener.
        dialog.setOnDismissListener(dismissed)
        val shown = Faults.guard("download notice", fallback = null) {
            page.showDialog(dialog, dismissed)
        }
        if (shown == null) {
            showing = false
            schedule(500)
        }
    }

    override fun onActivityResumed(activity: Activity) = schedule(100)
    override fun onActivityPaused(activity: Activity) { main.removeCallbacks(drain) }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

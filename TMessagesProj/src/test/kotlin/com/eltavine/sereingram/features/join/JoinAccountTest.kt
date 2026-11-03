package com.eltavine.sereingram.features.join

import android.app.Activity
import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.JoinGroupAlert

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], instrumentedPackages = ["org.telegram.tgnet"])
class JoinAccountTest {
    @Test fun joinSheetUsesTheRequestAccountEvenWhenTheSelectedAccountChanges() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        ApplicationLoader.applicationContext = activity.application
        AndroidUtilities.checkDisplaySize(activity, activity.resources.configuration)
        Theme.createCommonResources(activity)
        val before = UserConfig.selectedAccount
        try {
            UserConfig.selectedAccount = 0
            val parent = object : BaseFragment() {}.apply { setCurrentAccount(1) }
            val invite = TLRPC.TL_chatInvite().apply { title = "Test group"; photo = TLRPC.TL_photoEmpty() }
            assertEquals(1, JoinGroupAlert(activity, invite, "test", parent, null).currentAccount)
            assertEquals(2, JoinGroupAlert(activity, invite, "test", parent, null, JoinGroupAlert.ORIGINATION_OTHER, 2).currentAccount)
        } finally { UserConfig.selectedAccount = before; activity.finish() }
    }
}

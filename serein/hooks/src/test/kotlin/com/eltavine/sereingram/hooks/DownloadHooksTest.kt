package com.eltavine.sereingram.hooks

import kotlin.test.assertEquals
import kotlin.test.Test
import java.io.File

class DownloadHooksTest {
    @Test
    fun aFailingExporterDoesNotBreakOtherHandlersOrTheDownloadQueue() {
        val received = arrayListOf<String>()
        DownloadHooks.completedDownloads.install { _, _, _ -> error("Unavailable provider") }.use {
            DownloadHooks.completedDownloads.install { account, message, file -> received += "$account:$message:${file.name}" }.use {
                DownloadHooks.completed(2, "document", File("report.pdf"))
            }
        }
        assertEquals(listOf("2:document:report.pdf"), received)
    }
}

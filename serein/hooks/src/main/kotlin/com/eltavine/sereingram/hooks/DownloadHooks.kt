package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers
import java.io.File

/** Receives completed downloads after Telegram's completion events have been queued. */
public object DownloadHooks {
    public fun interface CompletedDownload {
        public fun completed(account: Int, message: Any, file: File)
    }

    public val completedDownloads: Handlers<CompletedDownload> = Handlers()

    @JvmStatic
    public fun completed(account: Int, message: Any, file: File) {
        completedDownloads.all.forEach { handler ->
            Faults.guard("completed download", fallback = Unit) { handler.completed(account, message, file) }
        }
    }
}

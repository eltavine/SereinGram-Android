package com.eltavine.sereingram.features.downloads

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.textOption

/** A picked directory belongs to this installation's URI grants, never to a settings backup. */
public object DownloadOptions {
    public val enabled: Option<Boolean> = booleanOption("downloads_external_enabled")
    public val directory: Option<String> = textOption("downloads_external_directory", backedUp = false)
    public val error: Option<String> = textOption("downloads_external_error", backedUp = false)
    public val all: List<Option<*>> = listOf(enabled, directory, error)
}

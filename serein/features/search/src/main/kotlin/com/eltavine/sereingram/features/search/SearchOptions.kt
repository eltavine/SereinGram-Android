package com.eltavine.sereingram.features.search

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** How searching from the chat list behaves, after the NagramXF requests; off by default. */
public object SearchOptions {
    public val hideGlobalResults: Option<Boolean> = booleanOption("search_hide_global_results")
    public val hideAppsTab: Option<Boolean> = booleanOption("search_hide_apps_tab")

    public val all: List<Option<*>> = listOf(hideGlobalResults, hideAppsTab)
}

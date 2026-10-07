package com.eltavine.sereingram.features.links

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** How links open, after NagramX's option of the same name; off by default. */
public object LinkOptions {
    public val confirmAll: Option<Boolean> = booleanOption("links_confirm_all")

    public val all: List<Option<*>> = listOf(confirmAll)
}

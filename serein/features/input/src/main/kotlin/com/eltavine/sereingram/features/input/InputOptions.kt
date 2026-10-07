package com.eltavine.sereingram.features.input

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** How the message field takes input, after NagramX's requests; off by default. */
public object InputOptions {
    public val plainPaste: Option<Boolean> = booleanOption("input_plain_paste")

    public val all: List<Option<*>> = listOf(plainPaste)
}

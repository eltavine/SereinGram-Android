package com.eltavine.sereingram.features.notifications

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** What message notifications show, after NagramX's request; off by default. */
public object NotificationOptions {
    public val contentWhenLocked: Option<Boolean> = booleanOption("notifications_content_when_locked")

    public val all: List<Option<*>> = listOf(contentWhenLocked)
}

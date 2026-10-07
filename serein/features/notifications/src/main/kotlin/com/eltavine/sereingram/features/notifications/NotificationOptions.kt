package com.eltavine.sereingram.features.notifications

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** What message notifications show and send, after NagramX's and Cherrygram's requests; off by default. */
public object NotificationOptions {
    public val contentWhenLocked: Option<Boolean> = booleanOption("notifications_content_when_locked")
    public val plainAnswers: Option<Boolean> = booleanOption("notifications_plain_answers")

    public val all: List<Option<*>> = listOf(contentWhenLocked, plainAnswers)
}

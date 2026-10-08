package com.eltavine.sereingram.features.backup

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.OptionType
import com.eltavine.sereingram.core.Options
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** What restoring a backup did. */
public sealed interface Restore {
    /**
     * [changed] options took other values; [skipped] entries were unknown or of the wrong kind.
     * A backup of another user's account brings back the options of the device only, [fromAnotherUser].
     */
    public class Done(public val changed: Int, public val skipped: Int, public val fromAnotherUser: Boolean = false) : Restore

    public data object NotABackup : Restore

    /** Written by a later release, whose format this one does not know. */
    public class TooNew(public val version: Int) : Restore
}

/**
 * SereinGram's settings as a JSON document, after Cherrygram's backups. A backup holds the
 * options of the device and of one account that differ from their defaults; options that are
 * not [Option.backedUp], such as secrets, stay out of it, and restoring one leaves them as they are.
 * It names the Telegram user of the account, whose options go back to that user only, in
 * whichever of the app's accounts they are signed in. It also lists every option the release
 * that wrote it could back up, so that restoring it leaves options added since as they are;
 * releases before the list ignore it, and backups without it reset whatever they do not hold.
 */
public object SettingsBackup {
    public const val FORMAT: String = "sereingram-settings"
    public const val VERSION: Int = 2

    /** [user] is the Telegram user signed in to [account]. */
    public fun write(options: Options, all: List<Option<*>>, account: Int, user: Long): String = buildJsonObject {
        put("format", JsonPrimitive(FORMAT))
        put("version", JsonPrimitive(VERSION))
        put("user", JsonPrimitive(user))
        put("known", JsonArray(all.filter { it.backedUp }.map { JsonPrimitive(it.key) }))
        OptionScope.entries.forEach { scope ->
            put(section(scope), buildJsonObject {
                backedUp(all, scope).forEach { option ->
                    val owner = owner(scope, account)
                    if (options.isModified(option, owner)) {
                        put(option.key, encode(options.get(option, owner)))
                    }
                }
            })
        }
    }.toString()

    /** [user] is the Telegram user signed in to [account]. */
    public fun restore(options: Options, all: List<Option<*>>, account: Int, user: Long, document: String): Restore {
        val root = runCatching { Json.parseToJsonElement(document) }.getOrNull() as? JsonObject ?: return Restore.NotABackup
        if ((root["format"] as? JsonPrimitive)?.contentOrNull != FORMAT) {
            return Restore.NotABackup
        }
        val version = (root["version"] as? JsonPrimitive)?.intOrNull ?: return Restore.NotABackup
        if (version > VERSION) {
            return Restore.TooNew(version)
        }
        // Backups of version 1 do not say whose they are, and go to any account as they always did.
        val savedBy = (root["user"] as? JsonPrimitive)?.longOrNull
        val fromAnotherUser = savedBy != null && savedBy != user
        val knownToWriter = (root["known"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf { key -> key.isString }?.content }?.toSet()
        var changed = 0
        var skipped = 0
        OptionScope.entries.forEach { scope ->
            if (scope == OptionScope.ACCOUNT && fromAnotherUser) {
                return@forEach
            }
            val owner = owner(scope, account)
            val values = root[section(scope)] as? JsonObject ?: JsonObject(emptyMap())
            val known = backedUp(all, scope).associateBy { it.key }
            skipped += values.keys.count { it !in known }
            known.values.forEach { option ->
                val stored = values[option.key]
                val value = stored?.let { decode(option.type, it) }
                if (stored != null && value == null) {
                    skipped++
                    return@forEach
                }
                val before = options.get(option, owner)
                when {
                    value != null -> options.setAny(option, value, owner)
                    knownToWriter == null || option.key in knownToWriter -> options.reset(option, owner)
                }
                if (options.get(option, owner) != before) {
                    changed++
                }
            }
        }
        return Restore.Done(changed, skipped, fromAnotherUser)
    }

    private fun backedUp(all: List<Option<*>>, scope: OptionScope) = all.filter { it.scope == scope && it.backedUp }

    private fun section(scope: OptionScope) = when (scope) {
        OptionScope.DEVICE -> "device"
        OptionScope.ACCOUNT -> "account"
    }

    private fun owner(scope: OptionScope, account: Int) = if (scope == OptionScope.ACCOUNT) account else Options.NO_ACCOUNT

    private fun encode(value: Any): JsonElement = when (value) {
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }

    // A value of another kind than the option's is not converted: such a file was edited by hand.
    private fun decode(type: OptionType<*>, element: JsonElement): Any? {
        val value = element as? JsonPrimitive ?: return null
        return when (type) {
            OptionType.Bool -> value.booleanOrNull.takeUnless { value.isString }
            OptionType.Int32 -> value.intOrNull.takeUnless { value.isString }
            OptionType.Int64 -> value.longOrNull.takeUnless { value.isString }
            OptionType.Text -> value.contentOrNull.takeIf { value.isString }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Options.setAny(option: Option<*>, value: Any, account: Int) = set(option as Option<Any>, value, account)
}

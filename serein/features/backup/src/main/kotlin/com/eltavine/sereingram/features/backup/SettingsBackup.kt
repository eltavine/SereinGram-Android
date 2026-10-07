package com.eltavine.sereingram.features.backup

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.OptionType
import com.eltavine.sereingram.core.Options
import kotlinx.serialization.json.Json
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
    /** [changed] options took other values; [skipped] entries were unknown or of the wrong kind. */
    public class Done(public val changed: Int, public val skipped: Int) : Restore

    public data object NotABackup : Restore

    /** Written by a later release, whose format this one does not know. */
    public class TooNew(public val version: Int) : Restore
}

/**
 * SereinGram's settings as a JSON document, after Cherrygram's backups. A backup holds the
 * options of the device and of one account that differ from their defaults; options that are
 * not [Option.backedUp], such as secrets, stay out of it, and restoring one leaves them as they are.
 */
public object SettingsBackup {
    public const val FORMAT: String = "sereingram-settings"
    public const val VERSION: Int = 1

    public fun write(options: Options, all: List<Option<*>>, account: Int): String = buildJsonObject {
        put("format", JsonPrimitive(FORMAT))
        put("version", JsonPrimitive(VERSION))
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

    public fun restore(options: Options, all: List<Option<*>>, account: Int, document: String): Restore {
        val root = runCatching { Json.parseToJsonElement(document) }.getOrNull() as? JsonObject ?: return Restore.NotABackup
        if ((root["format"] as? JsonPrimitive)?.contentOrNull != FORMAT) {
            return Restore.NotABackup
        }
        val version = (root["version"] as? JsonPrimitive)?.intOrNull ?: return Restore.NotABackup
        if (version > VERSION) {
            return Restore.TooNew(version)
        }
        var changed = 0
        var skipped = 0
        OptionScope.entries.forEach { scope ->
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
                if (value == null) options.reset(option, owner) else options.setAny(option, value, owner)
                if (options.get(option, owner) != before) {
                    changed++
                }
            }
        }
        return Restore.Done(changed, skipped)
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

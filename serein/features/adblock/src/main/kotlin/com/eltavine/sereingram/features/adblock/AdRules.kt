package com.eltavine.sereingram.features.adblock

import java.net.IDN
import java.net.URI
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.json.*

public enum class RuleKind { PREFIX, CONTAINS, DOMAIN, SENDER, MESSAGE, TEXT_HASH }
public data class AdRule(public val id: String, public val label: String, public val kind: RuleKind,
    public val value: String, public val dialogId: Long? = null)
public data class AdRuleSet(public val name: String, public val rules: List<AdRule> = emptyList(), public val domains: Set<String> = emptySet())
public data class AdMessage(public val dialogId: Long, public val id: Int, public val senderId: Long,
    public val text: String, public val urls: List<String> = emptyList())
public data class AdMatch(public val ruleId: String, public val label: String, public val source: String)

/** Only literal rules and domain matches are accepted; imports never execute arbitrary regex or scripts. */
public class AdRuleBook(sets: List<AdRuleSet>) {
    private val sets: List<AdRuleSet> = sets.map { set -> set.copy(rules = set.rules.map { rule ->
        if (rule.kind == RuleKind.PREFIX || rule.kind == RuleKind.CONTAINS) rule.copy(value = normalizeAdText(rule.value)) else rule
    }) }

    public fun match(message: AdMessage): AdMatch? {
        val text = normalizeAdText(message.text)
        val hash by lazy { adTextHash(text) }
        val hosts by lazy { (message.urls + LINK.findAll(message.text).map { it.value }).mapNotNull(::urlHost).toSet() }
        for (set in sets) {
            for (rule in set.rules) {
                if (rule.dialogId != null && rule.dialogId != message.dialogId) continue
                val matches = when (rule.kind) {
                    RuleKind.PREFIX -> text.startsWith(rule.value)
                    RuleKind.CONTAINS -> text.contains(rule.value)
                    RuleKind.DOMAIN -> hosts.any { it == rule.value || it.endsWith("." + rule.value) }
                    RuleKind.SENDER -> message.senderId.toString() == rule.value
                    RuleKind.MESSAGE -> message.id.toString() == rule.value
                    RuleKind.TEXT_HASH -> text.isNotEmpty() && hash == rule.value
                }
                if (matches) return AdMatch(rule.id, rule.label, set.name)
            }
            for (host in hosts) {
                var candidate = host
                while ('.' in candidate) {
                    if (candidate in set.domains) return AdMatch("domain:$candidate", candidate, set.name)
                    candidate = candidate.substringAfter('.')
                }
            }
        }
        return null
    }

    private companion object {
        val LINK = Regex("(?i)(?:https?://|www\\.)[^\\s<>\\\"\\u0000-\\u001f]+")
    }
}

public fun normalizeAdText(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

public fun adTextHash(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(normalizeAdText(text).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

public fun adDomain(value: String): String? = runCatching {
    val host = IDN.toASCII(value.trim().trimEnd('.'), IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
    host.takeIf { it.length <= 253 && '.' in it && it.split('.').all { label ->
        label.length in 1..63 && label.first().isLetterOrDigit() && label.last().isLetterOrDigit() &&
            label.all { c -> c.isLetterOrDigit() || c == '-' }
    } }
}.getOrNull()

private fun urlHost(url: String): String? = runCatching {
    val uri = URI(if (!url.contains("://") && !url.startsWith("mailto:", true) && !url.startsWith("tg:", true)) "https://$url" else url)
    if (uri.scheme?.lowercase(Locale.ROOT) !in listOf("http", "https")) null
    else (uri.host ?: uri.rawAuthority?.substringAfterLast('@')?.substringBefore(':'))?.let(::adDomain)
}.getOrNull()

public object AdRules {
    public const val MAX_BYTES: Long = 4L * 1024 * 1024
    public const val MAX_RULES: Int = 2_000
    public const val MAX_DOMAINS: Int = 150_000
    public const val RECOMMENDED_URL: String = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/popupads-onlydomains.txt"
    public val bundled: AdRuleSet = AdRuleSet("SereinGram", listOf("【广告】", "【推广】", "[广告]", "[推广]").mapIndexed { i, marker ->
        AdRule("bundled_$i", marker, RuleKind.PREFIX, marker)
    })

    public fun parse(text: String): AdRuleSet {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Rule file is too large" }
        val input = text.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        if (!input.startsWith('{')) return parseDomains(input)
        val root = Json.parseToJsonElement(input).jsonObject
        require(root["version"]?.jsonPrimitive?.intOrNull == 1) { "Unsupported rule format version" }
        val rulesJson = root["rules"]?.jsonArray ?: JsonArray(emptyList())
        require(rulesJson.size <= MAX_RULES) { "Too many rules" }
        val rules = rulesJson.map { item ->
            val row = item.jsonObject
            val kind = RuleKind.valueOf(row.getValue("kind").jsonPrimitive.content.uppercase(Locale.ROOT))
            val raw = row.getValue("value").jsonPrimitive.content
            val value = if (kind == RuleKind.DOMAIN) requireNotNull(adDomain(raw)) { "Invalid domain" } else raw
            val dialog = row["dialog_id"]?.jsonPrimitive?.content?.let { requireNotNull(it.toLongOrNull()) { "Invalid chat ID" } }
            val rule = AdRule(row.getValue("id").jsonPrimitive.content,
                row["label"]?.jsonPrimitive?.content ?: value, kind, value, dialog)
            validate(rule)
            rule
        }
        require(rules.map { it.id }.distinct().size == rules.size) { "Duplicate rule IDs" }
        val domains = root["domains"]?.jsonArray ?: JsonArray(emptyList())
        require(domains.size <= MAX_DOMAINS) { "Too many domains" }
        val normalized = domains.map { requireNotNull(adDomain(it.jsonPrimitive.content)) { "Invalid domain" } }.toSet()
        val name = root["name"]?.jsonPrimitive?.content ?: "Imported rules"
        require(name.isNotBlank() && name.length <= 120) { "Invalid rule set name" }
        return AdRuleSet(name, rules, normalized)
    }

    public fun encode(set: AdRuleSet): String = buildJsonObject {
        put("version", 1)
        put("name", set.name)
        putJsonArray("rules") { set.rules.forEach { rule -> add(buildJsonObject {
            put("id", rule.id); put("label", rule.label); put("kind", rule.kind.name.lowercase(Locale.ROOT)); put("value", rule.value)
            rule.dialogId?.let { put("dialog_id", it.toString()) }
        }) } }
        putJsonArray("domains") { set.domains.sorted().forEach { add(it) } }
    }.toString()

    private fun validate(rule: AdRule) {
        require(rule.id.isNotBlank() && rule.id.length <= 100 && rule.label.isNotBlank() && rule.label.length <= 200) { "Invalid rule ID or label" }
        require(rule.value.isNotBlank() && rule.value.length <= 512) { "Invalid rule value" }
        require(rule.dialogId == null || rule.dialogId < 0) { "Rules apply to groups and channels only" }
        when (rule.kind) {
            RuleKind.SENDER -> require(rule.value.toLongOrNull()?.let { it != 0L } == true) { "Invalid sender ID" }
            RuleKind.MESSAGE -> require(rule.dialogId != null && (rule.value.toIntOrNull() ?: 0) > 0) { "A message rule requires a chat ID and positive message ID" }
            RuleKind.TEXT_HASH -> require(Regex("[0-9a-f]{64}").matches(rule.value)) { "Invalid text hash" }
            else -> Unit
        }
    }

    private fun parseDomains(text: String): AdRuleSet {
        val domains = linkedSetOf<String>()
        for (line in text.lineSequence()) {
            val value = line.trim().removePrefix("\uFEFF")
            if (value.isEmpty() || value.startsWith('#') || value.startsWith('!')) continue
            val host = requireNotNull(adDomain(value.removePrefix("*."))) { "Expected JSON rules or one domain per line" }
            domains.add(host)
            require(domains.size <= MAX_DOMAINS) { "Too many domains" }
        }
        require(domains.isNotEmpty()) { "The domain list is empty" }
        return AdRuleSet("Domain subscription", domains = domains)
    }
}

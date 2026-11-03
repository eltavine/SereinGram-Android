package com.eltavine.sereingram.features.adblock

import android.content.Context
import android.util.AtomicFile
import com.eltavine.sereingram.core.Faults
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal data class BlockRecord(val dialog: Long, val message: Int, val rule: String, val source: String,
    val text: String, val time: Long)

/** Account data stays on this device. No message text is sent to a rule subscription. */
internal class AdBlockStore(context: Context, private val changed: (Int) -> Unit) {
    val io = Executors.newSingleThreadScheduledExecutor { Thread(it, "serein-adblock") }
    private val root = File(context.filesDir, "serein_adblock")
    private val states = ConcurrentHashMap<Int, State>()
    private val loading = ConcurrentHashMap.newKeySet<Int>()

    class State(val owner: Long) {
        var manual = emptyList<AdRule>()
        var imported = AdRuleSet("Imported")
        var remote = AdRuleSet("Subscription")
        val allowed = LinkedHashSet<String>()
        val records = LinkedHashMap<String, BlockRecord>()
        var book = AdRuleBook(emptyList())
        var withBundled = AdRuleBook(listOf(AdRules.bundled))
        val matches = object : LinkedHashMap<Pair<AdMessage, Boolean>, AdMatch?>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<AdMessage, Boolean>, AdMatch?>): Boolean = size > 512
        }
        val saving = AtomicBoolean()
        fun rebuild() {
            matches.clear()
            val sets = listOf(AdRuleSet("Manual", manual), imported, remote)
            book = AdRuleBook(sets)
            withBundled = AdRuleBook(listOf(AdRules.bundled) + sets)
        }
    }

    fun peek(account: Int): State? = states[account]

    fun load(account: Int, owner: Long) {
        if (owner == 0L || states[account]?.owner == owner || !loading.add(account)) return
        io.execute {
            try {
                ensure(account, owner)
                changed(account)
            } catch (error: Exception) {
                Faults.report("adblock data", error)
            } finally { loading.remove(account) }
        }
    }

    /** Called on the serial IO executor before imports, updates and manual edits. */
    @Synchronized
    fun ensure(account: Int, owner: Long): State = states[account]?.takeIf { it.owner == owner } ?: read(account, owner).also { states[account] = it }

    private fun read(account: Int, owner: Long): State {
        val state = State(owner)
        val file = file(account, owner, "state")
        if (file.exists()) {
            val json = JSONObject(AtomicFile(file).openRead().bufferedReader().use { it.readText() })
            state.manual = AdRules.parse(json.getString("manual")).rules
            state.imported = AdRules.parse(json.getString("imported"))
            json.optJSONArray("allowed")?.let { array -> for (i in 0 until minOf(array.length(), 1000)) state.allowed.add(array.getString(i)) }
            json.optJSONArray("records")?.let { array -> for (i in 0 until minOf(array.length(), 500)) {
                val row = array.getJSONObject(i)
                val record = BlockRecord(row.getLong("dialog"), row.getInt("message"), row.getString("rule"),
                    row.getString("source"), row.getString("text"), row.getLong("time"))
                state.records[key(record.dialog, record.message)] = record
            } }
        }
        val remote = file(account, owner, "subscription")
        if (remote.exists()) state.remote = AdRules.parse(AtomicFile(remote).openRead().bufferedReader().use { it.readText() })
        state.rebuild()
        return state
    }

    fun match(account: Int, owner: Long, message: AdMessage, bundled: Boolean): AdMatch? {
        val state = states[account]?.takeIf { it.owner == owner } ?: run { load(account, owner); return null }
        synchronized(state) {
            val key = key(message.dialogId, message.id)
            if (key in state.allowed) return null
            val cacheKey = message to bundled
            if (!state.matches.containsKey(cacheKey)) state.matches[cacheKey] = (if (bundled) state.withBundled else state.book).match(message)
            val match = state.matches[cacheKey] ?: return null
            if (state.records[key]?.let { it.rule == match.label && it.source == match.source && it.text == message.text.take(160) } != true) {
                state.records.remove(key)
                state.records[key] = BlockRecord(message.dialogId, message.id, match.label, match.source, message.text.take(160), System.currentTimeMillis())
                while (state.records.size > 500) state.records.remove(state.records.keys.first())
                saveLater(account, state)
            }
            return match
        }
    }

    fun add(account: Int, rule: AdRule, owner: Long? = null, markedMessage: Int? = null) = edit(account, owner) { state ->
        val previous = state.manual.filterNot { it.id == rule.id }
        require(previous.size + state.imported.rules.size < AdRules.MAX_RULES) { "Too many rules" }
        state.manual = previous + rule
        state.allowed.remove(key(rule.dialogId ?: 0, markedMessage ?: rule.value.toIntOrNull() ?: 0))
    }

    fun replaceImported(account: Int, set: AdRuleSet, owner: Long? = null) = edit(account, owner) {
        require(it.manual.size + set.rules.size <= AdRules.MAX_RULES) { "Too many rules" }
        it.imported = set
    }

    fun replaceRemote(account: Int, set: AdRuleSet, owner: Long? = null) {
        val state = ownedState(account, owner)
        write(file(account, state.owner, "subscription"), AdRules.encode(set))
        synchronized(state) { state.remote = set; state.rebuild() }
        changed(account)
    }

    fun remove(account: Int, id: String, manual: Boolean, owner: Long? = null) = edit(account, owner) {
        if (manual) it.manual = it.manual.filterNot { rule -> rule.id == id }
        else it.imported = it.imported.copy(rules = it.imported.rules.filterNot { rule -> rule.id == id })
    }

    fun allow(account: Int, record: BlockRecord, owner: Long? = null) = edit(account, owner) {
        it.allowed.add(key(record.dialog, record.message))
        while (it.allowed.size > 1000) it.allowed.remove(it.allowed.first())
    }

    fun clearHistory(account: Int, owner: Long? = null) = edit(account, owner) { it.records.clear() }
    fun clearAllowed(account: Int, owner: Long? = null) = edit(account, owner) { it.allowed.clear() }
    fun clearImported(account: Int, owner: Long? = null) = edit(account, owner) { it.imported = AdRuleSet("Imported") }

    fun export(account: Int, owner: Long? = null): String {
        val state = ownedState(account, owner)
        return synchronized(state) { AdRules.encode(AdRuleSet("My rules",
            (state.manual + state.imported.rules).mapIndexed { i, rule -> rule.copy(id = "rule_$i") }, state.imported.domains)) }
    }

    private fun ownedState(account: Int, owner: Long?): State = requireNotNull(states[account]?.takeIf { owner == null || it.owner == owner }) {
        "Account changed or rules are still loading"
    }

    private fun edit(account: Int, owner: Long?, change: (State) -> Unit) {
        val state = ownedState(account, owner)
        synchronized(state) { change(state); state.rebuild(); saveLater(account, state) }
        changed(account)
    }

    private fun saveLater(account: Int, state: State) {
        if (!state.saving.compareAndSet(false, true)) return
        io.schedule({
            state.saving.set(false)
            if (states[account] !== state) return@schedule
            try {
                val text = synchronized(state) {
                    JSONObject().put("manual", AdRules.encode(AdRuleSet("Manual", state.manual)))
                        .put("imported", AdRules.encode(state.imported)).put("allowed", JSONArray(state.allowed.toList()))
                        .put("records", JSONArray(state.records.values.map { row -> JSONObject()
                            .put("dialog", row.dialog).put("message", row.message).put("rule", row.rule)
                            .put("source", row.source).put("text", row.text).put("time", row.time) })).toString()
                }
                write(file(account, state.owner, "state"), text)
            } catch (error: Exception) { Faults.report("adblock save", error) }
        }, 300, TimeUnit.MILLISECONDS)
    }

    @Synchronized
    fun forget(account: Int) {
        val state = states.remove(account) ?: return
        io.execute {
            AtomicFile(file(account, state.owner, "state")).delete()
            AtomicFile(file(account, state.owner, "subscription")).delete()
        }
    }

    private fun file(account: Int, owner: Long, kind: String) = File(root, "$account-$owner-$kind.json")
    private fun write(file: File, text: String) {
        file.parentFile!!.mkdirs()
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try { stream.write(text.toByteArray(Charsets.UTF_8)); atomic.finishWrite(stream) }
        catch (error: Exception) { atomic.failWrite(stream); throw error }
    }
    private fun key(dialog: Long, message: Int) = "$dialog:$message"
}

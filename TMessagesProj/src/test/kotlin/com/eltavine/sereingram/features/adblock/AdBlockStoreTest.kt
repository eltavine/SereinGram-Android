package com.eltavine.sereingram.features.adblock

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34])
class AdBlockStoreTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private fun close(store: AdBlockStore) { store.io.shutdown(); assertTrue(store.io.awaitTermination(5, TimeUnit.SECONDS)) }
    @Test fun ruleChangesInvalidateCachedDecisionsAndRestoringAMessageSurvivesRestart() {
        val store = AdBlockStore(app) {}
        store.ensure(0, 123)
        val message = AdMessage(-42, 5, 7, "offer")
        assertNull(store.match(0, 123, message, false))
        store.add(0, AdRule("manual", "Offer", RuleKind.CONTAINS, "offer", -42))
        assertNotNull(store.match(0, 123, message, false))
        val record = store.peek(0)!!.records.values.single()
        store.allow(0, record)
        assertNull(store.match(0, 123, message, false))
        close(store)
        val restored = AdBlockStore(app) {}
        restored.ensure(0, 123)
        assertNull(restored.match(0, 123, message, false))
        assertEquals(1, restored.peek(0)!!.records.size)
        close(restored)
    }

    @Test fun accountSlotsAndUserIdentitiesDoNotShareRules() {
        val store = AdBlockStore(app) {}
        store.ensure(0, 123)
        store.add(0, AdRule("rule", "Ad", RuleKind.CONTAINS, "offer"))
        store.ensure(1, 456)
        assertNull(store.match(1, 456, AdMessage(-42, 5, 7, "offer"), false))
        store.ensure(0, 999)
        assertNull(store.match(0, 999, AdMessage(-42, 5, 7, "offer"), false))
        close(store)
    }

    @Test fun malformedImportCannotReplaceGoodRulesAndLogsAreBoundedAndDeduplicated() {
        val store = AdBlockStore(app) {}
        store.ensure(0, 123)
        store.replaceImported(0, AdRuleSet("Good", listOf(AdRule("ad", "Ad", RuleKind.CONTAINS, "offer"))))
        assertThrows(Exception::class.java) { store.replaceImported(0, AdRules.parse("broken import")) }
        for (id in 1..510) repeat(2) { store.match(0, 123, AdMessage(-42, id, 7, "offer $id"), false) }
        assertEquals(500, store.peek(0)!!.records.size)
        assertEquals("Good", store.peek(0)!!.imported.name)
        close(store)
    }

    @Test fun subscriptionUrlsRequireHttpsAndRejectCredentials() {
        assertTrue(AdBlockFeature.validSubscription(AdRules.RECOMMENDED_URL))
        assertFalse(AdBlockFeature.validSubscription("http://example.com/rules.json"))
        assertFalse(AdBlockFeature.validSubscription("https://user:password@example.com/rules.json"))
        assertFalse(AdBlockFeature.validSubscription("file:///sdcard/rules.json"))
    }

    @Test fun anOldAccountsDelayedUpdateCannotOverwriteANewLogin() {
        val store = AdBlockStore(app) {}
        store.ensure(0, 123)
        store.ensure(0, 456)
        val old = AdRuleSet("Old account", listOf(AdRule("old", "Old rule", RuleKind.CONTAINS, "offer")))
        assertThrows(Exception::class.java) { store.replaceRemote(0, old, 123) }
        assertThrows(Exception::class.java) { store.replaceImported(0, old, 123) }
        assertThrows(Exception::class.java) { store.export(0, 123) }
        assertTrue(store.peek(0)!!.remote.rules.isEmpty())
        assertEquals(456L, store.peek(0)!!.owner)
        close(store)
    }
}

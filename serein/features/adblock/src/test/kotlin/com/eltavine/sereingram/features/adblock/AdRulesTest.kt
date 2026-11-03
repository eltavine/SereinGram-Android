package com.eltavine.sereingram.features.adblock

import kotlin.test.*

class AdRulesTest {
    private fun message(text: String, dialog: Long = -42, id: Int = 5, sender: Long = 7, urls: List<String> = emptyList()) = AdMessage(dialog, id, sender, text, urls)
    private fun book(vararg rules: AdRule) = AdRuleBook(listOf(AdRuleSet("test", rules.toList())))

    @Test fun domainMatchesRespectLabelBoundariesAndHiddenLinks() {
        val engine = book(AdRule("ad", "Ads", RuleKind.DOMAIN, "ads.example"))
        assertNotNull(engine.match(message("https://sub.ads.example/path")))
        assertNotNull(engine.match(message("read more", urls = listOf("https://ads.example/path"))))
        assertNotNull(engine.match(message("ads.example/path", urls = listOf("ads.example/path"))))
        assertNull(engine.match(message("email", urls = listOf("mailto:user@ads.example"))))
        assertNull(engine.match(message("https://notads.example/")))
        assertNull(engine.match(message("https://ads.example.legitimate.example/")))
        assertNull(engine.match(message("https://legitimate.example/ads.example")))
        assertNull(engine.match(message("https://ads.example@legitimate.example/path")))
        assertNotNull(engine.match(message("HtTpS://ADS.EXAMPLE/")))
    }

    @Test fun scopedManualRulesDoNotHideUnrelatedChatsOrMessages() {
        val engine = book(AdRule("one", "One", RuleKind.MESSAGE, "5", -42))
        assertNotNull(engine.match(message("any")))
        assertNull(engine.match(message("any", dialog = -43)))
        assertNull(engine.match(message("any", id = 6)))
        val sender = book(AdRule("user", "User", RuleKind.SENDER, "7", -42))
        assertNotNull(sender.match(message("any")))
        assertNull(sender.match(message("any", sender = 8)))
    }

    @Test fun identicalTextIsNormalizedButOtherMessagesRemainVisible() {
        val hash = adTextHash("AD  OFFER")
        val engine = book(AdRule("repeat", "Repeated ad", RuleKind.TEXT_HASH, hash, -42))
        assertNotNull(engine.match(message("ＡＤ\nＯＦＦＥＲ")))
        assertNull(engine.match(message("ad offer tomorrow")))
        assertNull(engine.match(message("ad offer", dialog = -43)))
    }

    @Test fun bundledRulesUseExplicitAdvertisingMarkers() {
        val engine = AdRuleBook(listOf(AdRules.bundled))
        assertNotNull(engine.match(message("【广告】new offer")))
        assertNull(engine.match(message("讨论免费软件和会员服务")))
        assertNull(engine.match(message("这篇文章介绍【广告】标签")))
    }

    @Test fun jsonRoundTripsLargeIdsWithoutLoss() {
        val original = AdRuleSet("My rules", listOf(AdRule("x", "Sender", RuleKind.SENDER, "9223372036854775806", -1001234567890)), setOf("ads.example"))
        assertEquals(original, AdRules.parse("\uFEFF" + AdRules.encode(original)))
    }

    @Test fun malformedOrDangerouslyBroadRulesAreRejected() {
        assertFails { AdRules.parse("""{"version":2,"rules":[]}""") }
        assertFails { AdRules.parse("""{"version":1,"rules":[{"id":"x","kind":"contains","value":" "}]}""") }
        assertFails { AdRules.parse("""{"version":1,"rules":[{"id":"x","kind":"message","value":"5"}]}""") }
        assertFails { AdRules.parse("""{"version":1,"rules":[{"id":"x","kind":"regex","value":"(a+)+$"}]}""") }
        assertFails { AdRules.parse("||ads.example^") }
        assertFails { AdRules.parse("*.com") }
    }

    @Test fun subscriptionsSupportCommentsWildcardsAndSubdomains() {
        val set = AdRules.parse("# Source\n*.ads.example\nads.example\ntracker.example\n")
        assertEquals(2, set.domains.size)
        assertNotNull(AdRuleBook(listOf(set)).match(message("https://sub.ads.example/a")))
        assertNull(AdRuleBook(listOf(set)).match(message("https://ads.example.evil.test/a")))
    }

    @Test fun oversizedRulesAndDuplicateIdentifiersAreRejected() {
        assertFails { AdRules.parse("x".repeat(AdRules.MAX_BYTES.toInt() + 1)) }
        val rule = AdRule("same", "Ad", RuleKind.CONTAINS, "offer")
        assertFails { AdRules.parse(AdRules.encode(AdRuleSet("duplicate", listOf(rule, rule)))) }
    }
}

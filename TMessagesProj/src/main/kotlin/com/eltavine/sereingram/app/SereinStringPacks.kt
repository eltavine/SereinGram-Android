package com.eltavine.sereingram.app

import com.eltavine.sereingram.hooks.LocaleHooks
import org.telegram.localization.NamespaceLocalizationUtils
import java.util.Locale

/**
 * SereinGram's strings and its brand overlay, as packs of Telegram's localization. For a
 * locale they have no strings in, the generated lookup returns the English pack, which
 * Telegram already loads beneath every locale; loading it again would cover that locale's
 * own translations of the strings the brand overlay renames.
 */
internal object SereinStringPacks : LocaleHooks.StringPacks {
    private val lookups: List<(Locale) -> String?> = listOf(
        NamespaceLocalizationUtils::getLocalizationAssetSerein,
        NamespaceLocalizationUtils::getLocalizationAssetBrand,
    )

    override fun packsFor(locale: Locale): List<String> = lookups.mapNotNull { lookup ->
        lookup(locale)?.takeIf { locale.language == Locale.ENGLISH.language || it != lookup(Locale.ENGLISH) }
    }
}

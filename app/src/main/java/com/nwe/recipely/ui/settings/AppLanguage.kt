package com.nwe.recipely.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * In-app language choice. The empty tag means "follow the system locale". Persistence and
 * application are delegated to AppCompat (autoStoreLocales), so this lives in the Android layer,
 * not the ViewModel.
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    GERMAN("de");

    companion object {
        /** The language currently applied by AppCompat, mapped back to an enum value. */
        fun current(): AppLanguage {
            val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return entries.firstOrNull { it.tag.isNotEmpty() && tags.startsWith(it.tag) } ?: SYSTEM
        }
    }
}

/** Apply this language app-wide; AppCompat recreates the Activity and persists the choice. */
fun AppLanguage.applyNow() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}

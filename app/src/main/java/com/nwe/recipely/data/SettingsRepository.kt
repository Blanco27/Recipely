package com.nwe.recipely.data

import kotlinx.coroutines.flow.Flow

/** How the app resolves its light/dark appearance. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Persisted user preferences. Only the theme lives here; the language is owned by AppCompat's
 * locale store (see ui/settings/AppLanguage.kt), which keeps this interface pure-Kotlin and the
 * ViewModel JVM-unit-testable.
 */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

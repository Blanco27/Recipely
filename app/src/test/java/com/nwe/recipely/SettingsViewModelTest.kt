package com.nwe.recipely

import com.nwe.recipely.data.ThemeMode
import com.nwe.recipely.ui.settings.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun defaultThemeMode_isSystem() = runTest {
        val vm = SettingsViewModel(FakeSettingsRepository())
        backgroundScope.launch { vm.themeMode.collect {} }
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)
    }

    @Test
    fun setThemeMode_propagatesToFlow() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        backgroundScope.launch { vm.themeMode.collect {} }
        vm.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }
}

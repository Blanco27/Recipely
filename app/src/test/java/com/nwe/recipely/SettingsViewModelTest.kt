package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.ThemeMode
import com.nwe.recipely.ui.settings.BackupCommand
import com.nwe.recipely.ui.settings.BackupMessage
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

    private fun newVm(
        repo: FakeRecipeRepository = FakeRecipeRepository(),
        backup: FakeBackupManager = FakeBackupManager(),
    ) = SettingsViewModel(FakeSettingsRepository(), repo, backup)

    @Test
    fun defaultThemeMode_isSystem() = runTest {
        val vm = newVm()
        backgroundScope.launch { vm.themeMode.collect {} }
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)
    }

    @Test
    fun setThemeMode_propagatesToFlow() = runTest {
        val vm = newVm()
        backgroundScope.launch { vm.themeMode.collect {} }
        vm.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }

    @Test
    fun onExportClicked_whenEmpty_emitsEmptyMessage() = runTest {
        val repo = FakeRecipeRepository() // recipes default to emptyList()
        val vm = newVm(repo)
        val events = mutableListOf<BackupMessage>()
        backgroundScope.launch { vm.events.collect { events.add(it) } }

        vm.onExportClicked()
        advanceUntilIdle()

        assertEquals(listOf(BackupMessage.Empty), events)
    }

    @Test
    fun onExportClicked_whenNotEmpty_emitsPickExportCommand() = runTest {
        val repo = FakeRecipeRepository().apply { recipes.value = listOf(Recipe(name = "X")) }
        val vm = newVm(repo)
        val commands = mutableListOf<BackupCommand>()
        backgroundScope.launch { vm.commands.collect { commands.add(it) } }

        vm.onExportClicked()
        advanceUntilIdle()

        assertEquals(listOf(BackupCommand.PickExportTarget), commands)
    }

    @Test
    fun onImportClicked_emitsPickImportCommand() = runTest {
        val vm = newVm()
        val commands = mutableListOf<BackupCommand>()
        backgroundScope.launch { vm.commands.collect { commands.add(it) } }

        vm.onImportClicked()
        advanceUntilIdle()

        assertEquals(listOf(BackupCommand.PickImportSource), commands)
    }
}

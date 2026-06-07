package com.nwe.recipely.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.RecipeRepository
import com.nwe.recipely.data.SettingsRepository
import com.nwe.recipely.data.ThemeMode
import com.nwe.recipely.data.backup.BackupManager
import com.nwe.recipely.data.backup.ExportResult
import com.nwe.recipely.data.backup.ImportResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whether a backup operation is in flight (drives the progress overlay). */
enum class BackupState { Idle, Exporting, Importing }

/** One-shot requests for the UI to open a SAF picker. */
sealed interface BackupCommand {
    data object PickExportTarget : BackupCommand
    data object PickImportSource : BackupCommand
}

/** One-shot results to show as a snackbar. */
sealed interface BackupMessage {
    data object Empty : BackupMessage
    data class ExportDone(val count: Int) : BackupMessage
    data class ImportDone(val count: Int) : BackupMessage
    data object InvalidFile : BackupMessage
    data object ExportFailed : BackupMessage
    data object ImportFailed : BackupMessage
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val recipeRepository: RecipeRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    private val _backupState = MutableStateFlow(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    private val _commands = Channel<BackupCommand>(Channel.BUFFERED)
    val commands = _commands.receiveAsFlow()

    private val _events = Channel<BackupMessage>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /** Guards empty export before opening the file picker. */
    fun onExportClicked() {
        viewModelScope.launch {
            if (recipeRepository.observeRecipes().first().isEmpty()) {
                _events.send(BackupMessage.Empty)
            } else {
                _commands.send(BackupCommand.PickExportTarget)
            }
        }
    }

    fun onImportClicked() {
        viewModelScope.launch { _commands.send(BackupCommand.PickImportSource) }
    }

    fun export(target: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Exporting
            val message = when (val r = backupManager.export(target)) {
                is ExportResult.Success -> BackupMessage.ExportDone(r.count)
                ExportResult.Error -> BackupMessage.ExportFailed
            }
            _events.send(message)
            _backupState.value = BackupState.Idle
        }
    }

    fun import(source: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Importing
            val message = when (val r = backupManager.import(source)) {
                is ImportResult.Success -> BackupMessage.ImportDone(r.count)
                ImportResult.Invalid -> BackupMessage.InvalidFile
                ImportResult.Error -> BackupMessage.ImportFailed
            }
            _events.send(message)
            _backupState.value = BackupState.Idle
        }
    }
}

package com.nwe.recipely.ui.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.ThemeMode
import com.nwe.recipely.ui.theme.ExportIconBgDark
import com.nwe.recipely.ui.theme.ExportIconBgLight
import com.nwe.recipely.ui.theme.Fraunces
import com.nwe.recipely.ui.theme.ImportIconBgDark
import com.nwe.recipely.ui.theme.ImportIconBgLight
import com.nwe.recipely.ui.theme.LocalDarkTheme
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as RecipelyApp).container
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(container.settingsRepository, container.repository, container.backupManager)
            }
        }
    )
    val themeMode by vm.themeMode.collectAsState()
    var language by remember { mutableStateOf(AppLanguage.current()) }

    val context = LocalContext.current
    val backupState by vm.backupState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(vm::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::import) }

    LaunchedEffect(Unit) {
        vm.commands.collect { command ->
            when (command) {
                BackupCommand.PickExportTarget -> exportLauncher.launch(defaultExportFileName())
                BackupCommand.PickImportSource ->
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { message -> snackbarHostState.showSnackbar(message.toText(context)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                SectionLabel(stringResource(R.string.settings_appearance))
                ThemePanel(selected = themeMode, onSelect = vm::setThemeMode)

                Spacer(Modifier.height(20.dp))

                SectionLabel(stringResource(R.string.settings_language))
                LanguagePanel(
                    selected = language,
                    onSelect = {
                        language = it
                        it.applyNow()
                    },
                )

                Spacer(Modifier.height(20.dp))

                SectionLabel(stringResource(R.string.settings_data))
                DataPanel(
                    onExport = vm::onExportClicked,
                    onImport = vm::onImportClicked,
                )

                Spacer(Modifier.weight(1f))

                AboutFooter()
            }

            if (backupState != BackupState.Idle) {
                BackupOverlay(
                    label = stringResource(
                        if (backupState == BackupState.Exporting) R.string.backup_exporting
                        else R.string.backup_importing,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp),
    )
}

/** Paper card matching the mockup's `.panel` (elevated near-white, soft warm border). */
@Composable
private fun SettingsPanel(content: @Composable () -> Unit) {
    Surface(
        color = if (LocalDarkTheme.current) PaperDark else Paper,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(6.dp)) { content() }
    }
}

@Composable
private fun ThemePanel(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "🖥️", R.string.theme_system),
        Triple(ThemeMode.LIGHT, "☀️", R.string.theme_light),
        Triple(ThemeMode.DARK, "🌙", R.string.theme_dark),
    )
    SettingsPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (mode, emoji, labelRes) ->
                ThemeSegment(
                    selected = selected == mode,
                    emoji = emoji,
                    label = stringResource(labelRes),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(mode) },
                )
            }
        }
    }
}

@Composable
private fun ThemeSegment(
    selected: Boolean,
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

private data class LanguageOption(
    val lang: AppLanguage,
    val flag: String,
    val labelRes: Int,
    val captionRes: Int?,
)

@Composable
private fun LanguagePanel(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val options = listOf(
        LanguageOption(AppLanguage.SYSTEM, "🌐", R.string.language_system, R.string.language_system_caption),
        LanguageOption(AppLanguage.ENGLISH, "🇬🇧", R.string.language_english, null),
        LanguageOption(AppLanguage.GERMAN, "🇩🇪", R.string.language_german, null),
    )
    SettingsPanel {
        options.forEachIndexed { index, option ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )
            }
            LanguageRow(
                flag = option.flag,
                label = stringResource(option.labelRes),
                caption = option.captionRes?.let { stringResource(it) },
                selected = selected == option.lang,
                onClick = { onSelect(option.lang) },
            )
        }
    }
}

@Composable
private fun LanguageRow(
    flag: String,
    label: String,
    caption: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = flag, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun DataPanel(onExport: () -> Unit, onImport: () -> Unit) {
    val dark = LocalDarkTheme.current
    SettingsPanel {
        DataRow(
            iconBackground = if (dark) ExportIconBgDark else ExportIconBgLight,
            emoji = "📤",
            title = stringResource(R.string.export_recipes_title),
            subtitle = stringResource(R.string.export_recipes_subtitle),
            onClick = onExport,
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
        )
        DataRow(
            iconBackground = if (dark) ImportIconBgDark else ImportIconBgLight,
            emoji = "📥",
            title = stringResource(R.string.import_recipes_title),
            subtitle = stringResource(R.string.import_recipes_subtitle),
            onClick = onImport,
        )
    }
}

@Composable
private fun DataRow(
    iconBackground: Color,
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackupOverlay(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = if (LocalDarkTheme.current) PaperDark else Paper,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun defaultExportFileName(): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return "recipely-backup-$date.zip"
}

private fun BackupMessage.toText(context: Context): String = when (this) {
    BackupMessage.Empty -> context.getString(R.string.backup_export_empty)
    is BackupMessage.ExportDone ->
        context.resources.getQuantityString(R.plurals.backup_export_done, count, count)
    is BackupMessage.ImportDone ->
        context.resources.getQuantityString(R.plurals.backup_import_done, count, count)
    BackupMessage.InvalidFile -> context.getString(R.string.backup_error_invalid)
    BackupMessage.ExportFailed -> context.getString(R.string.backup_error_export)
    BackupMessage.ImportFailed -> context.getString(R.string.backup_error_import)
}

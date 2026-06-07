package com.nwe.recipely

import android.net.Uri
import com.nwe.recipely.data.backup.BackupManager
import com.nwe.recipely.data.backup.ExportResult
import com.nwe.recipely.data.backup.ImportResult

/** Test double. The ViewModel unit tests exercise click handlers, not the Uri paths. */
class FakeBackupManager(
    private val exportResult: ExportResult = ExportResult.Success(0),
    private val importResult: ImportResult = ImportResult.Success(0),
) : BackupManager {
    override suspend fun export(target: Uri): ExportResult = exportResult
    override suspend fun import(source: Uri): ImportResult = importResult
}

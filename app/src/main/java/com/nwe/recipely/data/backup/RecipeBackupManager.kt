package com.nwe.recipely.data.backup

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.nwe.recipely.data.ImageStore
import com.nwe.recipely.data.RecipeDao
import com.nwe.recipely.data.RecipeWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed interface ExportResult {
    data class Success(val count: Int) : ExportResult
    data object Error : ExportResult
}

sealed interface ImportResult {
    data class Success(val count: Int) : ImportResult
    data object Invalid : ImportResult
    data object Error : ImportResult
}

/** Reads/writes a [Uri] from SAF; both methods run on [Dispatchers.IO]. */
interface BackupManager {
    suspend fun export(target: Uri): ExportResult
    suspend fun import(source: Uri): ImportResult
}

class RecipeBackupManager(
    private val dao: RecipeDao,
    private val imageStore: ImageStore,
    private val contentResolver: ContentResolver,
) : BackupManager {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    override suspend fun export(target: Uri): ExportResult = withContext(Dispatchers.IO) {
        try {
            val recipes = dao.getAllRecipesWithDetails()
            val output = contentResolver.openOutputStream(target) ?: return@withContext ExportResult.Error
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                val imageEntries = writeImages(zip, recipes)
                val dtos = recipes.map { it.toBackupRecipe(imageEntries) }
                val file = BackupFile(SCHEMA_VERSION, nowIso(), dtos)
                zip.putNextEntry(ZipEntry(JSON_ENTRY))
                zip.write(json.encodeToString(file).toByteArray())
                zip.closeEntry()
            }
            ExportResult.Success(recipes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            ExportResult.Error
        }
    }

    override suspend fun import(source: Uri): ImportResult = withContext(Dispatchers.IO) {
        val copied = mutableListOf<String>()
        try {
            val input = contentResolver.openInputStream(source) ?: return@withContext ImportResult.Error
            var jsonText: String? = null
            val imagePaths = HashMap<String, String>()
            // Single streaming pass: copy each image entry straight to internal storage (never
            // buffering whole images in memory) and capture the JSON manifest.
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == JSON_ENTRY) {
                        jsonText = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (name.startsWith(IMAGES_DIR)) {
                        imageStore.importFromStream(zip)?.let { path ->
                            imagePaths[name] = path
                            copied.add(path)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val text = jsonText ?: return@withContext invalid(copied)
            val file = try {
                json.decodeFromString<BackupFile>(text)
            } catch (e: Exception) {
                return@withContext invalid(copied)
            }
            if (file.schemaVersion != SCHEMA_VERSION) return@withContext invalid(copied)

            val toInsert = file.recipes.map { it.toRecipeWithDetails(imagePaths) }
            dao.insertImported(toInsert)
            ImportResult.Success(toInsert.size)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            copied.forEach(imageStore::delete)
            ImportResult.Error
        }
    }

    /** Deletes images already copied this run, then returns [ImportResult.Invalid] (orphan cleanup). */
    private fun invalid(copied: List<String>): ImportResult {
        copied.forEach(imageStore::delete)
        return ImportResult.Invalid
    }

    /** Writes every referenced image file into the ZIP under images/, returning absolute-path -> entry-name. */
    private fun writeImages(zip: ZipOutputStream, recipes: List<RecipeWithDetails>): Map<String, String> {
        val entries = LinkedHashMap<String, String>()
        val usedNames = HashSet<String>()
        for (r in recipes) {
            val sources = buildList {
                r.recipe.imageUri?.let { add(it) }
                r.steps.forEach { s -> s.imageUri?.let { add(it) } }
            }
            for (absPath in sources) {
                if (entries.containsKey(absPath)) continue
                val src = File(absPath)
                if (!src.exists()) continue
                var base = src.name
                while (!usedNames.add(base)) base = "${System.nanoTime()}_${src.name}"
                val entryName = IMAGES_DIR + base
                zip.putNextEntry(ZipEntry(entryName))
                src.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                entries[absPath] = entryName
            }
        }
        return entries
    }

    private fun nowIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private companion object {
        const val TAG = "RecipeBackupManager"
    }
}

package com.nwe.recipely.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.nwe.recipely.data.ImageStore
import com.nwe.recipely.data.RecipeDao
import com.nwe.recipely.data.RecipeWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
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
            ExportResult.Error
        }
    }

    override suspend fun import(source: Uri): ImportResult = withContext(Dispatchers.IO) {
        val copied = mutableListOf<String>()
        try {
            val contents = readZip(source) ?: return@withContext ImportResult.Error
            val jsonText = contents.json ?: return@withContext ImportResult.Invalid
            val file = try {
                json.decodeFromString<BackupFile>(jsonText)
            } catch (e: Exception) {
                return@withContext ImportResult.Invalid
            }
            if (file.schemaVersion != SCHEMA_VERSION) return@withContext ImportResult.Invalid

            val imagePaths = HashMap<String, String>()
            for ((entryName, bytes) in contents.images) {
                val path = imageStore.importFromStream(ByteArrayInputStream(bytes)) ?: continue
                imagePaths[entryName] = path
                copied.add(path)
            }
            val toInsert = file.recipes.map { it.toRecipeWithDetails(imagePaths) }
            dao.insertImported(toInsert)
            ImportResult.Success(toInsert.size)
        } catch (e: Exception) {
            copied.forEach(imageStore::delete)
            ImportResult.Error
        }
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

    private class ZipContents(val json: String?, val images: Map<String, ByteArray>)

    private fun readZip(source: Uri): ZipContents? {
        val input = contentResolver.openInputStream(source) ?: return null
        var jsonText: String? = null
        val images = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val bytes = zip.readBytes()
                if (entry.name == JSON_ENTRY) jsonText = String(bytes) else images[entry.name] = bytes
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return ZipContents(jsonText, images)
    }

    private fun nowIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}

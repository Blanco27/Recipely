package com.nwe.recipely

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nwe.recipely.data.ImageStore
import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeDao
import com.nwe.recipely.data.RecipeDatabase
import com.nwe.recipely.data.Step
import com.nwe.recipely.data.backup.ExportResult
import com.nwe.recipely.data.backup.ImportResult
import com.nwe.recipely.data.backup.JSON_ENTRY
import com.nwe.recipely.data.backup.RecipeBackupManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class RecipeBackupManagerTest {

    private lateinit var context: Context
    private lateinit var db: RecipeDatabase
    private lateinit var dao: RecipeDao
    private lateinit var imageStore: ImageStore
    private lateinit var manager: RecipeBackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        dao = db.recipeDao()
        imageStore = ImageStore(context)
        manager = RecipeBackupManager(dao, imageStore, context.contentResolver)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun export_then_import_roundTrips_andMerges() = runTest {
        val titleImg = imageStore.importFromStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)))!!
        val stepImg = imageStore.importFromStream(ByteArrayInputStream(byteArrayOf(4, 5, 6)))!!
        dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "Cake", imageUri = titleImg, calories = 500, category = "DESSERT"),
            ingredients = listOf(Ingredient(recipeId = 0, text = "Flour", position = 0)),
            steps = listOf(Step(recipeId = 0, text = "Mix", imageUri = stepImg, position = 0)),
        )

        val zip = File(context.cacheDir, "backup-test.zip")
        val uri = Uri.fromFile(zip)

        val export = manager.export(uri)
        assertEquals(ExportResult.Success(1), export)
        assertTrue(zip.exists() && zip.length() > 0)

        val import = manager.import(uri)
        assertEquals(ImportResult.Success(1), import)

        val all = dao.getAllRecipesWithDetails()
        assertEquals(2, all.size) // merge: original + imported
        val imported = all.first { it.recipe.imageUri != titleImg }
        assertEquals("DESSERT", imported.recipe.category)
        assertEquals(500, imported.recipe.calories)
        assertEquals("Flour", imported.ingredients.single().text)
        val importedStep = imported.steps.single()
        assertEquals("Mix", importedStep.text)
        // restored images exist on disk as fresh copies (different paths)
        assertTrue(File(imported.recipe.imageUri!!).exists())
        assertTrue(File(importedStep.imageUri!!).exists())
        assertTrue(importedStep.imageUri != stepImg)
    }

    @Test
    fun import_wrongSchemaVersion_returnsInvalid() = runTest {
        val zip = File(context.cacheDir, "bad-schema.zip")
        ZipOutputStream(zip.outputStream()).use { z ->
            z.putNextEntry(ZipEntry(JSON_ENTRY))
            z.write("""{"schemaVersion":99,"exportedAt":"x","recipes":[]}""".toByteArray())
            z.closeEntry()
        }

        assertEquals(ImportResult.Invalid, manager.import(Uri.fromFile(zip)))
    }

    @Test
    fun import_missingJson_returnsInvalid() = runTest {
        val zip = File(context.cacheDir, "no-json.zip")
        ZipOutputStream(zip.outputStream()).use { z ->
            z.putNextEntry(ZipEntry("images/x.jpg"))
            z.write(byteArrayOf(9))
            z.closeEntry()
        }

        assertEquals(ImportResult.Invalid, manager.import(Uri.fromFile(zip)))
    }
}

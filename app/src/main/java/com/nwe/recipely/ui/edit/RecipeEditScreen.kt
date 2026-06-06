package com.nwe.recipely.ui.edit

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.ImageStore
import com.nwe.recipely.ui.theme.Fraunces
import java.io.File

private sealed interface ImageTarget {
    data object Title : ImageTarget
    data class StepImage(val index: Int) : ImageTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    recipeId: Long,
    onClose: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as RecipelyApp).container
    val imageStore: ImageStore = container.imageStore
    val vm: RecipeEditViewModel = viewModel(
        factory = viewModelFactory { initializer { RecipeEditViewModel(container.repository, recipeId) } }
    )
    val state by vm.state.collectAsState()

    var imageTarget by remember { mutableStateOf<ImageTarget?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var cameraTargetPath by remember { mutableStateOf<String?>(null) }

    BackHandler {
        vm.discardChanges()
        onClose()
    }

    fun applyImage(path: String?) {
        when (val target = imageTarget) {
            ImageTarget.Title -> vm.setTitleImage(path)
            is ImageTarget.StepImage -> vm.setStepImage(target.index, path)
            null -> Unit
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) applyImage(imageStore.importFromUri(uri))
    }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) applyImage(cameraTargetPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (recipeId == 0L) stringResource(R.string.new_recipe) else stringResource(R.string.edit_recipe),
                        fontFamily = Fraunces,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.discardChanges()
                        onClose()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    Button(
                        enabled = state.canSave,
                        onClick = { vm.save(onClose) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TitleImagePicker(
                    imagePath = state.imagePath,
                    onPick = { imageTarget = ImageTarget.Title; showSourceDialog = true },
                    onRemove = { vm.setTitleImage(null) },
                )
            }
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = vm::setName,
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.prepTime,
                        onValueChange = vm::setPrepTime,
                        label = { Text(stringResource(R.string.label_time)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.servings,
                        onValueChange = vm::setServings,
                        label = { Text(stringResource(R.string.label_servings)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { EditSectionHeader(stringResource(R.string.nutrition_optional)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.calories,
                        onValueChange = vm::setCalories,
                        label = { Text(stringResource(R.string.label_calories_input)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.carbs,
                        onValueChange = vm::setCarbs,
                        label = { Text(stringResource(R.string.label_carbs_input)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.protein,
                        onValueChange = vm::setProtein,
                        label = { Text(stringResource(R.string.label_protein_input)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.fat,
                        onValueChange = vm::setFat,
                        label = { Text(stringResource(R.string.label_fat_input)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { EditSectionHeader(stringResource(R.string.section_ingredients)) }
            items(state.ingredients.size) { index ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.ingredients[index].text,
                        onValueChange = { vm.setIngredient(index, it) },
                        label = { Text(stringResource(R.string.ingredient_n, index + 1)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.removeIngredient(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_ingredient))
                    }
                }
            }
            item { AddButton(stringResource(R.string.add_ingredient), onClick = vm::addIngredient) }

            item { EditSectionHeader(stringResource(R.string.section_steps)) }
            items(state.steps.size) { index ->
                StepEditor(
                    number = index + 1,
                    text = state.steps[index].text,
                    imagePath = state.steps[index].imagePath,
                    onTextChange = { vm.setStepText(index, it) },
                    onRemove = { vm.removeStep(index) },
                    onAddImage = { imageTarget = ImageTarget.StepImage(index); showSourceDialog = true },
                    onRemoveImage = { vm.setStepImage(index, null) },
                )
            }
            item { AddButton(stringResource(R.string.add_step), onClick = vm::addStep) }

            item { Box(Modifier.height(24.dp)) }
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text(stringResource(R.string.add_image_dialog_title)) },
            text = { Text(stringResource(R.string.add_image_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text(stringResource(R.string.gallery)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    val (uri, path) = imageStore.createCameraTarget()
                    cameraTargetPath = path
                    takePhoto.launch(uri)
                }) { Text(stringResource(R.string.camera)) }
            },
        )
    }
}

/** Dashed rounded outline used by the "add" buttons. */
private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp, strokeWidth: Dp = 1.5.dp): Modifier =
    this.drawBehind {
        val r = cornerRadius.toPx()
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(r, r),
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
            ),
        )
    }

@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .dashedBorder(tint.copy(alpha = 0.6f), 16.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(text, color = tint, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TitleImagePicker(imagePath: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledIconButton(
                    onClick = onPick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0x66000000),
                        contentColor = Color.White,
                    ),
                ) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.add_image_dialog_title)) }
                FilledIconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0x66000000),
                        contentColor = Color.White,
                    ),
                ) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_image)) }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    stringResource(R.string.title_image_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StepEditor(
    number: Int,
    text: String,
    imagePath: String?,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
    onAddImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    number.toString(),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.step_n, number)) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_step))
            }
        }
        if (imagePath != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp)),
                )
                FilledIconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0x66000000),
                        contentColor = Color.White,
                    ),
                ) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_step_image)) }
            }
        } else {
            AddImageButton(onClick = onAddImage)
        }
    }
}

@Composable
private fun AddImageButton(onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .dashedBorder(tint.copy(alpha = 0.5f), 14.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.add_step_image), color = tint, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EditSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp),
    )
}

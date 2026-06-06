package com.nwe.recipely.ui.edit

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.ImageStore
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
                title = { Text(if (recipeId == 0L) stringResource(R.string.new_recipe) else stringResource(R.string.edit_recipe)) },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.discardChanges()
                        onClose()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        enabled = state.canSave,
                        onClick = { vm.save(onClose) },
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.servings,
                        onValueChange = vm::setServings,
                        label = { Text(stringResource(R.string.label_servings)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.removeIngredient(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_ingredient))
                    }
                }
            }
            item {
                OutlinedButton(onClick = vm::addIngredient) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_ingredient))
                }
            }

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
            item {
                OutlinedButton(onClick = vm::addStep) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_step))
                }
            }
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

@Composable
private fun TitleImagePicker(imagePath: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
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
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .background(Color(0xAA000000), CircleShape),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_image), tint = Color.White)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.title_image_hint), color = MaterialTheme.colorScheme.primary)
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
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(number.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.step_n, number)) },
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
                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(10.dp)),
                )
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .background(Color(0xAA000000), CircleShape),
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_step_image), tint = Color.White)
                }
            }
        } else {
            OutlinedButton(onClick = onAddImage) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_step_image))
            }
        }
    }
}

@Composable
private fun EditSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

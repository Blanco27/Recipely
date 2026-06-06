package com.nwe.recipely.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nwe.recipely.ui.theme.FieldBorderDark
import com.nwe.recipely.ui.theme.FieldBorderLight
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark

/**
 * Outlined text field styled like the mockup `.tf`: near-white paper fill, soft warm
 * border, terracotta focus (border + label). Replaces the raw OutlinedTextField on the
 * edit screen so all fields lift off the cream background uniformly.
 */
@Composable
fun RecipelyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val dark = isSystemInDarkTheme()
    val paper = if (dark) PaperDark else Paper
    val border = if (dark) FieldBorderDark else FieldBorderLight
    val terra = MaterialTheme.colorScheme.secondary
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = paper,
            unfocusedContainerColor = paper,
            focusedBorderColor = terra,
            unfocusedBorderColor = border,
            focusedLabelColor = terra,
        ),
        modifier = modifier,
    )
}

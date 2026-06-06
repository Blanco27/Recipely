package com.nwe.recipely.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nwe.recipely.ui.theme.FieldBorderDark
import com.nwe.recipely.ui.theme.FieldBorderLight
import com.nwe.recipely.ui.theme.Ink
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark

/**
 * Mockup `.del` / `.x`: a paper square (or circle) with a soft warm border and a tinted
 * icon. Used for ingredient/step delete (terracotta icon) and the edit top-bar close
 * (circle, ink icon).
 */
@Composable
fun BoxedIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    size: Dp = 42.dp,
    tint: Color = MaterialTheme.colorScheme.secondary,
) {
    val dark = isSystemInDarkTheme()
    val paper = if (dark) PaperDark else Paper
    val border = if (dark) FieldBorderDark else FieldBorderLight
    val shape = if (circle) CircleShape else RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(paper)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

/**
 * Mockup `.hero-nav .icon` / `.titledrop .ctl`: a light translucent circle that reads over
 * a photo. `solid = true` is the near-opaque paper variant with a dark icon (title-image
 * controls); the default is a frosted white-translucent circle with a white icon (hero nav).
 */
@Composable
fun FrostedIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    solid: Boolean = false,
) {
    val dark = isSystemInDarkTheme()
    val bg = when {
        solid && dark -> PaperDark.copy(alpha = 0.85f)
        solid -> Paper.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.22f)
    }
    val borderColor = if (solid) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f)
    val tint = if (solid) (if (dark) Color.White else Ink) else Color.White
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

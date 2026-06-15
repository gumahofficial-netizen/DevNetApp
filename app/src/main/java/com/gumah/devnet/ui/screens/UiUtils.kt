package com.gumah.devnet.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor: Color = Color.Transparent,
    unfocusedBorderColor: Color = Color.Transparent,
    focusedLabelColor: Color = Color.Transparent,
    unfocusedLabelColor: Color = Color.Transparent,
    focusedTextColor: Color = Color.White,
    unfocusedTextColor: Color = Color.White,
    cursorColor: Color = Color(0xFF38BDF8)
): androidx.compose.material3.TextFieldColors {
    // Dynamically retrieve theme-aware colors
    val themeTextColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    // Map hardcoded white (default of old template) to theme's proper text color
    val finalFocusedTextColor = if (focusedTextColor == Color.White) themeTextColor else focusedTextColor
    val finalUnfocusedTextColor = if (unfocusedTextColor == Color.White) themeTextColor else unfocusedTextColor

    val finalFocusedLabelColor = if (focusedLabelColor == Color.Transparent) MaterialTheme.colorScheme.primary else focusedLabelColor
    val finalUnfocusedLabelColor = if (unfocusedLabelColor == Color.Transparent) labelColor else unfocusedLabelColor

    val finalFocusedBorderColor = if (focusedBorderColor == Color.Transparent) MaterialTheme.colorScheme.primary else focusedBorderColor
    val finalUnfocusedBorderColor = if (unfocusedBorderColor == Color.Transparent) MaterialTheme.colorScheme.outline else unfocusedBorderColor

    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = finalFocusedTextColor,
        unfocusedTextColor = finalUnfocusedTextColor,
        focusedBorderColor = finalFocusedBorderColor,
        unfocusedBorderColor = finalUnfocusedBorderColor,
        focusedLabelColor = finalFocusedLabelColor,
        unfocusedLabelColor = finalUnfocusedLabelColor,
        cursorColor = cursorColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor
    )
}

@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.16f)
            )
    )
}

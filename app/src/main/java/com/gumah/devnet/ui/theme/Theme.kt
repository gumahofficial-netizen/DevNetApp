package com.gumah.devnet.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = PrimaryCyan,
  onPrimary = OnPrimaryDark,
  primaryContainer = CardBg,
  onPrimaryContainer = LightText,
  secondary = AccentTeal,
  onSecondary = OnPrimaryDark,
  background = SlateBg,
  onBackground = LightText,
  surface = CardBg,
  onSurface = LightText,
  surfaceVariant = CodeBg,
  onSurfaceVariant = LightText,
  outline = BorderSlate,
  error = DarkRose
)

private val LightColorScheme = lightColorScheme(
  primary = LightPrimary,
  onPrimary = Color.White,
  primaryContainer = Color.White,
  onPrimaryContainer = LightTextDark,
  secondary = LightPrimary,
  onSecondary = Color.White,
  background = LightBg,
  onBackground = LightTextDark,
  surface = LightCard,
  onSurface = LightTextDark,
  outline = Color(0xFFCBD5E1) // soft grey border
)

@Composable
fun DevNetTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Supports premium dynamic colors on newer Android versions
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

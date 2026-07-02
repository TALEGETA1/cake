package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = BakeryPrimaryDark,
    secondary = BakerySecondaryDark,
    tertiary = BakeryTertiaryDark,
    background = BakeryBackgroundDark,
    surface = BakerySurfaceDark,
    surfaceVariant = BakerySurfaceVariantDark,
    onPrimary = Color(0xFF3A1F40),
    onSecondary = Color(0xFF3A1F40),
    onBackground = BakeryTextDark,
    onSurface = BakeryTextDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BakeryPrimary,
    secondary = BakerySecondary,
    tertiary = BakeryTertiary,
    background = BakeryBackground,
    surface = BakerySurface,
    surfaceVariant = BakerySurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = BakeryText,
    onSurface = BakeryText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic colors by default so our custom Bakery identity is fully preserved
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
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

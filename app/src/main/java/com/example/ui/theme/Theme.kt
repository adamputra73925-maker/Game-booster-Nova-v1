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

private val DarkColorScheme = darkColorScheme(
  primary = PrimaryNeon,
  secondary = SecondaryCyan,
  tertiary = AccentPurple,
  background = CarbonDark,
  surface = CardDark,
  surfaceVariant = CardDarkVariant,
  onPrimary = OnPrimaryDark,
  onSecondary = OnSecondaryDark,
  onTertiary = OnTertiaryDark,
  onBackground = TextPrimary,
  onSurface = TextPrimary,
  outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
  primary = PrimaryNeon,
  secondary = SecondaryCyan,
  tertiary = AccentPurple,
  background = Color(0xFFF4F6FA),
  surface = Color.White,
  onPrimary = OnPrimaryDark,
  onSecondary = OnSecondaryDark,
  onTertiary = OnTertiaryDark,
  onBackground = Color(0xFF1A1C1E),
  onSurface = Color(0xFF1A1C1E),
  outline = Color(0xFFE0E4EC)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set to false by default to strictly enforce the "Sleek Interface" brand identity
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

package com.space_explorer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette for the Space Explorer theme.
 *
 * Inspired by deep-space imagery: cosmic indigo as primary, nebula magenta
 * as secondary, plasma cyan as tertiary. Dark/Light pairs share the same
 * hue with adjusted lightness to keep semantic meaning across modes.
 *
 * Note: when the device supports Material You (Android 12+), these palettes
 * are overridden by `dynamicLightColorScheme` / `dynamicDarkColorScheme`.
 */

// Light scheme (saturation tuned for paper-white surfaces)
val CosmicIndigo40 = Color(0xFF3A41B5)
val NebulaMagenta40 = Color(0xFF8B3A8E)
val PlasmaCyan40 = Color(0xFF006A6A)

// Dark scheme (saturation lifted for OLED black)
val CosmicIndigo80 = Color(0xFFBBC2FF)
val NebulaMagenta80 = Color(0xFFFFA9F5)
val PlasmaCyan80 = Color(0xFF80D5D5)

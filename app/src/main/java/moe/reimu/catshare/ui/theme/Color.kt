package moe.reimu.catshare.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Brand palette generated from the launcher icon seed colour #684C77 using the
 * Material 3 tonal-palette algorithm: tones are CIELAB lightness values with the
 * brand hue held constant and chroma clipped to the sRGB gamut.
 *
 * Roles are spelled out rather than left at library defaults so CatShare keeps a
 * coherent identity on devices that do not offer dynamic colour.
 */

private const val BRAND_SEED_HUE = -44.3

private val BrandLightScheme = lightColorScheme(
    primary = Color(0xFF70547F), // primary 40
    onPrimary = Color(0xFFFFFFFF), // primary 100
    primaryContainer = Color(0xFFF3DAFF), // primary 90
    onPrimaryContainer = Color(0xFF573C66), // primary 30
    inversePrimary = Color(0xFFDCBBEC), // primary 80
    secondary = Color(0xFF695971), // secondary 40
    onSecondary = Color(0xFFFFFFFF), // secondary 100
    secondaryContainer = Color(0xFFEFDCF8), // secondary 90
    onSecondaryContainer = Color(0xFF504158), // secondary 30
    tertiary = Color(0xFF854F55), // tertiary 40
    onTertiary = Color(0xFFFFFFFF), // tertiary 100
    tertiaryContainer = Color(0xFFFFD9DC), // tertiary 90
    onTertiaryContainer = Color(0xFF6A383E), // tertiary 30
    background = Color(0xFFFDF8FF), // neutral 98
    onBackground = Color(0xFF1E1A1F), // neutral 10
    surface = Color(0xFFFDF8FF), // neutral 98
    onSurface = Color(0xFF1E1A1F), // neutral 10
    surfaceVariant = Color(0xFFE9DFED), // neutralVariant 90
    onSurfaceVariant = Color(0xFF4C444F), // neutralVariant 30
    inverseSurface = Color(0xFF332F34), // neutral 20
    inverseOnSurface = Color(0xFFF4EFF6), // neutral 95
    error = Color(0xFFB12938), // error 40
    onError = Color(0xFFFFFFFF), // error 100
    errorContainer = Color(0xFFFFDAD8), // error 90
    onErrorContainer = Color(0xFF920023), // error 30
    outline = Color(0xFF7D7480), // neutralVariant 50
    outlineVariant = Color(0xFFCDC3D1), // neutralVariant 80
    scrim = Color(0xFF000000), // neutral 0
    surfaceBright = Color(0xFFFDF8FF), // neutral 98
    surfaceDim = Color(0xFFDDD8DF), // neutral 87
    surfaceContainerLowest = Color(0xFFFFFFFF), // neutral 100
    surfaceContainerLow = Color(0xFFF7F2F9), // neutral 96
    surfaceContainer = Color(0xFFF1ECF3), // neutral 94
    surfaceContainerHigh = Color(0xFFEBE6ED), // neutral 92
    surfaceContainerHighest = Color(0xFFE6E1E8), // neutral 90
    primaryFixed = Color(0xFFF3DAFF), // primary 90
    primaryFixedDim = Color(0xFFDCBBEC), // primary 80
    onPrimaryFixed = Color(0xFF281137), // primary 10
    onPrimaryFixedVariant = Color(0xFF573C66), // primary 30
    secondaryFixed = Color(0xFFEFDCF8), // secondary 90
    secondaryFixedDim = Color(0xFFD3C0DB), // secondary 80
    onSecondaryFixed = Color(0xFF23162B), // secondary 10
    onSecondaryFixedVariant = Color(0xFF504158), // secondary 30
    tertiaryFixed = Color(0xFFFFD9DC), // tertiary 90
    tertiaryFixedDim = Color(0xFFF5B7BB), // tertiary 80
    onTertiaryFixed = Color(0xFF380B13), // tertiary 10
    onTertiaryFixedVariant = Color(0xFF6A383E), // tertiary 30
    surfaceTint = Color(0xFF70547F), // primary 40
)

private val BrandDarkScheme = darkColorScheme(
    primary = Color(0xFFDCBBEC), // primary 80
    onPrimary = Color(0xFF3F264E), // primary 20
    primaryContainer = Color(0xFF573C66), // primary 30
    onPrimaryContainer = Color(0xFFF3DAFF), // primary 90
    inversePrimary = Color(0xFF70547F), // primary 40
    secondary = Color(0xFFD3C0DB), // secondary 80
    onSecondary = Color(0xFF392B41), // secondary 20
    secondaryContainer = Color(0xFF504158), // secondary 30
    onSecondaryContainer = Color(0xFFEFDCF8), // secondary 90
    tertiary = Color(0xFFF5B7BB), // tertiary 80
    onTertiary = Color(0xFF512128), // tertiary 20
    tertiaryContainer = Color(0xFF6A383E), // tertiary 30
    onTertiaryContainer = Color(0xFFFFD9DC), // tertiary 90
    background = Color(0xFF161217), // neutral 6
    onBackground = Color(0xFFE6E1E8), // neutral 90
    surface = Color(0xFF161217), // neutral 6
    onSurface = Color(0xFFE6E1E8), // neutral 90
    surfaceVariant = Color(0xFF4C444F), // neutralVariant 30
    onSurfaceVariant = Color(0xFFCDC3D1), // neutralVariant 80
    inverseSurface = Color(0xFFE6E1E8), // neutral 90
    inverseOnSurface = Color(0xFF332F34), // neutral 20
    error = Color(0xFFFFB3B0), // error 80
    onError = Color(0xFF680016), // error 20
    errorContainer = Color(0xFF920023), // error 30
    onErrorContainer = Color(0xFFFFDAD8), // error 90
    outline = Color(0xFF968E9A), // neutralVariant 60
    outlineVariant = Color(0xFF4C444F), // neutralVariant 30
    scrim = Color(0xFF000000), // neutral 0
    surfaceBright = Color(0xFF3C383D), // neutral 24
    surfaceDim = Color(0xFF161217), // neutral 6
    surfaceContainerLowest = Color(0xFF110C13), // neutral 4
    surfaceContainerLow = Color(0xFF1E1A1F), // neutral 10
    surfaceContainer = Color(0xFF221E23), // neutral 12
    surfaceContainerHigh = Color(0xFF2C292E), // neutral 17
    surfaceContainerHighest = Color(0xFF373339), // neutral 22
    primaryFixed = Color(0xFFF3DAFF), // primary 90
    primaryFixedDim = Color(0xFFDCBBEC), // primary 80
    onPrimaryFixed = Color(0xFF281137), // primary 10
    onPrimaryFixedVariant = Color(0xFF573C66), // primary 30
    secondaryFixed = Color(0xFFEFDCF8), // secondary 90
    secondaryFixedDim = Color(0xFFD3C0DB), // secondary 80
    onSecondaryFixed = Color(0xFF23162B), // secondary 10
    onSecondaryFixedVariant = Color(0xFF504158), // secondary 30
    tertiaryFixed = Color(0xFFFFD9DC), // tertiary 90
    tertiaryFixedDim = Color(0xFFF5B7BB), // tertiary 80
    onTertiaryFixed = Color(0xFF380B13), // tertiary 10
    onTertiaryFixedVariant = Color(0xFF6A383E), // tertiary 30
    surfaceTint = Color(0xFFDCBBEC), // primary 80
)

internal fun brandColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) BrandDarkScheme else BrandLightScheme
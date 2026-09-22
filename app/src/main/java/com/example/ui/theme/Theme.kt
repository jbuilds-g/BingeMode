package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border

val LocalElementBorders = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun Modifier.elementBorder(
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
): Modifier {
    val enabled = LocalElementBorders.current
    return if (enabled) {
        this.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
            shape = shape
        )
    } else {
        this
    }
}

// --- 1. ORIGINAL KRAFT CINEMA THEME ---
private val DarkCinemaColorScheme = darkColorScheme(
    primary = Gold80,
    secondary = GoldGrey80,
    tertiary = AccentGold80,
    background = CharcoalDark,
    surface = CharcoalContainer,
    surfaceVariant = CharcoalVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    error = Color(0xFFCF6679)
)

private val LightCinemaColorScheme = lightColorScheme(
    primary = Color(0xFFD84315),
    secondary = Color(0xFF6D4C41),
    tertiary = Color(0xFFFF8F00),
    background = Color(0xFFFAF2ED),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1E6DF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF201A18),
    onSurface = Color(0xFF201A18),
    onSurfaceVariant = Color(0xFF53433E),
    outline = Color(0xFF85736E)
)

// --- 2. CYBERPUNK NEON ---
private val DarkCyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFFFF007F), // Vivid Cyber Pink
    secondary = Color(0xFF00F0FF), // Vivid Cyber Cyan
    tertiary = Color(0xFFBF5AF2), // Purple Ray
    background = Color(0xFF0B0714), // Midnight space
    surface = Color(0xFF141021),
    surfaceVariant = Color(0xFF211933),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFECE6F4),
    onSurface = Color(0xFFECE6F4),
    onSurfaceVariant = Color(0xFFD2CADC),
    outline = Color(0xFF988F9F),
    error = Color(0xFFFF2D55)
)

private val LightCyberpunkColorScheme = lightColorScheme(
    primary = Color(0xFFFF007F),
    secondary = Color(0xFF00838F),
    tertiary = Color(0xFF8E24AA),
    background = Color(0xFFFCEDF3),
    surface = Color.White,
    surfaceVariant = Color(0xFFF3DCE4),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF2A151C),
    onSurface = Color(0xFF2A151C),
    onSurfaceVariant = Color(0xFF534146),
    outline = Color(0xFF857176)
)

// --- 3. SUNSET AMBER ---
private val DarkSunsetColorScheme = darkColorScheme(
    primary = Color(0xFFFF7043), // Terracotta sunset
    secondary = Color(0xFFFFCA28), // Golden sand
    tertiary = Color(0xFFFF3D00),
    background = Color(0xFF140D0C), // Dusty canyon night
    surface = Color(0xFF1F1513),
    surfaceVariant = Color(0xFF33221E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFF4ECE9),
    onSurface = Color(0xFFF4ECE9),
    onSurfaceVariant = Color(0xFFDAC6C0),
    outline = Color(0xFF9E8D89),
    error = Color(0xFFE53935)
)

private val LightSunsetColorScheme = lightColorScheme(
    primary = Color(0xFFE64A19),
    secondary = Color(0xFFFFA000),
    tertiary = Color(0xFFD84315),
    background = Color(0xFFFFF7F4),
    surface = Color.White,
    surfaceVariant = Color(0xFFF9EAE5),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF24140F),
    onSurface = Color(0xFF24140F),
    onSurfaceVariant = Color(0xFF50413C),
    outline = Color(0xFF82706B)
)

// --- 4. FOREST SAGE ---
private val DarkForestColorScheme = darkColorScheme(
    primary = Color(0xFF81C784), // Mossy Silver Sage
    secondary = Color(0xFFAED581), // Light lime shoot
    tertiary = Color(0xFF4DB6AC),
    background = Color(0xFF0A0D0B), // Deep canopy shadow
    surface = Color(0xFF131714),
    surfaceVariant = Color(0xFF212822),
    onPrimary = Color(0xFF0C250E),
    onSecondary = Color(0xFF0C250E),
    onTertiary = Color.Black,
    onBackground = Color(0xFFE2E4DF),
    onSurface = Color(0xFFE2E4DF),
    onSurfaceVariant = Color(0xFFC0C9BF),
    outline = Color(0xFF8C9388),
    error = Color(0xFFE57373)
)

private val LightForestColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF558B2F),
    tertiary = Color(0xFF00796B),
    background = Color(0xFFF0F5F1),
    surface = Color.White,
    surfaceVariant = Color(0xFFDFEDE0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF101912),
    onSurface = Color(0xFF101912),
    onSurfaceVariant = Color(0xFF404E42),
    outline = Color(0xFF707E71)
)

// --- 5. SWEET LAVENDER ---
private val DarkLavenderColorScheme = darkColorScheme(
    primary = Color(0xFFB39DDB), // Lavender soft mist
    secondary = Color(0xFF80DEEA), // Dreamy teal
    tertiary = Color(0xFFF48FB1),
    background = Color(0xFF0B0910), // Midnight amethyst
    surface = Color(0xFF14121B),
    surfaceVariant = Color(0xFF252130),
    onPrimary = Color(0xFF1E1235),
    onSecondary = Color(0xFF00363A),
    onTertiary = Color.Black,
    onBackground = Color(0xFFE6E1E9),
    onSurface = Color(0xFFE6E1E9),
    onSurfaceVariant = Color(0xFFC9C4D3),
    outline = Color(0xFF938F9E),
    error = Color(0xFFFF8A80)
)

private val LightLavenderColorScheme = lightColorScheme(
    primary = Color(0xFF673AB7),
    secondary = Color(0xFF0097A7),
    tertiary = Color(0xFFD81B60),
    background = Color(0xFFF5F3FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9E4F3),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF150F22),
    onSurface = Color(0xFF150F22),
    onSurfaceVariant = Color(0xFF4B4356),
    outline = Color(0xFF7B738A)
)

@Composable
fun BingeModeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: String = "adaptive",
    isAmoled: Boolean = false,
    elementBorders: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var colorScheme = when (themeMode) {
        "adaptive" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                // Return Cinema fallback if system doesn't support wallpaper dynamic color
                if (darkTheme) DarkCinemaColorScheme else LightCinemaColorScheme
            }
        }
        "cinema" -> {
            if (darkTheme) DarkCinemaColorScheme else LightCinemaColorScheme
        }
        "cyberpunk" -> {
            if (darkTheme) DarkCyberpunkColorScheme else LightCyberpunkColorScheme
        }
        "sunset" -> {
            if (darkTheme) DarkSunsetColorScheme else LightSunsetColorScheme
        }
        "forest" -> {
            if (darkTheme) DarkForestColorScheme else LightForestColorScheme
        }
        "lavender" -> {
            if (darkTheme) DarkLavenderColorScheme else LightLavenderColorScheme
        }
        else -> {
            if (darkTheme) DarkCinemaColorScheme else LightCinemaColorScheme
        }
    }

    if (isAmoled && darkTheme) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalElementBorders provides elementBorders,
                content = content
            )
        }
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    BingeModeTheme(darkTheme = darkTheme, themeMode = "adaptive", content = content)
}

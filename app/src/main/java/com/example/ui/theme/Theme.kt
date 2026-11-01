package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ============================================================================
// MONOCHROME DARK COLOR SCHEME (Phase 2 Foundation)
// ============================================================================
private val DarkColorScheme = darkColorScheme(
    primary = DarkButtonPrimary,            // White #FFFFFF for primary actions
    onPrimary = DarkButtonOnPrimary,        // Black #000000 on primary
    primaryContainer = DarkSurfaceSecondary,// Charcoal #202020
    onPrimaryContainer = DarkTextPrimary,   // White #FFFFFF
    secondary = DarkTextSecondary,          // Charcoal-light #BDBDBD
    onSecondary = DarkButtonOnPrimary,      // Black #000000
    secondaryContainer = DarkSurfaceSecondary, // Charcoal #202020
    onSecondaryContainer = DarkTextPrimary, // White #FFFFFF
    tertiary = SemanticWarning,             // Restrained functional amber #F59E0B
    tertiaryContainer = SemanticWarningContainerDark, // #2D1D02
    onTertiaryContainer = SemanticWarning,  // #F59E0B
    background = DarkBackground,            // Pure Black #000000
    onBackground = DarkTextPrimary,         // White #FFFFFF
    surface = DarkSurface,                  // Deep Surface #101010
    onSurface = DarkTextPrimary,            // White #FFFFFF
    surfaceVariant = DarkSurfaceElevated,   // Elevated Surface #181818
    onSurfaceVariant = DarkTextSecondary,   // #BDBDBD
    outline = DarkBorder,                   // Main Border #303030
    outlineVariant = DarkDivider,           // Subtle Divider #262626
    error = SemanticError,                  // Functional Red #EF4444
    onError = DarkTextPrimary,
    errorContainer = SemanticErrorContainerDark, // #2B0E11
    onErrorContainer = SemanticError
)

// ============================================================================
// LIGHT COLOR SCHEME (Preserved Exactly)
// ============================================================================
private val LightColorScheme = lightColorScheme(
    primary = BurgundyPrimary,
    onPrimary = BurgundyOnPrimary,
    primaryContainer = BurgundyPrimaryContainer,
    onPrimaryContainer = BurgundyOnPrimaryContainer,
    secondary = PurpleSecondary,
    onSecondary = PurpleOnSecondary,
    secondaryContainer = PurpleSecondaryContainer,
    onSecondaryContainer = PurpleOnSecondaryContainer,
    tertiary = AmberTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

// ============================================================================
// CENTRALIZED TOKENS FOR DIRECT SCREEN INTEGRATION
// ============================================================================
val DarkThemeTokens = FashionAuraThemeTokens(
    isDark = true,
    background = DarkBackground,
    surfaceMain = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    surfaceSecondary = DarkSurfaceSecondary,
    surfaceInput = DarkInputSurface,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    borderMain = DarkBorder,
    borderSubtle = DarkDivider,
    buttonPrimaryBg = DarkButtonPrimary,
    buttonPrimaryText = DarkButtonOnPrimary,
    buttonSecondaryBg = DarkButtonSecondary,
    buttonSecondaryText = DarkButtonOnSecondary,
    success = SemanticSuccess,
    successContainer = SemanticSuccessContainerDark,
    successBorder = SemanticSuccessBorderDark,
    warning = SemanticWarning,
    warningContainer = SemanticWarningContainerDark,
    warningBorder = SemanticWarningBorderDark,
    error = SemanticError,
    errorContainer = SemanticErrorContainerDark,
    errorBorder = SemanticErrorBorderDark,
    info = SemanticInfo,
    infoContainer = SemanticInfoContainerDark,
    infoBorder = SemanticInfoBorderDark
)

val LightThemeTokens = FashionAuraThemeTokens(
    isDark = false,
    background = LightBackground,
    surfaceMain = LightSurface,
    surfaceElevated = LightSurface,
    surfaceSecondary = LightSurfaceVariant,
    surfaceInput = LightSurface,
    textPrimary = LightOnSurface,
    textSecondary = LightOnSurfaceVariant,
    textMuted = Color(0xFF64748B),
    borderMain = LightOutline,
    borderSubtle = Color(0xFFE2E8F0),
    buttonPrimaryBg = BurgundyPrimary,
    buttonPrimaryText = BurgundyOnPrimary,
    buttonSecondaryBg = LightSurfaceVariant,
    buttonSecondaryText = LightOnSurface,
    success = StatusGreen,
    successContainer = SemanticSuccessContainerLight,
    successBorder = SemanticSuccessBorderLight,
    warning = AmberTertiary,
    warningContainer = SemanticWarningContainerLight,
    warningBorder = SemanticWarningBorderLight,
    error = Color(0xFFDC2626),
    errorContainer = SemanticErrorContainerLight,
    errorBorder = SemanticErrorBorderLight,
    info = StatusBlue,
    infoContainer = SemanticInfoContainerLight,
    infoBorder = SemanticInfoBorderLight
)

val LocalFashionAuraTokens = staticCompositionLocalOf {
    LightThemeTokens
}

/**
 * Accessor for centralized tokens from any Composable.
 */
val MaterialTheme.auraTokens: FashionAuraThemeTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalFashionAuraTokens.current

@Composable
fun FashionAuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep brand aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val tokens = if (darkTheme) DarkThemeTokens else LightThemeTokens

    CompositionLocalProvider(LocalFashionAuraTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

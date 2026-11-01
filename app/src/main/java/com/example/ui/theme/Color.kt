package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ============================================================================
// LIGHT THEME PALETTE (Preserved Exactly)
// ============================================================================
val BurgundyPrimary = Color(0xFF8B1D3B)
val BurgundyOnPrimary = Color(0xFFFFFFFF)
val BurgundyPrimaryContainer = Color(0xFFFFD9DF)
val BurgundyOnPrimaryContainer = Color(0xFF3B000F)

val PurpleSecondary = Color(0xFF6B46C1)
val PurpleOnSecondary = Color(0xFFFFFFFF)
val PurpleSecondaryContainer = Color(0xFFEDE9FE)
val PurpleOnSecondaryContainer = Color(0xFF2E1065)

val AmberTertiary = Color(0xFF9E6500)
val AmberTertiaryContainer = Color(0xFFFFF0D4)
val AmberOnTertiaryContainer = Color(0xFF3D2500)

val LightBackground = Color(0xFFF6F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEFF1F5)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)

// Legacy functional status colors (preserved for backward compatibility)
val StatusGreen = Color(0xFF15803D)
val StatusGreenContainer = Color(0xFFDCFCE7)
val StatusGreenDarkContainer = Color(0xFF14532D)

val StatusBlue = Color(0xFF1D4ED8)
val StatusBlueContainer = Color(0xFFDBEAFE)
val StatusBlueDarkContainer = Color(0xFF1E3A8A)

val StatusOrange = Color(0xFFC2410C)
val StatusOrangeContainer = Color(0xFFFFEDD5)
val StatusOrangeDarkContainer = Color(0xFF7C2D12)

val StatusPurple = Color(0xFF6B21A8)
val StatusPurpleContainer = Color(0xFFF3E8FF)
val StatusPurpleDarkContainer = Color(0xFF581C87)

// ============================================================================
// MONOCHROME DARK THEME PALETTE (Phase 2 Design System)
// ============================================================================

// Background & Surfaces
val DarkBackground = Color(0xFF000000)          // Pitch Black
val DarkSurface = Color(0xFF101010)             // Main Surface
val DarkSurfaceElevated = Color(0xFF181818)     // Elevated Surface / Cards / Dialogs
val DarkSurfaceSecondary = Color(0xFF202020)    // Secondary Surface / Unselected Chips
val DarkInputSurface = Color(0xFF141414)        // Input Fields / Search / TextAreas

// Text Tokens
val DarkTextPrimary = Color(0xFFFFFFFF)         // 100% White (Primary Headings & Values)
val DarkTextSecondary = Color(0xFFBDBDBD)       // Secondary Text / Labels / Subtitles
val DarkTextMuted = Color(0xFF858585)           // Muted Text / Placeholders / Captions
val DarkTextDisabled = Color(0xFF707070)        // Disabled State Text

// Borders & Dividers
val DarkBorder = Color(0xFF303030)              // Main Borders & Outlines
val DarkDivider = Color(0xFF262626)             // Subtle Dividers

// Primary & Secondary Actions
val DarkButtonPrimary = Color(0xFFFFFFFF)       // Primary Button Background
val DarkButtonOnPrimary = Color(0xFF000000)     // Primary Button Text
val DarkButtonSecondary = Color(0xFF202020)     // Secondary Button Background
val DarkButtonOnSecondary = Color(0xFFFFFFFF)   // Secondary Button Text

// Backward-compatible dark aliases mapping to clean monochrome
val BurgundyDarkPrimary = DarkButtonPrimary
val BurgundyDarkOnPrimary = DarkButtonOnPrimary
val BurgundyDarkPrimaryContainer = DarkSurfaceSecondary
val BurgundyDarkOnPrimaryContainer = DarkTextPrimary

val PurpleDarkSecondary = DarkTextSecondary
val PurpleDarkOnSecondary = DarkButtonOnPrimary
val PurpleDarkSecondaryContainer = DarkSurfaceSecondary
val PurpleDarkOnSecondaryContainer = DarkTextPrimary

val AmberDarkTertiary = Color(0xFFF59E0B)
val AmberDarkTertiaryContainer = Color(0xFF2D1D02)

val DarkOnSurface = DarkTextPrimary
val DarkOnSurfaceVariant = DarkTextSecondary

// ============================================================================
// CENTRALIZED RESTRAINED SEMANTIC TOKENS
// ============================================================================

// Success (e.g. Verified customer, Paid status, Delivery completed)
val SemanticSuccess = Color(0xFF22C55E)
val SemanticSuccessContainerDark = Color(0xFF052E16)
val SemanticSuccessBorderDark = Color(0xFF166534)
val SemanticSuccessContainerLight = Color(0xFFDCFCE7)
val SemanticSuccessBorderLight = Color(0xFF86EFAC)

// Warning (e.g. Unconfirmed district, Estimated price, Conflicting totals)
val SemanticWarning = Color(0xFFF59E0B)
val SemanticWarningContainerDark = Color(0xFF2D1D02)
val SemanticWarningBorderDark = Color(0xFF78350F)
val SemanticWarningContainerLight = Color(0xFFFFFBEB)
val SemanticWarningBorderLight = Color(0xFFFDE68A)

// Error (e.g. Return risk alert, 48h active duplicate order, Invalid phone number)
val SemanticError = Color(0xFFEF4444)
val SemanticErrorContainerDark = Color(0xFF2B0E11)
val SemanticErrorBorderDark = Color(0xFF7F1D1D)
val SemanticErrorContainerLight = Color(0xFFFFE4E6)
val SemanticErrorBorderLight = Color(0xFFFECDD3)

// Information (e.g. Consignment ID, Courier tracking update, Info banners)
val SemanticInfo = Color(0xFF3B82F6)
val SemanticInfoContainerDark = Color(0xFF0B1E3B)
val SemanticInfoBorderDark = Color(0xFF1D4ED8)
val SemanticInfoContainerLight = Color(0xFFDBEAFE)
val SemanticInfoBorderLight = Color(0xFF93C5FD)

/**
 * Type-safe centralized semantic and surface tokens accessible across screens.
 */
@Immutable
data class FashionAuraThemeTokens(
    val isDark: Boolean,
    val background: Color,
    val surfaceMain: Color,
    val surfaceElevated: Color,
    val surfaceSecondary: Color,
    val surfaceInput: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderMain: Color,
    val borderSubtle: Color,
    val buttonPrimaryBg: Color,
    val buttonPrimaryText: Color,
    val buttonSecondaryBg: Color,
    val buttonSecondaryText: Color,
    // Semantic tokens
    val success: Color,
    val successContainer: Color,
    val successBorder: Color,
    val warning: Color,
    val warningContainer: Color,
    val warningBorder: Color,
    val error: Color,
    val errorContainer: Color,
    val errorBorder: Color,
    val info: Color,
    val infoContainer: Color,
    val infoBorder: Color
) {
    val surface: Color get() = surfaceMain
    val inputSurface: Color get() = surfaceInput
    val backgroundMain: Color get() = background
    val primaryAction: Color get() = buttonPrimaryBg
    val onPrimaryAction: Color get() = buttonPrimaryText
    val onErrorContainer: Color get() = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
    val borderActive: Color get() = if (isDark) Color(0xFF555555) else borderMain
    val successForeground: Color get() = success
    val warningForeground: Color get() = warning
    val errorForeground: Color get() = error
    val infoForeground: Color get() = info
}

package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable, resolution-independent Fashion Aura "A" Monogram Logo.
 *
 * Features:
 * - Smooth squircle container with deep plum to dark violet gradient (#2A0F3D -> #12061F).
 * - Subtle glowing pink-to-violet gradient border stroke (#FF6B9D -> #E935C1 -> #8B31FF).
 * - Bold geometric "A" monogram with crisp white-to-soft-lavender gradient and bottom arch cutout.
 * - Two dynamic, flowing ribbon waves in vibrant coral-pink -> magenta -> purple gradient.
 * - Razor-sharp dark cut-lines for depth and luxury boutique aesthetic at any size.
 */
@Composable
fun FashionAuraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    showSquircle: Boolean = true
) {
    val cornerRadius = size * 0.24f
    val squircleShape = remember(size) { RoundedCornerShape(cornerRadius) }

    val bgGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF2A0F3D),
                Color(0xFF1B0A28),
                Color(0xFF12061F)
            ),
            start = Offset(0f, 0f),
            end = Offset.Infinite
        )
    }

    val borderGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF6B9D).copy(alpha = 0.80f),
                Color(0xFFE935C1).copy(alpha = 0.60f),
                Color(0xFF8B31FF).copy(alpha = 0.75f)
            ),
            start = Offset(0f, 0f),
            end = Offset.Infinite
        )
    }

    val containerModifier = if (showSquircle) {
        modifier
            .size(size)
            .shadow(elevation = 2.dp, shape = squircleShape, clip = false)
            .clip(squircleShape)
            .background(bgGradient)
            .border(width = 1.dp, brush = borderGradient, shape = squircleShape)
    } else {
        modifier.size(size)
    }

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // 1. White to soft lavender gradient for "A" structure
            val whiteLavenderBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF7F3FD),
                    Color(0xFFE2D6F8)
                ),
                startY = h * 0.18f,
                endY = h * 0.74f
            )

            // 2. Coral-pink -> Magenta -> Purple gradient for signature swooshes
            val ribbonBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF6B9D),
                    Color(0xFFE935C1),
                    Color(0xFF8B31FF)
                ),
                start = Offset(w * 0.28f, h * 0.68f),
                end = Offset(w * 0.66f, h * 0.26f)
            )

            val cutLineColor = Color(0xFF160624)
            val cutLineStroke = Stroke(width = (w * 0.018f).coerceAtLeast(1.2f))

            // A. Top Apex Triangle
            val apexPath = Path().apply {
                moveTo(w * 0.512f, h * 0.222f) // apex top tip
                lineTo(w * 0.410f, h * 0.428f) // left slope down
                cubicTo(
                    w * 0.445f, h * 0.395f,
                    w * 0.510f, h * 0.350f,
                    w * 0.568f, h * 0.336f  // bottom cut curve following swoosh
                )
                lineTo(w * 0.512f, h * 0.222f) // right edge back to apex
                close()
            }

            // B. Upper Ribbon Swoosh (Top Wing / Wave)
            val upperRibbonPath = Path().apply {
                moveTo(w * 0.338f, h * 0.530f)
                cubicTo(
                    w * 0.370f, h * 0.435f,
                    w * 0.485f, h * 0.345f,
                    w * 0.630f, h * 0.282f // upper wing tip
                )
                cubicTo(
                    w * 0.585f, h * 0.380f,
                    w * 0.480f, h * 0.468f,
                    w * 0.342f, h * 0.596f // inner curve back
                )
                close()
            }

            // C. Lower Ribbon Swoosh (Middle Wave)
            val lowerRibbonPath = Path().apply {
                moveTo(w * 0.378f, h * 0.685f)
                cubicTo(
                    w * 0.375f, h * 0.565f,
                    w * 0.485f, h * 0.482f,
                    w * 0.650f, h * 0.396f // middle wing tip
                )
                cubicTo(
                    w * 0.590f, h * 0.495f,
                    w * 0.495f, h * 0.585f,
                    w * 0.398f, h * 0.685f // bottom arch curve back
                )
                close()
            }

            // D. Left Leg & Foot
            val leftLegPath = Path().apply {
                moveTo(w * 0.268f, h * 0.685f) // bottom-left outer corner
                lineTo(w * 0.378f, h * 0.685f) // flat bottom edge
                lineTo(w * 0.342f, h * 0.596f) // along inner cut
                lineTo(w * 0.338f, h * 0.530f)
                cubicTo(
                    w * 0.310f, h * 0.585f,
                    w * 0.285f, h * 0.640f,
                    w * 0.268f, h * 0.685f // outer slope down
                )
                close()
            }

            // E. Right Leg & Bottom Central Arch Cutout
            val rightLegPath = Path().apply {
                moveTo(w * 0.638f, h * 0.480f) // below lower ribbon
                cubicTo(
                    w * 0.675f, h * 0.560f,
                    w * 0.710f, h * 0.630f,
                    w * 0.745f, h * 0.685f // outer right edge down
                )
                lineTo(w * 0.622f, h * 0.685f) // bottom right flat foot
                // Central Arch Cutout between legs
                cubicTo(
                    w * 0.615f, h * 0.595f,
                    w * 0.480f, h * 0.595f,
                    w * 0.405f, h * 0.685f // arch apex and left inner side
                )
                lineTo(w * 0.398f, h * 0.685f)
                cubicTo(
                    w * 0.495f, h * 0.585f,
                    w * 0.590f, h * 0.495f,
                    w * 0.638f, h * 0.480f
                )
                close()
            }

            // Draw A Body pieces (White-Lavender gradient)
            drawPath(apexPath, brush = whiteLavenderBrush, style = Fill)
            drawPath(leftLegPath, brush = whiteLavenderBrush, style = Fill)
            drawPath(rightLegPath, brush = whiteLavenderBrush, style = Fill)

            // Draw Dark Cut Line outlines for depth separation
            drawPath(apexPath, color = cutLineColor, style = cutLineStroke)
            drawPath(leftLegPath, color = cutLineColor, style = cutLineStroke)
            drawPath(rightLegPath, color = cutLineColor, style = cutLineStroke)

            // Draw Two Dynamic Ribbon Swooshes (Coral -> Magenta -> Purple gradient)
            drawPath(lowerRibbonPath, brush = ribbonBrush, style = Fill)
            drawPath(upperRibbonPath, brush = ribbonBrush, style = Fill)

            // Cut-line stroke around ribbons
            drawPath(lowerRibbonPath, color = cutLineColor, style = cutLineStroke)
            drawPath(upperRibbonPath, color = cutLineColor, style = cutLineStroke)
        }
    }
}

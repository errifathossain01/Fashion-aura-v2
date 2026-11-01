package com.example.ui.screens

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.ui.theme.AppStrings
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.auraTokens
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Fashion Aura OMS Login Screen (Stage B.2 Motion & Animation)
 *
 * Motion Enhancements:
 * 1. Animated Hero Background:
 *    - Subtle flowing wave contour lines with a smooth 10s cycle using rememberInfiniteTransition
 *    - Silk-like flowing curves in Canvas with low opacity
 * 2. FA Logo Entrance Animation:
 *    - Runs once on screen entry: gentle fade-in and scale-up using spring physics
 * 3. Form Staggered Entrance Animation:
 *    - Staggered entrance for heading, subtitle, inputs, actions, button, and footer
 *    - Gentle fade-in and small upward translation via graphicsLayer
 * 4. Sign In Button Interaction:
 *    - Spring-based press-scale feedback without interfering with loading states
 * 5. Reduced-Motion Accessibility:
 *    - Detects system animator duration scale; renders final static UI immediately when reduced motion is preferred
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    language: AppLanguage,
    onLoginSuccess: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val state by authViewModel.uiState.collectAsStateWithLifecycle()
    val tokens = MaterialTheme.auraTokens
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Accessibility check: Respect system animator duration scale / reduced motion preference
    val isReducedMotion = remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    val formBgColor = if (tokens.isDark) tokens.surfaceMain else Color.White
    val screenBgColor = if (tokens.isDark) tokens.background else Color(0xFFF9FAFB)

    // ------------------------------------------------------------------------
    // 1. ANIMATED HERO BACKGROUND (Infinite 10-second wave cycle)
    // ------------------------------------------------------------------------
    val infiniteTransition = rememberInfiniteTransition(label = "hero_wave_transition")
    val wavePhase by if (isReducedMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 10000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "hero_wave_phase"
        )
    }

    // ------------------------------------------------------------------------
    // 2. FA LOGO ENTRANCE ANIMATION (Fade-in + Spring Scale-up, once per entry)
    // ------------------------------------------------------------------------
    val monogramAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val monogramScale = remember { Animatable(if (isReducedMotion) 1f else 0.82f) }

    LaunchedEffect(isReducedMotion) {
        if (!isReducedMotion) {
            launch {
                monogramAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                )
            }
            launch {
                monogramScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------------
    // 3. FORM STAGGERED ENTRANCE PROGRESS DRIVER
    // ------------------------------------------------------------------------
    val formEntranceProgress = remember { Animatable(if (isReducedMotion) 1f else 0f) }

    LaunchedEffect(isReducedMotion) {
        if (!isReducedMotion) {
            delay(100)
            formEntranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    // ------------------------------------------------------------------------
    // 4. SIGN IN BUTTON PRESS-SCALE INTERACTION
    // ------------------------------------------------------------------------
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed && !state.isLoading) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "signin_button_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ----------------------------------------------------------------
            // 1. BRAND HERO HEADER (Burgundy Gradient + Wave Contours + Monogram)
            // ----------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp)
            ) {
                // Burgundy Gradient Background
                val headerGradient = if (tokens.isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF22050B),
                            Color(0xFF340812),
                            Color(0xFF450B17)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF4A0E18),
                            Color(0xFF6B1525),
                            BurgundyPrimary
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(headerGradient)
                )

                // Subtle flowing wave / contour decorations animated over a 10s cycle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val phase = wavePhase.toDouble()
                    val waveShift1 = (sin(phase) * 10.dp.toPx()).toFloat()
                    val waveShift2 = (cos(phase) * 8.dp.toPx()).toFloat()

                    // Contour wave line 1
                    val contour1 = Path().apply {
                        moveTo(0f, h * 0.30f + waveShift1)
                        cubicTo(
                            w * 0.25f, h * 0.15f - waveShift1,
                            w * 0.60f, h * 0.45f + waveShift1,
                            w, h * 0.20f - waveShift1 * 0.5f
                        )
                    }
                    drawPath(
                        path = contour1,
                        color = Color.White.copy(alpha = 0.08f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Contour wave line 2
                    val contour2 = Path().apply {
                        moveTo(0f, h * 0.65f - waveShift2)
                        cubicTo(
                            w * 0.35f, h * 0.50f + waveShift2,
                            w * 0.70f, h * 0.80f - waveShift2,
                            w, h * 0.55f + waveShift2 * 0.5f
                        )
                    }
                    drawPath(
                        path = contour2,
                        color = Color.White.copy(alpha = 0.06f),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Soft decorative radial glow with subtle movement
                    val glowShiftX = (sin(phase * 0.5) * 14.dp.toPx()).toFloat()
                    drawCircle(
                        color = Color(0xFFFFD9DF).copy(alpha = 0.05f),
                        radius = w * 0.45f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f + glowShiftX, h * 0.35f)
                    )
                }

                // Brand Content inside Header
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 10.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // FA Monogram Badge with entrance animation (fade-in & spring scale)
                    Surface(
                        modifier = Modifier
                            .size(62.dp)
                            .graphicsLayer {
                                alpha = monogramAlpha.value
                                scaleX = monogramScale.value
                                scaleY = monogramScale.value
                            },
                        shape = CircleShape,
                        color = if (tokens.isDark) Color(0xFF28070D) else Color(0xFF3B000F),
                        border = BorderStroke(1.2.dp, Color(0xFFFFD9DF).copy(alpha = 0.45f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Inner subtle ring
                            Canvas(modifier = Modifier.size(54.dp)) {
                                drawCircle(
                                    color = Color(0xFFFFD9DF).copy(alpha = 0.2f),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                            Text(
                                text = "FA",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = Color(0xFFFFD9DF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Brand Title
                    Text(
                        text = AppStrings.appName(language),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Small subtitle: ORDER MANAGEMENT SYSTEM
                    Text(
                        text = AppStrings.orderManagementSystem(language),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD9DF).copy(alpha = 0.82f),
                        letterSpacing = 2.2.sp
                    )
                }

                // Exit / Back Button (Preview Mode)
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(8.dp)
                            .testTag("login_preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Exit Login Preview",
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Smooth Asymmetric Wave Transition at the bottom of the Header
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    val w = size.width
                    val h = size.height
                    val subtleWaveShift = if (isReducedMotion) 0f else (sin(wavePhase.toDouble() * 0.5) * 2.5.dp.toPx()).toFloat()
                    val wavePath = Path().apply {
                        moveTo(0f, h * 0.45f + subtleWaveShift)
                        cubicTo(
                            w * 0.32f, 0f + subtleWaveShift,
                            w * 0.68f, h * 0.98f - subtleWaveShift,
                            w, h * 0.32f + subtleWaveShift
                        )
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = wavePath, color = formBgColor)
                }
            }

            // ----------------------------------------------------------------
            // 2. WHITE / LUXURY SURFACE LOGIN SECTION
            // ----------------------------------------------------------------
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                color = formBgColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, vertical = 8.dp)
                ) {
                    if (state.isEmailVerificationPending) {
                        // Email Verification Pending State
                        EmailVerificationPendingSection(
                            state = state,
                            language = language,
                            tokens = tokens,
                            onResendEmail = { authViewModel.sendVerificationEmail() },
                            onCheckStatus = { authViewModel.checkEmailVerificationStatus(onVerified = onLoginSuccess) },
                            onCancel = { authViewModel.cancelVerificationPending() }
                        )
                    } else {
                        val formProgress = formEntranceProgress.value

                        // Staggered Element 0: Welcome Back Heading
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(
                                    index = 0,
                                    progress = formProgress,
                                    isReducedMotion = isReducedMotion
                                )
                        ) {
                            Text(
                                text = AppStrings.welcomeBack(language),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = tokens.textPrimary,
                                letterSpacing = 0.2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Staggered Element 1: Subtitle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(
                                    index = 1,
                                    progress = formProgress,
                                    isReducedMotion = isReducedMotion
                                )
                        ) {
                            Text(
                                text = AppStrings.welcomeBackSubtitle(language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textMuted,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Error Banner
                        AnimatedVisibility(
                            visible = state.errorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.errorContainer,
                                border = BorderStroke(1.dp, tokens.errorBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = tokens.errorForeground,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = mapSymbolicErrorMessage(state.errorMessage, language),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.errorForeground,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { authViewModel.clearMessages() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = tokens.errorForeground,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Info Banner
                        AnimatedVisibility(
                            visible = state.infoMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.successContainer,
                                border = BorderStroke(1.dp, tokens.successBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Info",
                                        tint = tokens.successForeground,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = mapSymbolicInfoMessage(state.infoMessage, language),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.successForeground,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { authViewModel.clearMessages() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = tokens.successForeground,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ------------------------------------------------------------
                        // 3. LOGIN FORM INPUTS (Soft Neutral BG + 14dp Corners + Hairline)
                        // ------------------------------------------------------------
                        val inputBg = if (tokens.isDark) Color(0xFF141414) else Color(0xFFF7F7F9)
                        val inputBorder = if (tokens.isDark) Color(0xFF262628) else Color(0xFFE5E7EB)
                        val iconColor = if (tokens.isDark) Color(0xFF9E9EA0) else Color(0xFF9CA3AF)
                        val brandPrimary = if (tokens.isDark) Color(0xFF9E2445) else BurgundyPrimary

                        // Staggered Element 2: Email Field Label & Input
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(
                                    index = 2,
                                    progress = formProgress,
                                    isReducedMotion = isReducedMotion
                                )
                        ) {
                            Text(
                                text = AppStrings.email(language),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.textSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = state.emailInput,
                                onValueChange = { authViewModel.onEmailChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_email_input"),
                                placeholder = {
                                    Text(
                                        text = AppStrings.emailPlaceholder(language),
                                        color = tokens.textMuted,
                                        fontSize = 14.sp
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email",
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBg,
                                    unfocusedContainerColor = inputBg,
                                    focusedBorderColor = brandPrimary,
                                    unfocusedBorderColor = inputBorder,
                                    focusedTextColor = tokens.textPrimary,
                                    unfocusedTextColor = tokens.textPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Staggered Element 3: Password Field Label & Input
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(
                                    index = 3,
                                    progress = formProgress,
                                    isReducedMotion = isReducedMotion
                                )
                        ) {
                            Text(
                                text = AppStrings.password(language),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.textSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = state.passwordInput,
                                onValueChange = { authViewModel.onPasswordChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                placeholder = {
                                    Text(
                                        text = AppStrings.passwordPlaceholder(language),
                                        color = tokens.textMuted,
                                        fontSize = 14.sp
                                    )
                                },
                                singleLine = true,
                                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        authViewModel.signIn(onSuccess = onLoginSuccess)
                                    }
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Password",
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { authViewModel.togglePasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (state.isPasswordVisible) "Hide password" else "Show password",
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBg,
                                    unfocusedContainerColor = inputBg,
                                    focusedBorderColor = brandPrimary,
                                    unfocusedBorderColor = inputBorder,
                                    focusedTextColor = tokens.textPrimary,
                                    unfocusedTextColor = tokens.textPrimary
                                )
                            )
                        }

                        // Staggered Element 4: Forgot Password Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .staggeredEntrance(
                                    index = 4,
                                    progress = formProgress,
                                    isReducedMotion = isReducedMotion
                                ),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { authViewModel.openForgotPasswordDialog() },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = AppStrings.forgotPassword(language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (tokens.isDark) Color(0xFFFFB2C1) else BurgundyPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Staggered Element 5: Primary Sign In Button (with press-scale animation)
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                authViewModel.signIn(onSuccess = onLoginSuccess)
                            },
                            enabled = !state.isLoading,
                            interactionSource = buttonInteractionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .graphicsLayer {
                                    scaleX = if (isReducedMotion) 1f else buttonScale
                                    scaleY = if (isReducedMotion) 1f else buttonScale
                                }
                                .staggeredEntrance(
                                    index = 5,
                                    progress = formProgress,
                                    isReducedMotion = isReducedMotion
                                )
                                .testTag("sign_in_submit_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brandPrimary,
                                contentColor = Color.White,
                                disabledContainerColor = brandPrimary.copy(alpha = 0.45f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 1.5.dp,
                                pressedElevation = 0.dp
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = AppStrings.signInButton(language),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Staggered Element 6: Brand Finish Footer
                    val formProgress = formEntranceProgress.value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(
                                index = 6,
                                progress = formProgress,
                                isReducedMotion = isReducedMotion
                            ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = tokens.textMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.secureAccessFooter(language),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.textMuted.copy(alpha = 0.75f),
                            letterSpacing = 0.4.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

        // Forgot Password Dialog
        if (state.isForgotPasswordDialogOpen) {
            ForgotPasswordDialog(
                state = state,
                language = language,
                tokens = tokens,
                onEmailChange = { authViewModel.onForgotPasswordEmailChanged(it) },
                onDismiss = { authViewModel.closeForgotPasswordDialog() },
                onConfirm = { authViewModel.sendPasswordReset() }
            )
        }
    }
}

/**
 * Modifier extension for staggered element entrance with fade-in and subtle upward slide.
 * When isReducedMotion is true, applies no transformation.
 */
private fun Modifier.staggeredEntrance(
    index: Int,
    progress: Float,
    isReducedMotion: Boolean
): Modifier = if (isReducedMotion) {
    this
} else {
    this.graphicsLayer {
        val step = 0.08f
        val start = (index * step).coerceAtMost(0.5f)
        val itemDuration = 0.48f
        val raw = ((progress - start) / itemDuration).coerceIn(0f, 1f)
        val eased = FastOutSlowInEasing.transform(raw)
        alpha = eased
        translationY = (1f - eased) * 16.dp.toPx()
    }
}

/**
 * Clean UI view displayed when the authenticated account needs email verification.
 */
@Composable
private fun EmailVerificationPendingSection(
    state: AuthUiState,
    language: AppLanguage,
    tokens: com.example.ui.theme.FashionAuraThemeTokens,
    onResendEmail: () -> Unit,
    onCheckStatus: () -> Unit,
    onCancel: () -> Unit
) {
    val brandPrimary = if (tokens.isDark) Color(0xFF9E2445) else BurgundyPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            color = tokens.warningContainer,
            border = BorderStroke(1.dp, tokens.warningBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = "Pending Verification",
                    tint = tokens.warningForeground,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = AppStrings.emailVerificationPendingTitle(language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = AppStrings.emailVerificationPendingDesc(language, state.unverifiedEmail),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Check Status Button (Reload user profile from Firebase)
        Button(
            onClick = onCheckStatus,
            enabled = !state.isReloadingUser,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("check_verification_status_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = brandPrimary,
                contentColor = Color.White
            )
        ) {
            if (state.isReloadingUser) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.checkVerificationStatus(language),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Resend Email with 60s cooldown
        OutlinedButton(
            onClick = onResendEmail,
            enabled = state.resendCooldownSeconds == 0 && !state.isResendingVerification,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("resend_verification_email_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, tokens.borderMain),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = tokens.textPrimary
            )
        ) {
            if (state.isResendingVerification) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = tokens.textPrimary,
                    strokeWidth = 2.dp
                )
            } else if (state.resendCooldownSeconds > 0) {
                Text(
                    text = AppStrings.resendCooldown(language, state.resendCooldownSeconds),
                    fontSize = 13.5.sp,
                    color = tokens.textMuted
                )
            } else {
                Text(
                    text = AppStrings.resendVerificationEmail(language),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Back to Login / Switch User
        TextButton(
            onClick = onCancel,
            modifier = Modifier.testTag("cancel_verification_button")
        ) {
            Text(
                text = AppStrings.backToLogin(language),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textMuted
            )
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    state: AuthUiState,
    language: AppLanguage,
    tokens: com.example.ui.theme.FashionAuraThemeTokens,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val brandPrimary = if (tokens.isDark) Color(0xFF9E2445) else BurgundyPrimary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.resetPasswordTitle(language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = AppStrings.resetPasswordDescription(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = state.forgotPasswordEmailInput,
                    onValueChange = onEmailChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_password_email_input"),
                    placeholder = {
                        Text(
                            text = AppStrings.emailPlaceholder(language),
                            color = tokens.textMuted,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (tokens.isDark) Color(0xFF141414) else Color(0xFFF7F7F9),
                        unfocusedContainerColor = if (tokens.isDark) Color(0xFF141414) else Color(0xFFF7F7F9),
                        focusedBorderColor = brandPrimary,
                        unfocusedBorderColor = if (tokens.isDark) Color(0xFF262628) else Color(0xFFE5E7EB),
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isSendingPasswordReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_password_reset_button")
            ) {
                if (state.isSendingPasswordReset) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = AppStrings.sendResetLink(language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel(language), color = tokens.textMuted)
            }
        },
        containerColor = if (tokens.isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp)
    )
}

/**
 * Maps clean symbolic error codes to localized messages.
 */
private fun mapSymbolicErrorMessage(code: String?, language: AppLanguage): String {
    return when (code) {
        "INVALID_EMAIL" -> AppStrings.errorInvalidEmail(language)
        "EMPTY_PASSWORD" -> AppStrings.errorEmptyPassword(language)
        "INVALID_CREDENTIALS" -> AppStrings.errorInvalidCredentials(language)
        "USER_DISABLED" -> AppStrings.errorUserDisabled(language)
        "TOO_MANY_REQUESTS" -> AppStrings.errorTooManyRequests(language)
        "NETWORK_ERROR" -> AppStrings.errorNetwork(language)
        "EMAIL_NOT_YET_VERIFIED" -> AppStrings.emailNotYetVerified(language)
        else -> AppStrings.errorGeneric(language)
    }
}

/**
 * Maps clean symbolic informational messages to localized text.
 */
private fun mapSymbolicInfoMessage(code: String?, language: AppLanguage): String {
    return when (code) {
        "VERIFICATION_EMAIL_SENT" -> AppStrings.verificationEmailSent(language)
        "PASSWORD_RESET_SENT" -> AppStrings.passwordResetSent(language)
        "EMAIL_VERIFIED_SUCCESS" -> AppStrings.emailVerifiedSuccess(language)
        else -> ""
    }
}

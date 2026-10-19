package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.JackCyanNeon
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JackOrb(
    state: AssistantState,
    audioLevel: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "JackOrbRotation")

    val rotationClockwise by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantState.THINKING) 3000 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbClockwise"
    )

    val rotationCounterClockwise by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantState.THINKING) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbCounterClockwise"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathing"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("jack_orb")
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.85f

            // Audio-reactive scale boost
            val audioBoost = if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                audioLevel * 0.35f
            } else {
                0f
            }
            val currentScale = (breathingScale + audioBoost).coerceIn(0.85f, 1.45f)
            val effectiveRadius = baseRadius * currentScale

            val primaryColor = state.color
            val glowColor = primaryColor.copy(alpha = 0.25f + (audioLevel * 0.4f))

            if (state != AssistantState.OFF) {
                // 1. Outermost Ambient Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = center,
                        radius = effectiveRadius * 1.35f
                    ),
                    radius = effectiveRadius * 1.35f,
                    center = center
                )

                // 2. Outer Dashed Sci-Fi Ring (Counter-Clockwise)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.4f),
                    radius = effectiveRadius,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), rotationCounterClockwise)
                    )
                )

                // 3. Middle Segmented Ring with Ticks (Clockwise)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.7f),
                    radius = effectiveRadius * 0.82f,
                    center = center,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 20f), rotationClockwise)
                    )
                )

                // 4. Radiating Holographic Arc Markers
                val markerCount = 12
                val markerRadius = effectiveRadius * 0.68f
                for (i in 0 until markerCount) {
                    val angleDeg = (i * (360f / markerCount)) + rotationClockwise
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val startX = center.x + (markerRadius - 8f) * cos(angleRad).toFloat()
                    val startY = center.y + (markerRadius - 8f) * sin(angleRad).toFloat()
                    val endX = center.x + (markerRadius + 8f) * cos(angleRad).toFloat()
                    val endY = center.y + (markerRadius + 8f) * sin(angleRad).toFloat()

                    drawLine(
                        color = primaryColor.copy(alpha = 0.8f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }

                // 5. Inner Core Reactor Gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            primaryColor,
                            primaryColor.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = effectiveRadius * 0.52f
                    ),
                    radius = effectiveRadius * 0.52f,
                    center = center
                )

                // 6. Core Inner Ring
                drawCircle(
                    color = primaryColor,
                    radius = effectiveRadius * 0.38f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            } else {
                // OFF State: Dim dormant metallic core
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = baseRadius * 0.6f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = baseRadius * 0.4f,
                    center = center
                )
            }
        }
    }
}

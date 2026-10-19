package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JackCyanNeon
import kotlin.random.Random

@Composable
fun AudioWaveform(
    audioLevel: Float, // 0.0 to 1.0
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 16,
    color: Color = JackCyanNeon
) {
    val animatedLevels = remember(barCount) {
        List(barCount) { Animatable(0.1f) }
    }

    LaunchedEffect(audioLevel, isActive) {
        if (isActive) {
            animatedLevels.forEachIndexed { index, animatable ->
                val randomVariation = 0.6f + Random.nextFloat() * 0.8f
                val targetHeight = (audioLevel * randomVariation).coerceIn(0.12f, 1.0f)
                animatable.animateTo(
                    targetValue = targetHeight,
                    animationSpec = tween(durationMillis = 100 + (index * 10), easing = FastOutSlowInEasing)
                )
            }
        } else {
            animatedLevels.forEach { it.animateTo(0.08f, tween(200)) }
        }
    }

    Canvas(
        modifier = modifier
            .width(180.dp)
            .height(36.dp)
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = 4.dp.toPx()
        val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1)
        val centerY = maxHeight / 2f

        animatedLevels.forEachIndexed { i, animatable ->
            val x = i * (barWidth + spacing) + (barWidth / 2f)
            val h = (animatable.value * maxHeight).coerceAtLeast(4.dp.toPx())
            val topY = centerY - (h / 2f)
            val bottomY = centerY + (h / 2f)

            drawLine(
                color = color.copy(alpha = if (isActive) 0.85f else 0.25f),
                start = Offset(x, topY),
                end = Offset(x, bottomY),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

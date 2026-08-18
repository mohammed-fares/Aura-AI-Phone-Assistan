package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PolishGlow
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryDark
import com.example.ui.theme.PolishSecondary
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishTertiary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NeuralOrbVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 800 else if (isProcessing) 500 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProcessing) 2500 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    val primaryColor = when {
        isListening -> PolishPrimary
        isSpeaking -> PolishSuccess
        isProcessing -> PolishTertiary
        else -> PolishPrimaryDark
    }

    val secondaryColor = when {
        isListening -> PolishGlow
        isSpeaking -> PolishPrimary
        isProcessing -> PolishSecondary
        else -> PolishGlow
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("neural_orb_visualizer")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = (this.size.minDimension / 2.6f) * pulseScale * (1f + audioLevel * 0.4f)

            // Outer Aura Rings
            for (i in 1..3) {
                val ringRadius = baseRadius + (i * 14.dp.toPx() * (if (isListening) 1f + audioLevel else 0.8f))
                val alpha = (0.35f / i) * (if (isListening || isSpeaking) 1.2f else 0.6f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = alpha), Color.Transparent),
                        center = center,
                        radius = ringRadius
                    ),
                    center = center,
                    radius = ringRadius
                )
            }

            // Rotating Neural Orbital Rings
            val ringCount = 8
            for (i in 0 until ringCount) {
                val angleRad = Math.toRadians((rotationAngle + (i * (360f / ringCount))).toDouble())
                val orbitX = center.x + (baseRadius * 0.95f) * cos(angleRad).toFloat()
                val orbitY = center.y + (baseRadius * 0.95f) * sin(angleRad).toFloat()

                drawCircle(
                    color = primaryColor.copy(alpha = 0.75f),
                    radius = 3.dp.toPx() + (if (isListening) audioLevel * 4.dp.toPx() else 0f),
                    center = Offset(orbitX, orbitY)
                )
            }

            // Core Glowing Orb Gradient
            val coreBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    primaryColor,
                    secondaryColor.copy(alpha = 0.8f),
                    Color.Transparent
                ),
                center = center - Offset(baseRadius * 0.2f, baseRadius * 0.2f),
                radius = baseRadius
            )

            drawCircle(
                brush = coreBrush,
                radius = baseRadius,
                center = center
            )

            // Dynamic Waveform Arc
            val arcStroke = Stroke(width = 3.5.dp.toPx())
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(primaryColor, secondaryColor, primaryColor),
                    center = center
                ),
                radius = baseRadius * 1.05f,
                center = center,
                style = arcStroke
            )
        }
    }
}

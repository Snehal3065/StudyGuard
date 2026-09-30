package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import kotlin.random.Random

private data class Particle(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float,
    val shape: Int // 0: rect, 1: circle
)

@Composable
fun ConfettiExplosion(
    visible: Boolean,
    onFinished: () -> Unit = {}
) {
    if (!visible) return

    val context = LocalContext.current
    val progress = remember { Animatable(0f) }

    val particles = remember {
        val colors = listOf(
            Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
            Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF8B5CF6),
            Color(0xFFF43F5E), Color(0xFFEAB308)
        )
        List(70) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 700f + 250f
            Particle(
                startX = 0.5f,
                startY = 0.4f,
                velocityX = kotlin.math.cos(angle) * speed,
                velocityY = kotlin.math.sin(angle) * speed - 200f,
                color = colors[Random.nextInt(colors.size)],
                size = Random.nextFloat() * 12f + 6f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                shape = Random.nextInt(2)
            )
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            triggerHapticCelebration(context)
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
            )
            onFinished()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val t = progress.value
        val width = size.width
        val height = size.height
        val gravity = 900f * t * t

        particles.forEach { p ->
            val curX = (width * p.startX) + (p.velocityX * t)
            val curY = (height * p.startY) + (p.velocityY * t) + gravity
            val alpha = (1f - t).coerceIn(0f, 1f)

            if (curX in 0f..width && curY in 0f..height) {
                rotate(degrees = p.rotationSpeed * t, pivot = Offset(curX, curY)) {
                    if (p.shape == 0) {
                        drawRect(
                            color = p.color.copy(alpha = alpha),
                            topLeft = Offset(curX - p.size / 2, curY - p.size / 2),
                            size = Size(p.size, p.size * 0.6f)
                        )
                    } else {
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.size / 2.5f,
                            center = Offset(curX, curY)
                        )
                    }
                }
            }
        }
    }
}

fun triggerHapticCelebration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40, 80, 100), intArrayOf(0, 180, 0, 220, 0, 255), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 60, 50), -1)
        }
    } catch (_: Exception) {}
}

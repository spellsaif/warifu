package org.warifu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.warifu.ui.theme.StatusCritical
import org.warifu.ui.theme.StatusWarning
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creative, Chronograph-inspired Precision Countdown Gauge.
 * Features an analog watch face track with micro tick marks, a luminous sweep arc,
 * a glowing satellite tip dot that glides along the perimeter, and urgency pulsing under 4s.
 */
@Composable
fun CountdownRing(
    remainingSeconds: UInt,
    period: UInt,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    strokeWidth: Dp = 2.5.dp,
    showSecondsLabel: Boolean = false
) {
    val totalPeriod = if (period == 0u) 30f else period.toFloat()
    val progress = (remainingSeconds.toFloat() / totalPeriod).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "timerProgress"
    )

    val isUrgent = remainingSeconds <= 4u
    val isWarning = remainingSeconds <= 7u && !isUrgent

    val targetColor = when {
        isUrgent -> StatusCritical
        isWarning -> StatusWarning
        else -> Color(0xFFFAFAFA)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 250),
        label = "timerColor"
    )

    // Pulse animation for critical seconds (< 4s)
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by if (isUrgent) {
        infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        rememberUpdatedState(1.0f)
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = "$remainingSeconds seconds remaining until security code refreshes"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokePx) / 2f
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)

            // 1. Subtle dark base track
            drawCircle(
                color = Color(0xFF1E1E26),
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokePx)
            )

            // 2. Chronograph micro tick marks
            if (size >= 80.dp) {
                // Large detail mode: 60 precision watch tick marks
                val tickCount = 60
                val tickLength = 5.dp.toPx()
                for (i in 0 until tickCount) {
                    val angleDeg = (i * (360f / tickCount)) - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val isMajor = i % 5 == 0
                    val currentTickLen = if (isMajor) tickLength * 1.5f else tickLength
                    val startR = radius - strokePx / 2f - currentTickLen
                    val endR = radius - strokePx / 2f

                    val p1 = Offset(
                        centerOffset.x + (startR * cos(angleRad)).toFloat(),
                        centerOffset.y + (startR * sin(angleRad)).toFloat()
                    )
                    val p2 = Offset(
                        centerOffset.x + (endR * cos(angleRad)).toFloat(),
                        centerOffset.y + (endR * sin(angleRad)).toFloat()
                    )

                    val tickColor = if (isMajor) Color(0xFF3F3F46) else Color(0xFF27272A)
                    drawLine(
                        color = tickColor,
                        start = p1,
                        end = p2,
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }
            } else {
                // Small card mode: 4 subtle cardinal tick dots at 12, 3, 6, 9
                listOf(-90f, 0f, 90f, 180f).forEach { cardinalAngle ->
                    val angleRad = Math.toRadians(cardinalAngle.toDouble())
                    val dotOffset = Offset(
                        centerOffset.x + (radius * cos(angleRad)).toFloat(),
                        centerOffset.y + (radius * sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = Color(0xFF2D2D38),
                        radius = (strokePx * 0.45f).coerceAtLeast(1f),
                        center = dotOffset
                    )
                }
            }

            // 3. Dynamic sweep arc with pulse alpha
            val sweepAngle = 360f * animatedProgress
            val activeColor = animatedColor.copy(alpha = pulseAlpha)

            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 4. Leading Satellite Dot at tip of the arc
            if (animatedProgress > 0.02f) {
                val tipAngleDeg = -90f + sweepAngle
                val tipAngleRad = Math.toRadians(tipAngleDeg.toDouble())
                val tipCenter = Offset(
                    centerOffset.x + (radius * cos(tipAngleRad)).toFloat(),
                    centerOffset.y + (radius * sin(tipAngleRad)).toFloat()
                )

                // Soft ambient glow halo
                drawCircle(
                    color = activeColor.copy(alpha = 0.35f * pulseAlpha),
                    radius = strokePx * 1.6f,
                    center = tipCenter
                )

                // Crisp solid satellite center
                drawCircle(
                    color = activeColor,
                    radius = strokePx * 0.85f,
                    center = tipCenter
                )
            }
        }

        // Center Typography
        if (showSecondsLabel) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = remainingSeconds.toString(),
                    color = animatedColor,
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SECONDS",
                    color = Color(0xFFA1A1AA),
                    fontSize = (size.value * 0.08f).sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        } else {
            Text(
                text = remainingSeconds.toString(),
                color = animatedColor,
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

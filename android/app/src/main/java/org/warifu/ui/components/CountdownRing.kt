package org.warifu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.warifu.ui.theme.TimerAmber
import org.warifu.ui.theme.TimerGreen
import org.warifu.ui.theme.TimerRed

/**
 * Modern, silky-smooth countdown ring with dynamic color transitions
 * and urgency pulse feedback when approaching expiration.
 */
@Composable
fun CountdownRing(
    remainingSeconds: UInt,
    period: UInt,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    strokeWidth: Dp = 3.dp
) {
    val totalPeriod = if (period == 0u) 30f else period.toFloat()
    val progress = (remainingSeconds.toFloat() / totalPeriod).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "timerProgress"
    )

    val targetColor = when {
        remainingSeconds <= 5u -> TimerRed
        remainingSeconds <= 10u -> TimerAmber
        else -> TimerGreen
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "timerColor"
    )

    // Pulse effect when under 5 seconds
    val isCritical = remainingSeconds <= 5u
    val pulseAlpha by rememberInfiniteTransition(label = "pulseTransition").animateFloat(
        initialValue = if (isCritical) 0.15f else 0.08f,
        targetValue = if (isCritical) 0.35f else 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()

            // Subtle track background
            drawCircle(
                color = animatedColor.copy(alpha = pulseAlpha),
                style = Stroke(width = strokePx)
            )

            // Dynamic progress sweep arc
            drawArc(
                color = animatedColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Text(
            text = remainingSeconds.toString(),
            color = animatedColor,
            fontSize = (size.value * 0.34f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

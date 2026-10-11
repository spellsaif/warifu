package org.openauth.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openauth.ui.theme.TimerAmber
import org.openauth.ui.theme.TimerGreen
import org.openauth.ui.theme.TimerRed

@Composable
fun CountdownRing(
    remainingSeconds: UInt,
    period: UInt,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    strokeWidth: Dp = 4.dp
) {
    val totalPeriod = if (period == 0u) 30f else period.toFloat()
    val progress = (remainingSeconds.toFloat() / totalPeriod).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
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

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Background track
            drawCircle(
                color = targetColor.copy(alpha = 0.15f),
                style = Stroke(width = strokeWidth.toPx())
            )

            // Animated arc
            drawArc(
                color = animatedColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Text(
            text = remainingSeconds.toString(),
            color = animatedColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

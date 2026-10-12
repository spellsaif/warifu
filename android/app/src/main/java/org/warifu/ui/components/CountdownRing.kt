package org.warifu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Minimalist, high-precision countdown ring with TalkBack accessibility semantics.
 * Features quiet monochromatic styling during steady state, shifting to warning
 * accents only during the final critical seconds.
 */
@Composable
fun CountdownRing(
    remainingSeconds: UInt,
    period: UInt,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    strokeWidth: Dp = 2.5.dp
) {
    val totalPeriod = if (period == 0u) 30f else period.toFloat()
    val progress = (remainingSeconds.toFloat() / totalPeriod).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "timerProgress"
    )

    val targetColor = when {
        remainingSeconds <= 4u -> StatusCritical
        remainingSeconds <= 8u -> StatusWarning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 250),
        label = "timerColor"
    )

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

            // Subtle track background
            drawCircle(
                color = animatedColor.copy(alpha = 0.15f),
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
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

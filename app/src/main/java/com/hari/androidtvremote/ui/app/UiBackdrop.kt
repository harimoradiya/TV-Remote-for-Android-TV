package com.hari.androidtvremote.ui.app

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

@Composable
fun AppBackdrop(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val primaryDrift = transition.animateFloat(
        initialValue = -28f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "primaryDrift"
    )
    val tertiaryDrift = transition.animateFloat(
        initialValue = 22f,
        targetValue = -22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tertiaryDrift"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .offset(x = primaryDrift.value.dp, y = 6.dp)
                .size(maxWidth * 0.56f)
                .alpha(0.18f)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                            Color.Transparent
                        )
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                )
        )
        Box(
            modifier = Modifier
                .offset(x = maxWidth * 0.44f, y = 180.dp + tertiaryDrift.value.dp)
                .size(maxWidth * 0.48f)
                .alpha(0.16f)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.34f),
                            Color.Transparent
                        )
                    ),
                    shape = MaterialTheme.shapes.large
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.10f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class FloatingPlusOne(
    val id: Long,
    val offsetX: Float,
    val animProgress: Animatable<Float, *>
)

@Composable
fun OneTapLoggerButton(
    onClick: () -> Unit,
    hapticEnabled: Boolean = true,
    isCountToTenMode: Boolean = false,
    sessionCount: Int = 0,
    sessionTarget: Int = 10,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth press scale effect
    val pressScale = if (isPressed) 0.92f else 1.0f

    // Soft resting pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Floating "+1" particles
    val floatingList = remember { mutableStateListOf<FloatingPlusOne>() }

    fun triggerTap() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        // Spawn floating indicator
        val newId = System.currentTimeMillis() + Random.nextLong(1000)
        val anim = Animatable(0f)
        val offsetX = (Random.nextFloat() - 0.5f) * 80f
        val item = FloatingPlusOne(newId, offsetX, anim)
        floatingList.add(item)

        scope.launch {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = LinearEasing)
            )
            floatingList.remove(item)
        }

        onClick()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(260.dp)
    ) {
        // Outer ambient glow ring
        Box(
            modifier = Modifier
                .size(255.dp)
                .scale(pulseGlow)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Mid accent decorative border ring
        Box(
            modifier = Modifier
                .size(228.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    shape = CircleShape
                )
        )

        // Main tactile button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(208.dp)
                .scale(pressScale)
                .shadow(
                    elevation = if (isPressed) 6.dp else 16.dp,
                    shape = CircleShape,
                    ambientColor = MaterialTheme.colorScheme.primary,
                    spotColor = MaterialTheme.colorScheme.primary
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                        )
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { triggerTap() }
                )
                .testTag("one_tap_logger_button")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isCountToTenMode) Icons.Default.Favorite else Icons.Default.TouchApp,
                    contentDescription = "Log Kick",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isCountToTenMode) "LOG KICK" else "TAP TO LOG",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = if (isCountToTenMode) {
                        "$sessionCount / $sessionTarget"
                    } else {
                        "Movement"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Render floating +1 animations
        floatingList.forEach { plusOne ->
            val progress = plusOne.animProgress.value
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val offsetY = -progress * 130f

            Text(
                text = "+1",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = plusOne.offsetX.dp.roundToPx(),
                            y = offsetY.dp.roundToPx()
                        )
                    }
            )
        }
    }
}

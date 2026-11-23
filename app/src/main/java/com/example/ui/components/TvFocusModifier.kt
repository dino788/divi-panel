package com.example.ui.components

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.DiviViolet

@Composable
fun rememberIsTvDevice(): Boolean {
    val context = LocalContext.current
    return remember(context) { isTvDevice(context) }
}

fun isTvDevice(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

/**
 * Adds crisp IBO Player-style Android TV D-Pad remote focus feedback (glowing cyan-violet border
 * + smooth spring scale elevation) while keeping normal phone/tablet touch ripples intact.
 */
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(16.dp),
    focusedScale: Float = 1.045f,
    borderWidth: Dp = 2.5.dp,
    focusColor: Color = DiviCyan,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "tv_focus_scale"
    )

    this
        .zIndex(if (isFocused) 2f else 0f)
        .scale(scale)
        .clip(shape)
        .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
        .then(
            if (isFocused) {
                Modifier.border(
                    width = borderWidth,
                    brush = Brush.linearGradient(listOf(Color.White, focusColor, DiviViolet)),
                    shape = shape
                )
            } else {
                Modifier
            }
        )
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
}

/**
 * Focus ring highlight for already-clickable M3 components (Button, IconButton, OutlinedButton)
 * when navigating with an Android TV remote D-Pad.
 */
fun Modifier.tvFocusRing(
    shape: Shape = CircleShape,
    focusedScale: Float = 1.08f,
    focusColor: Color = DiviCyan
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "tv_ring_scale"
    )
    this
        .zIndex(if (isFocused) 2f else 0f)
        .scale(scale)
        .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
        .then(
            if (isFocused) {
                Modifier.border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(listOf(Color.White, focusColor)),
                    shape = shape
                )
            } else {
                Modifier
            }
        )
}

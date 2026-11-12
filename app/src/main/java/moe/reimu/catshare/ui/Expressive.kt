package moe.reimu.catshare.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Corner radius that contracts while the surface is held down.
 *
 * This is the signature Expressive interaction: containers do not only change colour on press,
 * they change *shape*, and they do it with the springy fast-spatial motion spec so the movement
 * reads as physical rather than as a fade.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun pressMorphShape(
    interactionSource: MutableInteractionSource,
    resting: Dp = 28.dp,
    pressed: Dp = 20.dp,
): RoundedCornerShape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) pressed else resting,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressCorner",
    )
    return RoundedCornerShape(corner)
}

/**
 * Tonal card colour that stays distinguishable from the page surface.
 *
 * Android's pure-black dark theme can map every surface role to the same black colour. In that
 * case, lift the card with a small on-surface overlay so tonal layering remains visible.
 */
@Composable
fun expressiveCardContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    val preferred = scheme.surfaceContainerHigh
    return if (preferred.contrastRatio(scheme.surface) >= 1.12f) {
        preferred
    } else {
        scheme.onSurface.copy(alpha = 0.10f).compositeOver(scheme.surface)
    }
}

private fun Color.contrastRatio(other: Color): Float {
    val lighter = max(luminance(), other.luminance())
    val darker = min(luminance(), other.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * Primary surface used throughout the app: a large-radius container that morphs when pressed.
 *
 * Material 3 Expressive leans on generous corner radii and tonal layering instead of elevation, so
 * this exposes the container colour directly rather than a shadow.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = expressiveCardContainerColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    restingCorner: Dp = 28.dp,
    pressedCorner: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = pressMorphShape(interactionSource, restingCorner, pressedCorner)

    // Clip before clickable so the ripple is bounded by the animated corner radius; drawing the
    // indication outside the shape is what produced square ripples on rounded containers.
    Surface(
        modifier = if (onClick != null) {
            modifier
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick,
                )
        } else {
            modifier.clip(shape)
        },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(content = content)
    }
}

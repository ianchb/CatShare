package moe.reimu.catshare.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * CatShare's Material 3 Expressive theme.
 *
 * Expressive differs from the baseline Material 3 theme in three ways, all of which matter for how
 * the app feels: motion uses the springy "expressive" motion scheme (spatial springs overshoot
 * slightly instead of easing flat), shapes gain the larger 32dp/48dp corner steps, and typography
 * exposes emphasized weights. [MaterialExpressiveTheme] wires the first automatically and the
 * library defaults already carry the Expressive shape and type scales, so only the colour scheme
 * and motion scheme are chosen here.
 *
 * Colour prefers the wallpaper-derived dynamic scheme on Android 12+ so the app sits naturally in
 * the user's own theme, and falls back to the brand palette sampled from the launcher icon.
 */
@Composable
fun CatShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> brandColorScheme(darkTheme)
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes(),
        typography = Typography(),
        content = content,
    )
}
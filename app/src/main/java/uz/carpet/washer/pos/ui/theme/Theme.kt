package uz.carpet.washer.pos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ===== RANG TIZIMI (Color Palette) =====
val Primary = Color(0xFF2563EB)          // Asosiy ko'k
val PrimaryDark = Color(0xFF1D4ED8)      // To'q ko'k (hover)
val PrimaryLight = Color(0xFFEFF6FF)     // Och ko'k (background chip)
val Secondary = Color(0xFF10B981)        // Yashil (muvaffaqiyat)
val Warning = Color(0xFFF59E0B)          // Sariq (ogohlantirish)
val Danger = Color(0xFFEF4444)           // Qizil (xato)
val BackgroundApp = Color(0xFFF1F5F9)    // Asosiy fon
val CardSurface = Color(0xFFFFFFFF)      // Karta foni
val TextPrimary = Color(0xFF1E293B)      // Asosiy matn
val TextSecondary = Color(0xFF64748B)    // Ikkilamchi matn
val Divider = Color(0xFFE2E8F0)          // Ajratuvchi chiziq

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = BackgroundApp,
    surface = CardSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Danger,
    onError = Color.White,
)

@Composable
fun CarpetPosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

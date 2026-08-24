package uz.carpet.washer.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import uz.carpet.washer.pos.ui.navigation.AppNavigation
import uz.carpet.washer.pos.ui.theme.CarpetPosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarpetPosTheme {
                AppNavigation()
            }
        }
    }
}

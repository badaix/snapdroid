package de.badaix.snapcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import de.badaix.snapcast.ui.MainScreen
import de.badaix.snapcast.ui.theme.SnapdroidTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnapdroidTheme {
                MainScreen()
            }
        }
    }
}

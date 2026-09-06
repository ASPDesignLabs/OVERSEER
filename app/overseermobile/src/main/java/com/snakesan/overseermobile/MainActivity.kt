package com.snakesan.overseermobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.snakesan.overseermobile.ui.ConfigurationScreen
import com.snakesan.overseermobile.ui.theme.OverseerMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OverseerMobileTheme {
                Surface {
                    ConfigurationScreen()
                }
            }
        }
    }
}

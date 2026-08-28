package com.hereliesaz.lamplight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lamplight.ui.LamplightApp
import com.hereliesaz.lamplight.ui.LamplightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LamplightTheme {
                val vm: LamplightViewModel = viewModel()
                LamplightApp(vm)
            }
        }
    }
}

package com.hereliesaz.jamaisvu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.jamaisvu.ui.JamaisVuApp
import com.hereliesaz.jamaisvu.ui.JamaisVuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JamaisVuTheme {
                val vm: JamaisVuViewModel = viewModel()
                JamaisVuApp(vm)
            }
        }
    }
}

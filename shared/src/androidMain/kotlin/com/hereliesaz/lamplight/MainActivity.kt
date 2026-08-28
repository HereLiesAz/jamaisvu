package com.hereliesaz.lamplight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lamplight.ui.AndroidUpdateBanner
import com.hereliesaz.lamplight.ui.LamplightApp
import com.hereliesaz.lamplight.ui.LamplightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LamplightTheme {
                val vm: LamplightViewModel = viewModel { LamplightViewModel(AndroidSettingsStore(application)) }
                // Owned here, not by LamplightViewModel: the GitHub-releases self-update surface
                // is Android-only with no web counterpart, ever (see GitHubUpdateController), so
                // it's fed into LamplightApp's platformBanner slot instead of threading through
                // the shared ViewModel.
                val updateScope = rememberCoroutineScope()
                val updateController = remember { GitHubUpdateController(application, updateScope) }
                LamplightApp(vm, platformBanner = { AndroidUpdateBanner(updateController) })
            }
        }
    }
}

package com.hereliesaz.lamplight.ui

import androidx.compose.runtime.Composable

/**
 * Intercepts the system back gesture/button while [enabled], calling [onBack] instead of the
 * default action. No web equivalent: this app never pushes a browser history entry when opening
 * a place detail, so there's no back-navigation moment for a web actual to intercept -- a no-op
 * there is the honest answer, not a stand-in for a real implementation.
 */
@Composable
expect fun BackHandler(enabled: Boolean, onBack: () -> Unit)

package com.ali.agent.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AliAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString().orEmpty()
            if (pkg.isNotBlank() && pkg != "com.ali.agent") {
                _lastForegroundPackage.value = pkg
            }
        }
    }

    override fun onInterrupt() {
        // No-op: strictly passive local context observer
    }

    companion object {
        private val _lastForegroundPackage = MutableStateFlow("Home Screen")
        val lastForegroundPackage: StateFlow<String> = _lastForegroundPackage.asStateFlow()
    }
}

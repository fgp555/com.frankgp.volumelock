package com.frankgp.volumelock

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class VolumeAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not required for key event interception
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val prefs = getSharedPreferences("volume_lock_prefs", MODE_PRIVATE)
        val isDisabled = prefs.getBoolean("is_volume_disabled", false)

        if (!isDisabled) {
            return super.onKeyEvent(event)
        }

        val disableDown = prefs.getBoolean("disable_vol_down", true)
        val disableUp = prefs.getBoolean("disable_vol_up", false)
        val triggerButton = prefs.getString("trigger_button", "UP") ?: "UP"

        val keyCode = event.keyCode
        val isDownKey = (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        val isUpKey = (keyCode == KeyEvent.KEYCODE_VOLUME_UP)

        // Intercept if the key is blocked OR if it is configured to trigger the modal
        val shouldIntercept = (isDownKey && (disableDown || triggerButton == "DOWN")) ||
                              (isUpKey && (disableUp || triggerButton == "UP"))

        if (shouldIntercept) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                val shouldTrigger = (isUpKey && triggerButton == "UP") || (isDownKey && triggerButton == "DOWN")
                if (shouldTrigger) {
                    handler.post {
                        Toast.makeText(
                            applicationContext,
                            "Control de volumen abierto",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    val intent = Intent(this, VolumeDialogActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        )
                    }
                    startActivity(intent)
                } else {
                    handler.post {
                        Toast.makeText(
                            applicationContext,
                            "Botón de volumen desactivado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            return true // Consume key event globally
        }
        return super.onKeyEvent(event)
    }
}

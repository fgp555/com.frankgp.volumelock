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

        val keyCode = event.keyCode
        if (isDisabled && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    handler.post {
                        Toast.makeText(
                            applicationContext,
                            "Botón físico bloqueado (Use el control en pantalla)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    val intent = Intent(this, VolumeDialogActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                } else {
                    handler.post {
                        Toast.makeText(
                            applicationContext,
                            "Botón de bajar volumen desactivado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            return true // Consume key event globally for both volume keys when disabled
        }
        return super.onKeyEvent(event)
    }
}

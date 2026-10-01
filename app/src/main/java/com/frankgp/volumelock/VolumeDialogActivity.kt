package com.frankgp.volumelock

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frankgp.volumelock.ui.theme.FGPVolumeLockTheme

class VolumeDialogActivity : ComponentActivity() {

    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        setContent {
            FGPVolumeLockTheme {
                VolumeOnlyDialog(
                    audioManager = audioManager,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun VolumeOnlyDialog(
    audioManager: AudioManager,
    onDismiss: () -> Unit
) {
    var currentVolume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    val maxVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    }

    fun updateVolume(newVolume: Float) {
        val intVol = newVolume.toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, intVol, 0)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when {
                    currentVolume == 0f -> Icons.Default.VolumeMute
                    currentVolume < maxVolume / 2 -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                },
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Ajustar Volumen",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Volumen actual: ${currentVolume.toInt()} / ${maxVolume.toInt()}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Slider(
                    value = currentVolume,
                    onValueChange = { newVal ->
                        currentVolume = newVal
                        updateVolume(newVal)
                    },
                    valueRange = 0f..maxVolume,
                    steps = maxVolume.toInt() - 1
                )

                Text(
                    text = "🔒 El botón físico está bloqueado globalmente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Aceptar")
            }
        }
    )
}

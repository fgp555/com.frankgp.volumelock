package com.frankgp.volumelock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frankgp.volumelock.ui.theme.FGPVolumeLockTheme

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var audioManager: AudioManager

    var isVolumeDisabled by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sharedPreferences = getSharedPreferences("volume_lock_prefs", MODE_PRIVATE)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        isVolumeDisabled = sharedPreferences.getBoolean("is_volume_disabled", false)

        setContent {
            FGPVolumeLockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VolumeLockScreen(
                        modifier = Modifier.padding(innerPadding),
                        isVolumeDisabled = isVolumeDisabled,
                        onToggleDisable = { disabled ->
                            isVolumeDisabled = disabled
                            sharedPreferences.edit().putBoolean("is_volume_disabled", disabled).apply()
                        },
                        onOpenDialog = {
                            startActivity(Intent(this, VolumeDialogActivity::class.java))
                        },
                        context = this
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isVolumeDisabled = sharedPreferences.getBoolean("is_volume_disabled", false)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isVolumeDisabled && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                startActivity(Intent(this, VolumeDialogActivity::class.java))
                Toast.makeText(this, "Botón físico deshabilitado. Use el control en pantalla.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Botón de bajar volumen desactivado", Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isVolumeDisabled && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}

fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (service in enabledServices) {
        val info = service.resolveInfo
        if (info.serviceInfo.packageName == context.packageName && info.serviceInfo.name == serviceClass.name) {
            return true
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeLockScreen(
    modifier: Modifier = Modifier,
    isVolumeDisabled: Boolean,
    onToggleDisable: (Boolean) -> Unit,
    onOpenDialog: () -> Unit,
    context: Context
) {
    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, VolumeAccessibilityService::class.java))
    }

    LaunchedEffect(Unit) {
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context, VolumeAccessibilityService::class.java)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FGP Volume Lock") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVolumeDisabled) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isVolumeDisabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isVolumeDisabled) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (isVolumeDisabled) "Volumen Físico Deshabilitado" else "Volumen Físico Habilitado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isVolumeDisabled) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isVolumeDisabled) {
                                "Los botones físicos están bloqueados (Bajar volumen muestra un aviso discreto)."
                            } else {
                                "Los botones físicos de volumen funcionan con normalidad."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isVolumeDisabled) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Background / Accessibility Permission Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAccessibilityEnabled) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = if (isAccessibilityEnabled) "Modo Segundo Plano (YouTube): Activo" else "Modo Segundo Plano (YouTube): Inactivo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isAccessibilityEnabled) {
                                "El servicio de accesibilidad está activo. Funciona con YouTube u otras apps reproduciendo audio."
                            } else {
                                "Para bloquear el botón físico cuando YouTube reproduzca en segundo plano, activa el servicio de accesibilidad."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        if (!isAccessibilityEnabled) {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("Activar Permiso en Ajustes")
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onToggleDisable(!isVolumeDisabled) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVolumeDisabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isVolumeDisabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = if (isVolumeDisabled) "Habilitar Botón Físico" else "Deshabilitar Botón Físico",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onOpenDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Abrir Control de Volumen Manual",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

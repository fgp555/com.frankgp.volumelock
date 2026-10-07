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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

        if (isVolumeDisabled) {
            VolumeForegroundService.startService(this)
        }

        setContent {
            FGPVolumeLockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VolumeLockScreen(
                        modifier = Modifier.padding(innerPadding),
                        isVolumeDisabled = isVolumeDisabled,
                        onToggleDisable = { disabled ->
                            isVolumeDisabled = disabled
                            sharedPreferences.edit().putBoolean("is_volume_disabled", disabled).apply()
                            if (disabled) {
                                VolumeForegroundService.startService(this)
                            } else {
                                VolumeForegroundService.stopService(this)
                            }
                        },
                        onOpenDialog = {
                            startActivity(Intent(this, VolumeDialogActivity::class.java))
                        },
                        sharedPreferences = sharedPreferences,
                        context = this
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isVolumeDisabled = sharedPreferences.getBoolean("is_volume_disabled", false)
        if (isVolumeDisabled) {
            VolumeForegroundService.startService(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isVolumeDisabled) {
            return super.onKeyDown(keyCode, event)
        }

        val disableDown = sharedPreferences.getBoolean("disable_vol_down", true)
        val disableUp = sharedPreferences.getBoolean("disable_vol_up", false)
        val triggerButton = sharedPreferences.getString("trigger_button", "UP") ?: "UP"

        val isDownKey = (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        val isUpKey = (keyCode == KeyEvent.KEYCODE_VOLUME_UP)

        val shouldIntercept = (isDownKey && (disableDown || triggerButton == "DOWN")) ||
                              (isUpKey && (disableUp || triggerButton == "UP"))

        if (shouldIntercept) {
            val shouldTrigger = (isUpKey && triggerButton == "UP") || (isDownKey && triggerButton == "DOWN")
            if (shouldTrigger) {
                startActivity(Intent(this, VolumeDialogActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                })
                Toast.makeText(this, "Control de volumen abierto", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Botón de volumen desactivado", Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isVolumeDisabled) {
            return super.onKeyUp(keyCode, event)
        }

        val disableDown = sharedPreferences.getBoolean("disable_vol_down", true)
        val disableUp = sharedPreferences.getBoolean("disable_vol_up", false)
        val triggerButton = sharedPreferences.getString("trigger_button", "UP") ?: "UP"

        val isDownKey = (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        val isUpKey = (keyCode == KeyEvent.KEYCODE_VOLUME_UP)

        val shouldIntercept = (isDownKey && (disableDown || triggerButton == "DOWN")) ||
                              (isUpKey && (disableUp || triggerButton == "UP"))

        if (shouldIntercept) {
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
    sharedPreferences: SharedPreferences,
    context: Context
) {
    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, VolumeAccessibilityService::class.java))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context, VolumeAccessibilityService::class.java)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var disableVolDown by remember {
        mutableStateOf(sharedPreferences.getBoolean("disable_vol_down", true))
    }
    var disableVolUp by remember {
        mutableStateOf(sharedPreferences.getBoolean("disable_vol_up", false))
    }
    var triggerButton by remember {
        mutableStateOf(sharedPreferences.getString("trigger_button", "UP") ?: "UP")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FGP Volume Lock",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Card (Clickable to toggle protection)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDisable(!isVolumeDisabled) },
                shape = MaterialTheme.shapes.extraLarge,
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = if (isVolumeDisabled) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isVolumeDisabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isVolumeDisabled) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isVolumeDisabled) "Protección Activa" else "Protección Inactiva",
                        style = MaterialTheme.typography.titleLarge,
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
                            "Toca aquí para desactivar la protección."
                        } else {
                            "Toca aquí para activar la protección."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isVolumeDisabled) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Manual Control Button in second place
            OutlinedButton(
                onClick = onOpenDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Abrir Control de Volumen Manual",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Configuration Card: Select which buttons to disable and which triggers modal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isVolumeDisabled) 1f else 0.5f),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ Configuración de Botones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isVolumeDisabled) {
                            Text(
                                text = "(Activa la protección)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "1. Selecciona qué botones bloquear:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clickable {
                                if (isVolumeDisabled) {
                                    disableVolUp = !disableVolUp
                                    sharedPreferences.edit().putBoolean("disable_vol_up", disableVolUp).apply()
                                } else {
                                    Toast.makeText(context, "Activa la protección primero", Toast.LENGTH_SHORT).show()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = disableVolUp,
                            onCheckedChange = { checked ->
                                if (isVolumeDisabled) {
                                    disableVolUp = checked
                                    sharedPreferences.edit().putBoolean("disable_vol_up", checked).apply()
                                } else {
                                    Toast.makeText(context, "Activa la protección primero", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.scale(0.85f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Bloquear Subir Volumen (+)", style = MaterialTheme.typography.bodyMedium)
                    }


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clickable {
                                if (isVolumeDisabled) {
                                    disableVolDown = !disableVolDown
                                    sharedPreferences.edit().putBoolean("disable_vol_down", disableVolDown).apply()
                                } else {
                                    Toast.makeText(context, "Activa la protección primero", Toast.LENGTH_SHORT).show()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = disableVolDown,
                            onCheckedChange = { checked ->
                                if (isVolumeDisabled) {
                                    disableVolDown = checked
                                    sharedPreferences.edit().putBoolean("disable_vol_down", checked).apply()
                                } else {
                                    Toast.makeText(context, "Activa la protección primero", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.scale(0.85f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Bloquear Bajar Volumen (-)", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "2. Botón que abrirá el control flotante:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    listOf(
                        "UP" to "Subir Volumen (+)",
                        "DOWN" to "Bajar Volumen (-)",
                        "NONE" to "Ninguno (Solo bloqueo silencioso)"
                    ).forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clickable {
                                    if (isVolumeDisabled) {
                                        triggerButton = key
                                        sharedPreferences.edit().putString("trigger_button", key).apply()
                                    } else {
                                        Toast.makeText(context, "Activa la protección primero", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (triggerButton == key),
                                onClick = {
                                    if (isVolumeDisabled) {
                                        triggerButton = key
                                        sharedPreferences.edit().putString("trigger_button", key).apply()
                                    } else {
                                        Toast.makeText(context, "Activa la protección primero", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.scale(0.85f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Background / Accessibility Permission Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityEnabled) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
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
                            text = if (isAccessibilityEnabled) "Segundo Plano: Activo" else "Segundo Plano: Inactivo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isAccessibilityEnabled) {
                            "El servicio de accesibilidad opera correctamente en segundo plano."
                        } else {
                            "Activa el servicio de accesibilidad para proteger los botones en segundo plano."
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
                            modifier = Modifier.padding(top = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Text("Activar en Ajustes")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

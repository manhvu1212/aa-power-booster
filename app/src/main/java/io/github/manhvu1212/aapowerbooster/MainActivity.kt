package io.github.manhvu1212.aapowerbooster

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Quyền Bluetooth đã được cấp!", Toast.LENGTH_SHORT).show()
            autoConnectSaved()
        } else {
            Toast.makeText(this, "Bạn cần cấp quyền để quét thiết bị!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleManager = BleManager.getInstance(this)

        checkAndRequestPermissions()
        autoConnectSaved()

        setContent {
            AAPowerBoosterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompanionScreen(bleManager)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    // Auto-connect to the saved device when entering the app (no-op if none / no permission yet).
    private fun autoConnectSaved() {
        val saved = bleManager.getSavedAddress() ?: return
        if (bleManager.connectionState.value == BleManager.ConnectionState.DISCONNECTED) {
            bleManager.connect(saved)
        }
    }
}

@Composable
fun AAPowerBoosterTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF00FFCC),
        secondary = Color(0xFF00FFCC).copy(alpha = 0.7f),
        background = Color(0xFF0F0F16)
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionScreen(bleManager: BleManager) {
    val connectionState by bleManager.connectionState.collectAsState()
    val scannedDevices by bleManager.scannedDevices.collectAsState()
    val isScanning by bleManager.isScanning.collectAsState()
    val savedAddress by bleManager.savedDeviceAddress.collectAsState()
    val savedName by bleManager.savedDeviceName.collectAsState()

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E2C), Color(0xFF0F0F16))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            // Keep content clear of the status bar, navigation bar and display cutout (notch)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Connection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF26263B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Trạng thái kết nối",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText: String
                    val statusColor: Color
                    when (connectionState) {
                        BleManager.ConnectionState.CONNECTED -> {
                            statusText = "ĐÃ KẾT NỐI"
                            statusColor = Color(0xFF00FF66)
                        }
                        BleManager.ConnectionState.CONNECTING -> {
                            statusText = "ĐANG KẾT NỐI..."
                            statusColor = Color(0xFFFFCC00)
                        }
                        BleManager.ConnectionState.DISCONNECTED -> {
                            statusText = "CHƯA KẾT NỐI / THIẾT BỊ BẬN"
                            statusColor = Color(0xFFFF3366)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = statusText, color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                if (savedAddress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thiết bị hoạt động: $savedName\nMAC: $savedAddress",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        Button(
                            onClick = {
                                if (connectionState == BleManager.ConnectionState.CONNECTED) {
                                    bleManager.disconnect()
                                } else {
                                    savedAddress?.let { bleManager.connect(it) }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (connectionState == BleManager.ConnectionState.CONNECTED) Color(0xFFFF3366) else Color(0xFF00FFCC),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (connectionState == BleManager.ConnectionState.CONNECTED) "Ngắt kết nối BLE" else "Kết nối thiết bị")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                bleManager.disconnect()
                                bleManager.clearSavedDevice()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text("Xóa lưu")
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chưa lưu thiết bị. Dò quét ở bên dưới để kết nối.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        ModeControlCard(bleManager)

        // Scanner only shows when no device has been saved yet
        if (savedAddress == null) {
        Spacer(modifier = Modifier.height(16.dp))

        // Scanner Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thiết bị quét được",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
            Button(
                onClick = {
                    if (isScanning) {
                        bleManager.stopScan()
                    } else {
                        bleManager.startScan()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) Color.DarkGray else Color(0xFF00FFCC),
                    contentColor = Color.Black
                )
            ) {
                Text(text = if (isScanning) "Dừng quét" else "Dò tìm")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Device List
        if (scannedDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isScanning) "Đang tìm kiếm thiết bị..." else "Nhấn \"Dò tìm\" để quét thiết bị chân ga.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161622))
            ) {
                items(scannedDevices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                bleManager.saveDevice(device)
                                bleManager.connect(device.address)
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            @SuppressLint("MissingPermission")
                            Text(
                                text = device.name ?: "Unknown Device",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = device.address,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "Kết nối >",
                            color = Color(0xFF00FFCC),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                }
            }
        }
        }
    }
}

@Composable
fun ModeControlCard(bleManager: BleManager) {
    val connectionState by bleManager.connectionState.collectAsState()
    val activeMode by bleManager.activeMode.collectAsState()
    val activeLevel by bleManager.activeLevel.collectAsState()

    // Only show controls while connected (sending commands otherwise has no effect)
    if (connectionState != BleManager.ConnectionState.CONNECTED) return

    // Modes in display order: Race - Sport - City - Normal - Eco (icon only, no label needed)
    val modes = listOf(
        Triple(1, "Race", R.drawable.ic_mode_race),
        Triple(2, "Sport", R.drawable.ic_mode_sport),
        Triple(3, "City", R.drawable.ic_mode_city),
        Triple(4, "Normal", R.drawable.ic_mode_normal),
        Triple(5, "Eco", R.drawable.ic_mode_eco)
    )
    val isNormal = activeMode == 4
    val activeModeName = modes.firstOrNull { it.first == activeMode }?.second ?: "—"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF26263B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chọn chế độ lái",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = activeModeName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF00FFCC)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 5 modes as 5 evenly-sized icon tiles on a single row (no scrolling, no labels).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                modes.forEach { (id, name, iconRes) ->
                    val isActive = activeMode == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isActive) Color(0xFF00FFCC) else Color(0xFF161622))
                            .clickable {
                                // Each mode keeps its own level; restore that mode's stored level.
                                val lvl = if (id == 4) 0 else bleManager.levelForMode(id)
                                bleManager.setMode(id, lvl)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = name,
                            tint = if (isActive) Color.Black else Color(0xFF9AA0B5),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Cấp độ nhạy", color = Color.White, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            val newLevel = (activeLevel - 1).coerceAtLeast(1)
                            bleManager.setMode(activeMode, newLevel)
                        },
                        enabled = !isNormal,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FFCC))
                    ) {
                        Text(text = "−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (isNormal) "—" else "$activeLevel",
                        modifier = Modifier.widthIn(min = 48.dp),
                        textAlign = TextAlign.Center,
                        color = Color(0xFF00FFCC),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = {
                            val newLevel = (activeLevel + 1).coerceAtMost(9)
                            bleManager.setMode(activeMode, newLevel)
                        },
                        enabled = !isNormal,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FFCC))
                    ) {
                        Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (isNormal) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chế độ Normal (zin) không chỉnh cấp độ.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}



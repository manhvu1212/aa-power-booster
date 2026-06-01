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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
        } else {
            Toast.makeText(this, "Bạn cần cấp quyền để quét thiết bị!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleManager = BleManager.getInstance(this)

        checkAndRequestPermissions()

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
    val activeMode by bleManager.activeMode.collectAsState()
    val activeLevel by bleManager.activeLevel.collectAsState()
    val scannedDevices by bleManager.scannedDevices.collectAsState()
    val isScanning by bleManager.isScanning.collectAsState()

    val savedAddress = bleManager.getSavedAddress()
    val savedName = bleManager.getSavedName()

    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E2C), Color(0xFF0F0F16))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "AA Power Booster",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FFCC),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Companion Controller App • v$versionName",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        CrashLogCard()

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

                if (connectionState == BleManager.ConnectionState.CONNECTED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val modeName = when (activeMode) {
                        1 -> "Race"
                        2 -> "Sport"
                        3 -> "City"
                        4 -> "Normal"
                        5 -> "Eco"
                        else -> "Unknown"
                    }
                    Text(
                        text = "Chế độ hiện tại: $modeName (Cấp $activeLevel)",
                        color = Color(0xFF00FFCC),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
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
                                    bleManager.connect(savedAddress)
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

        Spacer(modifier = Modifier.height(12.dp))
        RawLogCard(bleManager)
    }
}

@Composable
fun ModeControlCard(bleManager: BleManager) {
    val connectionState by bleManager.connectionState.collectAsState()
    val activeMode by bleManager.activeMode.collectAsState()
    val activeLevel by bleManager.activeLevel.collectAsState()

    // Only show controls while connected (sending commands otherwise has no effect)
    if (connectionState != BleManager.ConnectionState.CONNECTED) return

    // Modes in display order: Race - Sport - Normal - City - Eco
    val modes = listOf(
        1 to "Race",
        2 to "Sport",
        4 to "Normal",
        3 to "City",
        5 to "Eco"
    )
    val isNormal = activeMode == 4

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF26263B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Chọn chế độ lái",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            modes.chunked(3).forEach { rowModes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowModes.forEach { (id, name) ->
                        val isActive = activeMode == id
                        Button(
                            onClick = {
                                val lvl = if (id == 4) 0 else (if (activeLevel == 0) 5 else activeLevel)
                                bleManager.setMode(id, lvl)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) Color(0xFF00FFCC) else Color(0xFF161622),
                                contentColor = if (isActive) Color.Black else Color.White
                            )
                        ) {
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    // Keep button widths aligned when the last row is not full
                    repeat(3 - rowModes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
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

@Composable
fun CrashLogCard() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var status by remember { mutableStateOf(PowerBoosterApp.getStatus(context)) }
    var crash by remember { mutableStateOf(PowerBoosterApp.getCrash(context)) }

    if (status == null && crash == null) return

    val combined = buildString {
        if (status != null) {
            append("== Trạng thái Android Auto (chạy tới đâu) ==\n")
            append(status)
            append("\n")
        }
        if (crash != null) {
            if (isNotEmpty()) append("\n")
            append("== Crash ==\n")
            append(crash)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1A1A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠ Chẩn đoán Android Auto",
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(combined)) }) {
                        Text("Copy", color = Color(0xFF00FFCC))
                    }
                    TextButton(onClick = {
                        PowerBoosterApp.clearCrash(context)
                        status = null
                        crash = null
                    }) {
                        Text("Xóa", color = Color.White)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A0D0D))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        text = combined,
                        color = Color(0xFFFFC2C2),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun RawLogCard(bleManager: BleManager) {
    val rawLog by bleManager.rawLog.collectAsState()
    val clipboard = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161622))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAW BLE Notify (debug)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(rawLog.joinToString("\n")))
                    }) {
                        Text("Copy", color = Color(0xFF00FFCC))
                    }
                    TextButton(onClick = { bleManager.clearRawLog() }) {
                        Text("Xóa", color = Color(0xFFFF3366))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F0F16))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        text = if (rawLog.isEmpty())
                            "Chưa có dữ liệu. Kết nối thiết bị rồi đổi chế độ/cấp độ để bắt gói tin."
                        else
                            rawLog.joinToString("\n"),
                        color = Color(0xFF9CFFE9),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

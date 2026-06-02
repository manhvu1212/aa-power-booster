package io.github.manhvu1212.aapowerbooster

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

@SuppressLint("MissingPermission")
class BleManager private constructor(context: Context) {

    companion object {
        private const val TAG = "BleManager"

        // Target GATT UUIDs based on decompiled app
        val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // P/R master toggle (the 0xFA command in the original app's setPMode). value[9] in the
        // status packet reports the current state. Mapping confirmed on a real device.
        private const val BOOST_VALUE_P = 0 // "P" — booster active
        private const val BOOST_VALUE_R = 1 // "R" — back to the original throttle signal

        private const val PREFS_NAME = "speed_prefs"
        private const val KEY_DEVICE_ADDRESS = "device_address"
        private const val KEY_DEVICE_NAME = "device_name"

        @Volatile
        private var instance: BleManager? = null

        fun getInstance(context: Context): BleManager {
            return instance ?: synchronized(this) {
                instance ?: BleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val appContext = context
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    // Connection States
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Mode States (Supported 5 modes: 1=Race, 2=Sport, 3=City/Comfort, 4=Normal, 5=Eco)
    private val _activeMode = MutableStateFlow(4) // Default is Normal (4)
    val activeMode: StateFlow<Int> = _activeMode.asStateFlow()

    private val _activeLevel = MutableStateFlow(0) // Default level is 0
    val activeLevel: StateFlow<Int> = _activeLevel.asStateFlow()

    // Per-mode stored levels parsed from the device's getData response (mode id -> level).
    // 1=Race, 2=Sport, 3=City, 5=Eco. Normal (4) has no level.
    private val _modeLevels = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val modeLevels: StateFlow<Map<Int, Int>> = _modeLevels.asStateFlow()

    // One-shot confirmation events: emitted (mode, level) when the device's data arrives in
    // response to a command the user just sent, so the UI can show a transient toast. Not emitted
    // for background syncs (initial connect query) because no user command is pending then.
    private val _commandConfirmed = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 4)
    val commandConfirmed: SharedFlow<Pair<Int, Int>> = _commandConfirmed.asSharedFlow()
    private var pendingUserCommand = false

    // Master booster on/off (P vs R), parsed from value[9] of the status packet. true = "P".
    private val _boostOn = MutableStateFlow(true)
    val boostOn: StateFlow<Boolean> = _boostOn.asStateFlow()

    // One-shot confirmation for the P/R toggle, mirroring commandConfirmed: emits the new state
    // (true = P) only when the device echoes back a toggle the user just sent (silent on syncs).
    private val _boostConfirmed = MutableSharedFlow<Boolean>(extraBufferCapacity = 4)
    val boostConfirmed: SharedFlow<Boolean> = _boostConfirmed.asSharedFlow()
    private var pendingBoostCommand = false

    // Saved device (reactive) so the UI can hide the scanner once a device is remembered.
    private val _savedDeviceAddress = MutableStateFlow(getSavedAddress())
    val savedDeviceAddress: StateFlow<String?> = _savedDeviceAddress.asStateFlow()
    private val _savedDeviceName = MutableStateFlow(getSavedName())
    val savedDeviceName: StateFlow<String?> = _savedDeviceName.asStateFlow()

    // Scan States
    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val scanResultsMap = mutableMapOf<String, BluetoothDevice>()

    // Safe permission and name helpers to prevent SecurityException crashes on Android Auto
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getSafeName(device: BluetoothDevice): String {
        return try {
            device.name ?: "Unknown Device"
        } catch (e: SecurityException) {
            "Unknown Device"
        }
    }

    // Save Selected MAC Address
    fun getSavedAddress(): String? {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_ADDRESS, null)
    }

    fun getSavedName(): String? {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_NAME, null)
    }

    fun saveDevice(device: BluetoothDevice) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DEVICE_ADDRESS, device.address)
            .putString(KEY_DEVICE_NAME, getSafeName(device))
            .apply()
        _savedDeviceAddress.value = device.address
        _savedDeviceName.value = getSafeName(device)
    }

    fun clearSavedDevice() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _savedDeviceAddress.value = null
        _savedDeviceName.value = null
    }

    /** Stored level for a given mode (from the latest getData), or a sensible default of 5. */
    fun levelForMode(mode: Int): Int {
        if (mode == 4) return 0 // Normal has no level
        val lvl = _modeLevels.value[mode] ?: 0
        return if (lvl in 1..9) lvl else 5
    }

    // Scanning
    fun startScan() {
        if (!hasPermission()) {
            Log.e(TAG, "Cannot start scan: Bluetooth permissions not granted")
            return
        }
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        if (_isScanning.value) return

        scanResultsMap.clear()
        _scannedDevices.value = emptyList()
        _isScanning.value = true

        // Stop scanning after 15 seconds
        handler.postDelayed({
            stopScan()
        }, 15000)

        try {
            scanner.startScan(scanCallback)
            Log.d(TAG, "BLE scan started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissions missing for scanning", e)
            _isScanning.value = false
        }
    }

    fun stopScan() {
        if (!hasPermission()) return
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        if (!_isScanning.value) return

        _isScanning.value = false
        try {
            scanner.stopScan(scanCallback)
            Log.d(TAG, "BLE scan stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissions missing to stop scanning", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val name = result.scanRecord?.deviceName ?: getSafeName(device)
            
            // Filter target device name containing "JDY"
            if (name.contains("JDY", ignoreCase = true)) {
                if (!scanResultsMap.containsKey(device.address)) {
                    scanResultsMap[device.address] = device
                    _scannedDevices.value = scanResultsMap.values.toList()
                    Log.d(TAG, "Found device: $name (${device.address})")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _isScanning.value = false
        }
    }

    // Connecting
    fun connect(address: String) {
        if (!hasPermission()) {
            Log.e(TAG, "Cannot connect: Bluetooth permissions not granted")
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }
        if (bluetoothAdapter == null) return
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return

        disconnect()
        _connectionState.value = ConnectionState.CONNECTING
        Log.d(TAG, "Connecting to ${getSafeName(device)} ($address)")

        try {
            bluetoothGatt = device.connectGatt(appContext, false, gattCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissions missing for connectGatt", e)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissions missing for disconnect", e)
        } finally {
            bluetoothGatt = null
            writeCharacteristic = null
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT Connected. Discovering services...")
                try {
                    gatt?.discoverServices()
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException during discoverServices", e)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT Disconnected")
                writeCharacteristic = null
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                    if (characteristic != null) {
                        writeCharacteristic = characteristic
                        _connectionState.value = ConnectionState.CONNECTED
                        Log.d(TAG, "Found write characteristic, enabling notification...")
                        enableNotification(gatt, characteristic)

                        // Wait 800ms to allow notification to stabilize, then query status
                        handler.postDelayed({
                            queryDeviceData()
                        }, 800)
                    }
                } else {
                    Log.e(TAG, "Target FFE0 service not found on device!")
                }
            } else {
                Log.e(TAG, "Service discovery failed: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val value = characteristic?.value ?: return
            parseNotificationData(value)
        }

        // Support for newer Android API (compileSdk 33+)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            parseNotificationData(value)
        }
    }

    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while enabling notifications", e)
        }
    }

    // Parsing Device Data Responses
    private fun parseNotificationData(value: ByteArray) {
        if (value.size < 5) return

        // Mode & Level Notify Packet checks:
        // bytes2Short2(new byte[]{value[1], value[2]}) == 61936 or -3600 (Hex: F1F0 in little-endian format)
        // Which means value[1] == 0xF0 (240) and value[2] == 0xF1 (241)
        val b1 = value[1].toInt() and 0xFF
        val b2 = value[2].toInt() and 0xFF
        val isModeData = b1 == 0xF0 && b2 == 0xF1
        if (!isModeData) return

        val mode = value[3].toInt()

        // The getData response carries the stored level of EVERY mode, laid out sequentially:
        //   value[4]=Race(1), value[5]=Sport(2), value[6]=City(3), value[7]=Normal(4), value[8]=Eco(5)
        // Capture every mode's level so switching modes can restore that mode's own level.
        if (value.size >= 9) {
            _modeLevels.value = mapOf(
                1 to (value[4].toInt() and 0xFF),
                2 to (value[5].toInt() and 0xFF),
                3 to (value[6].toInt() and 0xFF),
                5 to (value[8].toInt() and 0xFF)
            )
        }

        // value[9] (right after the five per-mode levels) carries the P/R master-toggle state.
        if (value.size >= 10) {
            val on = (value[9].toInt() and 0xFF) == BOOST_VALUE_P
            _boostOn.value = on
            // If this packet is the device's reply to a P/R toggle the user just sent, fire a
            // one-shot confirmation (consumed once so background syncs stay silent).
            if (pendingBoostCommand) {
                pendingBoostCommand = false
                _boostConfirmed.tryEmit(on)
            }
        }

        // The active mode's level is the slot matching the current mode (value[3 + mode]).
        val levelIndex = 3 + mode
        val level = when {
            mode == 4 -> 0 // Normal mode has no sensitivity level
            mode in 1..5 && levelIndex < value.size -> value[levelIndex].toInt() and 0xFF
            else -> 0
        }

        // Only update if it belongs to our supported 5 modes (1 to 5)
        if (mode in 1..5) {
            _activeMode.value = mode
            _activeLevel.value = level
            // If this packet is the device's reply to a command the user just sent, fire a
            // one-shot confirmation event (consumed once so background syncs stay silent).
            if (pendingUserCommand) {
                pendingUserCommand = false
                _commandConfirmed.tryEmit(mode to level)
            }
        }
    }

    // Send command helper
    private fun writeData(bytes: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val characteristic = writeCharacteristic ?: return false

        characteristic.value = bytes
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        return try {
            val success = gatt.writeCharacteristic(characteristic)
            Log.d(TAG, "Write packet bytes: ${bytes.joinToString(",") { it.toString() }} - Success: $success")
            success
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during writeCharacteristic", e)
            false
        }
    }

    // Query Device State [0xC0, 0xF0, 0xF8, 0x00, 0xC0]
    fun queryDeviceData() {
        val cmd = byteArrayOf(-64, -16, -8, 0, -64)
        writeData(cmd)
    }

    // Set Driver Mode [0xC0, 0xF0, 0xF1, mode, level, 0x00, 0xC0]
    fun setMode(mode: Int, level: Int) {
        val l = if (mode == 4) 0 else level
        val cmd = byteArrayOf(
            -64,             // 0xC0
            -16,             // 0xF0
            -15,             // 0xF1
            mode.toByte(),
            l.toByte(),
            0x00,
            -64              // 0xC0
        )
        if (writeData(cmd)) {
            // Optimistically update values locally
            _activeMode.value = mode
            _activeLevel.value = l
            // Mark that we're expecting a device response to this user action, so the next data
            // packet that arrives fires a confirmation toast.
            pendingUserCommand = true
            // Ask the device for a fresh full-status packet so the raw log reflects the new state
            handler.postDelayed({ queryDeviceData() }, 300)
        }
    }

    // Toggle the P/R master booster. Packet [0xC0, 0xF0, 0xFA, value, 0xC0]; value[9] echoes it back.
    fun setBoost(toP: Boolean) {
        val value = if (toP) BOOST_VALUE_P else BOOST_VALUE_R
        val cmd = byteArrayOf(
            -64,             // 0xC0
            -16,             // 0xF0
            -6,              // 0xFA
            value.toByte(),
            -64              // 0xC0
        )
        if (writeData(cmd)) {
            // Optimistically update locally and expect a device reply to fire the confirmation.
            _boostOn.value = toP
            pendingBoostCommand = true
            handler.postDelayed({ queryDeviceData() }, 300)
        }
    }
}

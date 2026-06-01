package io.github.manhvu1212.aapowerbooster

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine

class PowerBoosterScreen(carContext: CarContext) : Screen(carContext) {

    private val bleManager = BleManager.getInstance(carContext)
    private var scope: CoroutineScope? = null

    // Cache state variables for drawing UI
    private var connectionState = BleManager.ConnectionState.DISCONNECTED
    private var activeMode = 4
    private var activeLevel = 0

    init {
        // Register lifecycle observer to bind state Flow updates to UI invalidation
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                scope?.launch {
                    // Auto-connect on start if there is a saved device address
                    val savedAddress = bleManager.getSavedAddress()
                    if (savedAddress != null && bleManager.connectionState.value == BleManager.ConnectionState.DISCONNECTED) {
                        bleManager.connect(savedAddress)
                    }

                    // Listen to state changes and redraw the UI screen
                    combine(
                        bleManager.connectionState,
                        bleManager.activeMode,
                        bleManager.activeLevel
                    ) { conn, mode, lvl ->
                        Triple(conn, mode, lvl)
                    }.collect { (conn, mode, lvl) ->
                        connectionState = conn
                        activeMode = mode
                        activeLevel = lvl
                        invalidate() // Re-runs onGetTemplate()
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                scope?.cancel()
                scope = null
            }
        })
    }

    override fun onGetTemplate(): Template {
        val gridItemList = mutableListOf<GridItem>()

        // 1. Race Mode (ID: 1)
        gridItemList.add(
            createModeGridItem(
                modeId = 1,
                name = "Race Mode",
                description = "Chế độ đua xe - Cấp 1..9",
                isActive = activeMode == 1
            )
        )

        // 2. Sport Mode (ID: 2)
        gridItemList.add(
            createModeGridItem(
                modeId = 2,
                name = "Sport Mode",
                description = "Chế độ thể thao - Cấp 1..9",
                isActive = activeMode == 2
            )
        )

        // 3. Normal Mode (ID: 4)
        gridItemList.add(
            createModeGridItem(
                modeId = 4,
                name = "Normal Mode",
                description = "Chế độ zin của xe",
                isActive = activeMode == 4
            )
        )

        // 4. City/Comfort Mode (ID: 3)
        gridItemList.add(
            createModeGridItem(
                modeId = 3,
                name = "City Mode",
                description = "Chế độ đi phố mượt - Cấp 1..9",
                isActive = activeMode == 3
            )
        )

        // 5. Eco Mode (ID: 5)
        gridItemList.add(
            createModeGridItem(
                modeId = 5,
                name = "Eco Mode",
                description = "Tiết kiệm nhiên liệu - Cấp 1..9",
                isActive = activeMode == 5
            )
        )

        // 6. Decrease Level (-)
        gridItemList.add(
            GridItem.Builder()
                .setTitle("Giảm Cấp (-)")
                .setText(if (activeMode == 4) "Chế độ thường" else "Hiện tại: Cấp $activeLevel")
                .setImage(CarIcon.APP_ICON)
                .setOnClickListener {
                    if (connectionState == BleManager.ConnectionState.CONNECTED && activeMode != 4) {
                        val newLevel = (activeLevel - 1).coerceAtLeast(1)
                        bleManager.setMode(activeMode, newLevel)
                    }
                }
                .build()
        )

        // 7. Increase Level (+)
        gridItemList.add(
            GridItem.Builder()
                .setTitle("Tăng Cấp (+)")
                .setText(if (activeMode == 4) "Chế độ thường" else "Hiện tại: Cấp $activeLevel")
                .setImage(CarIcon.APP_ICON)
                .setOnClickListener {
                    if (connectionState == BleManager.ConnectionState.CONNECTED && activeMode != 4) {
                        val newLevel = (activeLevel + 1).coerceAtMost(9)
                        bleManager.setMode(activeMode, newLevel)
                    }
                }
                .build()
        )

        // Connectivity header status text
        val headerTitle = when (connectionState) {
            BleManager.ConnectionState.CONNECTED -> "Đã kết nối chân ga"
            BleManager.ConnectionState.CONNECTING -> "Đang kết nối chân ga..."
            BleManager.ConnectionState.DISCONNECTED -> "Chưa kết nối chân ga / Thiết bị bận"
        }

        // Action strip to allow manual refresh or reconnection
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Làm mới")
                    .setOnClickListener {
                        if (connectionState == BleManager.ConnectionState.CONNECTED) {
                            bleManager.queryDeviceData()
                        } else {
                            val savedAddress = bleManager.getSavedAddress()
                            if (savedAddress != null) {
                                bleManager.connect(savedAddress)
                            }
                        }
                    }
                    .build()
            )
            .build()

        val listBuilder = ItemList.Builder()
        for (item in gridItemList) {
            listBuilder.addItem(item)
        }

        return GridTemplate.Builder()
            .setTitle(headerTitle)
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun createModeGridItem(modeId: Int, name: String, description: String, isActive: Boolean): GridItem {
        val title = if (isActive) "★ $name ★" else name
        val text = if (isActive) {
            if (modeId == 4) "ĐANG CHỌN" else "Đang chọn - Cấp $activeLevel"
        } else {
            description
        }

        return GridItem.Builder()
            .setTitle(title)
            .setText(text)
            .setImage(CarIcon.APP_ICON)
            .setOnClickListener {
                if (connectionState == BleManager.ConnectionState.CONNECTED) {
                    // Set to default level 5 if previous level was 0 (Normal mode)
                    val lvl = if (modeId == 4) 0 else (if (activeLevel == 0) 5 else activeLevel)
                    bleManager.setMode(modeId, lvl)
                }
            }
            .build()
    }
}

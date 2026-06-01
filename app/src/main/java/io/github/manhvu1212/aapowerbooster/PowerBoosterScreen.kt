package io.github.manhvu1212.aapowerbooster

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
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

    // Only record breadcrumbs for the first template build to avoid spamming on every invalidate()
    private var breadcrumbed = false

    init {
        PowerBoosterApp.saveStatus(carContext, "Screen created")
        // Register lifecycle observer to bind state Flow updates to UI invalidation
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                PowerBoosterApp.saveStatus(carContext, "onStart")
                scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                scope?.launch {
                    try {
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
                    } catch (t: Throwable) {
                        PowerBoosterApp.saveCrash(carContext, "Session coroutine crash", t)
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
        return try {
            buildTemplate()
        } catch (t: Throwable) {
            // Surface the error on the car screen (photographable) and save it for the phone app.
            PowerBoosterApp.saveCrash(carContext, "onGetTemplate crash", t)
            MessageTemplate.Builder("Lỗi: ${t.message ?: t.toString()}")
                .setTitle("AA Power Booster - Lỗi")
                .setHeaderAction(Action.APP_ICON)
                .build()
        }
    }

    private fun buildTemplate(): Template {
        if (!breadcrumbed) PowerBoosterApp.saveStatus(carContext, "buildTemplate: enter")
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

        // Connectivity header status text
        val headerTitle = when (connectionState) {
            BleManager.ConnectionState.CONNECTED -> "Đã kết nối chân ga"
            BleManager.ConnectionState.CONNECTING -> "Đang kết nối chân ga..."
            BleManager.ConnectionState.DISCONNECTED -> "Chưa kết nối chân ga / Thiết bị bận"
        }

        // Level +/- live in the ActionStrip (not the grid) so the grid stays at 5 mode tiles —
        // safely under every head unit's grid item limit (guaranteed minimum 6). The active mode's
        // current level is shown on its grid tile. Kept to 2 actions to satisfy ActionStrip limits.
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Cấp -")
                    .setOnClickListener {
                        if (connectionState == BleManager.ConnectionState.CONNECTED && activeMode != 4) {
                            val newLevel = (activeLevel - 1).coerceAtLeast(1)
                            bleManager.setMode(activeMode, newLevel)
                        }
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Cấp +")
                    .setOnClickListener {
                        if (connectionState == BleManager.ConnectionState.CONNECTED && activeMode != 4) {
                            val newLevel = (activeLevel + 1).coerceAtMost(9)
                            bleManager.setMode(activeMode, newLevel)
                        }
                    }
                    .build()
            )
            .build()

        // Guard: many head units cap the number of grid items (often 6). If we exceed the host's
        // limit, the host rejects the template and the app crashes with a generic error, so detect
        // it here and show a clear message (and record the real limit for diagnosis) instead.
        val gridLimit = try {
            carContext.getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID)
        } catch (t: Throwable) {
            -1 // could not read the host limit
        }
        if (!breadcrumbed) {
            PowerBoosterApp.saveStatus(carContext, "items=${gridItemList.size} gridLimit=$gridLimit")
        }
        // Only trim when we actually know the host's limit and it is smaller than our item count.
        if (gridLimit in 1 until gridItemList.size) {
            PowerBoosterApp.saveStatus(carContext, "GRID LIMIT EXCEEDED -> showing message")
            return MessageTemplate.Builder(
                "Màn hình xe chỉ cho phép $gridLimit ô nhưng ứng dụng cần ${gridItemList.size} ô. " +
                    "Cần giảm bớt số nút trên giao diện Android Auto."
            )
                .setTitle("AA Power Booster")
                .setHeaderAction(Action.APP_ICON)
                .build()
        }

        val listBuilder = ItemList.Builder()
        for (item in gridItemList) {
            listBuilder.addItem(item)
        }

        val template = GridTemplate.Builder()
            .setTitle(headerTitle)
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)
            .setSingleList(listBuilder.build())
            .build()

        if (!breadcrumbed) {
            PowerBoosterApp.saveStatus(carContext, "buildTemplate: grid built OK (returning)")
            breadcrumbed = true
        }
        return template
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

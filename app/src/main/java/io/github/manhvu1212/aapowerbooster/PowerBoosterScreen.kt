package io.github.manhvu1212.aapowerbooster

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
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

        // Display order: Race - Sport - City - Normal - Eco (same as the phone app).
        // Icon-only tiles (mode icon matching the phone) for a clean, modern look.
        val modes = listOf(
            Triple(1, "Race", R.drawable.ic_mode_race),
            Triple(2, "Sport", R.drawable.ic_mode_sport),
            Triple(3, "City", R.drawable.ic_mode_city),
            Triple(4, "Normal", R.drawable.ic_mode_normal),
            Triple(5, "Eco", R.drawable.ic_mode_eco)
        )
        val gridItemList = modes.map { (id, name, iconRes) ->
            // The active mode shows a ringed variant of its icon so it stands out on the grid.
            val res = if (activeMode == id) activeIconRes(id) else iconRes
            createModeGridItem(modeId = id, name = name, iconRes = res)
        }.toMutableList()

        // Header shows the current mode + level when connected. (Android Auto's action strip is
        // capped at 2 buttons, so the level number can't be a 3rd item literally between -/+;
        // showing it in the title keeps it on the same top bar as the -/+ buttons.)
        val modeName = when (activeMode) {
            1 -> "Race"
            2 -> "Sport"
            3 -> "City"
            4 -> "Normal"
            5 -> "Eco"
            else -> "?"
        }
        val headerTitle = when (connectionState) {
            BleManager.ConnectionState.CONNECTED ->
                if (activeMode == 4) "$modeName (zin)" else "$modeName · Cấp $activeLevel"
            BleManager.ConnectionState.CONNECTING -> "Đang kết nối chân ga..."
            BleManager.ConnectionState.DISCONNECTED -> "Chưa kết nối chân ga / Thiết bị bận"
        }

        // Level +/- live in the ActionStrip (not the grid) so the grid stays at 5 mode tiles —
        // safely under every head unit's grid item limit (guaranteed minimum 6). The active mode's
        // current level is shown on its grid tile.
        // NOTE: an ActionStrip allows at most ONE action with a custom title, so use ICONS here
        // (icon-only actions do not count as custom titles).
        val minusIcon = CarIcon.Builder(
            IconCompat.createWithResource(carContext, R.drawable.ic_minus)
        ).build()
        val plusIcon = CarIcon.Builder(
            IconCompat.createWithResource(carContext, R.drawable.ic_plus)
        ).build()
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(minusIcon)
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
                    .setIcon(plusIcon)
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

    // Ringed icon variant used to mark the currently active mode on the grid.
    private fun activeIconRes(modeId: Int): Int = when (modeId) {
        1 -> R.drawable.ic_mode_race_active
        2 -> R.drawable.ic_mode_sport_active
        3 -> R.drawable.ic_mode_city_active
        4 -> R.drawable.ic_mode_normal_active
        5 -> R.drawable.ic_mode_eco_active
        else -> R.drawable.ic_mode_normal_active
    }

    private fun createModeGridItem(modeId: Int, name: String, iconRes: Int): GridItem {
        // Mode icon (matches the phone). IMAGE_TYPE_ICON lets the host tint it to its theme color.
        val icon = CarIcon.Builder(
            IconCompat.createWithResource(carContext, iconRes)
        ).build()

        // The Car App Library requires every grid item to have a title, so we can't make tiles
        // truly icon-only — keep just the short mode name. No level text below the icon
        // (the current level is shown in the header title instead).
        return GridItem.Builder()
            .setTitle(name)
            .setImage(icon, GridItem.IMAGE_TYPE_ICON)
            .setOnClickListener {
                if (connectionState == BleManager.ConnectionState.CONNECTED) {
                    // Each mode keeps its own level; restore that mode's stored level.
                    val lvl = if (modeId == 4) 0 else bleManager.levelForMode(modeId)
                    bleManager.setMode(modeId, lvl)
                }
            }
            .build()
    }
}

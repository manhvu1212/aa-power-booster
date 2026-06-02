package io.github.manhvu1212.aapowerbooster

import android.os.SystemClock
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine

class PowerBoosterScreen(carContext: CarContext) : Screen(carContext) {

    companion object {
        // Max one template refresh per this interval (ms) — see the collector for why.
        private const val REFRESH_THROTTLE_MS = 300L
    }

    private val bleManager = BleManager.getInstance(carContext)
    private var scope: CoroutineScope? = null

    // Cache state variables for drawing UI
    private var connectionState = BleManager.ConnectionState.DISCONNECTED
    private var activeMode = 4
    private var activeLevel = 0
    private var boostOn = true // P/R master toggle (true = P)

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

                        // Listen to state changes and redraw the UI screen.
                        // Android Auto caps how many template pushes an app may make while driving;
                        // only same-type/same-item-count refreshes are "free". A burst of BLE state
                        // changes (reconnect churn, command + follow-up query) can flood invalidate()
                        // and trip the host's "Can't complete this action while driving" guard. So we
                        // coalesce changes into at most one refresh per REFRESH_THROTTLE_MS, always
                        // keeping a trailing redraw so the final state is reflected. A single tap is
                        // still immediate (it only coalesces when changes arrive faster than that).
                        var lastRefresh = 0L
                        var trailing: Job? = null
                        combine(
                            bleManager.connectionState,
                            bleManager.activeMode,
                            bleManager.activeLevel,
                            bleManager.boostOn
                        ) { conn, mode, lvl, boost ->
                            arrayOf(conn, mode, lvl, boost)
                        }.collect { s ->
                            connectionState = s[0] as BleManager.ConnectionState
                            activeMode = s[1] as Int
                            activeLevel = s[2] as Int
                            boostOn = s[3] as Boolean

                            val now = SystemClock.uptimeMillis()
                            val elapsed = now - lastRefresh
                            if (elapsed >= REFRESH_THROTTLE_MS) {
                                trailing?.cancel(); trailing = null
                                lastRefresh = now
                                invalidate() // Re-runs onGetTemplate()
                            } else if (trailing == null) {
                                // Schedule one trailing redraw; cached vars above already hold the
                                // latest state, so whenever it fires it draws the newest values.
                                trailing = scope?.launch {
                                    delay(REFRESH_THROTTLE_MS - elapsed)
                                    lastRefresh = SystemClock.uptimeMillis()
                                    trailing = null
                                    invalidate()
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        PowerBoosterApp.saveCrash(carContext, "Session coroutine crash", t)
                    }
                }

                // Show a CarToast when the device confirms a command the user just sent (shares the
                // same one-shot event as the phone app, so it never fires on background syncs).
                scope?.launch {
                    bleManager.commandConfirmed.collect { (mode, level) ->
                        try {
                            val name = when (mode) {
                                1 -> "Race"
                                2 -> "Sport"
                                3 -> "City"
                                4 -> "Normal"
                                5 -> "Eco"
                                else -> "?"
                            }
                            val msg = if (mode == 4) "✓ $name" else "✓ $name · Level $level"
                            CarToast.makeText(carContext, msg, CarToast.LENGTH_SHORT).show()
                        } catch (t: Throwable) {
                            PowerBoosterApp.saveCrash(carContext, "CarToast failed", t)
                        }
                    }
                }

                // Confirmation toast for the P/R master toggle (shares the same one-shot pattern).
                scope?.launch {
                    bleManager.boostConfirmed.collect { on ->
                        try {
                            CarToast.makeText(
                                carContext,
                                if (on) "✓ Mode P" else "✓ Mode R",
                                CarToast.LENGTH_SHORT
                            ).show()
                        } catch (t: Throwable) {
                            PowerBoosterApp.saveCrash(carContext, "CarToast failed", t)
                        }
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
            PowerBoosterApp.saveCrash(carContext, "onGetTemplate crash", t)
            // Stay on a GridTemplate (same type, same 6 items) even on a transient failure, so the
            // redraw is still a "refresh" and never counts toward Android Auto's 5-template-per-task
            // quota. Switching template type (Grid -> Message) burns that quota and, under rapid use,
            // trips the host's "Can't complete this action while driving" guard. Only drop to a
            // message if even the minimal grid fails to build.
            try {
                fallbackGrid()
            } catch (t2: Throwable) {
                PowerBoosterApp.saveCrash(carContext, "fallbackGrid crash", t2)
                MessageTemplate.Builder("Error: ${t.message ?: t.toString()}")
                    .setTitle("AA Power Booster - Error")
                    .setHeaderAction(Action.APP_ICON)
                    .build()
            }
        }
    }

    // Minimal, always-6-item GridTemplate used only if buildTemplate() throws. Keeps the redraw a
    // same-type refresh (no Grid->Message switch -> no quota burn). Deliberately avoids the
    // ConstraintManager read and ActionStrip so it is as unlikely to throw as possible.
    private fun fallbackGrid(): Template {
        val modes = listOf(
            Triple(1, "Race", R.drawable.ic_mode_race),
            Triple(2, "Sport", R.drawable.ic_mode_sport),
            Triple(3, "City", R.drawable.ic_mode_city),
            Triple(4, "Normal", R.drawable.ic_mode_normal),
            Triple(5, "Eco", R.drawable.ic_mode_eco)
        )
        val listBuilder = ItemList.Builder()
        modes.forEach { (id, name, iconRes) ->
            val res = if (activeMode == id) activeIconRes(id) else iconRes
            listBuilder.addItem(createModeGridItem(modeId = id, name = name, iconRes = res))
        }
        listBuilder.addItem(createBoostGridItem())
        return GridTemplate.Builder()
            .setTitle("AA Power Booster")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .build()
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

        // 6th tile: P/R master toggle. Icon shows the active letter filled, the other outlined.
        gridItemList.add(createBoostGridItem())

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
                if (activeMode == 4) modeName else "$modeName · Level $activeLevel"
            BleManager.ConnectionState.CONNECTING -> "Connecting to throttle..."
            BleManager.ConnectionState.DISCONNECTED -> "Throttle not connected / device busy"
        }

        // Level +/- live in the ActionStrip (not the grid) so the grid stays at 6 tiles (5 modes +
        // the P/R toggle) — at the guaranteed-minimum grid limit of 6. The active mode's current
        // level is shown in the header title.
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

        // Guard: many head units cap the number of grid items. Google guarantees a minimum of 6 and
        // we use exactly 6, so this is extremely unlikely to trigger — but if a host caps below our
        // count, TRIM to fit rather than switching to a MessageTemplate. Staying a GridTemplate keeps
        // every redraw a quota-free refresh (a Grid->Message switch would burn the template quota and
        // trip the host's "can't complete while driving" guard under rapid use). gridLimit is a
        // static host capability, so trimming is consistent across redraws (still a refresh).
        val gridLimit = try {
            carContext.getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID)
        } catch (t: Throwable) {
            -1 // could not read the host limit
        }
        if (!breadcrumbed) {
            PowerBoosterApp.saveStatus(carContext, "items=${gridItemList.size} gridLimit=$gridLimit")
        }
        val itemsToShow = if (gridLimit in 1 until gridItemList.size) {
            PowerBoosterApp.saveStatus(carContext, "GRID LIMIT $gridLimit < ${gridItemList.size} -> trimming")
            gridItemList.take(gridLimit)
        } else {
            gridItemList
        }

        val listBuilder = ItemList.Builder()
        for (item in itemsToShow) {
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

    // P/R master-toggle tile. Tapping flips between P and R; the icon reflects the current state.
    private fun createBoostGridItem(): GridItem {
        val iconRes = if (boostOn) R.drawable.ic_boost_pr_p else R.drawable.ic_boost_pr_r
        val icon = CarIcon.Builder(
            IconCompat.createWithResource(carContext, iconRes)
        ).build()
        return GridItem.Builder()
            .setTitle(if (boostOn) "Mode P" else "Mode R")
            .setImage(icon, GridItem.IMAGE_TYPE_ICON)
            .setOnClickListener {
                if (connectionState == BleManager.ConnectionState.CONNECTED) {
                    bleManager.setBoost(!boostOn)
                }
            }
            .build()
    }
}

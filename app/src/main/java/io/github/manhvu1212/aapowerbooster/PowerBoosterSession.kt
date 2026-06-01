package io.github.manhvu1212.aapowerbooster

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class PowerBoosterSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        PowerBoosterApp.saveStatus(carContext, "Session.onCreateScreen")
        return PowerBoosterScreen(carContext)
    }
}

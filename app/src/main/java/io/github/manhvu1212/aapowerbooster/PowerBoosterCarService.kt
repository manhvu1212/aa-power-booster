package io.github.manhvu1212.aapowerbooster

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class PowerBoosterCarService : CarAppService() {
    override fun onCreateSession(): Session {
        PowerBoosterApp.saveStatus(this, "CarService.onCreateSession")
        return PowerBoosterSession()
    }

    override fun createHostValidator(): HostValidator {
        // First car-flow callback when the host connects -> mark a fresh Android Auto launch.
        PowerBoosterApp.saveStatus(this, "=== AA launch === createHostValidator")
        // Required for debug and developer sideloaded apps on Android Auto
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
}

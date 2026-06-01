package io.github.manhvu1212.aapowerbooster

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class PowerBoosterCarService : CarAppService() {
    override fun onCreateSession(): Session {
        return PowerBoosterSession()
    }

    override fun createHostValidator(): HostValidator {
        // Required for debug and developer sideloaded apps on Android Auto
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
}

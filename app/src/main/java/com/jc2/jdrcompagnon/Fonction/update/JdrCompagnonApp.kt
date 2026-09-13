package com.jc2.jdrcompagnon

import android.app.Application
import com.jc2.jdrcompagnon.update.UpdateManager

class JdrCompagnonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UpdateManager.schedulePeriodicChecks(this)
    }
}
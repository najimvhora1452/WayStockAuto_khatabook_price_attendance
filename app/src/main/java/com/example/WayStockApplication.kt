package com.example

import android.app.Application
import android.util.Log

class WayStockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("WayStockApp", "WayStockApplication created")
    }
}

package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class WayStockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyBd83Jk5n7M2mkumtFT-t_zktD8Wz0cZnM")
                    .setApplicationId("1:520506980567:web:e5d7661a3866d18d979892")
                    .setProjectId("stockmaster-94534")
                    .setGcmSenderId("520506980567")
                    .setStorageBucket("stockmaster-94534.firebasestorage.app")
                    .build()

                FirebaseApp.initializeApp(this, options)
                Log.i(TAG, "Firebase initialized with project: stockmaster-94534")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init notice: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "WayStockApp"
    }
}


package com.example.happyplaces

import android.app.Application

class HappyPlaceApp:Application() {
    val db by lazy {
        HappyPlaceDatabase.getInstance(this)
    }
}
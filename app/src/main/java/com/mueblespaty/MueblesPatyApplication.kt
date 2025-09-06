package com.mueblespaty

import android.app.Application

class MueblesPatyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Checkout Sheet Kit
        CheckoutSheetKitConfig.configure(this)
    }
}
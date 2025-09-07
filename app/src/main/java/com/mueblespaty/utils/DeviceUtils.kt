package com.mueblespaty.utils

import android.os.Build

object DeviceUtils {
    
    /**
     * Detects if the device is a Samsung Galaxy S24 series running One UI 7.0+
     * This helps apply specific fixes for system bar overlap issues
     */
    fun isSamsungS24WithOneUI7(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        val androidVersion = Build.VERSION.SDK_INT
        
        // Check if it's Samsung
        val isSamsung = manufacturer.contains("samsung")
        
        // Check if it's S24 series (S24, S24+, S24 Ultra)
        val isS24Series = model.contains("sm-s921") ||  // S24
                         model.contains("sm-s926") ||  // S24+
                         model.contains("sm-s928") ||  // S24 Ultra
                         model.contains("galaxy s24")
        
        // Check if Android version is 15+ (where One UI 7.0 runs)
        val isAndroid15Plus = androidVersion >= Build.VERSION_CODES.VANILLA_ICE_CREAM
        
        return isSamsung && isS24Series && isAndroid15Plus
    }
    
    /**
     * Detects if the device is Samsung with potential edge-to-edge issues
     * More general detection for Samsung devices that might need fixes
     */
    fun isSamsungWithEdgeToEdgeIssues(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val androidVersion = Build.VERSION.SDK_INT
        
        // Samsung devices on Android 14+ with One UI may have edge-to-edge issues
        return manufacturer.contains("samsung") && androidVersion >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
    
    /**
     * Gets a descriptive string for debugging
     */
    fun getDeviceInfo(): String {
        return "Manufacturer: ${Build.MANUFACTURER}, Model: ${Build.MODEL}, Android: ${Build.VERSION.SDK_INT}"
    }
}
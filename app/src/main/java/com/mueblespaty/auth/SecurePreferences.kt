package com.mueblespaty.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecurePreferences(context: Context) {
    
    private val sharedPreferences: SharedPreferences
    
    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        sharedPreferences = EncryptedSharedPreferences.create(
            AuthConfig.PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun storeTokens(accessToken: String, refreshToken: String?, idToken: String?) {
        with(sharedPreferences.edit()) {
            putString(AuthConfig.KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(AuthConfig.KEY_REFRESH_TOKEN, it) }
            idToken?.let { putString("id_token", it) }
            apply()
        }
    }
    
    fun getAccessToken(): String? {
        return sharedPreferences.getString(AuthConfig.KEY_ACCESS_TOKEN, null)
    }
    
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(AuthConfig.KEY_REFRESH_TOKEN, null)
    }
    
    fun getIdToken(): String? {
        return sharedPreferences.getString("id_token", null)
    }
    
    fun storeCodeVerifier(codeVerifier: String) {
        sharedPreferences.edit()
            .putString("code_verifier", codeVerifier)
            .apply()
    }
    
    fun getCodeVerifier(): String? {
        val verifier = sharedPreferences.getString("code_verifier", null)
        // Clear after use for security
        sharedPreferences.edit().remove("code_verifier").apply()
        return verifier
    }
    
    fun storeCustomerId(customerId: String) {
        sharedPreferences.edit()
            .putString(AuthConfig.KEY_CUSTOMER_ID, customerId)
            .apply()
    }
    
    fun getCustomerId(): String? {
        return sharedPreferences.getString(AuthConfig.KEY_CUSTOMER_ID, null)
    }
    
    fun clearTokens() {
        with(sharedPreferences.edit()) {
            remove(AuthConfig.KEY_ACCESS_TOKEN)
            remove(AuthConfig.KEY_REFRESH_TOKEN)
            remove("id_token")
            remove(AuthConfig.KEY_CUSTOMER_ID)
            remove("code_verifier")
            apply()
        }
    }
}
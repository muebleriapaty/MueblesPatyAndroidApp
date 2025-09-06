package com.mueblespaty.auth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import kotlin.coroutines.resume

class TokenManager(private val context: Context) {
    
    private val authService: AuthorizationService = AuthorizationService(context)
    private val securePrefs = SecurePreferences(context)
    
    data class TokenValidationResult(
        val isValid: Boolean,
        val accessToken: String? = null,
        val error: String? = null
    )
    
    suspend fun getValidAccessToken(): TokenValidationResult = withContext(Dispatchers.IO) {
        val currentToken = securePrefs.getAccessToken()
        
        if (currentToken == null) {
            return@withContext TokenValidationResult(
                isValid = false,
                error = "No access token found"
            )
        }
        
        // For now, assume token is valid
        // In production, you might want to validate the token expiration
        // by decoding the JWT token or making a test API call
        return@withContext TokenValidationResult(
            isValid = true,
            accessToken = currentToken
        )
    }
    
    suspend fun refreshTokenIfNeeded(): TokenValidationResult = withContext(Dispatchers.IO) {
        val refreshToken = securePrefs.getRefreshToken()
        
        if (refreshToken == null) {
            return@withContext TokenValidationResult(
                isValid = false,
                error = "No refresh token available"
            )
        }
        
        try {
            val serviceConfig = AuthorizationServiceConfiguration(
                android.net.Uri.parse(AuthConfig.AUTH_ENDPOINT),
                android.net.Uri.parse(AuthConfig.TOKEN_ENDPOINT)
            )
            
            val tokenRequest = TokenRequest.Builder(serviceConfig, AuthConfig.CLIENT_ID)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(refreshToken)
                .build()
            
            val result = performTokenRequestAsync(tokenRequest)
            
            if (result.first?.accessToken != null) {
                val response = result.first!!
                securePrefs.storeTokens(
                    accessToken = response.accessToken!!,
                    refreshToken = response.refreshToken ?: refreshToken,
                    idToken = response.idToken
                )
                
                return@withContext TokenValidationResult(
                    isValid = true,
                    accessToken = response.accessToken
                )
            } else if (result.second != null) {
                return@withContext TokenValidationResult(
                    isValid = false,
                    error = "Token refresh failed: ${result.second?.message}"
                )
            }
            
            return@withContext TokenValidationResult(
                isValid = false,
                error = "Unknown error during token refresh"
            )
            
        } catch (e: Exception) {
            Log.e("TokenManager", "Error refreshing token", e)
            return@withContext TokenValidationResult(
                isValid = false,
                error = "Token refresh error: ${e.message}"
            )
        }
    }
    
    private suspend fun performTokenRequestAsync(tokenRequest: TokenRequest): Pair<TokenResponse?, AuthorizationException?> = 
        suspendCancellableCoroutine { continuation ->
            authService.performTokenRequest(tokenRequest) { response, exception ->
                continuation.resume(Pair(response, exception))
            }
        }
    
    fun dispose() {
        authService.dispose()
    }
}
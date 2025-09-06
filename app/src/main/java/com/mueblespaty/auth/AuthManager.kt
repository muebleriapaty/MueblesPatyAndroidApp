package com.mueblespaty.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import org.json.JSONException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import kotlin.coroutines.resume
import com.mueblespaty.WebViewAuthActivity

class AuthManager(private val context: Context) {
    
    private val authService: AuthorizationService = AuthorizationService(context)
    private val securePrefs = SecurePreferences(context)
    private val tokenManager = TokenManager(context)
    
    data class AuthResult(
        val isSuccess: Boolean,
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val error: String? = null
    )
    
    fun isUserLoggedIn(): Boolean {
        return securePrefs.getAccessToken() != null
    }
    
    fun getCurrentAccessToken(): String? {
        return securePrefs.getAccessToken()
    }
    
    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        val validationResult = tokenManager.getValidAccessToken()
        
        if (validationResult.isValid) {
            return@withContext validationResult.accessToken
        }
        
        // Try to refresh token
        val refreshResult = tokenManager.refreshTokenIfNeeded()
        if (refreshResult.isValid) {
            return@withContext refreshResult.accessToken
        }
        
        return@withContext null
    }
    
    fun startAuthFlow(): Intent {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        
        // Store code verifier for token exchange
        securePrefs.storeCodeVerifier(codeVerifier)
        
        // Create proper AppAuth authorization request
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(AuthConfig.AUTH_ENDPOINT),
            Uri.parse(AuthConfig.TOKEN_ENDPOINT)
        )
        
        val authRequestBuilder = AuthorizationRequest.Builder(
            serviceConfig,
            AuthConfig.CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(AuthConfig.REDIRECT_URI)
        )
            .setScope(AuthConfig.SCOPE)
            .setCodeVerifier(codeVerifier)
            .setAdditionalParameters(mapOf("audience" to "shopify://customer-account-api"))
        
        val authRequest = authRequestBuilder.build()
        
        Log.d("AuthManager", "Starting AppAuth authorization flow")
        Log.d("AuthManager", "Redirect URI: ${AuthConfig.REDIRECT_URI}")
        
        // Return AppAuth intent which will handle the redirect properly
        return authService.getAuthorizationRequestIntent(authRequest)
    }
    
    suspend fun handleAuthorizationCode(authorizationCode: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val codeVerifier = securePrefs.getCodeVerifier()
            if (codeVerifier == null) {
                return@withContext AuthResult(
                    isSuccess = false,
                    error = "Code verifier not found"
                )
            }
            
            // Make manual token request
            val result = exchangeCodeForToken(authorizationCode, codeVerifier)
            
            if (result.isSuccess) {
                Log.d("AuthManager", "Manual token exchange successful")
                
                return@withContext AuthResult(
                    isSuccess = true,
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken
                )
            }
            
            return@withContext AuthResult(
                isSuccess = false,
                error = result.error
            )
            
        } catch (e: Exception) {
            return@withContext AuthResult(
                isSuccess = false,
                error = "Auth code processing error: ${e.message}"
            )
        }
    }

    suspend fun handleAuthResponse(
        authResponse: AuthorizationResponse?,
        authException: AuthorizationException?
    ): AuthResult = withContext(Dispatchers.IO) {
        
        if (authException != null) {
            return@withContext AuthResult(
                isSuccess = false,
                error = "Authorization failed: ${authException.message}"
            )
        }
        
        if (authResponse == null) {
            return@withContext AuthResult(
                isSuccess = false,
                error = "No authorization response received"
            )
        }
        
        try {
            // AppAuth handles PKCE automatically, no need to manually add code_verifier
            val tokenRequest = authResponse.createTokenExchangeRequest()
            
            val result = performTokenRequestAsync(tokenRequest)
            
            if (result.second != null) {
                return@withContext AuthResult(
                    isSuccess = false,
                    error = "Token exchange failed: ${result.second?.message}"
                )
            }
            
            if (result.first?.accessToken != null) {
                val response = result.first!!
                // Store tokens securely
                securePrefs.storeTokens(
                    accessToken = response.accessToken!!,
                    refreshToken = response.refreshToken,
                    idToken = response.idToken
                )
                
                Log.d("AuthManager", "Token exchange successful")
                
                return@withContext AuthResult(
                    isSuccess = true,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken
                )
            }
            
            return@withContext AuthResult(
                isSuccess = false,
                error = "Invalid token response"
            )
            
        } catch (e: Exception) {
            Log.e("Token error", "${e.message}")
            return@withContext AuthResult(
                isSuccess = false,
                error = "Token exchange error: ${e.message}"
            )
        }
    }
    
    suspend fun refreshAccessToken(): AuthResult = withContext(Dispatchers.IO) {
        val result = tokenManager.refreshTokenIfNeeded()
        
        return@withContext AuthResult(
            isSuccess = result.isValid,
            accessToken = result.accessToken,
            error = result.error
        )
    }
    
    fun logout() {
        securePrefs.clearTokens()
    }
    
    private suspend fun performTokenRequestAsync(tokenRequest: TokenRequest): Pair<TokenResponse?, AuthorizationException?> = 
        suspendCancellableCoroutine { continuation ->
            authService.performTokenRequest(tokenRequest) { response, exception ->
                continuation.resume(Pair(response, exception))
            }
        }
    
    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val codeVerifier = ByteArray(32)
        secureRandom.nextBytes(codeVerifier)
        return Base64.encodeToString(codeVerifier, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    private suspend fun exchangeCodeForToken(authorizationCode: String, codeVerifier: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val serviceConfig = AuthorizationServiceConfiguration(
                Uri.parse(AuthConfig.AUTH_ENDPOINT),
                Uri.parse(AuthConfig.TOKEN_ENDPOINT)
            )
            
            val tokenRequest = TokenRequest.Builder(serviceConfig, AuthConfig.CLIENT_ID)
                .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
                .setAuthorizationCode(authorizationCode)
                .setRedirectUri(Uri.parse(AuthConfig.REDIRECT_URI))
                .setCodeVerifier(codeVerifier)
                .build()
            
            val result = performTokenRequestAsync(tokenRequest)
            
            if (result.second != null) {
                return@withContext AuthResult(
                    isSuccess = false,
                    error = "Token exchange failed: ${result.second?.message}"
                )
            }
            
            if (result.first?.accessToken != null) {
                val response = result.first!!
                // Store tokens securely
                securePrefs.storeTokens(
                    accessToken = response.accessToken!!,
                    refreshToken = response.refreshToken,
                    idToken = response.idToken
                )
                
                return@withContext AuthResult(
                    isSuccess = true,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken
                )
            }
            
            return@withContext AuthResult(
                isSuccess = false,
                error = "Invalid token response"
            )
            
        } catch (e: Exception) {
            Log.e("Token error", "${e.message}")
            return@withContext AuthResult(
                isSuccess = false,
                error = "Token exchange error: ${e.message}"
            )
        }
    }

    fun dispose() {
        authService.dispose()
        tokenManager.dispose()
    }
}
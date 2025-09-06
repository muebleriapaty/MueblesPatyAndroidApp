package com.mueblespaty.auth

import com.mueblespaty.BuildConfig

object AuthConfig {
    // Replace with your actual shop domain and client ID
    val SHOP_DOMAIN = BuildConfig.SHOPIFY_DOMAIN
    val CLIENT_ID = BuildConfig.SHOPIFY_OAUTH_CLIENT_ID
    
    // OAuth URLs for Shopify Customer Account API
    val AUTH_ENDPOINT = BuildConfig.SHOPIFY_OAUTH_AUTH_ENDPOINT
    val TOKEN_ENDPOINT = BuildConfig.SHOPIFY_OAUTH_TOKEN_ENDPOINT
    val LOGOUT_ENDPOINT = BuildConfig.SHOPIFY_OAUTH_LOGOUT_ENDPOINT
    
    // Required scopes for customer account access
    const val SCOPE = "openid email customer-account-api:full"
    
    // Custom scheme for mobile app callback
    // For Shopify Customer Account API: must use format "shop.{shop_id}.*"
    val REDIRECT_SCHEME = BuildConfig.SHOPIFY_OAUTH_REDIRECT_SCHEME
    const val REDIRECT_HOST = "callback"
    val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST"
    
    // PKCE parameters
    const val CODE_CHALLENGE_METHOD = "S256"
    
    // Preferences key for storing auth state
    const val PREFS_NAME = "muebles_paty_auth"
    const val KEY_AUTH_STATE = "auth_state"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_CUSTOMER_ID = "customer_id"
}
package com.mueblespaty

import android.content.Context
import com.mueblespaty.BuildConfig
import android.util.Log
import com.mueblespaty.auth.AuthManager
import com.shopify.buy3.GraphClient
import com.shopify.buy3.Storefront
import com.shopify.checkoutsheetkit.ShopifyCheckoutSheetKit
import com.shopify.graphql.support.ID

data class CheckoutLineItem(
    val variantId: String,
    val quantity: Int
)

object CheckoutManager {
    
    private val variantQueries = mutableListOf<Storefront.ProductVariant>()
    private var completedQueries = 0
    private var totalQueries = 0

    /**
     * Create a checkout session and present the Checkout Sheet Kit
     */
    fun presentCheckout(
        context: Context,
        graphClient: GraphClient,
        cartItems: List<CartItem>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        presentCheckout(context, graphClient, cartItems, null, onSuccess, onError)
    }

    /**
     * Create a checkout session with optional customer authentication
     */
    fun presentCheckout(
        context: Context,
        graphClient: GraphClient,
        cartItems: List<CartItem>,
        authManager: AuthManager?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (cartItems.isEmpty()) {
            onError("El carrito está vacío")
            return
        }

        // Reset counters
        variantQueries.clear()
        completedQueries = 0
        totalQueries = cartItems.size

        // Check if customer is authenticated
        val customerAccessToken = authManager?.getCurrentAccessToken()
        val isAuthenticated = customerAccessToken != null
        
        Log.d("CheckoutManager", "Using Checkout Sheet Kit with ${cartItems.size} cart items, authenticated: $isAuthenticated")
        
        // Create checkout URL based on authentication status
        val checkoutUrl = if (isAuthenticated) {
            "https://${BuildConfig.SHOPIFY_DOMAIN}/cart?customer_access_token=$customerAccessToken"
        } else {
            "https://${BuildConfig.SHOPIFY_DOMAIN}/cart"
        }
        
        // Present checkout using Checkout Sheet Kit
        presentCheckoutWithSheetKit(checkoutUrl, context, authManager, onSuccess, onError)
    }

    private fun createCartAndGetCheckoutUrl(
        graphClient: GraphClient,
        variants: List<Storefront.ProductVariant>,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (variants.isEmpty()) {
            onError("No se pudieron encontrar las variantes de los productos")
            return
        }

        Log.d("CheckoutManager", "Creating checkout with ${variants.size} variants")
        // For now, use a simple checkout URL approach
        // The Storefront API cart creation requires specific API versions and exact method signatures
        val checkoutUrl = "https://${BuildConfig.SHOPIFY_DOMAIN}/cart"
        
        Log.d("CheckoutManager", "Using direct cart URL approach: $checkoutUrl")
        presentCheckoutWithSheetKit(checkoutUrl, context, null, onSuccess, onError)
    }

    /**
     * Present checkout using the Checkout Sheet Kit
     */
    private fun presentCheckoutWithSheetKit(
        checkoutUrl: String,
        context: Context,
        authManager: AuthManager?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            Log.d("CheckoutManager", "Presenting checkout with URL: $checkoutUrl")
            
            // Check if we have a customer cart ID to use instead
            val customerCartId = CartManager.getCustomerCartId()
            
            val intent = android.content.Intent(context, CheckoutActivity::class.java)
            if (customerCartId != null) {
                Log.d("CheckoutManager", "Using customer cart ID: $customerCartId")
                intent.putExtra(CheckoutActivity.EXTRA_CART_ID, customerCartId)
            } else {
                Log.d("CheckoutManager", "Using checkout URL: $checkoutUrl")
                intent.putExtra(CheckoutActivity.EXTRA_CHECKOUT_URL, checkoutUrl)
            }
            
            if (context is android.app.Activity) {
                context.startActivity(intent)
            } else {
                onError("Invalid context for starting checkout activity")
            }
            
        } catch (e: Exception) {
            Log.e("CheckoutManager", "Error in checkout process", e)
            onError("Error presenting checkout: ${e.message}")
        }
    }

    // Helper methods
    fun getCartUrl(): String {
        return "https://${BuildConfig.SHOPIFY_DOMAIN}/cart"
    }

    fun createSimpleCheckoutUrl(): String {
        return "https://${BuildConfig.SHOPIFY_DOMAIN}/checkout"
    }
}
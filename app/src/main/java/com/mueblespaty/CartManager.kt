package com.mueblespaty

import android.util.Log
import com.mueblespaty.auth.AuthManager
import com.mueblespaty.customer.CustomerApiClient
import com.shopify.buy3.Storefront
import kotlinx.coroutines.*

data class CartItem(
    val productHandle: String,
    val productTitle: String,
    val variantTitle: String,
    val variantId: String, // Add variant ID for checkout
    val price: Double,
    val currencyCode: String,
    val imageUrl: String?,
    var quantity: Int = 1
) {
    val totalPrice: Double
        get() = price * quantity
}

object CartManager {
    private val cartItems = mutableListOf<CartItem>()
    private val cartListeners = mutableListOf<CartUpdateListener>()
    private var authManager: AuthManager? = null
    private var customerApiClient: CustomerApiClient? = null
    private var customerCartId: String? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    interface CartUpdateListener {
        fun onCartUpdated(itemCount: Int, totalAmount: Double)
    }
    
    interface CartSyncCallback {
        fun onCartSyncCompleted(success: Boolean, cartId: String?)
    }
    
    fun initialize(authManager: AuthManager, customerApiClient: CustomerApiClient) {
        this.authManager = authManager
        this.customerApiClient = customerApiClient
        Log.d("CartManager", "Initialized with customer API client")
    }

    fun addListener(listener: CartUpdateListener) {
        cartListeners.add(listener)
    }

    fun removeListener(listener: CartUpdateListener) {
        cartListeners.remove(listener)
    }

    private fun notifyListeners() {
        val itemCount = getTotalItemCount()
        val totalAmount = getTotalAmount()
        cartListeners.forEach { it.onCartUpdated(itemCount, totalAmount) }
    }

    fun addToCart(item: CartItem) {
        Log.d("CartManager", "Adding item to cart: ${item.productTitle} - ${item.variantTitle}")
        
        // Check if item already exists in cart (same product and variant)
        val existingItem = cartItems.find { 
            it.productHandle == item.productHandle && it.variantTitle == item.variantTitle 
        }
        
        if (existingItem != null) {
            existingItem.quantity += item.quantity
        } else {
            cartItems.add(item)
        }
        
        notifyListeners()
        
        // Sync with customer cart if logged in
        syncWithCustomerCart()
    }

    fun removeFromCart(item: CartItem) {
        cartItems.remove(item)
        notifyListeners()
        // Sync with customer cart if logged in
        syncWithCustomerCart()
    }

    fun updateQuantity(item: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(item)
        } else {
            item.quantity = newQuantity
            notifyListeners()
            // Sync with customer cart if logged in
            syncWithCustomerCart()
        }
    }

    fun clearCart() {
        cartItems.clear()
        notifyListeners()
        // Clear customer cart as well
        customerCartId = null
    }

    fun getCartItems(): List<CartItem> {
        return cartItems.toList()
    }

    fun getTotalItemCount(): Int {
        return cartItems.sumOf { it.quantity }
    }

    fun getTotalAmount(): Double {
        return cartItems.sumOf { it.totalPrice }
    }

    fun isEmpty(): Boolean {
        return cartItems.isEmpty()
    }
    
    private fun syncWithCustomerCart() {
        val auth = authManager
        val client = customerApiClient
        
        if (auth == null || client == null) {
            Log.d("CartManager", "AuthManager or CustomerApiClient not initialized")
            return
        }
        
        if (!auth.isUserLoggedIn()) {
            Log.d("CartManager", "User not logged in, skipping cart sync")
            return
        }
        
        if (cartItems.isEmpty()) {
            Log.d("CartManager", "No items in cart to sync")
            return
        }
        
        Log.d("CartManager", "Syncing cart with customer account...")
        
        coroutineScope.launch {
            try {
                if (customerCartId == null) {
                    // Create new customer cart
                    Log.d("CartManager", "Creating new customer cart with ${cartItems.size} items")
                    val cartId = client.createCustomerCart(cartItems.toList())
                    if (cartId != null) {
                        customerCartId = cartId
                        Log.d("CartManager", "Customer cart created: $cartId")
                    } else {
                        Log.e("CartManager", "Failed to create customer cart")
                    }
                } else {
                    // Add items to existing cart
                    Log.d("CartManager", "Adding items to existing customer cart: $customerCartId")
                    val success = client.addItemsToCart(customerCartId!!, cartItems.toList())
                    if (success) {
                        Log.d("CartManager", "Items added to customer cart successfully")
                    } else {
                        Log.e("CartManager", "Failed to add items to customer cart")
                    }
                }
            } catch (e: Exception) {
                Log.e("CartManager", "Error syncing cart with customer: ${e.message}", e)
            }
        }
    }
    
    fun getCustomerCartId(): String? {
        return customerCartId
    }
    
    suspend fun getCustomerCheckoutUrl(): String? {
        val client = customerApiClient ?: return null
        val auth = authManager ?: return null
        
        if (!auth.isUserLoggedIn()) {
            return null
        }
        
        return try {
            client.getCustomerCheckoutUrl()
        } catch (e: Exception) {
            Log.e("CartManager", "Error getting customer checkout URL: ${e.message}", e)
            null
        }
    }
    
    fun loadCustomerCart() {
        loadCustomerCartWithCallback(null)
    }
    
    fun loadCustomerCartWithCallback(callback: CartSyncCallback?) {
        val auth = authManager
        val client = customerApiClient
        
        if (auth == null || client == null || !auth.isUserLoggedIn()) {
            Log.d("CartManager", "Cannot load customer cart - not authenticated")
            callback?.onCartSyncCompleted(false, null)
            return
        }
        
        Log.d("CartManager", "Loading/syncing customer cart...")
        
        coroutineScope.launch {
            try {
                var syncSuccess = false
                var finalCartId: String? = null
                
                // First try to load existing customer cart
                val existingCartId = client.getCustomerCartId()
                if (existingCartId != null) {
                    customerCartId = existingCartId
                    finalCartId = existingCartId
                    Log.d("CartManager", "Found existing customer cart ID: $existingCartId")
                    // TODO: Load cart items from the existing customer cart
                }
                
                // If we have local cart items, sync them to customer account
                if (cartItems.isNotEmpty()) {
                    Log.d("CartManager", "Syncing ${cartItems.size} local cart items to customer account")
                    
                    if (customerCartId == null) {
                        // Create new customer cart with current items
                        val cartId = client.createCustomerCart(cartItems.toList())
                        if (cartId != null) {
                            customerCartId = cartId
                            finalCartId = cartId
                            syncSuccess = true
                            Log.d("CartManager", "Created customer cart with local items: $cartId")
                        } else {
                            Log.e("CartManager", "Failed to create customer cart with local items")
                        }
                    } else {
                        // Add local items to existing customer cart
                        val success = client.addItemsToCart(customerCartId!!, cartItems.toList())
                        if (success) {
                            syncSuccess = true
                            finalCartId = customerCartId
                            Log.d("CartManager", "Added local items to existing customer cart")
                        } else {
                            Log.e("CartManager", "Failed to add local items to customer cart")
                        }
                    }
                } else {
                    // No local items to sync, but if we have existing cart, consider it success
                    syncSuccess = existingCartId != null
                    finalCartId = existingCartId
                }
                
                callback?.onCartSyncCompleted(syncSuccess, finalCartId)
                
            } catch (e: Exception) {
                Log.e("CartManager", "Error syncing customer cart: ${e.message}", e)
                callback?.onCartSyncCompleted(false, null)
            }
        }
    }
}
package com.mueblespaty

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.mueblespaty.auth.AuthManager
import com.mueblespaty.customer.CustomerApiClient
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

class LoginActivity : AppCompatActivity() {
    
    private lateinit var authManager: AuthManager
    private lateinit var customerApiClient: CustomerApiClient
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnContinueAsGuest: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    companion object {
        private const val AUTH_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        authManager = AuthManager(this)
        customerApiClient = CustomerApiClient(authManager)
        
        // Initialize CartManager if not already initialized
        CartManager.initialize(authManager, customerApiClient)
        
        // Check if user is already logged in
        if (authManager.isUserLoggedIn()) {
            handleSuccessfulLogin()
            return
        }
        
        setupViews()
        setupClickListeners()
    }
    
    private fun setupViews() {
        btnLogin = findViewById(R.id.btnLogin)
        btnContinueAsGuest = findViewById(R.id.btnContinueAsGuest)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            startAuthFlow()
        }
        
        btnContinueAsGuest.setOnClickListener {
            navigateToMain()
        }
    }
    
    private fun startAuthFlow() {
        showLoading(true)
        
        try {
            val authIntent = authManager.startAuthFlow()
            startActivityForResult(authIntent, AUTH_REQUEST_CODE)
            
        } catch (e: Exception) {
            showLoading(false)
            Log.e("LoginActivity", "Error starting authentication", e)
            showError("Failed to start authentication: ${e.message}")
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == AUTH_REQUEST_CODE) {
            handleAuthResult(data)
        }
    }
    
    private fun handleAuthResult(data: Intent?) {
        val authResponse = data?.let { AuthorizationResponse.fromIntent(it) }
        val authException = data?.let { AuthorizationException.fromIntent(it) }
        
        lifecycleScope.launch {
            val result = authManager.handleAuthResponse(authResponse, authException)
            
            showLoading(false)
            
            if (result.isSuccess) {
                showSuccess("Login successful!")
                handleSuccessfulLogin()
            } else {
                showError(result.error ?: "Login failed")
            }
        }
    }
    
    private fun handleSuccessfulLogin() {
        // Check if we should return to checkout flow
        val returnToCheckout = intent.getBooleanExtra("RETURN_TO_CHECKOUT", false)
        
        if (returnToCheckout) {
            // Sync cart items to customer account and wait for completion before returning
            syncCartToCustomerForCheckout()
        } else {
            // Normal navigation to main activity - sync cart in background
            syncCartToCustomer()
            navigateToMain()
        }
    }
    
    private fun syncCartToCustomer() {
        // Trigger cart sync to customer account
        // This will create a customer cart with current local cart items
        CartManager.loadCustomerCart()
        Log.d("LoginActivity", "Initiated cart sync to customer account after successful login")
    }
    
    private fun syncCartToCustomerForCheckout() {
        showLoading(true)
        Log.d("LoginActivity", "Syncing cart to customer for checkout flow")
        
        // Add cart sync completion callback
        val cartSyncCallback = object : CartManager.CartSyncCallback {
            override fun onCartSyncCompleted(success: Boolean, cartId: String?) {
                runOnUiThread {
                    showLoading(false)
                    if (success) {
                        Log.d("LoginActivity", "Cart sync successful, returning to checkout with cart ID: $cartId")
                        // Set result OK and finish - CartActivity will handle the customer checkout
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Log.e("LoginActivity", "Cart sync failed, proceeding with regular checkout")
                        // Even if sync failed, we can still proceed with regular checkout
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }
        }
        
        CartManager.loadCustomerCartWithCallback(cartSyncCallback)
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnContinueAsGuest.isEnabled = !show
    }
    
    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setAction("Retry") { startAuthFlow() }
            .show()
    }
    
    private fun showSuccess(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        authManager.dispose()
    }
}
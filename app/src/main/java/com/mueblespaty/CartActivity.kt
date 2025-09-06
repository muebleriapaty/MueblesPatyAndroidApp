package com.mueblespaty

import android.content.Intent
import com.mueblespaty.BuildConfig
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.mueblespaty.auth.AuthManager
import com.shopify.buy3.GraphClient
import com.shopify.checkoutsheetkit.ShopifyCheckoutSheetKit
import com.shopify.checkoutsheetkit.DefaultCheckoutEventProcessor
import java.text.NumberFormat
import java.util.*

class CartActivity : BaseActivity() {
    
    companion object {
        private const val REQUEST_CODE_LOGIN = 1001
    }

    private lateinit var recyclerViewCart: RecyclerView
    private lateinit var textViewSubtotal: TextView
    private lateinit var textViewTotal: TextView
    private lateinit var buttonCheckout: Button
    private lateinit var textViewEmptyCart: TextView
    private lateinit var cartItemAdapter: CartItemAdapter
    private lateinit var progressBarCheckout: ProgressBar
    private lateinit var graphClient: GraphClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Carrito de Compras"
        
        authManager = AuthManager(this)
        setupViews()
        setupShopifyClient()
        updateCartDisplay()
    }

    private fun setupViews() {
        recyclerViewCart = findViewById(R.id.recyclerViewCart)
        textViewSubtotal = findViewById(R.id.textViewSubtotal)
        textViewTotal = findViewById(R.id.textViewTotal)
        buttonCheckout = findViewById(R.id.buttonCheckout)
        textViewEmptyCart = findViewById(R.id.textViewEmptyCart)
        progressBarCheckout = findViewById(R.id.progressBarCheckout)

        recyclerViewCart.layoutManager = LinearLayoutManager(this)
        cartItemAdapter = CartItemAdapter(
            CartManager.getCartItems(),
            onQuantityChanged = { item, newQuantity ->
                CartManager.updateQuantity(item, newQuantity)
                updateCartDisplay()
            },
            onItemRemoved = { item ->
                CartManager.removeFromCart(item)
                updateCartDisplay()
            }
        )
        recyclerViewCart.adapter = cartItemAdapter

        buttonCheckout.setOnClickListener {
            if (!CartManager.isEmpty()) {
                proceedToCheckout()
            } else {
                Toast.makeText(this, getString(R.string.cart_empty), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupShopifyClient() {
        graphClient = GraphClient.build(
            context = this,
            shopDomain = BuildConfig.SHOPIFY_DOMAIN,
            accessToken = BuildConfig.SHOPIFY_STOREFRONT_ACCESS_TOKEN
        )
    }

    private fun updateCartDisplay() {
        val cartItems = CartManager.getCartItems()
        
        if (cartItems.isEmpty()) {
            // Show empty cart state
            recyclerViewCart.visibility = RecyclerView.GONE
            textViewEmptyCart.visibility = TextView.VISIBLE
            textViewSubtotal.text = "Subtotal: $0.00"
            textViewTotal.text = "Total: $0.00"
            buttonCheckout.isEnabled = false
        } else {
            // Show cart items
            recyclerViewCart.visibility = RecyclerView.VISIBLE
            textViewEmptyCart.visibility = TextView.GONE
            
            cartItemAdapter.updateItems(cartItems)
            
            val subtotal = CartManager.getTotalAmount()
            val total = subtotal // In a real app, you might add taxes, shipping, etc.
            
            textViewSubtotal.text = "Subtotal: ${formatPrice(subtotal, "MXN")}"
            textViewTotal.text = "Total: ${formatPrice(total, "MXN")}"
            buttonCheckout.isEnabled = true
        }
    }

    private fun proceedToCheckout() {
        val cartItems = CartManager.getCartItems()
        if (cartItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.cart_empty), Toast.LENGTH_SHORT).show()
            return
        }

        // Check authentication status first
        if (!authManager.isUserLoggedIn()) {
            // Show authentication prompt with benefits
            showAuthenticationPrompt()
            return
        }

        // User is authenticated, proceed with checkout
        startCheckoutFlow(cartItems)
    }
    
    private fun startCheckoutFlow(cartItems: List<CartItem>) {
        // Show loading state
        progressBarCheckout.visibility = View.VISIBLE
        buttonCheckout.isEnabled = false
        buttonCheckout.text = getString(R.string.creating_checkout)

        // Use CheckoutManager with customer authentication
        CheckoutManager.presentCheckout(
            context = this,
            graphClient = graphClient,
            cartItems = cartItems,
            authManager = authManager,
            onSuccess = {
                // Checkout completed successfully
                runOnUiThread {
                    handleCheckoutSuccess()
                }
            },
            onError = { error ->
                // Checkout failed or was canceled
                runOnUiThread {
                    handleCheckoutError(error)
                }
            }
        )
    }


    private fun handleCheckoutSuccess() {
        // Hide loading state
        progressBarCheckout.visibility = View.GONE
        buttonCheckout.isEnabled = true
        buttonCheckout.text = getString(R.string.proceed_to_checkout)
        
        // Show success message
        val message = "¡Pedido completado exitosamente!"
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Close activity and return to main
        finish()
    }

    private fun handleCheckoutError(error: String) {
        // Hide loading state
        progressBarCheckout.visibility = View.GONE
        buttonCheckout.isEnabled = true
        buttonCheckout.text = getString(R.string.proceed_to_checkout)
        
        // Show error message
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    override fun onCartUpdated(itemCount: Int, totalAmount: Double) {
        super.onCartUpdated(itemCount, totalAmount)
        updateCartDisplay()
    }

    private fun showAuthenticationPrompt() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.login_dialog_title))
            .setMessage(getString(R.string.login_dialog_message))
            .setPositiveButton(getString(R.string.login_button)) { _, _ ->
                // Launch login activity with return intent
                val intent = Intent(this, LoginActivity::class.java).apply {
                    putExtra("RETURN_TO_CHECKOUT", true)
                }
                startActivityForResult(intent, REQUEST_CODE_LOGIN)
            }
            .setNegativeButton(getString(R.string.continue_as_guest)) { _, _ ->
                // Proceed with guest checkout
                startGuestCheckout()
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
    
    private fun startGuestCheckout() {
        val cartItems = CartManager.getCartItems()
        if (cartItems.isNotEmpty()) {
            Toast.makeText(this, "Checkout como invitado no disponible actualmente", Toast.LENGTH_LONG).show()
            // For now, redirect to registration
            showRegistrationPrompt()
        }
    }
    
    private fun showRegistrationPrompt() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.registration_required_title))
            .setMessage(getString(R.string.registration_required_message))
            .setPositiveButton(getString(R.string.create_account)) { _, _ ->
                val intent = Intent(this, LoginActivity::class.java).apply {
                    putExtra("RETURN_TO_CHECKOUT", true)
                    putExtra("SHOW_REGISTRATION", true)
                }
                startActivityForResult(intent, REQUEST_CODE_LOGIN)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun formatPrice(amount: Double, currencyCode: String): String {
        return when (currencyCode) {
            "USD" -> NumberFormat.getCurrencyInstance(Locale.US).format(amount)
            "MXN" -> "$${String.format("%.2f", amount)}"
            "EUR" -> NumberFormat.getCurrencyInstance(Locale.GERMANY).format(amount)
            else -> "$currencyCode ${String.format("%.2f", amount)}"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == RESULT_OK) {
            // User successfully logged in, try customer checkout first
            val cartItems = CartManager.getCartItems()
            if (cartItems.isNotEmpty()) {
                startCustomerCheckoutFlow(cartItems)
            }
        }
    }
    
    private fun startCustomerCheckoutFlow(cartItems: List<CartItem>) {
        Log.d("CartActivity", "Attempting customer checkout flow")
        progressBarCheckout.visibility = View.VISIBLE
        buttonCheckout.isEnabled = false
        buttonCheckout.text = getString(R.string.preparing_customer_checkout)
        
        // Use coroutine to get customer checkout URL
        lifecycleScope.launch {
            try {
                val customerCheckoutUrl = CartManager.getCustomerCheckoutUrl()
                
                runOnUiThread {
                    if (customerCheckoutUrl != null) {
                        Log.d("CartActivity", "Using customer checkout URL: $customerCheckoutUrl")
                        // Launch customer checkout directly
                        val intent = Intent(this@CartActivity, CheckoutActivity::class.java).apply {
                            putExtra("CHECKOUT_URL", customerCheckoutUrl)
                            putExtra("IS_CUSTOMER_CHECKOUT", true)
                        }
                        startActivity(intent)
                    } else {
                        Log.d("CartActivity", "No customer checkout URL available, falling back to regular checkout")
                        // Fall back to regular checkout flow
                        startCheckoutFlow(cartItems)
                    }
                }
            } catch (e: Exception) {
                Log.e("CartActivity", "Error getting customer checkout URL: ${e.message}", e)
                runOnUiThread {
                    // Fall back to regular checkout flow
                    startCheckoutFlow(cartItems)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
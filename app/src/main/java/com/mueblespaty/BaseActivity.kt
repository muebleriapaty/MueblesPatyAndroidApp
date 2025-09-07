package com.mueblespaty

import android.content.Intent
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.mueblespaty.auth.AuthManager
import com.mueblespaty.utils.DeviceUtils

abstract class BaseActivity : AppCompatActivity(), CartManager.CartUpdateListener {
    
    private var cartBadge: TextView? = null
    private var cartMenuItem: MenuItem? = null
    protected lateinit var authManager: AuthManager
    private var accountMenuItem: MenuItem? = null
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Only apply edge-to-edge fixes for Samsung S24 with One UI 7.0
        if (DeviceUtils.isSamsungS24WithOneUI7()) {
            setupEdgeToEdge()
        }
    }
    
    private fun setupEdgeToEdge() {
        // Enable edge-to-edge display for Samsung S24 only
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Samsung S24 specific handling for One UI 7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }
    
    protected fun setupWindowInsetsForView(view: android.view.View) {
        // Only apply window insets handling for Samsung devices with known issues
        if (DeviceUtils.isSamsungWithEdgeToEdgeIssues()) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                
                // Apply padding to avoid overlap with system bars
                v.setPadding(
                    v.paddingLeft + insets.left,
                    v.paddingTop + insets.top, 
                    v.paddingRight + insets.right,
                    v.paddingBottom + insets.bottom
                )
                
                windowInsets
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        
        cartMenuItem = menu.findItem(R.id.action_cart)
        val actionView = cartMenuItem?.actionView
        cartBadge = actionView?.findViewById(R.id.cart_badge)
        
        actionView?.setOnClickListener {
            onCartClicked()
        }
        
        // Initialize AuthManager if not already done
        if (!::authManager.isInitialized) {
            authManager = AuthManager(this)
        }
        
        // Setup account menu item
        accountMenuItem = menu.findItem(R.id.action_account)

        updateCartBadge(CartManager.getTotalItemCount(), CartManager.getTotalAmount())
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cart -> {
                onCartClicked()
                true
            }
            R.id.action_account -> {
                onAccountClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onCartClicked() {
        val intent = Intent(this, CartActivity::class.java)
        startActivity(intent)
    }

    private fun onAccountClicked() {
        if (authManager.isUserLoggedIn()) {
            startActivity(Intent(this, AccountActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    override fun onCartUpdated(itemCount: Int, totalAmount: Double) {
        updateCartBadge(itemCount, totalAmount)
    }

    private fun updateCartBadge(itemCount: Int, totalAmount: Double) {
        // Hide entire cart menu item when cart is empty
        cartMenuItem?.isVisible = itemCount > 0
        
        // Update badge text and visibility
        cartBadge?.let { badge ->
            if (itemCount > 0) {
                badge.text = itemCount.toString()
                badge.visibility = TextView.VISIBLE
            } else {
                badge.visibility = TextView.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CartManager.addListener(this)
        updateCartBadge(CartManager.getTotalItemCount(), CartManager.getTotalAmount())
    }

    override fun onPause() {
        super.onPause()
        CartManager.removeListener(this)
    }
}
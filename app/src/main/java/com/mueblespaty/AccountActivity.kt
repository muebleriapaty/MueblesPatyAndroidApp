package com.mueblespaty

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.mueblespaty.auth.AuthManager
import com.mueblespaty.customer.CustomerApiClient
import com.mueblespaty.utils.DeviceUtils
import kotlinx.coroutines.launch

class AccountActivity : AppCompatActivity() {
    
    private lateinit var authManager: AuthManager
    private lateinit var customerApiClient: CustomerApiClient
    
    private lateinit var tvEmail: TextView
    private lateinit var tvCustomerId: TextView
    private lateinit var rvOrders: RecyclerView
    private lateinit var tvNoOrders: TextView
    private lateinit var btnRefreshToken: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var ordersAdapter: OrdersAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge for all devices (Android 15 requirement)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_account)
        
        // Setup window insets for all devices
        setupWindowInsets()
        
        // Setup toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.account_title)
        
        authManager = AuthManager(this)
        customerApiClient = CustomerApiClient(authManager)
        
        // Check if user is logged in
        if (!authManager.isUserLoggedIn()) {
            navigateToLogin()
            return
        }
        
        setupViews()
        setupClickListeners()
        loadCustomerData()
    }
    
    private fun setupViews() {
        tvEmail = findViewById(R.id.tvEmail)
        tvCustomerId = findViewById(R.id.tvCustomerId)
        rvOrders = findViewById(R.id.rvOrders)
        tvNoOrders = findViewById(R.id.tvNoOrders)
        btnRefreshToken = findViewById(R.id.btnRefreshToken)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)
        
        rvOrders.layoutManager = LinearLayoutManager(this)
        ordersAdapter = OrdersAdapter(emptyList())
        rvOrders.adapter = ordersAdapter
    }
    
    private fun setupClickListeners() {
        btnRefreshToken.setOnClickListener {
            refreshAuthentication()
        }
        
        btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun loadCustomerData() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val customer = customerApiClient.getCustomer()
                if (customer != null) {
                    tvEmail.text = customer.email ?: getString(R.string.account_data_na)
                    tvCustomerId.text = customer.id ?: getString(R.string.account_data_na)
                    
                    // Load orders
                    val orders = customerApiClient.getOrders()
                    if (orders.isNotEmpty()) {
                        ordersAdapter.updateOrders(orders)
                        rvOrders.visibility = View.VISIBLE
                        tvNoOrders.visibility = View.GONE
                    } else {
                        rvOrders.visibility = View.GONE
                        tvNoOrders.visibility = View.VISIBLE
                    }
                } else {
                    showError(getString(R.string.failed_load_customer_data))
                }
            } catch (e: Exception) {
                showError(getString(R.string.error_loading_customer_data, e.message))
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun refreshAuthentication() {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = authManager.refreshAccessToken()
            showLoading(false)
            
            if (result.isSuccess) {
                showSuccess(getString(R.string.auth_refreshed_successfully))
                loadCustomerData()
            } else {
                showError(getString(R.string.failed_refresh_auth, result.error))
            }
        }
    }
    
    private fun logout() {
        authManager.logout()
        navigateToLogin()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRefreshToken.isEnabled = !show
        btnLogout.isEnabled = !show
    }
    
    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .show()
    }
    
    private fun showSuccess(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
            .show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupWindowInsets() {
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            
            // Get Samsung-specific extra padding for better header spacing
            val extraTopPadding = DeviceUtils.getSamsungS24ExtraTopPadding()
            val extraBottomPadding = DeviceUtils.getSamsungExtraBottomPadding()
            
            // Apply enhanced padding for Samsung S24 with extra space for header text
            view.setPadding(
                insets.left + cutoutInsets.left,
                insets.top + cutoutInsets.top + extraTopPadding,
                insets.right + cutoutInsets.right,
                insets.bottom + cutoutInsets.bottom + extraBottomPadding
            )
            
            windowInsets
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        authManager.dispose()
    }
}
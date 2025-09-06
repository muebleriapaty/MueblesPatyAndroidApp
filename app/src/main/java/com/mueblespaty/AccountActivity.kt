package com.mueblespaty

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.mueblespaty.auth.AuthManager
import com.mueblespaty.customer.CustomerApiClient
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
        setContentView(R.layout.activity_account)
        
        // Setup toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Account"
        
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
                    tvEmail.text = customer.email ?: "N/A"
                    tvCustomerId.text = customer.id ?: "N/A"
                    
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
                    showError("Failed to load customer data")
                }
            } catch (e: Exception) {
                showError("Error loading customer data: ${e.message}")
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
                showSuccess("Authentication refreshed successfully")
                loadCustomerData()
            } else {
                showError("Failed to refresh authentication: ${result.error}")
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
    
    override fun onDestroy() {
        super.onDestroy()
        authManager.dispose()
    }
}
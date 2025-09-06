package com.mueblespaty

import android.content.Intent
import android.net.Uri
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
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_ORDER_NAME = "order_name"
    }
    
    private lateinit var authManager: AuthManager
    private lateinit var customerApiClient: CustomerApiClient
    
    private lateinit var tvOrderTitle: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvOrderTotal: TextView
    private lateinit var tvBillingAddress: TextView
    private lateinit var tvShippingAddress: TextView
    private lateinit var rvLineItems: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnContactWhatsApp: MaterialButton
    
    private var orderId: String? = null
    private var orderName: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)
        
        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: intent.getStringExtra("ORDER_ID")
        orderName = intent.getStringExtra(EXTRA_ORDER_NAME)
        
        // Setup toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = orderName ?: "Order Details"
        
        authManager = AuthManager(this)
        customerApiClient = CustomerApiClient(authManager)
        
        // Check if user is logged in
        if (!authManager.isUserLoggedIn()) {
            finish()
            return
        }
        
        setupViews()
        loadOrderDetails()
    }
    
    private fun setupViews() {
        tvOrderTitle = findViewById(R.id.tvOrderTitle)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvOrderStatus = findViewById(R.id.tvOrderStatus)
        tvOrderTotal = findViewById(R.id.tvOrderTotal)
        tvBillingAddress = findViewById(R.id.tvBillingAddress)
        tvShippingAddress = findViewById(R.id.tvShippingAddress)
        rvLineItems = findViewById(R.id.rvLineItems)
        progressBar = findViewById(R.id.progressBar)
        btnContactWhatsApp = findViewById(R.id.btnContactWhatsApp)
        
        rvLineItems.layoutManager = LinearLayoutManager(this)
        
        btnContactWhatsApp.setOnClickListener {
            openWhatsAppChat()
        }
    }
    
    private fun loadOrderDetails() {
        val orderIdValue = orderId
        if (orderIdValue == null) {
            showError("Order ID not provided")
            return
        }
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                Log.d("OrderDetailActivity", "Loading order details for ID: $orderIdValue")
                val orderDetail = customerApiClient.getOrderDetails(orderIdValue)
                if (orderDetail != null) {
                    Log.d("OrderDetailActivity", "Order detail loaded. Line items count: ${orderDetail.lineItems.size}")
                    displayOrderDetails(orderDetail)
                } else {
                    Log.e("OrderDetailActivity", "Order detail is null")
                    showError("Failed to load order details")
                }
            } catch (e: Exception) {
                Log.e("OrderDetailActivity", "Error loading order details", e)
                showError("Error loading order details: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun displayOrderDetails(order: CustomerApiClient.OrderDetail) {
        tvOrderTitle.text = order.name ?: orderName
        tvOrderStatus.text = getTranslatedOrderStatus(order.financialStatus)
        tvOrderTotal.text = order.totalPrice
        
        // Format the date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            val date = inputFormat.parse(order.createdAt)
            tvOrderDate.text = if (date != null) outputFormat.format(date) else order.createdAt
        } catch (e: Exception) {
            tvOrderDate.text = order.createdAt
        }
        
        // Display addresses
        tvBillingAddress.text = order.billingAddress ?: "No billing address"
        tvShippingAddress.text = order.shippingAddress ?: "No shipping address"
        
        // Setup line items
        Log.d("OrderDetailActivity", "Setting up line items. Count: ${order.lineItems.size}")
        if (order.lineItems.isNotEmpty()) {
            val lineItemsAdapter = OrderLineItemsAdapter(order.lineItems)
            rvLineItems.adapter = lineItemsAdapter
            Log.d("OrderDetailActivity", "Line items adapter set")
        } else {
            Log.w("OrderDetailActivity", "No line items found in order")
        }
    }
    
    private fun openWhatsAppChat() {
        val orderNumber = tvOrderTitle.text.toString()
        val message = getString(R.string.whatsapp_message, orderNumber)
        val phoneNumber = "+525510789026"
        
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$phoneNumber?text=${Uri.encode(message)}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("OrderDetailActivity", "Error opening WhatsApp", e)
            showError("WhatsApp is not installed or unavailable")
        }
    }
    
    private fun getTranslatedOrderStatus(status: String?): String {
        return when (status?.uppercase()) {
            "PENDING" -> getString(R.string.order_status_pending)
            "AUTHORIZED" -> getString(R.string.order_status_authorized)
            "PARTIALLY_PAID" -> getString(R.string.order_status_partially_paid)
            "PAID" -> getString(R.string.order_status_paid)
            "PARTIALLY_REFUNDED" -> getString(R.string.order_status_partially_refunded)
            "REFUNDED" -> getString(R.string.order_status_refunded)
            "VOIDED" -> getString(R.string.order_status_voided)
            "EXPIRED" -> getString(R.string.order_status_expired)
            "CANCELLED" -> getString(R.string.order_status_cancelled)
            "FULFILLED" -> getString(R.string.order_status_fulfilled)
            "PARTIAL" -> getString(R.string.order_status_partial)
            "UNFULFILLED" -> getString(R.string.order_status_unfulfilled)
            "RESTOCKED" -> getString(R.string.order_status_restocked)
            else -> status ?: "UNKNOWN"
        }
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
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
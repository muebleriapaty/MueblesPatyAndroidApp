package com.mueblespaty

import android.content.Intent
import com.mueblespaty.BuildConfig
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CartSummaryActivity : BaseActivity() {

    private lateinit var recyclerViewSummary: RecyclerView
    private lateinit var textViewInstructions: TextView
    private lateinit var buttonProceedToShop: Button
    private lateinit var cartSummaryAdapter: CartSummaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart_summary)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Resumen de Compra"

        setupViews()
        displayCartSummary()
    }

    private fun setupViews() {
        recyclerViewSummary = findViewById(R.id.recyclerViewSummary)
        textViewInstructions = findViewById(R.id.textViewInstructions)
        buttonProceedToShop = findViewById(R.id.buttonProceedToShop)

        recyclerViewSummary.layoutManager = LinearLayoutManager(this)
        cartSummaryAdapter = CartSummaryAdapter(CartManager.getCartItems())
        recyclerViewSummary.adapter = cartSummaryAdapter

        buttonProceedToShop.setOnClickListener {
            // Get the customer cart ID if available
            val customerCartId = CartManager.getCustomerCartId()
            
            val intent = Intent(this, CheckoutActivity::class.java)
            if (customerCartId != null) {
                // Use customer cart checkout URL
                intent.putExtra(CheckoutActivity.EXTRA_CART_ID, customerCartId)
            } else {
                // Fallback to generic shop URL for guest checkout
                intent.putExtra(CheckoutActivity.EXTRA_CHECKOUT_URL, "https://${BuildConfig.SHOPIFY_DOMAIN}")
            }
            
            startActivity(intent)
        }
    }

    private fun displayCartSummary() {
        val cartItems = CartManager.getCartItems()
        val totalAmount = CartManager.getTotalAmount()
        
        textViewInstructions.text = """
            Tu pedido incluye ${cartItems.size} producto(s) por un total de $${String.format("%.2f", totalAmount)}.
            
            Al hacer clic en "Ir a la Tienda", serás dirigido a nuestro sitio web donde podrás:
            • Ver todos nuestros productos
            • Agregar los productos deseados a tu carrito
            • Completar tu compra de forma segura
            
            ¡Gracias por elegir Muebles Paty!
        """.trimIndent()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
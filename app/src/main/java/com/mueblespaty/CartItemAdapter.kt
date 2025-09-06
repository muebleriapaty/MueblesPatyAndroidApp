package com.mueblespaty

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.*

class CartItemAdapter(
    private var cartItems: List<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onItemRemoved: (CartItem) -> Unit
) : RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder>() {

    class CartItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewCartItem)
        val titleTextView: TextView = itemView.findViewById(R.id.textViewCartItemTitle)
        val variantTextView: TextView = itemView.findViewById(R.id.textViewCartItemVariant)
        val priceTextView: TextView = itemView.findViewById(R.id.textViewCartItemPrice)
        val quantityTextView: TextView = itemView.findViewById(R.id.textViewQuantity)
        val buttonDecrease: ImageButton = itemView.findViewById(R.id.buttonDecrease)
        val buttonIncrease: ImageButton = itemView.findViewById(R.id.buttonIncrease)
        val buttonRemove: ImageButton = itemView.findViewById(R.id.buttonRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        val item = cartItems[position]

        holder.titleTextView.text = item.productTitle
        holder.variantTextView.text = item.variantTitle
        holder.quantityTextView.text = item.quantity.toString()
        
        // Format and display price
        val totalPrice = item.totalPrice
        holder.priceTextView.text = formatPrice(totalPrice, item.currencyCode)

        // Load product image
        if (item.imageUrl != null) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Quantity controls
        holder.buttonDecrease.setOnClickListener {
            if (item.quantity > 1) {
                onQuantityChanged(item, item.quantity - 1)
            }
        }

        holder.buttonIncrease.setOnClickListener {
            onQuantityChanged(item, item.quantity + 1)
        }

        // Remove item
        holder.buttonRemove.setOnClickListener {
            onItemRemoved(item)
        }

        // Disable decrease button if quantity is 1
        holder.buttonDecrease.isEnabled = item.quantity > 1
        holder.buttonDecrease.alpha = if (item.quantity > 1) 1.0f else 0.5f
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateItems(newItems: List<CartItem>) {
        cartItems = newItems
        notifyDataSetChanged()
    }

    private fun formatPrice(amount: Double, currencyCode: String): String {
        return when (currencyCode) {
            "USD" -> NumberFormat.getCurrencyInstance(Locale.US).format(amount)
            "MXN" -> "$${String.format("%.2f", amount)}"
            "EUR" -> NumberFormat.getCurrencyInstance(Locale.GERMANY).format(amount)
            else -> "$currencyCode ${String.format("%.2f", amount)}"
        }
    }
}
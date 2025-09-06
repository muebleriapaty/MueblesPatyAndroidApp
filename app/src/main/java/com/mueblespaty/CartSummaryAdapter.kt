package com.mueblespaty

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.*

class CartSummaryAdapter(
    private val cartItems: List<CartItem>
) : RecyclerView.Adapter<CartSummaryAdapter.SummaryViewHolder>() {

    class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewSummaryItem)
        val titleTextView: TextView = itemView.findViewById(R.id.textViewSummaryTitle)
        val variantTextView: TextView = itemView.findViewById(R.id.textViewSummaryVariant)
        val quantityTextView: TextView = itemView.findViewById(R.id.textViewSummaryQuantity)
        val priceTextView: TextView = itemView.findViewById(R.id.textViewSummaryPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val item = cartItems[position]

        holder.titleTextView.text = item.productTitle
        holder.variantTextView.text = item.variantTitle
        holder.quantityTextView.text = "Cantidad: ${item.quantity}"
        
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
    }

    override fun getItemCount(): Int = cartItems.size

    private fun formatPrice(amount: Double, currencyCode: String): String {
        return when (currencyCode) {
            "USD" -> NumberFormat.getCurrencyInstance(Locale.US).format(amount)
            "MXN" -> "$${String.format("%.2f", amount)}"
            "EUR" -> NumberFormat.getCurrencyInstance(Locale.GERMANY).format(amount)
            else -> "$currencyCode ${String.format("%.2f", amount)}"
        }
    }
}
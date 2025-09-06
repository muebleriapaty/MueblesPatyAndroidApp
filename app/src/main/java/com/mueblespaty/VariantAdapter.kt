package com.mueblespaty

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shopify.buy3.Storefront
import java.text.NumberFormat
import java.util.*

class VariantAdapter(
    private var variants: List<Storefront.ProductVariant>,
    private val onVariantSelected: (Storefront.ProductVariant) -> Unit
) : RecyclerView.Adapter<VariantAdapter.VariantViewHolder>() {

    private var selectedPosition = -1

    class VariantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardViewVariant)
        val titleTextView: TextView = itemView.findViewById(R.id.textViewVariantTitle)
        val priceTextView: TextView = itemView.findViewById(R.id.textViewVariantPrice)
        val statusTextView: TextView = itemView.findViewById(R.id.textViewVariantStatus)
        val optionsTextView: TextView = itemView.findViewById(R.id.textViewVariantOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_variant, parent, false)
        return VariantViewHolder(view)
    }

    override fun onBindViewHolder(holder: VariantViewHolder, position: Int) {
        val variant = variants[position]
        val context = holder.itemView.context
        
        holder.titleTextView.text = variant.title
        
        // Format price
        val price = variant.price.amount.toDoubleOrNull() ?: 0.0
        val currencyCode = variant.price.currencyCode.toString()
        holder.priceTextView.text = formatPrice(price, currencyCode)
        
        // Show availability status
        if (variant.availableForSale) {
            holder.statusTextView.text = holder.itemView.context.getString(R.string.variant_available)
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
        } else {
            holder.statusTextView.text = holder.itemView.context.getString(R.string.variant_out_of_stock)
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
        }
        
        // Show variant options (size, color, etc.)
        val options = variant.selectedOptions.joinToString(", ") { "${it.name}: ${it.value}" }
        holder.optionsTextView.text = if (options.isNotEmpty()) options else "Opción estándar"
        
        // Handle selection state
        val isSelected = position == selectedPosition
        if (isSelected) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.purple_200))
            holder.cardView.strokeWidth = 4
            holder.cardView.strokeColor = ContextCompat.getColor(context, R.color.purple_500)
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            holder.cardView.strokeWidth = 1
            holder.cardView.strokeColor = ContextCompat.getColor(context, android.R.color.darker_gray)
        }
        
        // Handle click
        holder.itemView.setOnClickListener {
            if (variant.availableForSale) {
                val previousSelected = selectedPosition
                selectedPosition = position
                
                // Update UI for previous and current selection
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                
                onVariantSelected(variant)
            }
        }
        
        // Disable interaction for unavailable variants
        holder.itemView.alpha = if (variant.availableForSale) 1.0f else 0.6f
        holder.itemView.isEnabled = variant.availableForSale
    }

    override fun getItemCount(): Int = variants.size

    fun updateVariants(newVariants: List<Storefront.ProductVariant>) {
        variants = newVariants
        selectedPosition = -1 // Reset selection
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
package com.mueblespaty

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mueblespaty.customer.CustomerApiClient

class OrderLineItemsAdapter(private val lineItems: List<CustomerApiClient.OrderLineItem>) :
    RecyclerView.Adapter<OrderLineItemsAdapter.LineItemViewHolder>() {

    class LineItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val productName: TextView = itemView.findViewById(R.id.tvProductName)
        val variantTitle: TextView = itemView.findViewById(R.id.tvVariantTitle)
        val quantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val price: TextView = itemView.findViewById(R.id.tvPrice)
        val totalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_line_item, parent, false)
        return LineItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: LineItemViewHolder, position: Int) {
        val lineItem = lineItems[position]
        
        holder.productName.text = lineItem.title
        holder.variantTitle.text = lineItem.variantTitle ?: ""
        holder.quantity.text = "Qty: ${lineItem.quantity}"
        holder.price.text = lineItem.unitPrice
        holder.totalPrice.text = lineItem.totalPrice
        
        // Load product image
        if (lineItem.imageUrl != null) {
            Glide.with(holder.itemView.context)
                .load(lineItem.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.productImage)
        } else {
            holder.productImage.setImageResource(R.drawable.ic_placeholder)
        }
        
        // Hide variant title if empty
        if (lineItem.variantTitle.isNullOrEmpty()) {
            holder.variantTitle.visibility = View.GONE
        } else {
            holder.variantTitle.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = lineItems.size
}
package com.mueblespaty

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mueblespaty.customer.CustomerApiClient
import java.text.SimpleDateFormat
import java.util.*

class OrdersAdapter(private var orders: List<CustomerApiClient.Order>) : 
    RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderNumber: TextView = itemView.findViewById(R.id.tvOrderNumber)
        val orderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        val orderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        val orderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        
        holder.orderNumber.text = "#${order.orderNumber}"
        holder.orderTotal.text = order.totalPrice
        holder.orderStatus.text = getTranslatedOrderStatus(holder.itemView.context, order.financialStatus)
        
        // Format the date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(order.createdAt)
            holder.orderDate.text = if (date != null) outputFormat.format(date) else order.createdAt
        } catch (e: Exception) {
            holder.orderDate.text = order.createdAt
        }
        
        // Set click listener to open order details
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, OrderDetailActivity::class.java)
            intent.putExtra("ORDER_ID", order.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = orders.size

    fun updateOrders(newOrders: List<CustomerApiClient.Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
    
    private fun getTranslatedOrderStatus(context: Context, status: String?): String {
        return when (status?.uppercase()) {
            "PENDING" -> context.getString(R.string.order_status_pending)
            "AUTHORIZED" -> context.getString(R.string.order_status_authorized)
            "PARTIALLY_PAID" -> context.getString(R.string.order_status_partially_paid)
            "PAID" -> context.getString(R.string.order_status_paid)
            "PARTIALLY_REFUNDED" -> context.getString(R.string.order_status_partially_refunded)
            "REFUNDED" -> context.getString(R.string.order_status_refunded)
            "VOIDED" -> context.getString(R.string.order_status_voided)
            "EXPIRED" -> context.getString(R.string.order_status_expired)
            "CANCELLED" -> context.getString(R.string.order_status_cancelled)
            "FULFILLED" -> context.getString(R.string.order_status_fulfilled)
            "PARTIAL" -> context.getString(R.string.order_status_partial)
            "UNFULFILLED" -> context.getString(R.string.order_status_unfulfilled)
            "RESTOCKED" -> context.getString(R.string.order_status_restocked)
            else -> status ?: "UNKNOWN"
        }
    }
}
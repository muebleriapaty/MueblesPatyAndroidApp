package com.mueblespaty

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shopify.buy3.Storefront
import java.text.NumberFormat
import java.util.*

class ProductAdapter(
    private var products: List<Storefront.Product>,
    private val onProductClick: (Storefront.Product) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isLoading = false
    private val SKELETON_COUNT = 6

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_SKELETON = 1
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewProductTitle)
        val priceTextView: TextView = itemView.findViewById(R.id.textViewProductPrice)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewProductDescription)
        val imageView: ImageView = itemView.findViewById(R.id.imageViewProduct)
    }

    class SkeletonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return if (isLoading) TYPE_SKELETON else TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SKELETON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_product_skeleton, parent, false)
                SkeletonViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_product, parent, false)
                ProductViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ProductViewHolder -> bindProductViewHolder(holder, position)
            is SkeletonViewHolder -> {
                // Skeleton views don't need binding, they're just placeholders
            }
        }
    }

    private fun bindProductViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        
        holder.titleTextView.text = product.title
        holder.descriptionTextView.text = product.description?.take(100) ?: "Sin descripción"
        
        // Format price
        val minPrice = product.priceRange.minVariantPrice.amount.toDoubleOrNull() ?: 0.0
        val currencyCode = product.priceRange.minVariantPrice.currencyCode.toString()
        val formattedPrice = formatPrice(minPrice, currencyCode)
        holder.priceTextView.text = formattedPrice
        
        // Load product image
        val imageUrl = product.images.edges.firstOrNull()?.node?.url
        if (imageUrl != null) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }
        
        holder.itemView.setOnClickListener {
            // Navigate to product detail activity
            val context = holder.itemView.context
            val intent = Intent(context, ProductDetailActivity::class.java).apply {
                putExtra(ProductDetailActivity.EXTRA_PRODUCT_HANDLE, product.handle)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return if (isLoading) SKELETON_COUNT else products.size
    }

    fun showLoading() {
        isLoading = true
        notifyDataSetChanged()
    }

    fun hideLoading() {
        isLoading = false
        notifyDataSetChanged()
    }

    fun updateProducts(newProducts: List<Storefront.Product>) {
        products = newProducts
        isLoading = false
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
package com.mueblespaty

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shopify.buy3.Storefront

class CatalogAdapter(
    private var collections: List<Storefront.Collection>,
    private val onCollectionClick: (Storefront.Collection) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CATALOG = 0
        private const val TYPE_WEBSITE_LINK = 1
    }

    class CatalogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        val imageView: ImageView = itemView.findViewById(R.id.imageViewCatalog)
    }
    
    class WebsiteLinkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val websiteTitleTextView: TextView = itemView.findViewById(R.id.textViewWebsiteTitle)
        val websiteDescriptionTextView: TextView = itemView.findViewById(R.id.textViewWebsiteDescription)
        val websiteImageView: ImageView = itemView.findViewById(R.id.imageViewWebsite)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < collections.size) TYPE_CATALOG else TYPE_WEBSITE_LINK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CATALOG -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_catalog, parent, false)
                CatalogViewHolder(view)
            }
            TYPE_WEBSITE_LINK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_website_link, parent, false)
                WebsiteLinkViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CatalogViewHolder -> {
                val collection = collections[position]
                
                holder.titleTextView.text = collection.title
                holder.descriptionTextView.text = collection.description ?: "Ver productos"
                
                // Load image using Glide
                val imageUrl = collection.image?.url
                if (imageUrl != null) {
                    Glide.with(holder.itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .centerCrop()
                        .into(holder.imageView)
                } else {
                    // Use placeholder if no image available
                    holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)
                }
                
                holder.itemView.setOnClickListener {
                    onCollectionClick(collection)
                }
            }
            is WebsiteLinkViewHolder -> {
                holder.itemView.setOnClickListener {
                    val context = holder.itemView.context
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mueblespaty.com"))
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount(): Int = collections.size + 1 // +1 for website link

    fun updateCollections(newCollections: List<Storefront.Collection>) {
        collections = newCollections
        notifyDataSetChanged()
    }
}
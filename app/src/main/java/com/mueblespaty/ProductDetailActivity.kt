package com.mueblespaty

import android.os.Bundle
import com.mueblespaty.BuildConfig
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.shopify.buy3.GraphClient
import com.shopify.buy3.Storefront
import java.text.NumberFormat
import java.util.*

class ProductDetailActivity : BaseActivity() {

    private lateinit var layoutSkeleton: View
    private lateinit var layoutContent: View
    private lateinit var viewPagerImages: ViewPager2
    private lateinit var textViewTitle: TextView
    private lateinit var textViewPrice: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewVariantsTitle: TextView
    private lateinit var recyclerViewVariants: RecyclerView
    private lateinit var buttonAddToCart: Button
    
    private lateinit var graphClient: GraphClient
    private var productHandle: String? = null
    private var currentProduct: Storefront.Product? = null
    private var selectedVariant: Storefront.ProductVariant? = null
    private lateinit var variantAdapter: VariantAdapter

    companion object {
        const val EXTRA_PRODUCT_HANDLE = "product_handle"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        productHandle = intent.getStringExtra(EXTRA_PRODUCT_HANDLE)
        
        setupViews()
        setupShopifyClient()
        showLoading()
        
        productHandle?.let { loadProductDetails(it) }
    }

    private fun setupViews() {
        layoutSkeleton = findViewById(R.id.layoutSkeleton)
        layoutContent = findViewById(R.id.layoutContent)
        viewPagerImages = findViewById(R.id.viewPagerImages)
        textViewTitle = findViewById(R.id.textViewProductTitle)
        textViewPrice = findViewById(R.id.textViewProductPrice)
        textViewDescription = findViewById(R.id.textViewProductDescription)
        textViewVariantsTitle = findViewById(R.id.textViewVariantsTitle)
        recyclerViewVariants = findViewById(R.id.recyclerViewVariants)
        buttonAddToCart = findViewById(R.id.buttonAddToCart)

        // Setup variants RecyclerView
        recyclerViewVariants.layoutManager = LinearLayoutManager(this)
        variantAdapter = VariantAdapter(emptyList()) { variant ->
            selectedVariant = variant
            updatePriceForVariant(variant)
            updateAddToCartButton()
        }
        recyclerViewVariants.adapter = variantAdapter

        buttonAddToCart.setOnClickListener {
            selectedVariant?.let { variant ->
                addToCart(variant)
            } ?: run {
                Toast.makeText(this, getString(R.string.select_variant), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupShopifyClient() {
        graphClient = GraphClient.build(
            context = this,
            shopDomain = BuildConfig.SHOPIFY_DOMAIN,
            accessToken = BuildConfig.SHOPIFY_STOREFRONT_ACCESS_TOKEN
        )
    }

    private fun loadProductDetails(productHandle: String) {
        val query = Storefront.query { rootQueryBuilder ->
            rootQueryBuilder.productByHandle(productHandle) { product ->
                product
                    .handle()
                    .title()
                    .description()
                    .priceRange { priceRange ->
                        priceRange
                            .minVariantPrice { price ->
                                price.amount().currencyCode()
                            }
                            .maxVariantPrice { price ->
                                price.amount().currencyCode()
                            }
                    }
                    .images({ args ->
                        args.first(10)
                    }) { imageConnection ->
                        imageConnection.edges { edge ->
                            edge.node { image ->
                                image.url()
                            }
                        }
                    }
                    .variants({ args ->
                        args.first(20)
                    }) { variantConnection ->
                        variantConnection.edges { edge ->
                            edge.node { variant ->
                                variant
                                    .title()
                                    .price { price ->
                                        price.amount().currencyCode()
                                    }
                                    .availableForSale()
                                    .selectedOptions { option ->
                                        option.name().value()
                                    }
                            }
                        }
                    }
            }
        }

        val call = graphClient.queryGraph(query)
        call.enqueue { result ->
            when (result) {
                is com.shopify.buy3.GraphCallResult.Success -> {
                    val product = result.response.data?.productByHandle
                    product?.let {
                        currentProduct = it
                        runOnUiThread {
                            displayProductDetails(it)
                        }
                    }
                }
                is com.shopify.buy3.GraphCallResult.Failure -> {
                    println("GraphQL call failed: ${result.error}")
                }
            }
        }
    }

    private fun displayProductDetails(product: Storefront.Product) {
        hideLoading()
        
        textViewTitle.text = product.title
        textViewDescription.text = product.description ?: getString(R.string.no_description_available)
        
        // Display price range
        val minPrice = product.priceRange.minVariantPrice.amount.toDoubleOrNull() ?: 0.0
        val maxPrice = product.priceRange.maxVariantPrice.amount.toDoubleOrNull() ?: 0.0
        val currencyCode = product.priceRange.minVariantPrice.currencyCode.toString()
        
        val priceText = if (minPrice == maxPrice) {
            formatPrice(minPrice, currencyCode)
        } else {
            "${formatPrice(minPrice, currencyCode)} - ${formatPrice(maxPrice, currencyCode)}"
        }
        textViewPrice.text = priceText

        // Setup image gallery
        val imageUrls = product.images.edges.map { it.node.url }
        val imageAdapter = ProductImageAdapter(imageUrls)
        viewPagerImages.adapter = imageAdapter

        // Setup variants
        val variants = product.variants.edges.map { it.node }
        
        // Hide variants section if there's only one variant with "Default Title"
        val shouldHideVariants = variants.size == 1 && variants.first().title.equals("Default Title", ignoreCase = true)
        Log.d("ProductDetailActivity", "Product has ${variants.size} variants. First variant title: '${variants.firstOrNull()?.title}'. Should hide variants: $shouldHideVariants")
        textViewVariantsTitle.visibility = if (shouldHideVariants) View.GONE else View.VISIBLE
        recyclerViewVariants.visibility = if (shouldHideVariants) View.GONE else View.VISIBLE
        
        if (!shouldHideVariants) {
            variantAdapter.updateVariants(variants)
        }
        
        // Select first available variant by default
        selectedVariant = variants.firstOrNull { it.availableForSale }
        selectedVariant?.let { updatePriceForVariant(it) }
        updateAddToCartButton()
    }

    private fun updatePriceForVariant(variant: Storefront.ProductVariant) {
        val price = variant.price.amount.toDoubleOrNull() ?: 0.0
        val currencyCode = variant.price.currencyCode.toString()
        textViewPrice.text = formatPrice(price, currencyCode)
    }

    private fun updateAddToCartButton() {
        val isAvailable = selectedVariant?.availableForSale ?: false
        buttonAddToCart.isEnabled = isAvailable
        buttonAddToCart.text = if (isAvailable) getString(R.string.add_to_cart) else getString(R.string.not_available)
    }

    private fun addToCart(variant: Storefront.ProductVariant) {
        val product = currentProduct
        if (product != null) {
            val imageUrl = product.images.edges.firstOrNull()?.node?.url
            
            // Use the actual Shopify variant ID instead of generating a fake one
            val variantId = variant.id.toString()
            Log.d("ProductDetailActivity", "Adding variant to cart: $variantId (${variant.title})")
            
            val cartItem = CartItem(
                productHandle = product.handle,
                productTitle = product.title,
                variantTitle = variant.title,
                variantId = variantId,
                price = variant.price.amount.toDoubleOrNull() ?: 0.0,
                currencyCode = variant.price.currencyCode.toString(),
                imageUrl = imageUrl,
                quantity = 1
            )
            
            CartManager.addToCart(cartItem)
            Toast.makeText(this, getString(R.string.product_added_to_cart), Toast.LENGTH_SHORT).show()
        }
    }


    private fun showLoading() {
        layoutSkeleton.visibility = View.VISIBLE
        layoutContent.visibility = View.GONE
    }

    private fun hideLoading() {
        layoutSkeleton.visibility = View.GONE
        layoutContent.visibility = View.VISIBLE
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
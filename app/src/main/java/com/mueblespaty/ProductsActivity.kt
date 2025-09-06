package com.mueblespaty

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shopify.buy3.GraphClient
import com.shopify.buy3.Storefront
import com.mueblespaty.BuildConfig

class ProductsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var graphClient: GraphClient
    private lateinit var titleTextView: TextView
    private var collectionHandle: String? = null
    private var collectionTitle: String? = null

    companion object {
        const val EXTRA_COLLECTION_HANDLE = "collection_handle"
        const val EXTRA_COLLECTION_TITLE = "collection_title"
        private const val STATE_COLLECTION_HANDLE = "state_collection_handle"
        private const val STATE_COLLECTION_TITLE = "state_collection_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        // Debug savedInstanceState and intent
        Log.d("ProductsActivity", "=== ProductsActivity onCreate ===")
        Log.d("ProductsActivity", "savedInstanceState: $savedInstanceState")
        Log.d("ProductsActivity", "intent: $intent")
        Log.d("ProductsActivity", "intent action: ${intent.action}")
        Log.d("ProductsActivity", "intent flags: ${intent.flags}")
        Log.d("ProductsActivity", "intent extras: ${intent.extras}")
        Log.d("ProductsActivity", "savedState handle: ${savedInstanceState?.getString(STATE_COLLECTION_HANDLE)}")
        Log.d("ProductsActivity", "savedState title: ${savedInstanceState?.getString(STATE_COLLECTION_TITLE)}")
        Log.d("ProductsActivity", "intent handle: ${intent.getStringExtra(EXTRA_COLLECTION_HANDLE)}")
        Log.d("ProductsActivity", "intent title: ${intent.getStringExtra(EXTRA_COLLECTION_TITLE)}")
        Log.d("ProductsActivity", "=== End Debug Info ===")

        // Try to get from saved state first, then from intent, then keep existing values
        val savedHandle = savedInstanceState?.getString(STATE_COLLECTION_HANDLE)
        val intentHandle = intent.getStringExtra(EXTRA_COLLECTION_HANDLE)
        val savedTitle = savedInstanceState?.getString(STATE_COLLECTION_TITLE)  
        val intentTitle = intent.getStringExtra(EXTRA_COLLECTION_TITLE)
        
        // Use saved state if available, otherwise intent, otherwise keep existing (for recreation scenarios)
        collectionHandle = savedHandle ?: intentHandle ?: collectionHandle
        collectionTitle = savedTitle ?: intentTitle ?: collectionTitle

        Log.d("ProductsActivity", "Collection handle: $collectionHandle")
        Log.d("ProductsActivity", "Collection title: '$collectionTitle'")
        Log.d("ProductsActivity", "Restored from: ${if (savedInstanceState != null) "savedState" else "intent"}")

        // Validate that we have the required data BEFORE setting up views
        val handle = collectionHandle
        if (handle.isNullOrBlank()) {
            Log.w("ProductsActivity", "No collection handle available, finishing activity")
            finish()
            return
        }

        setupViews()
        setupShopifyClient()

        // Set both the TextView and ActionBar title
        val displayTitle = collectionTitle ?: "Productos"
        titleTextView.text = displayTitle
        
        Log.d("ProductsActivity", "Display title set to: '$displayTitle'")
        
        // Set ActionBar title and enable back navigation
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = displayTitle
        }
        
        loadProducts(handle)
    }

    private fun setupViews() {
        titleTextView = findViewById(R.id.textViewCollectionTitle)
        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        productAdapter = ProductAdapter(emptyList()) { product ->
            // Handle product click - can expand later for product details
        }
        recyclerView.adapter = productAdapter
    }

    private fun setupShopifyClient() {
        graphClient = GraphClient.build(
            context = this,
            shopDomain = BuildConfig.SHOPIFY_DOMAIN,
            accessToken = BuildConfig.SHOPIFY_STOREFRONT_ACCESS_TOKEN
        )
    }

    private fun loadProducts(collectionHandle: String) {
        // Show skeleton loading
        productAdapter.showLoading()
        
        val query = Storefront.query { rootQueryBuilder ->
            rootQueryBuilder.collectionByHandle(collectionHandle) { collection ->
                collection
                    .title()
                    .description()
                    .products({ args ->
                        args.first(50)
                    }) { productConnection ->
                        productConnection.edges { edge ->
                            edge.node { product ->
                                product
                                    .handle()
                                    .title()
                                    .description()
                                    .priceRange { priceRange ->
                                        priceRange.minVariantPrice { price ->
                                            price.amount().currencyCode()
                                        }
                                    }
                                    .images({ imageArgs ->
                                        imageArgs.first(1)
                                    }) { imageConnection ->
                                        imageConnection.edges { imageEdge ->
                                            imageEdge.node { image ->
                                                image.url()
                                            }
                                        }
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
                    val collection = result.response.data?.collectionByHandle
                    val products = collection?.products?.edges?.map { edge -> edge.node } ?: emptyList()
                    runOnUiThread {
                        productAdapter.updateProducts(products)
                    }
                }
                is com.shopify.buy3.GraphCallResult.Failure -> {
                    println("GraphQL call failed: ${result.error}")
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        collectionHandle?.let { outState.putString(STATE_COLLECTION_HANDLE, it) }
        collectionTitle?.let { outState.putString(STATE_COLLECTION_TITLE, it) }
        Log.d("ProductsActivity", "Saved state: handle='$collectionHandle', title='$collectionTitle'")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
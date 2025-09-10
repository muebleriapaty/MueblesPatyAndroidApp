package com.mueblespaty

import android.content.Intent
import com.mueblespaty.BuildConfig
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mueblespaty.auth.AuthManager
import com.mueblespaty.customer.CustomerApiClient
import com.shopify.buy3.GraphClient
import com.shopify.buy3.Storefront

class MainActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var catalogAdapter: CatalogAdapter
    private lateinit var graphClient: GraphClient
    private lateinit var customerApiClient: CustomerApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        authManager = AuthManager(this)
        customerApiClient = CustomerApiClient(authManager)
        
        // Setup window insets for edge-to-edge compatibility
        val rootView = findViewById<android.widget.LinearLayout>(R.id.main_container)
        setupWindowInsetsForView(rootView)
        
        // Initialize CartManager with customer integration
        CartManager.initialize(authManager, customerApiClient)
        
        setupViews()
        setupShopifyClient()
        loadCatalogs()
        
        // Load customer cart if user is logged in
        if (authManager.isUserLoggedIn()) {
            CartManager.loadCustomerCart()
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        return result
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewCatalogs)
        
        // Initialize adapter first
        catalogAdapter = CatalogAdapter(emptyList()) { collection ->
            // Navigate to products activity
            val intent = Intent(this, ProductsActivity::class.java).apply {
                putExtra(ProductsActivity.EXTRA_COLLECTION_HANDLE, collection.handle)
                putExtra(ProductsActivity.EXTRA_COLLECTION_TITLE, collection.title)
            }
            startActivity(intent)
        }
        
        // Set up GridLayoutManager with span size lookup
        recyclerView.layoutManager = GridLayoutManager(this, 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    // Website link (last item) spans 2 columns, catalogs span 1
                    return if (catalogAdapter.getItemViewType(position) == 1) 2 else 1
                }
            }
        }
        
        recyclerView.adapter = catalogAdapter
    }

    private fun setupShopifyClient() {
        // To get your Storefront API access token:
        // 1. Go to Shopify Admin → Settings → Apps and sales channels
        // 2. Click "Develop apps" → "Create an app" 
        // 3. Configure your app → API integrations → Storefront API
        // 4. Enable Storefront API access and configure permissions
        // 5. Install the app and copy the Storefront access token
        
        graphClient = GraphClient.build(
            context = this,
            shopDomain = BuildConfig.SHOPIFY_DOMAIN,
            accessToken = BuildConfig.SHOPIFY_STOREFRONT_ACCESS_TOKEN
        )
    }

    private fun loadCatalogs() {
        val query = Storefront.query { rootQueryBuilder ->
            rootQueryBuilder.collections({ args ->
                args.first(20)
            }) { collectionConnection ->
                collectionConnection.edges { edge ->
                    edge.node { collection ->
                        collection
                            .handle()
                            .title()
                            .description()
                            .image { image ->
                                image.url()
                            }
                    }
                }
            }
        }

        val call = graphClient.queryGraph(query)
        call.enqueue { result ->
            when (result) {
                is com.shopify.buy3.GraphCallResult.Success -> {
                    val collections = result.response.data?.collections?.edges?.map { edge -> edge.node } ?: emptyList()
                    Log.d("MainActivity", "Loaded ${collections.size} collections:")
                    collections.forEach { collection ->
                        Log.d("MainActivity", "Collection: handle='${collection.handle}', title='${collection.title}'")
                    }
                    runOnUiThread {
                        catalogAdapter.updateCollections(collections)
                    }
                }
                is com.shopify.buy3.GraphCallResult.Failure -> {
                    println("GraphQL call failed: ${result.error}")
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        authManager.dispose()
    }
}
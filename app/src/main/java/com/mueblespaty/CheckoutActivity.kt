package com.mueblespaty

import android.annotation.SuppressLint
import com.mueblespaty.BuildConfig
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.mueblespaty.auth.AuthManager
import com.shopify.buy3.GraphClient
import com.shopify.buy3.Storefront
import com.shopify.checkoutsheetkit.CheckoutException
import com.shopify.checkoutsheetkit.DefaultCheckoutEventProcessor
import com.shopify.checkoutsheetkit.ShopifyCheckoutSheetKit
import com.shopify.checkoutsheetkit.lifecycleevents.CheckoutCompletedEvent
import com.shopify.graphql.support.ID

class CheckoutActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var graphClient: GraphClient
    private lateinit var authManager: AuthManager

    companion object {
        const val EXTRA_CHECKOUT_URL = "checkout_url"
        const val EXTRA_CART_ID = "cart_id"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge for all devices (Android 15 requirement)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        setContentView(R.layout.activity_checkout)
        val activity = this

        // Setup window insets for edge-to-edge compatibility
        val rootView = findViewById<android.widget.RelativeLayout>(R.id.checkout_container)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            
            v.setPadding(
                v.paddingLeft + insets.left + cutoutInsets.left,
                v.paddingTop + insets.top + cutoutInsets.top,
                v.paddingRight + insets.right + cutoutInsets.right,
                v.paddingBottom + insets.bottom + cutoutInsets.bottom
            )
            
            windowInsets
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Checkout"

        webView = findViewById(R.id.webViewCheckout)
        progressBar = findViewById(R.id.progressBarCheckout)
        
        authManager = AuthManager(this)
        setupShopifyClient()
        setupWebView()
        
        // Check if we have a cart ID to fetch checkout URL from
        val cartId = intent.getStringExtra(EXTRA_CART_ID)
        if (cartId != null) {
            fetchCheckoutUrlFromCart(cartId, activity)
        } else {
            val checkoutUrl = intent.getStringExtra(EXTRA_CHECKOUT_URL) 
                ?: CheckoutManager.getCartUrl()
            presentCheckoutSheetKit(checkoutUrl, activity)
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            allowFileAccess = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                
                // Check if we've reached a success/completion page
                url?.let { checkCheckoutCompletion(it) }
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Handle different URL patterns
                return when {
                    url?.contains("/thank_you") == true || 
                    url?.contains("/orders/") == true ||
                    url?.contains("/checkout/thank_you") == true -> {
                        // Order completed successfully
                        handleCheckoutSuccess()
                        true
                    }
                    url?.contains("/cart") == true -> {
                        // Still in cart/checkout flow
                        false
                    }
                    url?.startsWith("https://${BuildConfig.SHOPIFY_DOMAIN}") == true -> {
                        // Stay within our shop domain
                        false
                    }
                    else -> {
                        // External links - could be payment processors, etc.
                        false
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                progressBar.visibility = View.GONE
                Toast.makeText(this@CheckoutActivity, 
                    "Error cargando checkout: $description", 
                    Toast.LENGTH_LONG).show()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                Toast.makeText(this@CheckoutActivity, message, Toast.LENGTH_SHORT).show()
                result?.confirm()
                return true
            }
        }
    }

    private fun loadCheckout(url: String) {
        // Add headers for better compatibility
        val headers = mutableMapOf<String, String>().apply {
            put("User-Agent", "MueblesPaty Android App")
            
            // Add customer access token if available
            val accessToken = authManager.getCurrentAccessToken()
            if (accessToken != null) {
                put("Authorization", "Bearer $accessToken")
                Log.d("CheckoutActivity", "Adding customer access token to checkout request")
            }
        }
        
        webView.loadUrl(url, headers)
    }

    private fun checkCheckoutCompletion(url: String) {
        when {
            url.contains("/thank_you") || 
            url.contains("/orders/") ||
            url.contains("/checkout/thank_you") -> {
                handleCheckoutSuccess()
            }
        }
    }

    private fun handleCheckoutSuccess() {
        // Clear the cart since order was successful
        CartManager.clearCart()
        
        Toast.makeText(this, "¡Pedido completado exitosamente!", Toast.LENGTH_LONG).show()
        
        // Return to main activity
        finish()
    }

    private fun setupShopifyClient() {
        graphClient = GraphClient.build(
            context = this,
            shopDomain = BuildConfig.SHOPIFY_DOMAIN,
            accessToken = BuildConfig.SHOPIFY_STOREFRONT_ACCESS_TOKEN
        )
    }

    private fun fetchCheckoutUrlFromCart(cartId: String, activity: CheckoutActivity) {
        Log.d("CheckoutActivity", "Fetching checkout URL for cart ID: $cartId")
        
        val cartQuery = Storefront.query { query ->
            query.cart(ID(cartId)) {
                it.checkoutUrl()
            }
        }

        graphClient.queryGraph(cartQuery).enqueue {
            if (it is com.shopify.buy3.GraphCallResult.Success) {
                val checkoutUrl = it.response.data?.cart?.checkoutUrl
                if (checkoutUrl != null) {
                    Log.d("CheckoutActivity", "Got checkout URL from cart: $checkoutUrl")
                    runOnUiThread {
                        presentCheckoutSheetKit(checkoutUrl, activity)
                    }
                } else {
                    Log.e("CheckoutActivity", "No checkout URL found in cart response")
                    runOnUiThread {
                        showError("Error: No checkout URL found")
                    }
                }
            } else {
                Log.e("CheckoutActivity", "Failed to fetch cart checkout URL")
                runOnUiThread {
                    showError("Error fetching checkout URL")
                }
            }
        }
    }

    private fun presentCheckoutSheetKit(checkoutUrl: String, context: CheckoutActivity) {
        try {
            Log.d("CheckoutActivity", "Presenting Checkout Sheet Kit with URL: $checkoutUrl")
            
            val checkoutEventProcessor = object : DefaultCheckoutEventProcessor(context) {
                override fun onCheckoutCompleted(checkoutCompletedEvent: CheckoutCompletedEvent) {
                    Log.d("CheckoutActivity", "Checkout completed successfully")
                   handleCheckoutSuccess()
                }

                override fun onCheckoutCanceled() {
                    Log.d("CheckoutActivity", "Checkout was canceled")
                    finish()
                }

                override fun onCheckoutFailed(error: CheckoutException) {
                    Log.e("CheckoutActivity", "Checkout failed", error)
                    showError("Checkout failed: ${error.message}")
                }

                override fun onCheckoutLinkClicked(uri: Uri) {
                    Log.d("CheckoutActivity", "Link clicked in checkout: $uri")
                }

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: WebChromeClient.FileChooserParams,
                ): Boolean {
                    filePathCallback.onReceiveValue(null)
                    return true
                }

                override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                    callback.invoke(origin, false, false)
                }

                override fun onGeolocationPermissionsHidePrompt() {
                    Log.d("CheckoutActivity", "Geolocation permissions prompt hidden")
                }

                override fun onPermissionRequest(permissionRequest: PermissionRequest) {
                    permissionRequest.deny()
                }
            }
            
            ShopifyCheckoutSheetKit.present(
                checkoutUrl = checkoutUrl, 
                context = context,
                checkoutEventProcessor = checkoutEventProcessor
            )
            
        } catch (e: Exception) {
            Log.e("CheckoutActivity", "Error presenting Checkout Sheet Kit", e)
            // Fallback to WebView
            loadCheckout(checkoutUrl)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
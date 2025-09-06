package com.mueblespaty

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mueblespaty.auth.AuthManager
import kotlinx.coroutines.launch

class WebViewAuthActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var authManager: AuthManager
    
    companion object {
        const val EXTRA_AUTH_URL = "auth_url"
        const val EXTRA_REDIRECT_URI = "redirect_uri"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_auth)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sign In"

        authManager = AuthManager(this)
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        
        val authUrl = intent.getStringExtra(EXTRA_AUTH_URL)
        val redirectUri = intent.getStringExtra(EXTRA_REDIRECT_URI)
        
        if (authUrl != null && redirectUri != null) {
            Log.d("WebViewAuth", "Loading auth URL: $authUrl")
            Log.d("WebViewAuth", "Redirect URI: $redirectUri")
            webView.loadUrl(authUrl)
        } else {
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            allowFileAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                
                Log.d("WebViewAuth", "Page started: $url")
                
                // Check if this is a callback URL
                if (url != null && isCallbackUrl(url)) {
                    handleCallback(url)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                
                Log.d("WebViewAuth", "Page finished: $url")
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString()
                Log.d("WebViewAuth", "Should override URL loading: $url")
                
                if (url != null && isCallbackUrl(url)) {
                    handleCallback(url)
                    return true
                }
                
                return false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e("WebViewAuth", "WebView error: ${error?.description}")
            }
        }
    }

    private fun isCallbackUrl(url: String): Boolean {
        val redirectUri = intent.getStringExtra(EXTRA_REDIRECT_URI) ?: return false
        return url.startsWith(redirectUri) || 
               url.contains("/callback") ||
               url.contains("code=") ||
               url.contains("error=")
    }

    private fun handleCallback(url: String) {
        Log.d("WebViewAuth", "Handling callback: $url")
        
        val uri = Uri.parse(url)
        val authorizationCode = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")

        if (error != null) {
            Log.e("WebViewAuth", "OAuth error: $error")
            setResult(RESULT_CANCELED, Intent().apply {
                putExtra("error", error)
            })
            finish()
            return
        }

        if (authorizationCode != null) {
            Log.d("WebViewAuth", "Got authorization code: $authorizationCode")
            
            // Process the authorization code
            lifecycleScope.launch {
                val result = authManager.handleAuthorizationCode(authorizationCode)
                
                if (result.isSuccess) {
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("access_token", result.accessToken)
                        putExtra("refresh_token", result.refreshToken)
                    })
                } else {
                    setResult(RESULT_CANCELED, Intent().apply {
                        putExtra("error", result.error)
                    })
                }
                finish()
            }
        } else {
            Log.e("WebViewAuth", "No authorization code found in callback")
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        setResult(RESULT_CANCELED)
        finish()
        return true
    }
}
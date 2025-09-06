package com.mueblespaty.customer

import android.util.Log
import com.mueblespaty.BuildConfig
import com.mueblespaty.CartItem
import com.mueblespaty.auth.AuthConfig
import com.mueblespaty.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class CustomerApiClient(private val authManager: AuthManager) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val mediaType = "application/json".toMediaType()
    
    data class Customer(
        val id: String?,
        val email: String?,
        val firstName: String?,
        val lastName: String?,
        val phone: String?,
        val acceptsMarketing: Boolean?
    )
    
    data class Order(
        val id: String,
        val orderNumber: String,
        val createdAt: String,
        val totalPrice: String,
        val financialStatus: String,
        val fulfillmentStatus: String?
    )
    
    data class OrderDetail(
        val id: String,
        val name: String?,
        val number: String?,
        val createdAt: String,
        val totalPrice: String,
        val financialStatus: String,
        val billingAddress: String?,
        val shippingAddress: String?,
        val lineItems: List<OrderLineItem>
    )
    
    data class OrderLineItem(
        val id: String,
        val title: String,
        val variantTitle: String?,
        val quantity: Int,
        val unitPrice: String,
        val totalPrice: String,
        val imageUrl: String?
    )
    
    suspend fun getCustomer(): Customer? = withContext(Dispatchers.IO) {
        val accessToken = authManager.getCurrentAccessToken()
        if (accessToken == null) {
            Log.e("CustomerApiClient", "No access token available")
            return@withContext null
        }
        
        Log.d("CustomerApiClient", "Getting customer data with token: ${accessToken.take(10)}...")
        
        val query = """
            query getCustomer {
                customer {
                    id
                    emailAddress {
                        emailAddress
                    }
                    firstName
                    lastName
                    phoneNumber {
                        phoneNumber
                    }
                }
            }
        """.trimIndent()
        
        val requestBody = JSONObject().apply {
            put("query", query)
        }.toString().toRequestBody(mediaType)
        
        val url = "https://shopify.com/36340432941/account/customer/api/2025-07/graphql"
        Log.d("CustomerApiClient", "Making request to: $url")
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", accessToken)
            .addHeader("Content-Type", "application/json")
            .build()
        
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d("CustomerApiClient", "Response code: ${response.code}")
            Log.d("CustomerApiClient", "Response body: $responseBody")
            
            if (response.isSuccessful) {
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    
                    // Check for GraphQL errors
                    val errors = jsonResponse.optJSONArray("errors")
                    if (errors != null && errors.length() > 0) {
                        for (i in 0 until errors.length()) {
                            val error = errors.getJSONObject(i)
                            Log.e("CustomerApiClient", "GraphQL error: ${error.getString("message")}")
                        }
                        return@withContext null
                    }
                    
                    val data = jsonResponse.optJSONObject("data")
                    val customer = data?.optJSONObject("customer")
                    
                    if (customer != null) {
                        Log.d("CustomerApiClient", "Successfully parsed customer data")
                        
                        // Extract nested email field
                        val emailAddress = customer.optJSONObject("emailAddress")
                        val email = emailAddress?.optString("emailAddress")
                        
                        // Extract nested phone field
                        val phoneNumber = customer.optJSONObject("phoneNumber")
                        val phone = phoneNumber?.optString("phoneNumber")
                        
                        // Marketing consent field not available in current API version
                        val acceptsMarketing = false
                        
                        return@withContext Customer(
                            id = customer.optString("id"),
                            email = email,
                            firstName = customer.optString("firstName"),
                            lastName = customer.optString("lastName"),
                            phone = phone,
                            acceptsMarketing = acceptsMarketing
                        )
                    } else {
                        Log.e("CustomerApiClient", "Customer data is null in response")
                    }
                } else {
                    Log.e("CustomerApiClient", "Response body is null")
                }
            } else if (response.code == 401) {
                Log.w("CustomerApiClient", "Token expired, attempting refresh")
                // Token expired, try to refresh
                val refreshResult = authManager.refreshAccessToken()
                if (refreshResult.isSuccess) {
                    Log.d("CustomerApiClient", "Token refreshed, retrying request")
                    // Retry the request with new token
                    return@withContext getCustomer()
                } else {
                    Log.e("CustomerApiClient", "Token refresh failed: ${refreshResult.error}")
                }
            } else {
                Log.e("CustomerApiClient", "HTTP error ${response.code}: ${response.message}")
            }
        } catch (e: IOException) {
            Log.e("CustomerApiClient", "Network error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("CustomerApiClient", "Unexpected error: ${e.message}", e)
        }
        
        return@withContext null
    }
    
    suspend fun getOrders(first: Int = 10): List<Order> = withContext(Dispatchers.IO) {
        val accessToken = authManager.getCurrentAccessToken()
        if (accessToken == null) {
            Log.e("CustomerApiClient", "No access token available for orders")
            return@withContext emptyList()
        }
        
        Log.d("CustomerApiClient", "Getting orders with token: ${accessToken.take(10)}...")
        
        val query = """
            query getOrders(${'$'}first: Int!) {
                customer {
                    orders(first: ${'$'}first) {
                        edges {
                            node {
                                id
                                name
                                number
                                createdAt
                                totalPrice {
                                    amount
                                    currencyCode
                                }
                                financialStatus
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val variables = JSONObject().apply {
            put("first", first)
        }
        
        val requestBody = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://shopify.com/36340432941/account/customer/api/2025-07/graphql")
            .post(requestBody)
            .addHeader("Authorization", accessToken)
            .addHeader("Content-Type", "application/json")
            .build()
        
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d("CustomerApiClient", "Orders response code: ${response.code}")
            Log.d("CustomerApiClient", "Orders response body: $responseBody")
            
            if (response.isSuccessful) {
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    
                    // Check for GraphQL errors
                    val errors = jsonResponse.optJSONArray("errors")
                    if (errors != null && errors.length() > 0) {
                        for (i in 0 until errors.length()) {
                            val error = errors.getJSONObject(i)
                            Log.e("CustomerApiClient", "Orders GraphQL error: ${error.getString("message")}")
                        }
                        return@withContext emptyList()
                    }
                    
                    val data = jsonResponse.optJSONObject("data")
                    val customer = data?.optJSONObject("customer")
                    val orders = customer?.optJSONObject("orders")
                    val edges = orders?.optJSONArray("edges")
                    
                    Log.d("CustomerApiClient", "Orders edges found: ${edges?.length() ?: 0}")
                    
                    val orderList = mutableListOf<Order>()
                    if (edges != null) {
                        for (i in 0 until edges.length()) {
                            val edge = edges.getJSONObject(i)
                            val node = edge.getJSONObject("node")
                            val totalPrice = node.getJSONObject("totalPrice")
                            
                            orderList.add(
                                Order(
                                    id = node.getString("id"),
                                    orderNumber = node.optString("number", node.optString("name", "N/A")),
                                    createdAt = node.getString("createdAt"),
                                    totalPrice = "${totalPrice.getString("amount")} ${totalPrice.getString("currencyCode")}",
                                    financialStatus = node.getString("financialStatus"),
                                    fulfillmentStatus = null // Not available in Customer Account API
                                )
                            )
                        }
                    }
                    Log.d("CustomerApiClient", "Successfully loaded ${orderList.size} orders")
                    return@withContext orderList
                } else {
                    Log.e("CustomerApiClient", "Orders response body is null")
                }
            } else if (response.code == 401) {
                // Token expired, try to refresh
                val refreshResult = authManager.refreshAccessToken()
                if (refreshResult.isSuccess) {
                    // Retry the request with new token
                    return@withContext getOrders(first)
                } else {
                    Log.e("CustomerApiClient", "Orders token refresh failed: ${refreshResult.error}")
                }
            } else {
                Log.e("CustomerApiClient", "Orders HTTP error ${response.code}: ${response.message}")
            }
        } catch (e: IOException) {
            Log.e("CustomerApiClient", "Orders network error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("CustomerApiClient", "Orders unexpected error: ${e.message}", e)
        }
        
        Log.d("CustomerApiClient", "Returning empty orders list")
        return@withContext emptyList()
    }
    
    suspend fun getOrderDetails(orderId: String): OrderDetail? = withContext(Dispatchers.IO) {
        val accessToken = authManager.getCurrentAccessToken()
        if (accessToken == null) {
            Log.e("CustomerApiClient", "No access token available for order details")
            return@withContext null
        }
        
        Log.d("CustomerApiClient", "Getting order details for: $orderId")
        
        val query = """
            query getOrderDetails {
                customer {
                    orders(first: 50) {
                        edges {
                            node {
                                id
                                name
                                number
                                createdAt
                                totalPrice {
                                    amount
                                    currencyCode
                                }
                                financialStatus
                                billingAddress {
                                    formatted
                                }
                                shippingAddress {
                                    formatted
                                }
                                lineItems(first: 20) {
                                    edges {
                                        node {
                                            id
                                            title
                                            variantTitle
                                            quantity
                                            price {
                                                amount
                                                currencyCode
                                            }
                                            totalPrice {
                                                amount
                                                currencyCode
                                            }
                                            image {
                                                url
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val variables = JSONObject()
        
        val requestBody = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://shopify.com/36340432941/account/customer/api/2025-07/graphql")
            .post(requestBody)
            .addHeader("Authorization", accessToken)
            .addHeader("Content-Type", "application/json")
            .build()
        
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d("CustomerApiClient", "Order details response code: ${response.code}")
            Log.d("CustomerApiClient", "Order details response body: $responseBody")
            
            if (response.isSuccessful) {
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    
                    // Check for GraphQL errors
                    val errors = jsonResponse.optJSONArray("errors")
                    if (errors != null && errors.length() > 0) {
                        for (i in 0 until errors.length()) {
                            val error = errors.getJSONObject(i)
                            Log.e("CustomerApiClient", "Order details GraphQL error: ${error.getString("message")}")
                        }
                        return@withContext null
                    }
                    
                    val data = jsonResponse.optJSONObject("data")
                    val customer = data?.optJSONObject("customer")
                    val orders = customer?.optJSONObject("orders")
                    val ordersEdges = orders?.optJSONArray("edges")
                    
                    // Find the specific order by ID
                    var targetOrder: JSONObject? = null
                    if (ordersEdges != null) {
                        for (i in 0 until ordersEdges.length()) {
                            val orderEdge = ordersEdges.getJSONObject(i)
                            val orderNode = orderEdge.getJSONObject("node")
                            if (orderNode.getString("id") == orderId) {
                                targetOrder = orderNode
                                break
                            }
                        }
                    }
                    
                    if (targetOrder != null) {
                        Log.d("CustomerApiClient", "Successfully found order details for: $orderId")
                        
                        val totalPrice = targetOrder.getJSONObject("totalPrice")
                        val billingAddress = targetOrder.optJSONObject("billingAddress")
                        val shippingAddress = targetOrder.optJSONObject("shippingAddress")
                        val lineItemsData = targetOrder.getJSONObject("lineItems")
                        val lineItemsEdges = lineItemsData.getJSONArray("edges")
                        
                        Log.d("CustomerApiClient", "Line items edges count: ${lineItemsEdges.length()}")
                        
                        // Parse line items
                        val lineItems = mutableListOf<OrderLineItem>()
                        for (i in 0 until lineItemsEdges.length()) {
                            val edge = lineItemsEdges.getJSONObject(i)
                            val lineItemNode = edge.getJSONObject("node")
                            val lineItemPrice = lineItemNode.getJSONObject("price")
                            val lineItemTotalPrice = lineItemNode.getJSONObject("totalPrice")
                            val image = lineItemNode.optJSONObject("image")
                            
                            lineItems.add(
                                OrderLineItem(
                                    id = lineItemNode.getString("id"),
                                    title = lineItemNode.getString("title"),
                                    variantTitle = lineItemNode.optString("variantTitle"),
                                    quantity = lineItemNode.getInt("quantity"),
                                    unitPrice = "${lineItemPrice.getString("amount")} ${lineItemPrice.getString("currencyCode")}",
                                    totalPrice = "${lineItemTotalPrice.getString("amount")} ${lineItemTotalPrice.getString("currencyCode")}",
                                    imageUrl = image?.optString("url")
                                )
                            )
                        }
                        
                        Log.d("CustomerApiClient", "Parsed ${lineItems.size} line items")
                        
                        return@withContext OrderDetail(
                            id = targetOrder.getString("id"),
                            name = targetOrder.optString("name"),
                            number = targetOrder.optString("number"),
                            createdAt = targetOrder.getString("createdAt"),
                            totalPrice = "${totalPrice.getString("amount")} ${totalPrice.getString("currencyCode")}",
                            financialStatus = targetOrder.getString("financialStatus"),
                            billingAddress = billingAddress?.optString("formatted"),
                            shippingAddress = shippingAddress?.optString("formatted"),
                            lineItems = lineItems
                        )
                    } else {
                        Log.e("CustomerApiClient", "Order with ID $orderId not found in response")
                    }
                } else {
                    Log.e("CustomerApiClient", "Order details response body is null")
                }
            } else if (response.code == 401) {
                Log.w("CustomerApiClient", "Order details token expired, attempting refresh")
                val refreshResult = authManager.refreshAccessToken()
                if (refreshResult.isSuccess) {
                    Log.d("CustomerApiClient", "Token refreshed, retrying order details request")
                    return@withContext getOrderDetails(orderId)
                } else {
                    Log.e("CustomerApiClient", "Order details token refresh failed: ${refreshResult.error}")
                }
            } else {
                Log.e("CustomerApiClient", "Order details HTTP error ${response.code}: ${response.message}")
            }
        } catch (e: IOException) {
            Log.e("CustomerApiClient", "Order details network error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("CustomerApiClient", "Order details unexpected error: ${e.message}", e)
        }
        
        Log.d("CustomerApiClient", "Returning null for order details")
        return@withContext null
    }
    
    suspend fun updateCustomer(
        firstName: String?,
        lastName: String?,
        phone: String?,
        acceptsMarketing: Boolean?
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = authManager.getCurrentAccessToken() ?: return@withContext false
        
        val mutation = """
            mutation customerUpdate(${"$"}input: CustomerUpdateInput!) {
                customerUpdate(input: ${"$"}input) {
                    customer {
                        id
                        firstName
                        lastName
                        phone
                        acceptsMarketing
                    }
                    userErrors {
                        field
                        message
                    }
                }
            }
        """.trimIndent()
        
        val inputObj = JSONObject().apply {
            firstName?.let { put("firstName", it) }
            lastName?.let { put("lastName", it) }
            phone?.let { put("phone", it) }
            acceptsMarketing?.let { put("acceptsMarketing", it) }
        }
        
        val variables = JSONObject().apply {
            put("input", inputObj)
        }
        
        val requestBody = JSONObject().apply {
            put("query", mutation)
            put("variables", variables)
        }.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://shopify.com/36340432941/account/customer/api/2025-07/graphql")
            .post(requestBody)
            .addHeader("Authorization", accessToken)
            .addHeader("Content-Type", "application/json")
            .build()
        
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val data = jsonResponse.optJSONObject("data")
                    val customerUpdate = data?.optJSONObject("customerUpdate")
                    val userErrors = customerUpdate?.optJSONArray("userErrors")
                    
                    return@withContext userErrors == null || userErrors.length() == 0
                }
            } else if (response.code == 401) {
                // Token expired, try to refresh
                val refreshResult = authManager.refreshAccessToken()
                if (refreshResult.isSuccess) {
                    // Retry the request with new token
                    return@withContext updateCustomer(firstName, lastName, phone, acceptsMarketing)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        
        return@withContext false
    }
    
    // Customer cart management using Storefront API with customer context
    suspend fun createCustomerCart(cartItems: List<CartItem>): String? = withContext(Dispatchers.IO) {
        val accessToken = authManager.getCurrentAccessToken()
        if (accessToken == null) {
            Log.e("CustomerApiClient", "No access token for cart creation")
            return@withContext null
        }
        
        if (cartItems.isEmpty()) {
            Log.w("CustomerApiClient", "No items to add to cart")
            return@withContext null
        }
        
        // Use Storefront API for cart creation with customer context
        val storefrontUrl = "https://${AuthConfig.SHOP_DOMAIN}/api/2024-10/graphql.json"
        
        // Build cart lines
        val lines = JSONArray()
        cartItems.forEach { item ->
            val line = JSONObject().apply {
                put("merchandiseId", item.variantId)
                put("quantity", item.quantity)
            }
            lines.put(line)
        }
        
        val cartInput = JSONObject().apply {
            put("lines", lines)
        }
        
        val variables = JSONObject().apply {
            put("input", cartInput)
        }
        
        val mutation = """
            mutation cartCreate(${'$'}input: CartInput!) {
                cartCreate(input: ${'$'}input) {
                    cart {
                        id
                        checkoutUrl
                        totalQuantity
                        cost {
                            totalAmount {
                                amount
                                currencyCode
                            }
                        }
                        lines(first: 50) {
                            edges {
                                node {
                                    id
                                    quantity
                                    merchandise {
                                        ... on ProductVariant {
                                            id
                                            title
                                            product {
                                                title
                                                handle
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    userErrors {
                        field
                        message
                    }
                }
            }
        """.trimIndent()
        
        val requestBody = JSONObject().apply {
            put("query", mutation)
            put("variables", variables)
        }.toString().toRequestBody(mediaType)
        
        // Use Shopify storefront access token for cart creation
        val storefrontToken = getStorefrontAccessToken()
        
        val request = Request.Builder()
            .url(storefrontUrl)
            .post(requestBody)
            .addHeader("X-Shopify-Storefront-Access-Token", storefrontToken)
            .addHeader("Content-Type", "application/json")
            .build()
        
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d("CustomerApiClient", "Cart creation response code: ${response.code}")
            Log.d("CustomerApiClient", "Cart creation response body: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val data = jsonResponse.optJSONObject("data")
                val cartCreate = data?.optJSONObject("cartCreate")
                val cart = cartCreate?.optJSONObject("cart")
                val cartId = cart?.optString("id")
                
                if (cartId != null) {
                    Log.d("CustomerApiClient", "Cart created successfully: $cartId")
                    
                    // Store cart ID and checkout URL
                    val checkoutUrl = cart.optString("checkoutUrl")
                    storeCartIdForCustomer(cartId)
                    if (checkoutUrl.isNotEmpty()) {
                        storeCheckoutUrlForCustomer(checkoutUrl)
                        Log.d("CustomerApiClient", "Cart checkout URL: $checkoutUrl")
                    }
                    
                    return@withContext cartId
                } else {
                    val userErrors = cartCreate?.optJSONArray("userErrors")
                    if (userErrors != null && userErrors.length() > 0) {
                        for (i in 0 until userErrors.length()) {
                            val error = userErrors.getJSONObject(i)
                            Log.e("CustomerApiClient", "Cart creation error: ${error.getString("message")}")
                        }
                    }
                }
            } else {
                Log.e("CustomerApiClient", "Cart creation HTTP error ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("CustomerApiClient", "Cart creation error: ${e.message}", e)
        }
        
        return@withContext null
    }
    
    suspend fun addItemsToCart(cartId: String, cartItems: List<CartItem>): Boolean = withContext(Dispatchers.IO) {
        if (cartItems.isEmpty()) return@withContext true
        
        val storefrontUrl = "https://${AuthConfig.SHOP_DOMAIN}/api/2024-10/graphql.json"
        
        // Build cart lines
        val lines = JSONArray()
        cartItems.forEach { item ->
            val line = JSONObject().apply {
                put("merchandiseId", item.variantId)
                put("quantity", item.quantity)
            }
            lines.put(line)
        }
        
        val variables = JSONObject().apply {
            put("cartId", cartId)
            put("lines", lines)
        }
        
        val mutation = """
            mutation cartLinesAdd(${'$'}cartId: ID!, ${'$'}lines: [CartLineInput!]!) {
                cartLinesAdd(cartId: ${'$'}cartId, lines: ${'$'}lines) {
                    cart {
                        id
                        totalQuantity
                    }
                    userErrors {
                        field
                        message
                    }
                }
            }
        """.trimIndent()
        
        val requestBody = JSONObject().apply {
            put("query", mutation)
            put("variables", variables)
        }.toString().toRequestBody(mediaType)
        
        val storefrontToken = getStorefrontAccessToken()
        
        val request = Request.Builder()
            .url(storefrontUrl)
            .post(requestBody)
            .addHeader("X-Shopify-Storefront-Access-Token", storefrontToken)
            .addHeader("Content-Type", "application/json")
            .build()
        
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d("CustomerApiClient", "Add to cart response code: ${response.code}")
            Log.d("CustomerApiClient", "Add to cart response: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val data = jsonResponse.optJSONObject("data")
                val cartLinesAdd = data?.optJSONObject("cartLinesAdd")
                val userErrors = cartLinesAdd?.optJSONArray("userErrors")
                
                return@withContext userErrors == null || userErrors.length() == 0
            }
        } catch (e: Exception) {
            Log.e("CustomerApiClient", "Add to cart error: ${e.message}", e)
        }
        
        return@withContext false
    }
    
    private fun getStorefrontAccessToken(): String {
        // Return the hardcoded storefront access token from MainActivity
        // In production, this should be moved to a secure configuration
        return BuildConfig.SHOPIFY_STOREFRONT_ACCESS_TOKEN
    }
    
    private suspend fun storeCartIdForCustomer(cartId: String) {
        // Store cart ID in customer preferences or metadata
        // This is a simplified approach - in production you might want to use
        // customer metafields or a more robust storage method
        Log.d("CustomerApiClient", "Storing cart ID for customer: $cartId")
        // Could implement customer preference storage here
    }
    
    private suspend fun storeCheckoutUrlForCustomer(checkoutUrl: String) {
        // Store checkout URL for easy retrieval
        Log.d("CustomerApiClient", "Storing checkout URL for customer: $checkoutUrl")
        // Could implement customer preference storage here
    }
    
    suspend fun getCustomerCartId(): String? = withContext(Dispatchers.IO) {
        // Retrieve stored cart ID for the customer
        // This would be implemented based on how you store the cart ID
        return@withContext null
    }
    
    suspend fun getCustomerCheckoutUrl(): String? = withContext(Dispatchers.IO) {
        // Retrieve stored checkout URL for the customer
        // This would be implemented based on how you store the checkout URL
        return@withContext null
    }
}
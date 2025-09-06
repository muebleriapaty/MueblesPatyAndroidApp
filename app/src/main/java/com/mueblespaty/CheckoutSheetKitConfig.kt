package com.mueblespaty

import android.app.Application
import android.content.Context
import android.util.Log
import com.shopify.checkoutsheetkit.DefaultCheckoutEventProcessor
import com.shopify.checkoutsheetkit.CheckoutException
import com.shopify.checkoutsheetkit.lifecycleevents.CheckoutCompletedEvent

class CheckoutSheetKitConfig {

    companion object {
        fun configure(application: Application) {
            try {
                // Basic ShopifyCheckoutSheetKit configuration
                Log.d("CheckoutSheetKit", "Checkout Sheet Kit configuration attempted")

            } catch (e: Exception) {
                Log.e("CheckoutSheetKit", "Checkout Sheet Kit configuration not needed or API different", e)
            }
        }

        fun createEventProcessor(context: Context, onResult: (String) -> Unit): DefaultCheckoutEventProcessor {
            return object : DefaultCheckoutEventProcessor(context) {
                override fun onCheckoutCompleted(checkoutCompletedEvent: CheckoutCompletedEvent) {
                    Log.d("CheckoutSheetKit", "Checkout completed successfully")
                    onResult("completed")
                }

                override fun onCheckoutCanceled() {
                    Log.d("CheckoutSheetKit", "Checkout was canceled")
                    onResult("canceled")
                }

                override fun onCheckoutFailed(error: CheckoutException) {
                    Log.e("CheckoutSheetKit", "Checkout failed", error)
                    onResult("failed")
                }
            }
        }
    }
}
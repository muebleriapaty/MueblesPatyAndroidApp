# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build Commands
- **Build debug APK**: `./gradlew assembleDebug`
- **Build release APK**: `./gradlew assembleRelease`
- **Clean build**: `./gradlew clean`
- **Install debug build**: `./gradlew installDebug`

### Testing Commands
- **Run unit tests**: `./gradlew test`
- **Run instrumentation tests**: `./gradlew connectedAndroidTest`

### Lint and Code Quality
- **Run lint**: `./gradlew lint`
- **Check code style**: `./gradlew ktlintCheck`

### Java Environment
- Project requires Java 17 (configured in gradle.properties)
- Use `setup-java17.sh` script if Java 17 setup is needed

## Application Architecture

### Core Components
This is a Shopify-integrated Android e-commerce application with the following key components:

- **MueblesPatyApplication**: Application class that initializes Checkout Sheet Kit
- **MainActivity**: Entry point displaying product catalogs/collections
- **ProductsActivity**: Shows products within a selected collection
- **ProductDetailActivity**: Displays detailed product information and variants
- **CartActivity/CartManager**: Local cart management with singleton pattern
- **CheckoutActivity/CheckoutManager**: Handles checkout flow via Shopify Checkout Sheet Kit

### Data Flow
1. **Product Browsing**: GraphQL queries to Shopify Storefront API for collections and products
2. **Cart Management**: Local state management using CartManager singleton
3. **Checkout**: Redirects to Shopify web checkout via Checkout Sheet Kit

### Key Dependencies
- **Shopify Mobile Buy SDK** (`buy3:17.0.0`): GraphQL API integration
- **Shopify Checkout Sheet Kit** (`checkout-sheet-kit:3.5.0`): Native checkout flow
- **Glide** (`glide:4.16.0`): Image loading and caching
- **AndroidX libraries**: Core Android components

### Storefront API Usage Pattern
For fetching checkout URLs from cart IDs, use this pattern:
```kotlin
val cartQuery = Storefront.query { query ->
    query.cart(ID(id)) {
        it.checkoutUrl()
    }
}

client.queryGraph(cartQuery).enqueue {
    if (it is GraphCallResult.Success) {
        val checkoutUrl = it.response.data?.cart?.checkoutUrl
        // Use checkoutUrl with Checkout Sheet Kit
    }
}
```

### Architecture Patterns
- **Activity-based navigation**: Traditional Android activity stack
- **Singleton pattern**: CartManager for global cart state
- **Observer pattern**: CartUpdateListener for cart state changes
- **Adapter pattern**: RecyclerView adapters for list displays

### File Organization
- Activities: Core UI screens
- Adapters: RecyclerView adapters for data display
- Managers: Business logic (CartManager, CheckoutManager)
- Models: Data classes (CartItem, CheckoutLineItem)


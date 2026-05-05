import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.mueblespaty"
    compileSdk = 36
    
    buildFeatures {
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    defaultConfig {
        applicationId = "com.mueblespaty"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Inject Shopify properties as build config fields
        buildConfigField(
            "String", 
            "SHOPIFY_DOMAIN", 
            "\"${localProperties.getProperty("shopify.domain") ?: throw GradleException("shopify.domain must be defined in local.properties")}\""
        )
        buildConfigField(
            "String", 
            "SHOPIFY_STOREFRONT_ACCESS_TOKEN", 
            "\"${localProperties.getProperty("shopify.storefront.access.token") ?: throw GradleException("shopify.storefront.access.token must be defined in local.properties")}\""
        )
        buildConfigField(
            "String", 
            "SHOPIFY_OAUTH_CLIENT_ID", 
            "\"${localProperties.getProperty("shopify.oauth.client.id") ?: throw GradleException("shopify.oauth.client.id must be defined in local.properties")}\""
        )
        buildConfigField(
            "String", 
            "SHOPIFY_OAUTH_AUTH_ENDPOINT", 
            "\"${localProperties.getProperty("shopify.oauth.auth.endpoint") ?: throw GradleException("shopify.oauth.auth.endpoint must be defined in local.properties")}\""
        )
        buildConfigField(
            "String", 
            "SHOPIFY_OAUTH_TOKEN_ENDPOINT", 
            "\"${localProperties.getProperty("shopify.oauth.token.endpoint") ?: throw GradleException("shopify.oauth.token.endpoint must be defined in local.properties")}\""
        )
        buildConfigField(
            "String", 
            "SHOPIFY_OAUTH_LOGOUT_ENDPOINT", 
            "\"${localProperties.getProperty("shopify.oauth.logout.endpoint") ?: throw GradleException("shopify.oauth.logout.endpoint must be defined in local.properties")}\""
        )
        buildConfigField(
            "String", 
            "SHOPIFY_OAUTH_REDIRECT_SCHEME", 
            "\"${localProperties.getProperty("shopify.oauth.redirect.scheme") ?: throw GradleException("shopify.oauth.redirect.scheme must be defined in local.properties")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Shopify Mobile Buy SDK
    implementation("com.shopify.mobilebuysdk:buy3:17.0.0") {
        exclude(group = "com.shopify.graphql.support", module = "support")
    }
    
    // Shopify Checkout Sheet Kit
    implementation("com.shopify:checkout-sheet-kit:3.5.0")
    
    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // OAuth and authentication  
    implementation("net.openid:appauth:0.11.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
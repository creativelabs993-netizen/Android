plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "site.coinforge.mobile"
    compileSdk = 35
    defaultConfig {
        applicationId = "site.coinforge.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

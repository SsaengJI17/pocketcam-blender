plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.ssaengji17.pocketcam"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.ssaengji17.pocketcam"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0"
    }
}

kotlin {
    jvmToolchain(17)
}

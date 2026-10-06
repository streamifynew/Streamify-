plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// every GitHub build gets a higher number, so the app can tell when a newer build exists
val appVersionCode: Int =
    (project.findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.streamify.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.streamify.app"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = "0.1.$appVersionCode"

        buildConfigField(
            "String",
            "TMDB_API_KEY",
            "\"${project.findProperty("TMDB_API_KEY") ?: ""}\""
        )
    }

    // the same key for every build, so a new APK installs over the old app
    signingConfigs {
        create("streamify") {
            storeFile = file("streamify.keystore")
            storePassword = "streamify123"
            keyAlias = "streamify"
            keyPassword = "streamify123"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("streamify")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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

    implementation("androidx.core:core-ktx:1.15.0")

    implementation("androidx.activity:activity-compose:1.10.1")

    implementation(
        platform(
            "androidx.compose:compose-bom:2025.01.00"
        )
    )

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Material icons used by Streamify UI
    implementation(
    "androidx.compose.material:material-icons-extended"

    )

    // TMDB networking
    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )

    // TMDB poster/backdrop images
    implementation(
        "io.coil-kt:coil-compose:2.7.0"
    )

    // video player
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )
}

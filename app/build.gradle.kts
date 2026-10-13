import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "com.cocrank.dashboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cocrank.dashboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "1.0.15"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val secretsFile = rootProject.file("secrets.properties")
        val secrets = Properties()
        if (secretsFile.exists()) {
            secrets.load(FileInputStream(secretsFile))
        }
        val localPropsFile = rootProject.file("local.properties")
        val localProps = Properties()
        if (localPropsFile.exists()) {
            localProps.load(FileInputStream(localPropsFile))
        }

        val cocApiToken = (secrets.getProperty("COC_API_TOKEN")
            ?: localProps.getProperty("COC_API_TOKEN")
            ?: System.getenv("COC_API_TOKEN") ?: "").replace("\"", "\\\"")

        val cocProxyUrl = (secrets.getProperty("COC_PROXY_URL")
            ?: localProps.getProperty("COC_PROXY_URL")
            ?: System.getenv("COC_PROXY_URL") ?: "https://cocproxy.royaleapi.dev/v1").replace("\"", "\\\"")

        buildConfigField("String", "COC_API_TOKEN", "\"$cocApiToken\"")
        buildConfigField("String", "COC_PROXY_URL", "\"$cocProxyUrl\"")
    }

    val keystorePropsFile = file("keystore.properties")
    val keystoreProps = Properties()
    if (keystorePropsFile.exists()) {
        keystoreProps.load(FileInputStream(keystorePropsFile))
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                val storePath = keystoreProps.getProperty("storeFile") ?: "keystore/release.jks"
                storeFile = file(storePath)
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            firebaseAppDistribution {
                appId = (project.findProperty("FIREBASE_APP_ID") as? String)
                    ?: System.getenv("FIREBASE_APP_ID") ?: ""
                artifactType = "APK"
                releaseNotes = (project.findProperty("FIREBASE_RELEASE_NOTES") as? String)
                    ?: "CoC Rank Mobile Automated Release"
                groups = (project.findProperty("FIREBASE_GROUPS") as? String) ?: "testers"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Network & Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)

    // Coil Image Loading
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

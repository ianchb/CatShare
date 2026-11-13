import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "moe.reimu.catshare"
    compileSdk = 37

    defaultConfig {
        applicationId = "moe.reimu.catshare"
        minSdk = 30
        targetSdk = 37
        versionCode = 10
        versionName = "1.9"
    }

    signingConfigs {
        create("release") {
            file("../signing.properties").let { propFile ->
                if (propFile.canRead()) {
                    val properties = Properties()
                    properties.load(propFile.inputStream())

                    storeFile = file(properties.getProperty("KEYSTORE_FILE"))
                    storePassword = properties.getProperty("KEYSTORE_PASSWORD")
                    keyAlias = properties.getProperty("SIGNING_KEY_ALIAS")
                    keyPassword = properties.getProperty("SIGNING_KEY_PASSWORD")
                } else {
                    println("Unable to read signing.properties")
                }
            }
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Only sign when a keystore is configured; otherwise emit an unsigned APK that can be
            // signed separately (apksigner, platform keys, F-Droid, etc.).
            if (signingConfigs.findByName("release")?.storeFile != null) {
                signingConfig = signingConfigs.findByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "CatShare (Debug)")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/*.properties"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("no.nordicsemi.android.kotlin.ble:client:1.3.1")

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty) {
        exclude(group = "io.netty", module = "netty-codec-native-quic")
    }
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.network.tls.certificates)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

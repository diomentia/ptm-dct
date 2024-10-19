import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "space.diomentia.ptm_dct"
    compileSdk = 34

    defaultConfig {
        applicationId = "space.diomentia.ptm_dct"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "0.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        versionNameSuffix = "-hotfix.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationVariants.all {
                outputs.all {
                    this as BaseVariantOutputImpl
                    outputFileName = "ptm-dct_${versionName}${versionNameSuffix ?: ""}.apk"
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    implementation(files("libs/platform_sdk_v4.1.0326.jar"))
    implementation(files("libs/USDKLibrary-v2.3.0628.aar"))
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.ui.tiles)
    implementation(libs.ui.tiles.extended)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.blegattcoroutines.core)
    implementation(libs.blegattcoroutines.genericaccess)
    implementation(libs.androidx.animation.graphics)
    implementation(files("libs/poishadow-all.jar"))
    implementation(libs.androidx.ui.test.android)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(kotlin("reflect"))
}
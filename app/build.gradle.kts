import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.roborazzi)
}

val keystoreConfigFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystoreConfigFile.exists()) {
    FileInputStream(keystoreConfigFile).use { keystoreProperties.load(it) }
}

android {
    namespace = "com.shopkeeper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shopkeeper"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders["appName"] = "ShopKeeper"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootProject.rootDir}/keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val envStoreFile = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
                ?: keystoreProperties.getProperty("storeFile")?.takeIf { it.isNotBlank() }
            val envStorePassword = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
                ?: keystoreProperties.getProperty("storePassword")?.takeIf { it.isNotBlank() }
            val envKeyAlias = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
                ?: keystoreProperties.getProperty("keyAlias")?.takeIf { it.isNotBlank() }
            val envKeyPassword = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() }
                ?: keystoreProperties.getProperty("keyPassword")?.takeIf { it.isNotBlank() }

            if (envStoreFile != null && envStorePassword != null && envKeyAlias != null && envKeyPassword != null) {
                storeFile = if (File(envStoreFile).isAbsolute) File(envStoreFile) else rootProject.file(envStoreFile)
                storePassword = envStorePassword
                keyAlias = envKeyAlias
                keyPassword = envKeyPassword
            } else {
                // Safe fallback to debug keystore for local & CI test builds without private release keystore
                storeFile = file("${rootProject.rootDir}/keystore/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appName"] = "ShopKeeper Dev"
        }
        create("prod") {
            dimension = "environment"
            manifestPlaceholders["appName"] = "ShopKeeper"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
        getByName("test").assets.srcDirs("$projectDir/schemas")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

roborazzi {
    outputDir.set(file("src/test/snapshots/images"))
}

dependencies {
    // AndroidX & Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM & UI
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room Database (Offline Cache)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Dagger Hilt
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // WorkManager & Hilt Work
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Modern Credential Manager & Google ID Option
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    // Security & Protobuf (CVE-2024-7254 fix)
    implementation(libs.protobuf.javalite)

    // Firebase (Auth, Cloud Firestore & Crashlytics)
    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.crashlytics.ktx)

    // LeakCanary (Debug builds only)
    debugImplementation(libs.leakcanary.android)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // DataStore (Settings & User Session)
    implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Screenshot Testing (Roborazzi & Robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}

tasks.matching { it.name.startsWith("uploadCrashlyticsMappingFile") }.configureEach {
    mustRunAfter(tasks.matching { it.name.contains("GoogleServices") })
}

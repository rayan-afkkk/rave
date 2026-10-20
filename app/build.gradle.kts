import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.secrets)
}

// Firebase is optional: the app runs fully offline (guest mode) without it.
// Drop app/google-services.json in to enable sign-in, friends and competitions.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Populate/sync .env from environment variables provided in AI Studio Secrets panel
val envFile = rootProject.file(".env")
val envExampleFile = rootProject.file(".env.example")
if (envExampleFile.exists()) {
    // First sync: start .env as a copy of .env.example so every key is there, ready to fill in.
    if (!envFile.exists()) envExampleFile.copyTo(envFile)
    val props = Properties()
    var changed = false
    if (envFile.exists()) {
        envFile.inputStream().use { input -> props.load(input) }
    }
    listOf(
        "MAPS_API_KEY",
        "MAPTILER_API_KEY",
        "TOMTOM_API_KEY",
        "GEOAPIFY_API_KEY",
        "MAPBOX_ACCESS_TOKEN",
        "INVITE_DOMAIN",
        "FACEBOOK_APP_ID",
    ).forEach { key ->
        val v = System.getenv(key)
        if (!v.isNullOrEmpty() && props.getProperty(key) != v) {
            props.setProperty(key, v)
            changed = true
        }
    }
    // .env is tracked in Git, so only rewrite it when an environment variable actually changed a value.
    if (changed) envFile.outputStream().use { output -> props.store(output, null) }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

android {
    namespace = "com.rave.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.rave.applet"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Feature flag: reward inviter + invitee with a streak freeze.
        buildConfigField("boolean", "INVITE_REWARD_ENABLED", "true")
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/release.keystore")
            storePassword = "raveapprelease"
            keyAlias = "releasekey"
            keyPassword = "raveapprelease"
        }
        create("releaseConfig") {
            storeFile = file("${rootDir}/release.keystore")
            storePassword = "raveapprelease"
            keyAlias = "releasekey"
            keyPassword = "raveapprelease"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("releaseConfig")
        }
        // Same optimised code as release (R8 + baseline profiles) but signed with the debug key,
        // so it installs straight from Android Studio. Compose debug builds are several times
        // slower than this: pick the "staging" build variant to judge smoothness.
        create("staging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debugConfig")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
        release {
            signingConfig = signingConfigs.getByName("releaseConfig")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric screenshot tests (app/src/test/.../screens) need the merged resources.
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.maxHeapSize = "3g" }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    // Exported schemas are committed so migrations can be tested later.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Installs the Compose/AndroidX baseline profiles on release installs: much less jank on first runs.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation (type-safe routes via kotlinx.serialization)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Local storage
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Async & background
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.work.runtime)

    // Location & maps
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    // Recording API: Google Play services counts steps in the background, even when Rave is closed.
    implementation(libs.play.services.fitness)
    implementation(libs.maps.compose)
    implementation(libs.coil.compose)

    // Widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Firebase (only active when google-services.json is present)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    // Gemini via Firebase AI Logic (food photo scan). Enable AI Logic in the Firebase console.
    implementation(libs.firebase.ai)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.installreferrer)

    // QR: scan with CameraX + ML Kit, generate with ZXing
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation("com.google.guava:guava:33.3.1-android")
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.okhttp)
    implementation(libs.billing.ktx)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Screenshot tests: real screens rendered on the JVM with Robolectric + Hilt.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("com.google.dagger:hilt-android-testing:${libs.versions.hilt.get()}")
    kspTest(libs.hilt.compiler)
    testImplementation("androidx.work:work-testing:${libs.versions.work.get()}")
    testImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
}

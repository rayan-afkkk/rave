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

val appId = "app.rave.fitness"

// Firebase is optional: the app runs fully offline (guest mode) without it.
// Drop app/google-services.json in to enable sign-in, friends and competitions. It must be
// registered for [appId]; a file for another package is ignored rather than breaking the build.
val googleServices = file("google-services.json")
if (googleServices.exists() && googleServices.readText().contains("\"$appId\"")) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
} else if (googleServices.exists()) {
    logger.warn("google-services.json has no client for $appId: Firebase is off in this build.")
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
        applicationId = appId
        minSdk = 26
        targetSdk = 36
        // CI release builds pass these in; local builds use the defaults.
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Feature flag: reward inviter + invitee with a streak freeze.
        buildConfigField("boolean", "INVITE_REWARD_ENABLED", "true")
    }

    // Every build is signed with release.keystore when it's present, so one SHA-1 in Firebase works
    // on every machine. The keystore and its passwords stay out of Git: put RELEASE_STORE_PASSWORD,
    // RELEASE_KEY_ALIAS and RELEASE_KEY_PASSWORD in local.properties (or environment variables).
    // Without them (e.g. CI) the default debug key is used.
    val localProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    // Blank counts as unset: CI passes a missing secret as an empty variable.
    fun signingValue(key: String): String? =
        (System.getenv(key) ?: localProps.getProperty(key))?.takeIf { it.isNotBlank() }
    val releaseKeystore = rootProject.file("release.keystore")
    val storePass = signingValue("RELEASE_STORE_PASSWORD")
    val hasReleaseKey = releaseKeystore.exists() && storePass != null
    if (hasReleaseKey) {
        signingConfigs {
            create("releaseConfig") {
                storeFile = releaseKeystore
                storePassword = storePass
                keyAlias = signingValue("RELEASE_KEY_ALIAS") ?: "releasekey"
                keyPassword = signingValue("RELEASE_KEY_PASSWORD") ?: storePass
            }
        }
    }
    val buildSigning = if (hasReleaseKey) "releaseConfig" else "debug"

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName(buildSigning)
        }
        // Same optimised code as release (R8 + baseline profiles) but signed with the debug key,
        // so it installs straight from Android Studio. Compose debug builds are several times
        // slower than this: pick the "staging" build variant to judge smoothness.
        create("staging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName(buildSigning)
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
        release {
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("releaseConfig")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Ship native debug symbols in the bundle so Play can read native crash reports.
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
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
    implementation(libs.health.connect)
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

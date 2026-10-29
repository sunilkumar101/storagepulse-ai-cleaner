import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Google's public AdMob test ids (https://developers.google.com/admob/android/test-ads).
val TEST_ADMOB_APP = "ca-app-pub-3940256099942544~3347511713"
val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
// Upload-key signing: credentials live in the git-ignored keystore/keystore.properties.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun prop(name: String, fallback: String): String = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: fallback

android {
    namespace = "com.storagepulse.cleaner"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    // 16 KB page-size alignment (required by Google Play for Android 15/16 targets).
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.storagepulse.cleaner"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file("keystore").resolve(keystoreProps.getProperty("storeFile").removePrefix("../keystore/"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Google's official AdMob TEST ids: safe to click, never earn or risk the account.
            manifestPlaceholders["admobAppId"] = TEST_ADMOB_APP
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$TEST_BANNER\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$TEST_INTERSTITIAL\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$TEST_REWARDED\"")
        }
        release {
            // Signed with the upload key when keystore/keystore.properties exists (else unsigned).
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
            // Production ids come from gradle.properties / ~/.gradle/gradle.properties
            // (ADMOB_APP_ID, ADMOB_BANNER_ID, ...). Until they are set, release falls back to the
            // test ids so a build never ships an invalid id.
            manifestPlaceholders["admobAppId"] = prop("ADMOB_APP_ID", TEST_ADMOB_APP)
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${prop("ADMOB_BANNER_ID", TEST_BANNER)}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${prop("ADMOB_INTERSTITIAL_ID", TEST_INTERSTITIAL)}\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${prop("ADMOB_REWARDED_ID", TEST_REWARDED)}\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // Perf testing only: release code (R8, no debuggable) signed with the debug key.
    buildTypes.create("staging") {
        initWith(buildTypes.getByName("release"))
        signingConfig = signingConfigs.getByName("debug")
        matchingFallbacks += "release"
        isDebuggable = false
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
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    // Room schema history, needed for future migrations.
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)

    implementation(libs.androidx.biometric)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.play.services.ads)
    implementation(libs.billing.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.work.testing)
}

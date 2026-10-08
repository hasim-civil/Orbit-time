plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.hasim.orbittime"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hasim.orbittime"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.3"
    }

    signingConfigs {
        // Checked into the repo deliberately: debug keystores use Android's
        // well-known, publicly documented default credentials and are never
        // accepted by Play Store, so sharing one is safe. Doing so keeps the
        // debug SHA-1 identical across every machine and CI runner that
        // builds this project, instead of each one registering its own with
        // Firebase — required for Google Sign-In to work everywhere.
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // Signed with the same shared key every published Orbit Time APK so far has used
            // (v.1.1 and v.1.2 were debug builds signed with it). Android only installs an update
            // over an existing app when both carry the same signature, and Firebase's Google
            // Sign-In is registered against this key's SHA-1 — so a new key here would make the
            // in-app updater fail on every phone and break Google Sign-In. Moving to a private
            // release key is a separate, deliberate step: it needs its SHA-1 added in Firebase
            // and a one-time reinstall for everyone.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
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
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)
    // Plain JVM unit tests for the pure calculation layer (util/): attendance rollups, the
    // overtime balance, date/time formatting and the App Time clock, plus the update checker's
    // release parsing and download logic. No Android or Firebase types are involved in any of
    // them, so they need no instrumentation to run.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
}

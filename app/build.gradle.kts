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
        // Both must change for every release. versionCode is what Android compares when
        // installing over an existing copy (1.1 shipped 1, 1.2 shipped 2); versionName is what
        // the in-app updater compares against the release tag, so a release tagged v1.3 must be
        // built with versionName "1.3" — the release workflow refuses to publish otherwise.
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
            // Published APKs are release builds signed with this same shared key, because every
            // Orbit Time APK already on phones (v.1.1, v.1.2) was signed with it. Android only
            // installs an update over an existing app when the signatures match, and Firebase's
            // Google Sign-In is registered against this key's SHA-1 — a different key here would
            // make the in-app update fail on every phone and break Google Sign-In. Moving to a
            // private key is a separate, deliberate step: register its SHA-1 in Firebase and
            // accept a one-time uninstall/reinstall for everyone.
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

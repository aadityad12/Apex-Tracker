import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.screenshot)
}

// Release signing, for Play Store packaging. Loaded from a local, gitignored properties file
// (see keystore.properties.example and docs/release-signing.md) so the keystore path and
// passwords never touch version control. Absent file means "no release keystore set up yet":
// assembleRelease/bundleRelease keep working exactly as before, just producing an *unsigned*
// artifact — fine for local verification, not uploadable to Play Console until this exists.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasReleaseSigning = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasReleaseSigning) keystorePropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.example.apextracker"
    compileSdk = 37

    defaultConfig {
        // Issue #254: com.example.apextracker was the unmodified Android Studio template
        // default. applicationId is permanent once published to Play Store, so this was changed
        // deliberately before first submission while it still cost nothing. `namespace` above is
        // left untouched on purpose — it's the R/BuildConfig package, which AGP allows to differ
        // from applicationId, and every source file's own `package com.example.apextracker`
        // declaration still matches it. Changing applicationId alone needed no source file moves.
        applicationId = "dev.aadityad.apextracker"
        minSdk = 26

        // Was 35, two levels behind compileSdk (37) -- a real risk of failing Play Console's
        // target-API-level floor at submission or shortly after (Issue #240). Matches compileSdk
        // exactly rather than picking an arbitrary intermediate value.
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Issue #198. This was false, which also made the proguardFiles line below inert:
            // release builds shipped every unused class from Compose, Firebase, Glance and
            // material-icons-extended, with full class and method names intact.
            //
            // Turning it on is not a flag flip — Gson reflects over BackupData and the entities
            // by field name, so app/proguard-rules.pro has to keep them or a minified build
            // writes and reads a different backup format. Read that file before changing this.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
    // Issue #221: auto-generates res/xml's locale_config + the AndroidManifest localeConfig
    // attribute from whichever values-<locale>/ directories exist, so the per-app language
    // picker (Settings > Apps > ApexTracker > Language, API 33+) stays in sync on its own —
    // adding a new values-<locale>/strings.xml is the only step a future locale needs.
    androidResources {
        generateLocaleConfig = true
    }
    // Compose Preview Screenshot Testing renders @Preview composables to PNGs and diffs them —
    // the only safety net a 20-screen visual redesign has. It uses Layoutlib, not Robolectric, so
    // it sidesteps the serialization clash that blocks Room's MigrationTestHelper here.
    //
    // Both this AND the matching line in gradle.properties are required: the plugin reads the
    // properties file at apply time and this one when the android block is evaluated. Dropping
    // either produces a confusing "enable screenshotTest source set first" failure.
    //   record:   ./gradlew updateDebugScreenshotTest
    //   validate: ./gradlew validateDebugScreenshotTest
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

// Room exports schema JSONs here (checked in) so future migrations can be
// written and tested against the historical schema. See AppDatabase.kt.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.play.services.auth)

    // Google Sign-In via Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.sqlcipher.android)

    testImplementation(libs.junit)
    // android.jar's org.json is a stub in JVM unit tests; the real artifact lets the pure
    // Semantic Scholar parsing functions be tested without Robolectric.
    testImplementation("org.json:json:20240303")
    // ApexPaletteSlotsTest reflects over every ColorScheme property (Issue #245) so a slot the
    // app never explicitly authors is caught automatically, rather than needing a hand-maintained
    // list of "slots reached by component defaults" updated every time M3 adds a role.
    testImplementation(kotlin("reflect"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Screenshot tests render previews via Layoutlib, so the tooling artifact is needed on that
    // source set's compile classpath too.
    screenshotTestImplementation(platform(libs.androidx.compose.bom))
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
    // @PreviewTest lives here. The plugin puts it on the runtime classpath but not the compile
    // one, so without this the annotation is an unresolved reference.
    screenshotTestImplementation(libs.screenshot.validation.api)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp")
}

var versionMajor = 1
var versionMinor = 0
var versionBuild = 0

android {
    namespace = "com.canopas.yourspace"
    compileSdk = 34

    if (System.getenv("CI_RUN_NUMBER") != null) {
        versionBuild = System.getenv("CI_RUN_NUMBER").toInt()
    } else {
        versionMajor = 1
        versionMinor = 0
        versionBuild = 0
    }

    defaultConfig {
        applicationId = "com.canopas.yourspace"
        minSdk = 24
        targetSdk = 34
        versionCode = versionMajor * 1000000 + versionMinor * 10000 + versionBuild
        versionName = "$versionMajor.$versionMinor.$versionBuild"
        setProperty("archivesBaseName", "GroupTrack-$versionName-$versionCode")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        if (System.getenv("MAPS_API_KEY") != null) {
            manifestPlaceholders["MAPS_API_KEY"] = System.getenv("MAPS_API_KEY")
        } else {
            val p = Properties()
            p.load(project.rootProject.file("local.properties").reader())
            if (p.hasProperty("MAPS_API_KEY")) {
                manifestPlaceholders["MAPS_API_KEY"] = p.getProperty("MAPS_API_KEY")
            } else {
                manifestPlaceholders["MAPS_API_KEY"] = ""
            }
        }

        if (System.getenv("PLACE_API_KEY") != null) {
            buildConfigField("String", "PLACE_API_KEY", "\"${System.getenv("PLACE_API_KEY")}\"")
        } else {
            val p = Properties()
            p.load(project.rootProject.file("local.properties").reader())
            buildConfigField("String", "PLACE_API_KEY", "\"${p.getProperty("PLACE_API_KEY")}\"")
        }
    }

    signingConfigs {
        if (System.getenv("APKSIGN_KEYSTORE") != null) {
            create("release") {
                keyAlias = System.getenv("APKSIGN_KEY_ALIAS")
                keyPassword = System.getenv("APKSIGN_KEYSTORE_PASS")
                storeFile = file(System.getenv("APKSIGN_KEYSTORE"))
                storePassword = System.getenv("APKSIGN_KEY_PASS")
            }
        } else {
            create("release") {
                keyAlias = "yourspace"
                keyPassword = "yourspace"
                storeFile = file("debug.keystore")
                storePassword = "yourspace"
            }
        }
        getByName("debug") {
            keyAlias = "yourspace"
            keyPassword = "yourspace"
            storeFile = file("debug.keystore")
            storePassword = "yourspace"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    ktlint {
        debug = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.compose.foundation:foundation:1.7.3")

    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")
    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.5.1")
    testImplementation("org.mockito:mockito-core:5.7.0")

    // Hilt
    val hilt = "2.50"
    implementation("com.google.dagger:hilt-android:$hilt")
    ksp("com.google.dagger:hilt-compiler:$hilt")

    // Work manager
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // country-picker
    implementation("com.canopas.jetcountrypicker:jetcountrypicker:1.1.1")

    // coil
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Accompanist permission
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Map
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.maps.android:android-maps-utils:0.4.4")

    // Image cropper
    implementation("com.vanniktech:android-image-cropper:4.5.0")

    // Place
    implementation("com.google.android.libraries.places:places:4.0.0")

    // Room-DB
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    implementation(project(":data"))
}

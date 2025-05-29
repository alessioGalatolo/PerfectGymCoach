plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.plugin)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "agdesigns.elevatefitness"
    compileSdk = 35

    defaultConfig {
        applicationId = "agdesigns.elevatefitness"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }

    kotlinOptions {
        jvmTarget = "19"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }

    applicationVariants.configureEach {
        kotlin.sourceSets.named(name) {
            kotlin.srcDir("build/generated/ksp/$name/kotlin")
        }
    }
}

dependencies {
    implementation(libs.compose.destinations.core)
    implementation(libs.compose.destinations.bottom.sheet)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.wearable.play.services)
    implementation(libs.graphs)
    implementation(libs.palette.ktx)
    implementation(libs.coil.compose)
    implementation(libs.accompanist)

    implementation(libs.gson)
    implementation(libs.datastore.preferences)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.dagger)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.core.ktx)
    implementation(libs.compose.icons)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.test.manifest)
}

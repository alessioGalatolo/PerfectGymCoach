plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.plugin)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "agdesigns.elevatefitness"
    compileSdk = 35

    defaultConfig {
        applicationId = "agdesigns.elevatefitness"
        minSdk = 30
        targetSdk = 35
        versionCode = 5  // cannot match app version
        versionName = "0.0.2b" // match phone app version + eventual revisions
    }

    buildTypes {
        release {
            // Enables code-related app optimization.
            isMinifyEnabled = true

            // Enables resource shrinking.
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.service)
    implementation(libs.datastore.preferences)
    implementation(libs.compose.destinations.wear)
    implementation(libs.accompanist.permissions)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.hilt.android)
    ksp(libs.dagger)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.wearable.play.services)

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.wear.remote.interactions)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.icons)

    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.wear.ongoing)

    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.test.manifest)
}

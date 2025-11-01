import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.plugin)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.proto)
    alias(libs.plugins.kotlin.serialization)
}

room {
    schemaDirectory("$projectDir/schemas")
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
//                TODO: kotlin compiling does not work atm
//                create("kotlin") {
//                    option("lite")
//                }
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

android {
    namespace = "agdesigns.elevatefitness"
    compileSdk = 36

    defaultConfig {
        applicationId = "agdesigns.elevatefitness"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "0.0.4a"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            load(localPropertiesFile.inputStream())
        }
        val huggingFaceDownloadApiKey = localProperties.getProperty("huggingface.api_key")
        buildConfigField("String", "HUGGINGFACE_DOWNLOAD_API_KEY", "\"$huggingFaceDownloadApiKey\"")
    }

    buildTypes {
        release {
            isDebuggable = false

            // FIXME: Optimisation currently disabled as it makes app crash upon entering
            //  ViewExercises: Field queries_ for c.u not found (protobuf stuff)
            //  also there are some glitches around the same screens
            // TODO: add proper proguard rules
            // Enables code-related app optimization.
            isMinifyEnabled = false
            // Enables resource shrinking.
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Override the app name
            resValue("string", "app_name", "EF (Debug)")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_19
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.destinations.core)
    implementation(libs.compose.destinations.bottom.sheet)
    implementation(libs.wear.remote.interactions)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.accompanist.permissions)
    implementation(libs.work.runtime.ktx)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.wearable.play.services)
    implementation(libs.graphs)
    implementation(libs.palette.ktx)
    implementation(libs.coil.compose)

    implementation(libs.gson)
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.proto)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.protobuf.protoc)

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
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    androidTestImplementation(libs.espresso.core)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.test.manifest)

    // GENAI
    implementation(libs.mediapipe.genai)
    implementation(libs.gemini.nano.prompt)
}

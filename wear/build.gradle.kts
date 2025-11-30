import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.plugin)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.proto)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.stnd.get().toString()
    }
    plugins {
        create("javalite") {
            artifact = libs.protobuf.protoc.gen.javalite.get().toString()
        }
        create("grpc") {
            artifact = libs.protobuf.protoc.gen.grpc.java.get().toString()
        }
        create("grpckt") {
            artifact = libs.protobuf.protoc.gen.grpc.kotlin.get().toString()
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
            task.plugins {
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
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
        minSdk = 30
        targetSdk = 36
        versionCode = 7  // cannot match app version
        versionName = "0.0.3" // match phone app version + eventual revisions
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
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Override the app name
            resValue("string", "app_name", "EF (Debug)")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
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
    implementation(libs.accompanist.permissions)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.hilt.android)
    ksp(libs.dagger)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.wearable.play.services)

    implementation(libs.wear.remote.interactions)

    implementation(libs.wear.compose.ui.tooling)
    implementation(libs.compose.icons)

    implementation(libs.wear.compose.material3)
    implementation(libs.horologist.material)
    implementation(libs.horologist.media.ui)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.binder)
    implementation(libs.grpc.android)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.kotlin)
    implementation(libs.horologist.datalayer.watch)
    implementation(libs.horologist.datalayer)
    implementation(libs.horologist.datalayer.grpc)
    implementation(libs.kotlin.coroutines.play.services)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.datastore.proto)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.wear.ongoing)

    implementation(libs.android.shapes)

    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.test.manifest)
}

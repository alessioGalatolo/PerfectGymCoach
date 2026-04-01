import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.plugin)
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
    namespace = "agdesigns.elevatefitness.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

dependencies {
    api(libs.grpc.stub)
    api(libs.grpc.binder)
    api(libs.grpc.android)
    api(libs.grpc.protobuf.lite)
    api(libs.grpc.kotlin)
    api(libs.protobuf.kotlin.lite)
    api(libs.datastore.proto)

    implementation(libs.compose.icons)
    implementation(libs.wearable.play.services)
    implementation(libs.horologist.datalayer)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    implementation(libs.kotlin.coroutines.play.services)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
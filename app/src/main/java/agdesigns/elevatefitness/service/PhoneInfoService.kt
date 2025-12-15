package agdesigns.elevatefitness.service

import agdesigns.elevatefitness.BuildConfig
import agdesigns.elevatefitness.shared.grpc.Info
import agdesigns.elevatefitness.data.PhoneWorkoutRepository
import agdesigns.elevatefitness.shared.grpc.PhoneInfoServiceGrpcKt
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.server.BaseGrpcDataService
import com.google.protobuf.Empty
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@OptIn(ExperimentalHorologistApi::class)
class PhoneInfoService: BaseGrpcDataService<PhoneInfoServiceGrpcKt.PhoneInfoServiceCoroutineImplBase>() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PhoneInfoServiceEntryPoint {
        fun registry(): WearDataLayerRegistry
    }

    private val entryPoint: PhoneInfoServiceEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            PhoneInfoServiceEntryPoint::class.java
        )
    }

    override val registry: WearDataLayerRegistry by lazy {
        entryPoint.registry()
    }

    override fun buildService(): PhoneInfoServiceGrpcKt.PhoneInfoServiceCoroutineImplBase {

        return object : PhoneInfoServiceGrpcKt.PhoneInfoServiceCoroutineImplBase() {

            override suspend fun versionInfo(request: Empty): Info.VersionInfo {
                val name = BuildConfig.VERSION_NAME
                // name is <major>.<minor>.<patch><hotfix>[-debug]
                val nameNoSuffix = name.split("-")[0]
                val singleDigits = nameNoSuffix.split(".")
                val major = singleDigits[0].toIntOrNull() ?: 0
                val minor = singleDigits[1].toIntOrNull() ?: 0
                // hotfix is a letter after patch (if any)
                val hotfix = singleDigits[2].filterNot { it.isDigit() }
                val patch = singleDigits[2].filter { it.isDigit() }.toIntOrNull() ?: 0
                return Info.VersionInfo.newBuilder()
                    .setVersionCode(BuildConfig.VERSION_CODE)
                    .setVersionName(
                        Info.VersionName.newBuilder()
                            .setMajor(major)
                            .setMinor(minor)
                            .setPatch(patch)
                            .setHotfix(hotfix)
                            .build()
                    )
                    .build()
            }
        }
    }
}
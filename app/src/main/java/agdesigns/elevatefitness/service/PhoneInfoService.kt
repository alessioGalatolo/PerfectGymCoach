package agdesigns.elevatefitness.service

import agdesigns.elevatefitness.BuildConfig
import agdesigns.elevatefitness.shared.grpc.Info
import agdesigns.elevatefitness.shared.grpc.PhoneInfoServiceGrpcKt
import android.util.Log
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.server.BaseGrpcDataService
import com.google.protobuf.Empty
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class PhoneInfoService: BaseGrpcDataService<PhoneInfoServiceGrpcKt.PhoneInfoServiceCoroutineImplBase>() {

    @Inject
    override lateinit var registry: WearDataLayerRegistry

    override fun buildService(): PhoneInfoServiceGrpcKt.PhoneInfoServiceCoroutineImplBase {

        return object : PhoneInfoServiceGrpcKt.PhoneInfoServiceCoroutineImplBase() {

            override suspend fun versionInfo(request: Empty): Info.VersionInfo {
                val name = BuildConfig.VERSION_NAME
                // name is <major>.<minor>.<patch><hotfix>[-debug]
                val nameNoSuffix = name.split("-")[0]
                val singleDigits = nameNoSuffix.split(".")
                val major = singleDigits.getOrNull(0)?.toIntOrNull() ?: 0
                val minor = singleDigits.getOrNull(1)?.toIntOrNull() ?: 0
                val patchString = singleDigits.getOrNull(2) ?: ""
                // hotfix is a letter after patch (if any)
                val hotfix = patchString.filterNot { it.isDigit() }
                val patch = patchString.filter { it.isDigit() }.toIntOrNull() ?: 0
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
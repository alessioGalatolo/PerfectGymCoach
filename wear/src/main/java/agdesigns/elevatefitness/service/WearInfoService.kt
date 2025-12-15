package agdesigns.elevatefitness.service

import agdesigns.elevatefitness.BuildConfig
import agdesigns.elevatefitness.shared.grpc.Info
import agdesigns.elevatefitness.shared.grpc.WearInfoServiceGrpcKt
import android.content.Intent
import android.util.Log
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.server.BaseGrpcDataService
import com.google.protobuf.Empty
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class WearInfoService: BaseGrpcDataService<WearInfoServiceGrpcKt.WearInfoServiceCoroutineImplBase>() {

    @Inject
    override lateinit var registry: WearDataLayerRegistry

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY // or START_NOT_STICKY depending on your needs
    }

    override fun buildService(): WearInfoServiceGrpcKt.WearInfoServiceCoroutineImplBase {

        return object : WearInfoServiceGrpcKt.WearInfoServiceCoroutineImplBase() {

            override suspend fun versionInfo(request: Empty): Info.VersionInfo {
                Log.d("WearInfoService", "versionInfo called")
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